import { ApiError } from '../../lib/api/client';

/** Muestra el `code` y el mensaje de un error de la API (o el mensaje de cualquier otro error). */
export function ProblemMessage({ error }: { error: unknown }) {
  if (!error) return null;
  if (error instanceof ApiError) return <p role="alert"><strong>{error.problem.code}</strong>: {error.problem.mensaje}</p>;
  return <p role="alert">{error instanceof Error ? error.message : 'No se pudo completar la operación.'}</p>;
}
