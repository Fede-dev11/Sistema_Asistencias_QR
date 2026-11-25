'use client';
import { useEffect, useRef } from 'react';
import { Button } from './Button';

/** Fila de confirmación en la página (render condicional del padre). message: pregunta; onConfirm/onCancel: callbacks; confirmLabel: def. "Confirmar"; danger: confirmar en rojo. Foco al montar; Escape cancela. */
export function ConfirmInline({ message, onConfirm, onCancel, confirmLabel = 'Confirmar', danger = false }:
  { message: string; onConfirm: () => void; onCancel: () => void; confirmLabel?: string; danger?: boolean }) {
  const ref = useRef<HTMLDivElement>(null);
  useEffect(() => { ref.current?.focus(); }, []);
  return (
    <div ref={ref} tabIndex={-1} role="group" aria-label={message} className="ui-confirm"
      onKeyDown={(e) => { if (e.key === 'Escape') { e.stopPropagation(); onCancel(); } }}>
      <span>{message}</span>
      <Button variant={danger ? 'danger' : 'primary'} onClick={onConfirm}>{confirmLabel}</Button>
      <Button variant="secondary" onClick={onCancel}>Cancelar</Button>
    </div>
  );
}
