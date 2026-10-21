export type AttendanceState = 'CONFIRMADA' | 'REPETIR' | 'PENDIENTE_REVISION' | 'RECHAZADA';

export function AttendanceResult({ estado, mensaje }: { estado: AttendanceState; mensaje: string }) {
  const confirmed = estado === 'CONFIRMADA';
  return (
    <section className={`attendance-result ${confirmed ? 'confirmed' : 'attention'}`} role={confirmed ? 'status' : 'alert'}>
      <strong>{estado.replaceAll('_', ' ')}</strong>
      <p>{mensaje}</p>
    </section>
  );
}
