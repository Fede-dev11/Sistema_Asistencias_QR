import { cleanup, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { RoleGuard } from './RoleGuard';

let session: { roles: string[] } | null = null;
vi.mock('../auth/SessionProvider', () => ({ useSession: () => ({ session }) }));
afterEach(() => { cleanup(); session = null; });

describe('RoleGuard', () => {
  it('pide ingresar sin sesión y ofrece el acceso', () => {
    render(<RoleGuard roles={['DOCENTE']}>x</RoleGuard>);
    expect(screen.getByRole('alert')).toHaveTextContent('Ingresa');
    expect(screen.getByRole('link', { name: 'Ingresar' })).toHaveAttribute('href', '/login');
  });
  it('niega el acceso sin el rol y permite volver al inicio', () => {
    session = { roles: ['ESTUDIANTE'] };
    render(<RoleGuard roles={['DOCENTE']}>x</RoleGuard>);
    expect(screen.getByRole('alert')).toHaveTextContent('No tienes acceso a esta sección');
    expect(screen.getByRole('link', { name: 'Volver al inicio' })).toHaveAttribute('href', '/');
  });
  it('muestra los hijos con alguno de los roles', () => { session = { roles: ['ENCARGADO'] }; render(<RoleGuard roles={['DOCENTE', 'ENCARGADO']}>contenido</RoleGuard>); expect(screen.getByText('contenido')).toBeTruthy(); });
});
