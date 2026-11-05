"use client";
import { useEffect, useState, type FormEvent } from 'react';
import { useRouter } from 'next/navigation';
import type { components } from '../../generated/api';
import { request } from '../../lib/api/client';
import { useSession } from './SessionProvider';
import { homeFor } from './sso';
import { Button } from '../ui/Button';
type Sso = components['schemas']['SsoConfiguration'];
export function LoginForm() {
 const { session, login, logout, ssoLogoutUrl } = useSession(); const router = useRouter(); const home = session ? homeFor(session.roles) : null; const [error, setError] = useState(''); const [busy, setBusy] = useState(false);
 // Modo: con /auth/sso-config disponible solo se ofrece el SSO; si falla, se asume login local (más simple que sondear /auth/login, que exigiría credenciales).
 const [sso, setSso] = useState<Sso | 'cargando' | null>('cargando');
 useEffect(() => { let vivo = true; request<Sso>('/auth/sso-config', { authentication: 'none' }).then(c => { if (vivo) setSso(c); }, () => { if (vivo) setSso(null); }); return () => { vivo = false; }; }, []);
 useEffect(() => { if (home && home !== '/login') router.replace(home); }, [home, router]);
 async function submit(event: FormEvent<HTMLFormElement>) { event.preventDefault(); setError(''); setBusy(true); const form = event.currentTarget; const data = new FormData(form); try { await login(String(data.get('correo')), String(data.get('password'))); form.reset(); } catch (e) { setError(e instanceof Error ? e.message : 'No hay conexión. Reintenta.'); } finally { setBusy(false); } }
 if (session) return home !== '/login' ? <section><p role="status">Entrando…</p></section> : <section><h1>Tu asistencia se registra desde la app</h1><p>Abre GeoAsist en tu teléfono para registrar asistencia. Esta web es para docentes, responsables y administración.</p><Button variant="secondary" onClick={() => { void logout().catch(() => setError('Sesión local cerrada. No se pudo confirmar la revocación en el servidor.')); }}>Cerrar sesión</Button>{error && <p role="alert">{error}</p>}</section>;
 if (sso === 'cargando') return <section><h1>Ingresar</h1><p role="status">Cargando opciones de acceso…</p></section>;
 const salida = ssoLogoutUrl && <p><a href={ssoLogoutUrl}>Cerrar también la sesión universitaria</a></p>;
 const ir = () => {
  if (typeof sso !== 'object' || !sso) return;
  // eslint-disable-next-line @next/next/no-location-assign-relative-destination -- destino externo (SSO), no una ruta de Next
  window.location.href = `${sso.login_url}?service=${encodeURIComponent(sso.web_service)}`;
 };
 if (sso) return <section><h1>Ingresar</h1><Button variant="primary" onClick={ir}>Ingresar con cuenta universitaria</Button>{salida}</section>;
 return <form onSubmit={submit}><h1>Ingresar con credenciales</h1><label>Correo<input type="email" name="correo" autoComplete="username" required /></label><label>Contraseña<input type="password" name="password" autoComplete="current-password" required /></label><Button variant="primary" type="submit" disabled={busy}>{busy ? 'Ingresando…' : 'Ingresar'}</Button>{error && <p role="alert">{error}</p>}{salida}</form>;
}
