import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { request } from '../../lib/api/client';
import { GeoEvidencePanel } from './GeoEvidencePanel';

vi.mock('../../lib/api/client', () => ({ ApiError: class extends Error {}, request: vi.fn() }));
afterEach(() => { cleanup(); vi.clearAllMocks(); });

const ev = { id: 'g1', espacio_id: 's1', version: 2, referencia: 'Medición GPS', realizada_en: '2026-05-01T15:00:00Z', responsable_id: 'r', falsos_positivos: 0.05, falsos_negativos: 0.1, verificada_en: '2026-05-01T16:00:00Z' };

describe('evidencia geográfica', () => {
  it('es solo lectura sin canCreate', async () => {
    vi.mocked(request).mockResolvedValue([ev]);
    render(<GeoEvidencePanel spaceId="s1" />);
    expect(await screen.findByText('Medición GPS')).toBeTruthy();
    expect(screen.queryByRole('form')).toBeNull();
  });

  it('indica historial vacío', async () => {
    vi.mocked(request).mockResolvedValue([]);
    render(<GeoEvidencePanel spaceId="s1" />);
    expect(await screen.findByText('Sin evidencias registradas.')).toBeTruthy();
  });

  it('muestra el error de carga', async () => {
    vi.mocked(request).mockRejectedValue(new Error('Espacio no encontrado'));
    render(<GeoEvidencePanel spaceId="s1" />);
    expect(await screen.findByRole('alert')).toHaveTextContent('Espacio no encontrado');
  });

  it('registra evidencia con decimales y recarga', async () => {
    vi.mocked(request).mockImplementation(async (_p: string, o?: RequestInit) => (o?.method === 'POST' ? ev : []));
    render(<GeoEvidencePanel spaceId="s1" canCreate />);
    await screen.findByText('Sin evidencias registradas.');
    fireEvent.change(screen.getByLabelText('Referencia'), { target: { value: ' Medición GPS ' } });
    fireEvent.change(screen.getByLabelText('Realizada en'), { target: { value: '2026-05-01T10:00' } });
    fireEvent.change(screen.getByLabelText(/Falsos positivos/), { target: { value: '0.05' } });
    fireEvent.change(screen.getByLabelText(/Falsos negativos/), { target: { value: '0.1' } });
    fireEvent.click(screen.getByText('Registrar evidencia', { selector: 'button' }));
    expect(await screen.findByRole('status')).toHaveTextContent('Evidencia registrada.');
    const post = vi.mocked(request).mock.calls.find(c => c[1]?.method === 'POST')!;
    expect(post[0]).toBe('/spaces/s1/geo-evidence');
    expect(post[1]?.headers).toHaveProperty('Idempotency-Key');
    expect(JSON.parse(String(post[1]?.body))).toMatchObject({ referencia: 'Medición GPS', falsos_positivos: 0.05, falsos_negativos: 0.1 });
    await waitFor(() => expect(vi.mocked(request).mock.calls.filter(c => !c[1]).length).toBeGreaterThan(1));
  });

  it('muestra el error del alta', async () => {
    vi.mocked(request).mockImplementation(async (_p: string, o?: RequestInit) => { if (o?.method === 'POST') throw new Error('ESPACIO_SIN_UBICACION'); return []; });
    render(<GeoEvidencePanel spaceId="s1" canCreate />);
    await screen.findByText('Sin evidencias registradas.');
    fireEvent.change(screen.getByLabelText('Referencia'), { target: { value: 'x' } });
    fireEvent.change(screen.getByLabelText('Realizada en'), { target: { value: '2026-05-01T10:00' } });
    fireEvent.change(screen.getByLabelText(/Falsos positivos/), { target: { value: '0' } });
    fireEvent.change(screen.getByLabelText(/Falsos negativos/), { target: { value: '0' } });
    fireEvent.click(screen.getByText('Registrar evidencia', { selector: 'button' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('ESPACIO_SIN_UBICACION');
  });
});
