import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { request } from '../../lib/api/client';
import { SpaceForm } from './SpaceForm';

vi.mock('../../lib/api/client', () => ({ ApiError: class extends Error {}, request: vi.fn() }));
vi.mock('leaflet', () => {
  const map = { setView: () => map, on: () => map, remove: () => {} };
  return { map: () => map, tileLayer: () => ({ addTo: () => {} }), circleMarker: () => ({ addTo: () => ({}), setLatLng: () => {} }) };
});
vi.mock('leaflet/dist/leaflet.css', () => ({}));

beforeEach(() => vi.clearAllMocks());
afterEach(cleanup);

describe('formulario de espacio', () => {
  it('exige latitud y longitud juntas', () => {
    render(<SpaceForm />);
    fireEvent.change(screen.getByLabelText('Código'), { target: { value: 'A334' } });
    fireEvent.change(screen.getByLabelText('Nombre'), { target: { value: 'Aula ficticia' } });
    fireEvent.change(screen.getByLabelText('Latitud'), { target: { value: '-4.03' } });
    fireEvent.click(screen.getByText('Guardar espacio'));
    expect(screen.getByRole('alert')).toHaveTextContent('juntas');
    expect(request).not.toHaveBeenCalled();
  });

  it('envía coordenadas escritas a mano', async () => {
    vi.mocked(request).mockResolvedValue({});
    render(<SpaceForm />);
    fireEvent.change(screen.getByLabelText('Código'), { target: { value: 'A334' } });
    fireEvent.change(screen.getByLabelText('Nombre'), { target: { value: 'Aula ficticia' } });
    fireEvent.change(screen.getByLabelText('Latitud'), { target: { value: '-4.03' } });
    fireEvent.change(screen.getByLabelText('Longitud'), { target: { value: '-79.2' } });
    fireEvent.click(screen.getByText('Guardar espacio'));
    await waitFor(() => expect(request).toHaveBeenCalled());
    const [path, init] = vi.mocked(request).mock.calls[0];
    expect(path).toBe('/spaces');
    expect(JSON.parse(String(init?.body))).toMatchObject({ codigo: 'A334', latitud: -4.03, longitud: -79.2, radio_defecto_m: 20 });
  });
});
