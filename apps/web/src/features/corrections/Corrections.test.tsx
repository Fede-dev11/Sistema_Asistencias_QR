import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { request } from '../../lib/api/client';
import { AbsencesPanel } from './AbsencesPanel';
import { JustificationInbox } from './JustificationInbox';
import { ManualAttendanceForm } from './ManualAttendanceForm';

vi.mock('../../lib/api/client', () => ({ ApiError: class extends Error {}, request: vi.fn() }));

beforeEach(() => {
  vi.clearAllMocks();
  vi.stubGlobal('crypto', { randomUUID: () => 'key-1' });
});
afterEach(cleanup);

describe('ManualAttendanceForm', () => {
  it('exige razón y envía con Idempotency-Key', async () => {
    vi.mocked(request).mockImplementation(async (path: string) => path.includes('/students')
      ? { items: [{ persona_id: 'p1', cedula: '1100000000', nombres: 'Ana Ficticia', vigente: true }] } : {});
    render(<ManualAttendanceForm activityId="a1" sessionId="s1" />);
    await screen.findByRole('option', { name: /Ana Ficticia/ });
    fireEvent.change(screen.getByLabelText('Estudiante'), { target: { value: 'p1' } });
    fireEvent.click(screen.getByRole('button', { name: 'Registrar asistencia' }));
    expect(screen.getByRole('alert')).toHaveTextContent('razón');
    fireEvent.change(screen.getByLabelText('Razón'), { target: { value: 'Falló el lector' } });
    fireEvent.click(screen.getByRole('button', { name: 'Registrar asistencia' }));
    await screen.findByText('Asistencia manual registrada.');
    expect(request).toHaveBeenLastCalledWith('/sessions/s1/manual-attendances', expect.objectContaining({
      method: 'POST', headers: { 'Idempotency-Key': 'key-1' }, body: JSON.stringify({ persona_id: 'p1', razon: 'Falló el lector' }),
    }));
  });
});

describe('AbsencesPanel', () => {
  it('justifica una ausencia con razón', async () => {
    vi.mocked(request).mockResolvedValue({ items: [{ persona_id: 'p1', cedula: '1100000000', nombres: 'Ana', estado: 'AUSENTE' }], total: 1, page: 0, size: 20 });
    render(<AbsencesPanel sessionId="s1" />);
    fireEvent.click(await screen.findByRole('button', { name: 'Justificar Ana' }));
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar justificación' }));
    expect(screen.getByRole('alert')).toHaveTextContent('razón');
    fireEvent.change(screen.getByLabelText('Razón de justificación'), { target: { value: 'Certificado' } });
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar justificación' }));
    await waitFor(() => expect(request).toHaveBeenCalledWith('/sessions/s1/absences/p1/justify', expect.objectContaining({ method: 'POST', body: JSON.stringify({ razon: 'Certificado' }) })));
  });
});

describe('JustificationInbox', () => {
  const item = { id: 'j1', sesion_id: 's1', persona_id: 'p1', cedula: '1100000000', nombres: 'Ana', origen: 'ESTUDIANTE', motivo: 'Enferma', estado: 'PENDIENTE' };
  it('lista pendientes y rechaza con razón', async () => {
    vi.mocked(request).mockResolvedValue({ items: [item], total: 1, page: 0, size: 10 });
    render(<JustificationInbox activityId="a1" />);
    await screen.findByText(/Enferma/);
    expect(request).toHaveBeenCalledWith('/activities/a1/justification-requests?page=0&size=10&estado=PENDIENTE');
    fireEvent.click(screen.getByRole('button', { name: 'Rechazar' }));
    expect(screen.getByRole('alert')).toHaveTextContent('razón');
    fireEvent.change(screen.getByLabelText('Razón de decisión de Ana'), { target: { value: 'Sin soporte' } });
    fireEvent.click(screen.getByRole('button', { name: 'Rechazar' }));
    await screen.findByText('Solicitud rechazada.');
    expect(request).toHaveBeenCalledWith('/justification-requests/j1/decision', expect.objectContaining({ body: JSON.stringify({ decision: 'RECHAZAR', razon: 'Sin soporte' }) }));
  });
  it('aprueba con razón', async () => {
    vi.mocked(request).mockResolvedValue({ items: [item], total: 1, page: 0, size: 10 });
    render(<JustificationInbox activityId="a1" />);
    await screen.findByText(/Enferma/);
    fireEvent.change(screen.getByLabelText('Razón de decisión de Ana'), { target: { value: 'Certificado válido' } });
    fireEvent.click(screen.getByRole('button', { name: 'Aprobar' }));
    await screen.findByText('Solicitud aprobada.');
  });
});
