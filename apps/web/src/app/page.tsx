'use client';
import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useSession } from '../features/auth/SessionProvider';
import { homeFor } from '../features/auth/sso';
import { ButtonLink } from '../features/ui/Button';

export default function Home() {
 const { session } = useSession(); const router = useRouter();
 useEffect(() => { if (session) router.replace(homeFor(session.roles)); }, [session, router]);
 if (session) return <section><p role="status">Entrando…</p></section>;
 return <section><p className="eyebrow">UNIVERSIDAD NACIONAL DE LOJA</p><h1>Tu asistencia,<br/>en el momento.</h1><p>Registra tu asistencia desde la app GeoAsist. La web es para docentes, responsables y administración.</p><ButtonLink href="/login">Ingresar</ButtonLink></section>;
}
