import type { ReactNode } from 'react';

/** title: encabezado; text: explicación; action: nodo opcional (p. ej. ButtonLink) con el siguiente paso. */
export function EmptyState({ title, text, action }: { title: string; text?: string; action?: ReactNode }) {
  return <div className="ui-empty"><h2>{title}</h2>{text && <p>{text}</p>}{action}</div>;
}
