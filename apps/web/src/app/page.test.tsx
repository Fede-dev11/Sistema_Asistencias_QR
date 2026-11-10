import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen, waitFor } from '@testing-library/react';
import Home from './page';

const auth = vi.hoisted(() => ({ session: null as { roles: string[] } | null }));
const replace = vi.hoisted(() => vi.fn());
vi.mock('../features/auth/SessionProvider', () => ({ useSession: () => auth }));
vi.mock('next/navigation', () => ({ useRouter: () => ({ replace }) }));

afterEach(() => {
  cleanup();
  auth.session = null;
  replace.mockClear();
});

describe('entrada por rol desde la portada', () => {
  it('ofrece ingresar sin una sesión', () => {
    render(<Home />);
    expect(screen.getByRole('link', { name: 'Ingresar' })).toHaveAttribute('href', '/login');
    expect(replace).not.toHaveBeenCalled();
  });

  it.each([
    ['DOCENTE', '/hoy'],
    ['ENCARGADO', '/hoy'],
    ['DIRECTOR_INSTITUCIONAL', '/inicio'],
    ['DIRECTOR_CARRERA', '/inicio'],
    ['ADMINISTRADOR', '/inicio'],
  ])('redirige %s a %s sin quedarse en Entrando', async (role, path) => {
    auth.session = { roles: [role] };
    render(<Home />);
    await waitFor(() => expect(replace).toHaveBeenCalledWith(path));
  });
});
