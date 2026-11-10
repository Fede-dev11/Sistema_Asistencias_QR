"use client";
import { createContext, useCallback, useContext, useState, useEffect, type ReactNode } from 'react';
import type { components } from '../../generated/api';
import { ApiError, request, setBearer, onUnauthorized } from '../../lib/api/client';
import { hasSsoFlag, safeDestino, serviceFor, setSsoFlag } from './sso';
export type Session = components['schemas']['LoginResult'];
export type Profile = components['schemas']['MeProfile'];
const RENEW_BEFORE_MS = 10 * 60 * 1000;
const REJECTED = [401, 403, 502];
type Auth = { session: Session | null; profile: Profile | null; login: (correo: string, password: string) => Promise<void>; logout: () => Promise<void>; establish: (result: Session) => void; ssoLogoutUrl: string | null };
const Context = createContext<Auth | null>(null);
export function SessionProvider({ children }: { children: ReactNode }) {
 const [session, setSession] = useState<Session | null>(null); const [ssoLogoutUrl, setSsoLogoutUrl] = useState<string | null>(null);
 const [profile, setProfile] = useState<Profile | null>(null); const [notice, setNotice] = useState(''); const [silent, setSilent] = useState(false);
 const expire = useCallback((message: string) => { setBearer(null); setSession(null); setProfile(null); setNotice(message); }, []);
 useEffect(()=>onUnauthorized(()=>expire('Tu sesión terminó. Ingresa de nuevo.')),[expire]);
 // Re-autenticación silenciosa: sin sesión en memoria pero con indicador, se pide un ticket al SSO (cookie vigente) y se vuelve a la ruta pedida.
 useEffect(() => {
  const { pathname, search } = window.location;
  if (!hasSsoFlag() || pathname.startsWith('/auth/callback')) return;
  setSsoFlag(false); // se borra antes de salir: si el SSO pide credenciales o falla, no hay bucle
  setSilent(true); // eslint-disable-line react-hooks/set-state-in-effect -- depende de sessionStorage (solo existe en el navegador)
  request<{ login_url: string; web_service: string }>('/auth/sso-config', { authentication: 'none' }).then(c => {
   const destino = pathname === '/' || pathname === '/login' ? null : safeDestino(pathname + search);
   // eslint-disable-next-line @next/next/no-location-assign-relative-destination -- destino externo (SSO)
   window.location.href = `${c.login_url}?service=${encodeURIComponent(serviceFor(c.web_service, destino))}`;
  }, () => setSilent(false));
 }, []);
 // Perfil tras ingresar; solo un rechazo de la sesión (401/403/502) la cierra.
 const token = session?.access_token;
 useEffect(() => { if (!token) return; let vivo = true; request<Profile>('/me').then(p => { if (vivo) setProfile(p); }, e => { if (vivo && e instanceof ApiError && REJECTED.includes(e.status)) expire('No se pudo validar tu sesión. Ingresa de nuevo.'); }); return () => { vivo = false; }; }, [token, expire]);
 // Renueva 10 min antes de vencer (a la mitad si queda menos); si ya venció, cierra.
 useEffect(() => {
  if (!session) return; const left = Date.parse(session.vence_en) - Date.now();
  const timer = setTimeout(() => {
   if (left <= 0) return expire('Tu sesión venció. Ingresa de nuevo.');
   request<Session>('/auth/renew', { method: 'POST' }).then(r => { setBearer(r.access_token); setSession(r); }, e => { if (e instanceof ApiError && REJECTED.includes(e.status)) expire('No se pudo renovar tu sesión. Ingresa de nuevo.'); });
  }, Math.max(0, left > RENEW_BEFORE_MS ? left - RENEW_BEFORE_MS : left / 2));
  return () => clearTimeout(timer);
 }, [session, expire]);
 async function login(correo: string, password: string) { const result = await request<Session>('/auth/login', { method: 'POST', authentication: 'none', body: JSON.stringify({ correo, password }) }); setBearer(result.access_token); setSession(result); setNotice(''); }
 const establish = useCallback((result: Session) => { setBearer(result.access_token); setSession(result); setSsoLogoutUrl(null); setNotice(''); setSsoFlag(true); }, []);
 async function logout() { setSsoFlag(false); try { const out = await request<{ sso_logout_url?: string } | undefined>('/auth/logout', { method: 'POST' }); setSsoLogoutUrl(out?.sso_logout_url ?? null); } finally { setBearer(null); setSession(null); setProfile(null); } }
 return <Context.Provider value={{ session, profile, login, logout, establish, ssoLogoutUrl }}>{notice && !session && <p role="alert">{notice}</p>}{silent && !session ? <p role="status">Restableciendo tu sesión…</p> : children}</Context.Provider>;
}
export function useSession() { const value = useContext(Context); if (!value) throw new Error('Falta SessionProvider'); return value; }
