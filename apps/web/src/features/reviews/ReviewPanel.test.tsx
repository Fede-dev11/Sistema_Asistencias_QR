import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { request } from '../../lib/api/client';
import { ReviewPanel } from './ReviewPanel';

vi.mock('../../lib/api/client', () => ({
  ApiError: class extends Error {},
  request: vi.fn(),
}));

beforeEach(() => {
  vi.clearAllMocks();
  vi.stubGlobal('crypto', { randomUUID: () => '11111111-1111-4111-8111-111111111111' });
});
afterEach(cleanup);

describe('revisión responsable', () => {
  it('exige razón no blanca antes de enviar', () => {
    render(<ReviewPanel />);
    fireEvent.change(screen.getByLabelText('ID del intento'), { target: { value: 'intento-ficticio' } });
    fireEvent.change(screen.getByLabelText('Razón de revisión'), { target: { value: '   ' } });
    fireEvent.click(screen.getByText('Aprobar'));
    expect(screen.getByRole('alert')).toHaveTextContent('1 a 2000');
    expect(request).not.toHaveBeenCalled();
  });

  it('envía decisión y muestra el estado confirmado por servidor', async () => {
    vi.mocked(request).mockResolvedValue({
      attempt_id: '11111111-1111-4111-8111-111111111112',
      asistencia_id: null,
      decision: 'RECHAZAR',
      revisada_en: '2026-10-20T01:00:00Z',
    });
    render(<ReviewPanel />);
    fireEvent.change(screen.getByLabelText('ID del intento'), { target: { value: '11111111-1111-4111-8111-111111111112' } });
    fireEvent.change(screen.getByLabelText('Razón de revisión'), { target: { value: 'Medición ficticia insuficiente' } });
    fireEvent.click(screen.getByText('Rechazar'));
    await waitFor(() => expect(screen.getByRole('status')).toHaveTextContent('rechazar'));
    expect(request).toHaveBeenCalledWith(
      '/attempts/11111111-1111-4111-8111-111111111112/review',
      expect.objectContaining({ body: JSON.stringify({ decision: 'RECHAZAR', razon: 'Medición ficticia insuficiente' }) }),
    );
  });
});
