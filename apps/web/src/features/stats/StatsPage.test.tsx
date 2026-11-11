import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { request } from '../../lib/api/client';
import StatsPage from '../../app/estadisticas/page';

vi.mock('../../lib/api/client', () => ({ ApiError: class extends Error {}, request: vi.fn() }));
let roles: string[] = ['DOCENTE'];
vi.mock('../auth/SessionProvider', () => ({
  useSession: () => ({ session: { roles }, profile: { paralelos: [{ paralelo_id: 'p1', codigo: 'A', carrera_id: 'c1', carrera_codigo: 'CIS', carrera_nombre: 'Sistemas' }] } }),
}));
afterEach(() => { cleanup(); vi.clearAllMocks(); roles = ['DOCENTE']; });

const base = { sesiones_computables: 4, asistencias: 1, ausencias: 3, ausencias_justificadas: 0 };

const classPage = (items: unknown[], total = items.length) => ({
  umbral_asistencia: 70,
  resumen: { ...base, porcentaje: 50 },
  page: 0,
  size: 20,
  total,
  items,
});

describe('estadísticas', () => {
  it('niega el acceso a estudiantes', () => {
    roles = ['ESTUDIANTE'];
    render(<StatsPage />);
    expect(screen.getByRole('alert')).toHaveTextContent('No tienes acceso');
  });

  it('resalta con texto a quien está bajo umbral y muestra "Sin datos"', async () => {
    vi.mocked(request).mockImplementation(async (path: string) => path.startsWith('/activities?')
      ? { items: [{ id: 'a1', nombre: 'Física I' }] }
      : classPage([
        { ...base, persona_id: 'x', cedula: '111', nombres: 'Ana', porcentaje: 25, bajo_umbral: true },
        { ...base, persona_id: 'y', cedula: '222', nombres: 'Luis', sesiones_computables: 0, porcentaje: null, bajo_umbral: false },
      ]));
    render(<StatsPage />);
    await screen.findByText('Física I');
    fireEvent.change(screen.getByLabelText('Clase'), { target: { value: 'a1' } });
    const anaRow = (await screen.findByText('Ana')).closest('tr') as HTMLElement;
    expect(within(anaRow).getByText('Bajo umbral')).toBeTruthy();
    expect(within(anaRow).getByText('25%')).toBeTruthy();
    const luisRow = screen.getByText('Luis').closest('tr') as HTMLElement;
    expect(luisRow).toHaveTextContent('Sin datos');
    expect(screen.getAllByText(/Bajo umbral/)).toHaveLength(1);
    expect(request).toHaveBeenCalledWith('/activities/a1/stats?page=0&size=20');
  });

  it('distingue un porcentaje de 0 % de la ausencia de datos', async () => {
    vi.mocked(request).mockImplementation(async (path: string) => path.startsWith('/activities?')
      ? { items: [{ id: 'a1', nombre: 'Física I' }] }
      : classPage([
        { ...base, persona_id: 'z', cedula: '333', nombres: 'Cero', porcentaje: 0, bajo_umbral: true },
      ]));
    render(<StatsPage />);
    await screen.findByText('Física I');
    fireEvent.change(screen.getByLabelText('Clase'), { target: { value: 'a1' } });
    const row = (await screen.findByText('Cero')).closest('tr') as HTMLElement;
    expect(within(row).getByText('0%')).toBeTruthy();
    expect(row).not.toHaveTextContent('Sin datos');
  });

  it('pagina el listado de estudiantes', async () => {
    vi.mocked(request).mockImplementation(async (path: string) => path.startsWith('/activities?')
      ? { items: [{ id: 'a1', nombre: 'Física I' }] }
      : classPage([{ ...base, persona_id: 'x', cedula: '111', nombres: 'Ana', porcentaje: 25, bajo_umbral: true }], 45));
    render(<StatsPage />);
    await screen.findByText('Física I');
    fireEvent.change(screen.getByLabelText('Clase'), { target: { value: 'a1' } });
    await screen.findByText('Ana');
    fireEvent.click(screen.getByRole('button', { name: 'Siguiente' }));
    await waitFor(() => expect(request).toHaveBeenCalledWith('/activities/a1/stats?page=1&size=20'));
  });

  it('muestra el error de la clase', async () => {
    vi.mocked(request).mockImplementation(async (path: string) => {
      if (path.startsWith('/activities?')) return { items: [{ id: 'a1', nombre: 'Física I' }] };
      throw new Error('Sin permiso');
    });
    render(<StatsPage />);
    await screen.findByText('Física I');
    fireEvent.change(screen.getByLabelText('Clase'), { target: { value: 'a1' } });
    expect(await screen.findByRole('alert')).toHaveTextContent('Sin permiso');
  });

  it('consulta paralelos con carrera y periodo, y carreras para directores', async () => {
    roles = ['DIRECTOR_CARRERA'];
    vi.mocked(request).mockResolvedValue({ items: [{ ...base, porcentaje: null, carrera: 'CIS', paralelo: 'A', paralelo_id: 'p1' }] });
    render(<StatsPage />);
    const form = screen.getByRole('form', { name: /paralelo/ });
    const dates = form.querySelectorAll('input');
    fireEvent.change(dates[0], { target: { value: '2026-01-01' } });
    fireEvent.change(dates[1], { target: { value: '2026-02-01' } });
    fireEvent.change(screen.getByLabelText('Carrera'), { target: { value: 'c1' } });
    fireEvent.click(screen.getAllByText('Consultar')[0]);
    await waitFor(() => expect(request).toHaveBeenCalledWith(expect.stringMatching(/^\/stats\/parallels\?careerId=c1&desde=2026-01-01T00%3A00%3A00-05%3A00&hasta=2026-02-01/)));
    expect(await screen.findByText('Sin datos')).toBeTruthy();
    fireEvent.click(screen.getAllByText('Consultar')[1]);
    await waitFor(() => expect(request).toHaveBeenCalledWith('/stats/careers'));
  });

  it('rechaza un periodo invertido sin llamar a la API', () => {
    roles = ['DIRECTOR_INSTITUCIONAL'];
    render(<StatsPage />);
    const form = screen.getByRole('form', { name: /carrera/ });
    const dates = form.querySelectorAll('input');
    fireEvent.change(dates[0], { target: { value: '2026-03-01' } });
    fireEvent.change(dates[1], { target: { value: '2026-02-01' } });
    fireEvent.click(form.querySelector('button')!);
    expect(screen.getByRole('alert')).toHaveTextContent('posterior');
    expect(request).not.toHaveBeenCalled();
  });
});
