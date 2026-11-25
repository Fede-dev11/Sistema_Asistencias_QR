import { notFound } from 'next/navigation';
import { ParticipantSimulator } from '../../../features/testing/ParticipantSimulator';

export default function ParticipantTestPage() {
  if (process.env.NEXT_PUBLIC_HERRAMIENTAS_PRUEBA !== 'true') notFound();
  return <ParticipantSimulator />;
}
