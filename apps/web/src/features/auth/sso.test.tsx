import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render, screen, waitFor } from '@testing-library/react';
import { SessionProvider } from './SessionProvider';
import { homeFor, safeDestino, setSsoFlag, SSO_FLAG } from './sso';

afterEach(() => { cleanup(); sessionStorage.clear(); vi.unstubAllGlobals(); vi.unstubAllEnvs(); });
const cfg = { ok: true, status: 200, json: async () => ({ login_url: 'https://sso.example.test/cas/login', web_service: 'http://localhost:3000/auth/callback' }) };

describe('destino y rol', () => {
  it('solo acepta rutas internas', () => {
    expect(safeDestino('/actividades/1?tab=a')).toBe('/actividades/1?tab=a');
    for (const bad of ['//evil.test', 'https://evil.test', '/\\evil', 'javascript:1', '', null, '/auth/callback?x=1']) expect(safeDestino(bad)).toBeNull();
  });
  it('inicio por rol', () => {
    expect(homeFor(['DOCENTE'])).toBe('/hoy');
    expect(homeFor(['ENCARGADO'])).toBe('/hoy');
    expect(homeFor(['DIRECTOR_INSTITUCIONAL'])).toBe('/inicio');
    expect(homeFor(['DIRECTOR_CARRERA'])).toBe('/inicio');
    expect(homeFor(['ADMINISTRADOR'])).toBe('/inicio');
    expect(homeFor(['ESTUDIANTE'])).toBe('/login');
    vi.stubEnv('NEXT_PUBLIC_HERRAMIENTAS_PRUEBA', 'true');
    expect(homeFor(['ESTUDIANTE'])).toBe('/pruebas/participante');
  });
});

describe('re-autenticación silenciosa', () => {
  it('con indicador y sin sesión redirige al SSO llevando el destino, y borra el indicador', async () => {
    setSsoFlag(true); const loc = { pathname: '/actividades/9', search: '?tab=x', href: '' }; vi.stubGlobal('location', loc);
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(cfg));
    render(<SessionProvider><p>contenido</p></SessionProvider>);
    await waitFor(() => expect(loc.href).not.toBe(''));
    expect(loc.href).toBe('https://sso.example.test/cas/login?service=' + encodeURIComponent('http://localhost:3000/auth/callback?destino=' + encodeURIComponent('/actividades/9?tab=x')));
    expect(sessionStorage.getItem(SSO_FLAG)).toBeNull(); // si el SSO pide credenciales, no hay bucle
    expect(screen.queryByText('contenido')).toBeNull();
  });
  it('sin indicador no redirige; en el callback tampoco', () => {
    const fetcher = vi.fn().mockResolvedValue(cfg); vi.stubGlobal('fetch', fetcher);
    vi.stubGlobal('location', { pathname: '/hoy', search: '', href: '' });
    render(<SessionProvider><p>contenido</p></SessionProvider>);
    expect(screen.getByText('contenido')).toBeTruthy(); cleanup();
    setSsoFlag(true); vi.stubGlobal('location', { pathname: '/auth/callback', search: '?ticket=ST-1', href: '' });
    render(<SessionProvider><p>contenido</p></SessionProvider>);
    expect(fetcher).not.toHaveBeenCalled();
  });
  it('si la configuración falla, muestra el contenido y no queda indicador', async () => {
    setSsoFlag(true); vi.stubGlobal('location', { pathname: '/hoy', search: '', href: '' });
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: false, status: 502, json: async () => ({ type: 'u', title: 't', status: 502, code: 'X', mensaje: 'caído' }) }));
    render(<SessionProvider><p>contenido</p></SessionProvider>);
    expect(await screen.findByText('contenido')).toBeTruthy();
    expect(sessionStorage.getItem(SSO_FLAG)).toBeNull();
  });
});
