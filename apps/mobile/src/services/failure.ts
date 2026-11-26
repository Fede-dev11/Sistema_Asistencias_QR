import type { Problem } from './api';

export type Failure = { message: string; code: string; action: string; schedule?: string };

export function describeFailure(error: unknown): Failure {
  const source = typeof error === 'object' && error !== null ? error : {};
  const problem = 'problem' in source && typeof source.problem === 'object' && source.problem !== null
    ? source.problem as Problem : null;
  const format = (value: string) => new Date(value).toLocaleString('es-EC', { timeZone: 'America/Guayaquil' });
  return {
    message: problem?.mensaje ?? (error instanceof Error ? error.message : 'No se pudo completar la operación.'),
    code: problem?.code ?? ('code' in source && typeof source.code === 'string' ? source.code : 'OPERACION_FALLIDA'),
    action: problem?.accion_recomendada ?? ('action' in source && typeof source.action === 'string' ? source.action : 'Revisa la conexión y vuelve a intentarlo; si persiste, contacta al responsable.'),
    schedule: problem?.horario_inicio && problem.horario_fin
      ? `Horario permitido: ${format(problem.horario_inicio)} – ${format(problem.horario_fin)} (hora de Ecuador; fin excluido).` : undefined,
  };
}

export function failureText(error: unknown): string {
  const value = describeFailure(error);
  return [`${value.code}: ${value.message}`, value.schedule, `Acción recomendada: ${value.action}`].filter(Boolean).join('\n');
}
