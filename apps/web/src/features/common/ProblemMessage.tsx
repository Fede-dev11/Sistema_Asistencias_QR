import { ApiError } from '../../lib/api/client';

/** Muestra el `code` y el mensaje de un error de la API (o el mensaje de cualquier otro error). */
export function ProblemMessage({ error }: { error: unknown }) {
  if (!error) return null;
  if (error instanceof ApiError) {
    const problem = error.problem;
    const format = (value: string) => new Date(value).toLocaleString('es-EC', { timeZone: 'America/Guayaquil', dateStyle: 'medium', timeStyle: 'short' });
    return <div role="alert">
      <p><strong>{problem.code}</strong>: {problem.mensaje}</p>
      {problem.horario_inicio && problem.horario_fin && <p>Horario permitido: {format(problem.horario_inicio)} – {format(problem.horario_fin)} (hora de Ecuador; fin excluido).</p>}
      <p>Acción recomendada: {problem.accion_recomendada ?? 'Revisa los datos y vuelve a intentarlo; si persiste, contacta al responsable.'}</p>
    </div>;
  }
  return <div role="alert"><p><strong>OPERACION_FALLIDA</strong>: {error instanceof Error ? error.message : 'No se pudo completar la operación.'}</p><p>Acción recomendada: Revisa la conexión y vuelve a intentarlo; si persiste, contacta al responsable.</p></div>;
}
