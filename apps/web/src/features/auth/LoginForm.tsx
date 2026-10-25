"use client";
import { useState, type FormEvent } from 'react';
import Link from 'next/link';
import { useSession } from './SessionProvider';
export function LoginForm() {
 const { session, login, logout } = useSession(); const [error, setError] = useState(''); const [busy, setBusy] = useState(false);
 async function submit(event: FormEvent<HTMLFormElement>) { event.preventDefault(); setError(''); setBusy(true); const form = event.currentTarget; const data = new FormData(form); try { await login(String(data.get('correo')), String(data.get('password'))); form.reset(); } catch (e) { setError(e instanceof Error ? e.message : 'No hay conexión. Reintenta.'); } finally { setBusy(false); } }
 if (session) return <section><h1>Sesión iniciada</h1><p role="status">Acceso con credenciales confirmado.</p><nav><Link href="/sesiones">Gestionar actividades</Link><Link href="/reportes">Consultar asistencias</Link></nav><button onClick={() => { void logout().catch(() => setError('Sesión local cerrada. No se pudo confirmar la revocación en el servidor.')); }}>Cerrar sesión</button>{error && <p role="alert">{error}</p>}</section>;
 return <form onSubmit={submit}><h1>Ingresar con credenciales</h1><label>Correo<input type="email" name="correo" autoComplete="username" required /></label><label>Contraseña<input type="password" name="password" autoComplete="current-password" required /></label><button disabled={busy}>{busy ? 'Ingresando…' : 'Ingresar'}</button>{error && <p role="alert">{error}</p>}</form>;
}
