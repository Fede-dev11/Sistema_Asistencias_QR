import { act, cleanup, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { SessionProvider, useSession } from './SessionProvider';
import { setBearer } from '../../lib/api/client';

const result = (token: string, ms: number) => ({ access_token: token, token_type: 'Bearer' as const, vence_en: new Date(Date.now() + ms).toISOString(), roles: ['DOCENTE' as const] });
const problem = (status: number) => ({ ok: false, status, json: async () => ({ type: 'u', title: 't', status, code: 'X', mensaje: 'Rechazada.' }) });
function Probe({ first }: { first: ReturnType<typeof result> }) {
  const { establish, profile, session } = useSession();
  return <><button onClick={() => establish(first)}>entrar</button><p>{profile?.nombres ?? 'sin perfil'}|{session?.access_token ?? 'sin sesión'}</p></>;
}
async function entrar(fetcher: ReturnType<typeof vi.fn>, ms: number) {
  vi.stubGlobal('fetch', fetcher);
  render(<SessionProvider><Probe first={result('t1', ms)} /></SessionProvider>);
  await act(async () => { screen.getByText('entrar').click(); });
}
beforeEach(() => { vi.useFakeTimers(); });
afterEach(() => { cleanup(); vi.useRealTimers(); vi.unstubAllGlobals(); setBearer(null); });

describe('sesión: perfil y renovación', () => {
  it('carga /me y renueva 10 minutos antes de vencer', async () => {
    const fetcher = vi.fn().mockImplementation(async (url: string) => ({ ok: true, status: 200, json: async () => url.endsWith('/me') ? { nombres: 'Ana' } : result('t2', 3_600_000) }));
    await entrar(fetcher, 3_600_000);
    expect(screen.getByText('Ana|t1')).toBeTruthy();
    await act(async () => { await vi.advanceTimersByTimeAsync(49 * 60_000); });
    expect(fetcher.mock.calls.some(c => String(c[0]).endsWith('/auth/renew'))).toBe(false);
    await act(async () => { await vi.advanceTimersByTimeAsync(2 * 60_000); });
    const renew = fetcher.mock.calls.find(c => String(c[0]).endsWith('/auth/renew'))!;
    expect(new Headers(renew[1].headers).get('Authorization')).toBe('Bearer t1');
    expect(screen.getByText('Ana|t2')).toBeTruthy();
  });
  it.each([401, 403, 502])('si la renovación responde %i cierra la sesión con mensaje', async status => {
    const fetcher = vi.fn().mockImplementation(async (url: string) => String(url).endsWith('/auth/renew') ? problem(status) : { ok: true, status: 200, json: async () => ({ nombres: 'Ana' }) });
    await entrar(fetcher, 15 * 60_000);
    await act(async () => { await vi.advanceTimersByTimeAsync(6 * 60_000); });
    expect(screen.getByText('sin perfil|sin sesión')).toBeTruthy();
    expect(screen.getByRole('alert')).toHaveTextContent('Ingresa de nuevo');
  });
  it('si /me es rechazado cierra la sesión', async () => {
    await entrar(vi.fn().mockResolvedValue(problem(403)), 3_600_000);
    expect(screen.getByText('sin perfil|sin sesión')).toBeTruthy();
    expect(screen.getByRole('alert')).toBeTruthy();
  });
  it('al desmontar no queda renovación pendiente', async () => {
    const fetcher = vi.fn().mockResolvedValue({ ok: true, status: 200, json: async () => ({ nombres: 'Ana' }) });
    await entrar(fetcher, 3_600_000); cleanup();
    const antes = fetcher.mock.calls.length;
    await vi.advanceTimersByTimeAsync(2 * 3_600_000);
    expect(fetcher.mock.calls.length).toBe(antes);
  });
});
