"use client";
import { createContext, useContext, useState, useEffect, type ReactNode } from 'react';
import type { components } from '../../generated/api';
import { request, setBearer, onUnauthorized } from '../../lib/api/client';
import { clearGuestResume } from '../attendance/guestResume';
type Session = components['schemas']['LoginResult'];
type Auth = { session: Session | null; login: (correo: string, password: string) => Promise<void>; logout: () => Promise<void> };
const Context = createContext<Auth | null>(null);
export function SessionProvider({ children }: { children: ReactNode }) {
 const [session, setSession] = useState<Session | null>(null);
 useEffect(()=>onUnauthorized(()=>{clearGuestResume();setSession(null);}),[]);
 useEffect(() => { if (!session) return; const timer = setTimeout(() => { setBearer(null); clearGuestResume(); setSession(null); }, Math.max(0, Date.parse(session.vence_en) - Date.now())); return () => clearTimeout(timer); }, [session]);
 async function login(correo: string, password: string) { const result = await request<Session>('/auth/login', { method: 'POST', authentication: 'none', body: JSON.stringify({ correo, password }) }); setBearer(result.access_token); setSession(result); }
 async function logout() { try { await request<void>('/auth/logout', { method: 'POST' }); } finally { setBearer(null); clearGuestResume(); setSession(null); } }
 return <Context.Provider value={{ session, login, logout }}>{children}</Context.Provider>;
}
export function useSession() { const value = useContext(Context); if (!value) throw new Error('Falta SessionProvider'); return value; }
