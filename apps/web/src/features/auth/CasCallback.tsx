"use client";
import { useEffect, useRef, useState } from 'react';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import { request } from '../../lib/api/client';
import { useSession, type Session } from './SessionProvider';
export function CasCallback() {
 const router = useRouter(); const params = useSearchParams(); const { establish } = useSession();
 const started = useRef(false); const [error, setError] = useState('');
 useEffect(() => {
  if (started.current) return; started.current = true; // strict mode ejecuta el efecto dos veces; el ticket es de un solo uso
  const ticket = params.get('ticket');
  router.replace('/auth/callback'); // saca el ticket de la URL
  void (async () => {
   try {
    if (!ticket) throw new Error('No se recibió el ticket de acceso universitario.');
    const { web_service } = await request<{ web_service: string }>('/auth/sso-config', { authentication: 'none' });
    establish(await request<Session>('/auth/cas', { method: 'POST', authentication: 'none', body: JSON.stringify({ ticket, service: web_service }) }));
    router.replace('/login');
   } catch (e) { setError(e instanceof Error ? e.message : 'No hay conexión. Reintenta.'); }
  })();
 }, [params, router, establish]);
 if (error) return <section><h1>No se pudo iniciar sesión</h1><p role="alert">{error}</p><Link href="/login">Reintentar</Link></section>;
 return <section><h1>Verificando acceso</h1><p role="status">Validando tu cuenta universitaria…</p></section>;
}
