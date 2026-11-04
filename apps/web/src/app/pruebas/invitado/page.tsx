import { notFound } from 'next/navigation';
import { GuestSimulator } from '../../../features/testing/GuestSimulator';

export default function GuestTestPage() {
  if (process.env.NEXT_PUBLIC_HERRAMIENTAS_PRUEBA !== 'true') notFound();
  return <GuestSimulator />;
}
