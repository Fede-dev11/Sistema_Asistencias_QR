import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { RoleNav } from './RoleNav';

let session: { roles: string[] } | null = null;
let ssoLogoutUrl: string | null = null;
vi.mock('next/navigation', () => ({ usePathname: () => '/actividades/12' }));
vi.mock('./SessionProvider', () => ({ useSession: () => ({ session, profile: { nombres: 'Ana Pérez' }, logout: vi.fn().mockResolvedValue(undefined), ssoLogoutUrl }) }));
afterEach(() => { cleanup(); session = null; ssoLogoutUrl = null; vi.unstubAllEnvs(); });
const links = () => screen.getAllByRole('link').map(a => a.textContent);

describe('RoleNav', () => {
  it('sin sesión solo ofrece ingresar y el cierre SSO pendiente', () => {
    ssoLogoutUrl = 'https://sso.example.test/logout';
    render(<RoleNav />);
    expect(links()).toEqual(['Ingresar', 'Cerrar también la sesión universitaria']);
  });
  it('docente y encargado: Hoy · Actividades · Estadísticas · Reportes, página actual marcada', () => {
    session = { roles: ['DOCENTE'] };
    render(<RoleNav />);
    expect(screen.getByRole('navigation', { name: 'Principal' })).toBeTruthy();
    expect(links()).toEqual(['Hoy', 'Actividades', 'Estadísticas', 'Reportes']);
    expect(screen.getByRole('link', { name: 'Actividades' })).toHaveAttribute('aria-current', 'page');
    expect(screen.getByRole('link', { name: 'Hoy' })).not.toHaveAttribute('aria-current');
    expect(screen.getByText(/Ana Pérez/)).toHaveTextContent('DOCENTE');
  });
  it('director y administrador ven los suyos', () => {
    session = { roles: ['DIRECTOR_CARRERA'] };
    const { unmount } = render(<RoleNav />);
    expect(links()).toEqual(['Resumen', 'Estadísticas', 'Reportes']); unmount();
    session = { roles: ['ADMINISTRADOR'] };
    render(<RoleNav />);
    expect(links()).toEqual(['Inicio', 'Personas', 'Carreras y paralelos', 'Espacios', 'Importar']);
  });
  it('la sección Pruebas aparece separada y solo con la bandera', () => {
    session = { roles: ['DOCENTE'] };
    const { unmount } = render(<RoleNav />);
    expect(screen.queryByRole('group', { name: 'Pruebas' })).toBeNull(); unmount();
    vi.stubEnv('NEXT_PUBLIC_HERRAMIENTAS_PRUEBA', 'true');
    render(<RoleNav />);
    expect(links()).toEqual(['Hoy', 'Actividades', 'Estadísticas', 'Reportes', 'Invitado']);
    expect(screen.getByRole('group', { name: 'Pruebas' })).toContainElement(screen.getByRole('link', { name: 'Invitado' }));
  });
  it('el menú compacto se abre y cierra con el botón (teclado: es un button)', () => {
    session = { roles: ['DOCENTE'] };
    render(<RoleNav />);
    const toggle = screen.getByRole('button', { name: 'Menú' });
    expect(toggle).toHaveAttribute('aria-expanded', 'false');
    fireEvent.click(toggle);
    expect(toggle).toHaveAttribute('aria-expanded', 'true');
  });
  it('Escape cierra el menú compacto y devuelve el foco al botón', () => {
    session = { roles: ['DOCENTE'] };
    render(<RoleNav />);
    const toggle = screen.getByRole('button', { name: 'Menú' });
    fireEvent.click(toggle);
    fireEvent.keyDown(screen.getByRole('navigation', { name: 'Principal' }), { key: 'Escape' });
    expect(toggle).toHaveAttribute('aria-expanded', 'false');
    expect(toggle).toHaveFocus();
  });
});
