"use client";
import { createContext, useCallback, useContext, useState, useEffect, type ReactNode } from 'react';
import type { components } from '../../generated/api';
import { ApiError, request, setBearer, onUnauthorized } from '../../lib/api/client';
import { hasSsoFlag, safeDestino, serviceFor, setSsoFlag } from './sso';
export type Session = components['schemas']['LoginResult'];
type Auth = { session: Session | null; ssoLogoutUrl: string | null; nombres: string | null; login: (correo: string, password: string) => Promise<void>; logout: () => Promise<void>; establish: (result: Session, nombres?: string) => void };
const Context = createContext<Auth | null>(null);
const RENEW_BEFORE_MS = 10 * 60 * 1000;
const REJECTED = [401, 403, 502];
export function SessionProvider({ children }: { children: ReactNode }) {
 const [session, setSession] = useState<Session | null>(null);
 const [ssoLogoutUrl, setSsoLogoutUrl] = useState<string | null>(null);
 const [nombres, setNombres] = useState<string | null>(null);
 const [notice, setNotice] = useState('');
 const [silent, setSilent] = useState(() => hasSsoFlag());
 const expire = useCallback((message: string) => { setBearer(null); setSession(null); setNotice(message); }, []);
 useEffect(()=>onUnauthorized(()=>expire('Tu sesión terminó. Ingresa de nuevo.')),[expire]);
 // Re-autenticación silenciosa: sin sesión en memoria pero con indicador, se pide un ticket al SSO (cookie vigente) y se vuelve a la ruta pedida.
 useEffect(() => {
  const { pathname, search } = window.location;
  if (!hasSsoFlag() || pathname.startsWith('/auth/callback')) return;
  setSsoFlag(false);
  request<{ login_url: string; web_service: string }>('/auth/sso-config', { authentication: 'none' }).then(c => {
   const destino = pathname === '/' || pathname === '/login' ? null : safeDestino(pathname + search);
   // eslint-disable-next-line @next/next/no-location-assign-relative-destination -- destino externo (SSO)
   window.location.href = `${c.login_url}?service=${encodeURIComponent(serviceFor(c.web_service, destino))}`;
  }, () => setSilent(false));
 }, []);
 // Perfil tras ingresar; solo un rechazo de la sesión (401/403/502) la cierra.
 useEffect(() => { if (!session?.access_token) return; let vivo = true; request('/me', { authentication: 'account' }).then(() => { /* perfil válido */ }, e => { if (vivo && e instanceof ApiError && REJECTED.includes(e.status)) expire('No se pudo validar tu sesión. Ingresa de nuevo.'); }); return () => { vivo = false; }; }, [session?.access_token, expire]);
 // Renueva 10 min antes de vencer.
 useEffect(() => {
  if (!session) return; const left = Date.parse(session.vence_en) - Date.now();
  const timer = setTimeout(() => {
   if (left <= 0) return expire('Tu sesión venció. Ingresa de nuevo.');
   request<Session>('/auth/renew', { method: 'POST', authentication: 'account' }).then(r => { setBearer(r.access_token); setSession(r); }, e => { if (e instanceof ApiError && REJECTED.includes(e.status)) expire('No se pudo renovar tu sesión. Ingresa de nuevo.'); });
  }, Math.max(0, left > RENEW_BEFORE_MS ? left - RENEW_BEFORE_MS : left / 2));
  return () => clearTimeout(timer);
 }, [session, expire]);
 async function login(correo: string, password: string) { const result = await request<Session>('/auth/login', { method: 'POST', authentication: 'none', body: JSON.stringify({ correo, password }) }); setBearer(result.access_token); setSession(result); setNotice(''); }
 const establish = useCallback((result: Session, nombresParam?: string) => { setBearer(result.access_token); setSession(result); setSsoLogoutUrl(null); setNotice(''); setSsoFlag(true); if (nombresParam) setNombres(nombresParam); }, []);
 async function logout() { setSsoFlag(false); setNombres(null); try { await request<{ sso_logout_url?: string }>('/auth/logout', { method: 'POST', authentication: 'account' }).then(out => setSsoLogoutUrl(out?.sso_logout_url ?? null)).catch(() => setSsoLogoutUrl(null)); } finally { setBearer(null); setSession(null); } }
 return <Context.Provider value={{ session, ssoLogoutUrl, nombres, login, logout, establish }}>{notice && !session && <p role="alert">{notice}</p>}{silent && !session ? <p role="status">Restableciendo tu sesión…</p> : children}</Context.Provider>;
}
export function useSession() { const value = useContext(Context); if (!value) throw new Error('Falta SessionProvider'); return value; }
