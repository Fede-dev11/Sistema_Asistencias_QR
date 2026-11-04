import { cleanup, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { RoleNav } from './RoleNav';

let session: { roles: string[] } | null = null;
let ssoLogoutUrl: string | null = null;
vi.mock('next/navigation', () => ({ usePathname: () => '/sesiones' }));
vi.mock('./SessionProvider', () => ({ useSession: () => ({ session, profile: { nombres: 'Ana Pérez' }, logout: vi.fn().mockResolvedValue(undefined), ssoLogoutUrl }) }));
afterEach(() => { cleanup(); session = null; ssoLogoutUrl = null; vi.unstubAllEnvs(); });
const links = () => screen.getAllByRole('link').map(a => a.textContent);

describe('RoleNav', () => {
  it('sin sesión solo ofrece ingresar y el cierre SSO pendiente', () => {
    ssoLogoutUrl = 'https://sso.example.test/logout';
    render(<RoleNav />);
    expect(links()).toEqual(['Ingresar', 'Cerrar también la sesión universitaria']);
  });
  it('docente: enlaces de responsable, página actual marcada, sin simulador por defecto', () => {
    session = { roles: ['DOCENTE'] };
    render(<RoleNav />);
    expect(screen.getByRole('navigation', { name: 'Principal' })).toBeTruthy();
    expect(links()).toEqual(['Mis actividades', 'Sesiones', 'Estadísticas']);
    expect(screen.getByRole('link', { name: 'Sesiones' })).toHaveAttribute('aria-current', 'page');
    expect(screen.getByText(/Ana Pérez/)).toHaveTextContent('DOCENTE');
  });
  it('director y administrador ven los suyos', () => {
    session = { roles: ['DIRECTOR_CARRERA'] };
    const { unmount } = render(<RoleNav />);
    expect(links()).toEqual(['Estadísticas', 'Reportes']); unmount();
    session = { roles: ['ADMINISTRADOR'] };
    render(<RoleNav />);
    expect(links()).toEqual(['Personas', 'Carreras y paralelos', 'Espacios', 'Importar']);
  });
  it('el simulador aparece solo con la bandera', () => {
    vi.stubEnv('NEXT_PUBLIC_HERRAMIENTAS_PRUEBA', 'true');
    session = { roles: ['DOCENTE'] };
    render(<RoleNav />);
    expect(links()).toContain('Simulador');
  });
});
