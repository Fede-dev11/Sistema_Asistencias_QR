import Link from 'next/link';
import type { Metadata } from 'next';
import './globals.css';
import { SessionProvider } from '../features/auth/SessionProvider';
export const metadata: Metadata = { title: 'UNL GeoAsist', description: 'Asistencia académica con QR dinámico.' };
export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
 return <html lang="es"><body><header><Link href="/">UNL · GeoAsist</Link><span>Asistencia académica</span></header><SessionProvider><main>{children}</main></SessionProvider></body></html>;
}
