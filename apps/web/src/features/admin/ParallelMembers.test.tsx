import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { request } from '../../lib/api/client';
import { ParallelMembers } from './ParallelMembers';

vi.mock('../../lib/api/client', () => ({ ApiError: class extends Error {}, request: vi.fn() }));
afterEach(() => { cleanup(); vi.clearAllMocks(); });

function list(path: string) {
  return path.includes('/enrollments')
    ? { items: [{ persona_id: 'e1', cedula: '1100000001', nombres: 'Ana', vigente: true }], total: 1 }
    : { items: [{ cuenta_id: 't1', correo: 'doc@unl.edu.ec', nombres: 'Doc', vigente: false }], total: 1 };
}

describe('miembros del paralelo', () => {
  it('lista matriculados y docentes', async () => {
    vi.mocked(request).mockImplementation(async (p: string) => list(p));
    render(<ParallelMembers parallelId="p1" />);
    expect(await screen.findByText(/1100000001/)).toBeTruthy();
    expect(screen.getByText(/doc@unl.edu.ec/)).toHaveTextContent('(no vigente)');
    expect(request).toHaveBeenCalledWith('/admin/parallels/p1/teachers?page=0&size=20');
  });

  it('reemplaza con una cédula por línea y muestra rechazadas', async () => {
    vi.mocked(request).mockImplementation(async (p: string, o?: RequestInit) => o?.method === 'PUT'
      ? { matriculados: 1, rechazadas: [{ entrada: '999', motivo: 'No es estudiante.' }] } : list(p));
    const onChanged = vi.fn();
    render(<ParallelMembers parallelId="p1" onChanged={onChanged} />);
    await screen.findByText(/1100000001/);
    const section = screen.getByRole('region', { name: 'Matriculados' });
    fireEvent.change(within(section).getByLabelText(/Cédulas/), { target: { value: '1100000001\n 999 \n\n' } });
    fireEvent.click(within(section).getByText('Reemplazar matriculados'));
    expect(await screen.findByText('No es estudiante.')).toBeTruthy();
    expect(request).toHaveBeenCalledWith('/admin/parallels/p1/enrollments', { method: 'PUT', body: JSON.stringify({ cedulas: ['1100000001', '999'] }) });
    expect(within(section).getByRole('status')).toHaveTextContent('1 vigentes, 1 rechazadas');
    expect(onChanged).toHaveBeenCalled();
  });

  it('muestra el error del reemplazo de docentes', async () => {
    vi.mocked(request).mockImplementation(async (p: string, o?: RequestInit) => {
      if (o?.method === 'PUT') throw new Error('Paralelo inexistente');
      return list(p);
    });
    render(<ParallelMembers parallelId="p1" />);
    await screen.findByText(/doc@unl/);
    const section = screen.getByRole('region', { name: 'Docentes' });
    fireEvent.change(within(section).getByLabelText(/Correos/), { target: { value: 'a@unl.edu.ec' } });
    fireEvent.click(within(section).getByText('Reemplazar docentes'));
    await waitFor(() => expect(within(section).getByRole('alert')).toHaveTextContent('Paralelo inexistente'));
  });
});
