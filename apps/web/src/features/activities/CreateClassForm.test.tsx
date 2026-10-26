import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { request } from '../../lib/api/client';
import { CreateClassForm } from './CreateClassForm';
import { previewDates } from './schedulePreview';

vi.mock('../../lib/api/client', () => ({ ApiError: class extends Error {}, request: vi.fn() }));
vi.mock('../spaces/SpacePicker', () => ({
  SpacePicker: ({ onSelect }: { onSelect: (s: unknown) => void }) => <button type="button" onClick={() => onSelect({ id: 's1', codigo: 'A1', nombre: 'Aula' })}>elegir</button>
}));

beforeEach(() => {
  vi.clearAllMocks();
  vi.mocked(request).mockImplementation(async (path: string) =>
    (path === '/me/parallels' ? { items: [{ paralelo_id: 'p1', codigo: 'A', carrera_id: 'c', carrera_codigo: 'COMP', carrera_nombre: 'X', matriculados: 2 }] } : { id: 'a1', sesiones: [] }) as never);
});
afterEach(cleanup);

describe('previewDates', () => {
  it('cuenta lunes y miércoles con hasta inclusivo', () => {
    expect(previewDates([1, 3], '2026-11-02', '2026-11-27')).toHaveLength(8);
    expect(previewDates([5], '2026-11-27', '2026-11-27')).toEqual(['2026-11-27']);
  });
});

describe('formulario de clase', () => {
  it('exige espacio y paralelo antes de enviar', () => {
    render(<CreateClassForm />);
    fireEvent.change(screen.getByLabelText('Nombre de la clase'), { target: { value: 'Redes' } });
    fireEvent.click(screen.getByText('Crear clase'));
    expect(screen.getByRole('alert')).toHaveTextContent('espacio');
  });

  it('envía paralelos, espacio y horario', async () => {
    render(<CreateClassForm />);
    await screen.findByText(/COMP A/);
    fireEvent.change(screen.getByLabelText('Nombre de la clase'), { target: { value: 'Redes' } });
    fireEvent.click(screen.getByLabelText(/COMP A/));
    fireEvent.click(screen.getByText('elegir'));
    fireEvent.click(screen.getByLabelText('Lunes'));
    fireEvent.change(screen.getByLabelText('Desde'), { target: { value: '2026-11-02' } });
    fireEvent.change(screen.getByLabelText('Hasta'), { target: { value: '2026-11-23' } });
    expect(await screen.findByText(/4 sesiones/)).toBeTruthy();
    fireEvent.click(screen.getByText('Crear clase'));
    await waitFor(() => expect(request).toHaveBeenCalledWith('/activities', expect.objectContaining({ method: 'POST' })));
    const call = vi.mocked(request).mock.calls.find(c => c[0] === '/activities')!;
    expect(JSON.parse((call[1] as RequestInit).body as string)).toMatchObject({
      modalidad: 'CLASE', espacio_id: 's1', paralelo_ids: ['p1'], horario: { dias: [1], desde: '2026-11-02', hasta: '2026-11-23' }
    });
  });
});
