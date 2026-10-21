import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  clearGuestResume,
  createGuestResume,
  loadGuestResume,
  saveGuestResume,
} from './guestResume';

describe('recuperación temporal del formulario invitado', () => {
  afterEach(() => {
    sessionStorage.clear();
    vi.useRealTimers();
  });

  it('conserva solo capacidad, tiempos del servidor y UUID idempotente', () => {
    const resume = createGuestResume({
      registro_invitado_token: 'x'.repeat(64),
      inicio_en: '2026-10-20T01:00:00Z',
      vence_en: '2026-10-20T01:02:00Z',
    });
    saveGuestResume(resume);

    const raw = sessionStorage.getItem('geoasist-guest-resume') ?? '';
    expect(raw).not.toMatch(/cedula|nombres|bearer|access_token/i);
    expect(loadGuestResume()).toEqual(resume);
    expect(resume.idempotencyKey).toMatch(/^[0-9a-f-]{36}$/i);
  });

  it('no prolonga ni invalida el plazo por recarga o por el reloj del teléfono', () => {
    const resume = createGuestResume({
      registro_invitado_token: 'y'.repeat(64),
      inicio_en: '2026-10-20T01:00:00Z',
      vence_en: '2026-10-20T01:00:41Z',
    });
    saveGuestResume(resume);
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2040-01-01T00:00:00Z'));

    expect(loadGuestResume()).toEqual(resume);
    expect(loadGuestResume()?.idempotencyKey).toBe(resume.idempotencyKey);
  });

  it('descarta datos manipulados y se limpia tras consumo o rechazo de expiración', () => {
    sessionStorage.setItem(
      'geoasist-guest-resume',
      JSON.stringify({ token: 'corto', inicioEn: 'x', venceEn: 'y', idempotencyKey: 'z' }),
    );
    expect(loadGuestResume()).toBeNull();
    expect(sessionStorage.getItem('geoasist-guest-resume')).toBeNull();

    saveGuestResume(
      createGuestResume({
        registro_invitado_token: 'z'.repeat(64),
        inicio_en: '2026-10-20T01:00:00Z',
        vence_en: '2026-10-20T01:02:00Z',
      }),
    );
    clearGuestResume();
    expect(loadGuestResume()).toBeNull();
  });
});
