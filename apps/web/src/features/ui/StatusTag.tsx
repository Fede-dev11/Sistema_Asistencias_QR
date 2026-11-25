type Tone = 'ok' | 'info' | 'warn' | 'danger' | 'neutral';
const MAP: Record<string, [string, Tone, string]> = {
  PROGRAMADA: ['Programada', 'neutral', '○'], ACTIVA: ['Activa', 'info', '●'], ABIERTA: ['Abierta', 'info', '●'],
  CERRADA: ['Cerrada', 'ok', '✓'], CANCELADA: ['Cancelada', 'danger', '✕'],
  ACEPTADO: ['Aceptado', 'ok', '✓'], CONFIRMADA: ['Confirmada', 'ok', '✓'],
  RECHAZADO: ['Rechazado', 'danger', '✕'], RECHAZADA: ['Rechazada', 'danger', '✕'],
  PENDIENTE_REVISION: ['Pendiente de revisión', 'warn', '!'], AUSENTE: ['Ausente', 'danger', '✕'], JUSTIFICADA: ['Justificada', 'warn', '✓'],
  INACTIVA: ['Inactiva', 'danger', '✕'], SIN_CUENTA: ['Sin cuenta', 'neutral', '•'],
};

/** status: código del backend. Muestra icono + etiqueta en español; un código desconocido se muestra tal cual. */
export function StatusTag({ status }: { status: string }) {
  const [label, tone, icon] = MAP[status] ?? [status, 'neutral' as Tone, '•'];
  return <span className={`ui-tag ui-tag-${tone}`}><span aria-hidden="true">{icon}</span> {label}</span>;
}
