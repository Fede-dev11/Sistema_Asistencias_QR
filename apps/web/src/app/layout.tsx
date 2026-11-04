import Link from 'next/link';
import type { Metadata } from 'next';
import { Atkinson_Hyperlegible } from 'next/font/google';
import './globals.css';
const atkinson = Atkinson_Hyperlegible({ weight: ['400', '700'], subsets: ['latin'], display: 'swap', variable: '--font-atkinson' });
import { SessionProvider } from '../features/auth/SessionProvider';
import { RoleNav } from '../features/auth/RoleNav';
export const metadata: Metadata = { title: 'UNL GeoAsist', description: 'Asistencia académica con QR dinámico.' };
export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
 return <html lang="es" className={atkinson.variable}><body><div className="shell"><SessionProvider><header><Link href="/">UNL · GeoAsist</Link><RoleNav /></header><main>{children}</main></SessionProvider></div></body></html>;
}
