import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { request } from '../../lib/api/client';
import { ImportPanel } from './ImportPanel';

vi.mock('../../lib/api/client', () => ({ ApiError: class extends Error {}, request: vi.fn() }));
let roles: string[] = ['ADMINISTRADOR'];
vi.mock('../auth/SessionProvider', () => ({ useSession: () => ({ session: { roles } }) }));
afterEach(() => { cleanup(); vi.clearAllMocks(); roles = ['ADMINISTRADOR']; });

describe('importación CSV', () => {
  it('solo se muestra a administradores', () => {
    roles = ['DOCENTE'];
    render(<ImportPanel />);
    expect(screen.getByRole('alert')).toHaveTextContent('solo para administradores');
  });

  it('muestra el informe con las filas rechazadas', async () => {
    vi.mocked(request).mockResolvedValue({ procesadas: 2, creadas: 1, actualizadas: 0, sin_cambios: 0, rechazadas: [{ fila: 3, motivo: 'Cédula inválida.' }] });
    const { container } = render(<ImportPanel />);
    const input = container.querySelector('input[type=file]') as HTMLInputElement;
    fireEvent.change(input, { target: { files: [new File(['cedula,nombres,correo\n'], 'a.csv', { type: 'text/csv' })] } });
    fireEvent.click(screen.getByText('Importar', { selector: 'button' }));
    await waitFor(() => expect(screen.getByRole('status')).toHaveTextContent('1 rechazadas'));
    expect(screen.getByText('Cédula inválida.')).toBeTruthy();
  });
});
