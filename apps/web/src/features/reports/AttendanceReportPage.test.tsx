import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, expect, test, vi } from 'vitest';
import { request } from '../../lib/api/client';
import { AttendanceReportPage } from './AttendanceReportPage';

vi.mock('../../lib/api/client', () => ({ request: vi.fn(), requestText: vi.fn() }));
vi.mock('../auth/SessionProvider', () => ({
  useSession: () => ({ session: { roles: ['DIRECTOR_INSTITUCIONAL'], vence_en: '2030-01-01T00:00:00Z', access_token: 'omitido', token_type: 'Bearer' } }),
}));

beforeEach(() => {
  vi.clearAllMocks();
  vi.mocked(request).mockImplementation(async path =>
    path.startsWith('/activities')
      ? { items: [{ id: 'a1', nombre: 'Evento', tipo: 'EVENTO', estado: 'ABIERTA', cerrada_en: null, anonimizar_en: null, responsable_id: 'r1', carrera_ids: ['c1'], paralelo_ids: ['p1'] }], page: 0, size: 100, total: 1 }
      : { items: [{ asistencia_id: null, persona_id: 'p1', cedula: '0000000001', nombres: 'Persona ficticia', estado: 'PENDIENTE_REVISION', registrada_en: '2026-10-20T01:00:00Z', attempt_id: 'i1', code: 'UBICACION_INCIERTA', mensaje: 'El encargado revisará el intento.' }], page: 0, size: 20, total: 1 },
  );
});
afterEach(cleanup);

test('muestra estado y motivo y no ofrece exportación al director', async () => {
  render(<AttendanceReportPage />);
  await waitFor(() => expect(screen.getByText(/Evento/)).toBeInTheDocument());
  fireEvent.change(screen.getByLabelText('Actividad'), { target: { value: 'a1' } });
  fireEvent.change(screen.getByLabelText('Sesión'), { target: { value: 's1' } });
  fireEvent.change(screen.getByLabelText('Estado'), { target: { value: 'PENDIENTE_REVISION' } });
  fireEvent.click(screen.getByText('Consultar'));
  await waitFor(() => expect(screen.getByText('El encargado revisará el intento.')).toBeInTheDocument());
  expect(screen.getByText('PENDIENTE REVISION')).toBeInTheDocument();
  expect(screen.queryByText('Descargar CSV')).not.toBeInTheDocument();
  expect(request).toHaveBeenCalledWith('/sessions/s1/attendances?estado=PENDIENTE_REVISION');
});
