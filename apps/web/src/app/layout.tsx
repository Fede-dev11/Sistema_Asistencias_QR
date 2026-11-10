import Link from 'next/link';
import type { Metadata } from 'next';
import './globals.css';
import { SessionProvider } from '../features/auth/SessionProvider';
import { RoleNav } from '../features/auth/RoleNav';
export const metadata: Metadata = { title: 'UNL · GeoAsist', description: 'Asistencia académica con QR dinámico.' };
export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
 return <html lang="es"><body><div className="shell"><SessionProvider><header><Link href="/" className="brand">UNL · GeoAsist</Link><RoleNav /></header><main>{children}</main></SessionProvider></div></body></html>;
}
