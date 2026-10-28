import { describe, it, expect, vi, afterEach } from 'vitest';
import { StrictMode } from 'react';
import { render, screen, waitFor, cleanup } from '@testing-library/react';
import { SessionProvider } from './SessionProvider';
import { CasCallback } from './CasCallback';
const nav = vi.hoisted(() => ({ replace: vi.fn(), query: '' }));
vi.mock('next/navigation', () => ({ useRouter: () => ({ replace: nav.replace }), useSearchParams: () => new URLSearchParams(nav.query) }));
afterEach(() => { cleanup(); vi.unstubAllGlobals(); nav.replace.mockClear(); });
const cfg = { ok: true, status: 200, json: async () => ({ web_service: 'https://web.example.test/auth/callback' }) };
const montar = () => render(<StrictMode><SessionProvider><CasCallback /></SessionProvider></StrictMode>);
describe('callback CAS', () => {
 it('canjea el ticket una sola vez con el mismo service y limpia la URL', async () => {
  nav.query = 'ticket=ST-1-abc';
  const setItem = vi.spyOn(Storage.prototype, 'setItem');
  const fetcher = vi.fn().mockImplementation(async (url: string) => url.endsWith('/auth/sso-config') ? cfg : { ok: true, status: 200, json: async () => ({ access_token: 't', token_type: 'Bearer', vence_en: new Date(Date.now() + 60000).toISOString(), roles: ['ESTUDIANTE'] }) });
  vi.stubGlobal('fetch', fetcher); montar();
  await waitFor(() => expect(nav.replace).toHaveBeenCalledWith('/login'));
  const cas = fetcher.mock.calls.filter(c => String(c[0]).endsWith('/auth/cas'));
  expect(cas).toHaveLength(1);
  expect(JSON.parse(cas[0][1].body)).toEqual({ ticket: 'ST-1-abc', service: 'https://web.example.test/auth/callback' });
  expect(nav.replace).toHaveBeenCalledWith('/auth/callback');
  expect(setItem).not.toHaveBeenCalled();
 });
 it('sin ticket muestra error y enlace para reintentar sin llamar al servidor', async () => {
  nav.query = ''; const fetcher = vi.fn(); vi.stubGlobal('fetch', fetcher); montar();
  expect(await screen.findByRole('alert')).toHaveTextContent('ticket');
  expect(screen.getByRole('link', { name: 'Reintentar' })).toHaveAttribute('href', '/login');
  expect(fetcher).not.toHaveBeenCalled();
 });
 it('ticket rechazado muestra el problema y permite reintentar', async () => {
  nav.query = 'ticket=ST-2';
  vi.stubGlobal('fetch', vi.fn().mockImplementation(async (url: string) => url.endsWith('/auth/sso-config') ? cfg : { ok: false, status: 401, json: async () => ({ type: 'urn:t', title: 'x', status: 401, code: 'TICKET_INVALIDO', mensaje: 'Ticket inválido o vencido.' }) }));
  montar();
  expect(await screen.findByRole('alert')).toHaveTextContent('Ticket inválido o vencido.');
  expect(screen.getByRole('link', { name: 'Reintentar' })).toBeInTheDocument();
 });
});
