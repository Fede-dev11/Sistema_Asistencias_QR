import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { request } from '../../lib/api/client';
import { DirectorSummary } from './DirectorSummary';

vi.mock('../../lib/api/client', () => ({ ApiError: class extends Error {}, request: vi.fn() }));
afterEach(() => { cleanup(); vi.clearAllMocks(); });

const base = { sesiones_computables: 10, asistencias: 5, ausencias: 5, ausencias_justificadas: 0 };
const group = (id: string, porcentaje: number | null, paralelo = 'A') => ({
  ...base,
  paralelo_id: id,
  carrera_id: 'c1',
  carrera: 'CIS',
  paralelo,
  porcentaje,
});

describe('resumen de directores', () => {
  it('cuenta los paralelos bajo 70 % y muestra hasta cinco de menor porcentaje', async () => {
    vi.mocked(request).mockResolvedValue({ items: [
      group('p1', 10, 'A'), group('p2', 20, 'B'), group('p3', 30, 'C'),
      group('p4', 40, 'D'), group('p5', 50, 'E'), group('p6', 60, 'F'),
      group('p7', 80, 'G'), group('p8', null, 'H'),
    ] });
    render(<DirectorSummary />);
    expect(await screen.findByText(/6 de 8 paralelos están por debajo de 70 %/)).toBeTruthy();
    const items = screen.getAllByRole('listitem');
    expect(items).toHaveLength(5);
    expect(items[0]).toHaveTextContent('CIS · A');
    expect(items[0]).toHaveTextContent('10 %');
    expect(items[4]).toHaveTextContent('CIS · E');
    expect(screen.getByRole('link', { name: 'Ver estadísticas completas' })).toHaveAttribute('href', '/estadisticas');
  });

  it('informa cuando todos cumplen el umbral', async () => {
    vi.mocked(request).mockResolvedValue({ items: [group('p1', 70), group('p2', 95, 'B')] });
    render(<DirectorSummary />);
    expect(await screen.findByText(/Todos los paralelos \(2\) están en 70 % o más/)).toBeTruthy();
    expect(screen.queryByText(/paralelos están por debajo/)).toBeNull();
  });

  it('muestra el estado vacío cuando no hay sesiones cerradas', async () => {
    vi.mocked(request).mockResolvedValue({ items: [] });
    render(<DirectorSummary />);
    expect(await screen.findByText('Aún no hay datos de asistencia')).toBeTruthy();
    expect(screen.getByRole('link', { name: 'Ver estadísticas completas' })).toBeTruthy();
  });

  it('muestra el error y permite reintentar', async () => {
    vi.mocked(request)
      .mockRejectedValueOnce(new Error('Sin conexión'))
      .mockResolvedValueOnce({ items: [group('p1', 50)] });
    render(<DirectorSummary />);
    expect(await screen.findByRole('alert')).toHaveTextContent('Sin conexión');
    fireEvent.click(screen.getByRole('button', { name: 'Reintentar' }));
    await waitFor(() => expect(screen.getByText(/1 de 1 paralelos están por debajo de 70 %/)).toBeTruthy());
  });
});
