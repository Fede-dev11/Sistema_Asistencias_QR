"use client";
import { createContext, useCallback, useContext, useState, useEffect, type ReactNode } from 'react';
import type { components } from '../../generated/api';
import { request, setBearer, onUnauthorized } from '../../lib/api/client';
export type Session = components['schemas']['LoginResult'];
type Auth = { session: Session | null; login: (correo: string, password: string) => Promise<void>; logout: () => Promise<void>; establish: (result: Session) => void; ssoLogoutUrl: string | null };
const Context = createContext<Auth | null>(null);
export function SessionProvider({ children }: { children: ReactNode }) {
 const [session, setSession] = useState<Session | null>(null); const [ssoLogoutUrl, setSsoLogoutUrl] = useState<string | null>(null);
 useEffect(()=>onUnauthorized(()=>{setSession(null);}),[]);
 useEffect(() => { if (!session) return; const timer = setTimeout(() => { setBearer(null); setSession(null); }, Math.max(0, Date.parse(session.vence_en) - Date.now())); return () => clearTimeout(timer); }, [session]);
 async function login(correo: string, password: string) { const result = await request<Session>('/auth/login', { method: 'POST', authentication: 'none', body: JSON.stringify({ correo, password }) }); setBearer(result.access_token); setSession(result); }
 const establish = useCallback((result: Session) => { setBearer(result.access_token); setSession(result); setSsoLogoutUrl(null); }, []);
 async function logout() { try { const out = await request<{ sso_logout_url?: string } | undefined>('/auth/logout', { method: 'POST' }); setSsoLogoutUrl(out?.sso_logout_url ?? null); } finally { setBearer(null); setSession(null); } }
 return <Context.Provider value={{ session, login, logout, establish, ssoLogoutUrl }}>{children}</Context.Provider>;
}
export function useSession() { const value = useContext(Context); if (!value) throw new Error('Falta SessionProvider'); return value; }
