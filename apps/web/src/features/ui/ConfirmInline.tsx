import { useEffect, useRef } from 'react';

/** Mensaje de confirmación integrado: foco en el grupo, Enter/espacio confirma, Escape cancela. */
export function ConfirmInline({ message, onConfirm, onCancel }: { message: string; onConfirm: () => void; onCancel: () => void }) {
  const groupRef = useRef<HTMLDivElement>(null);
  useEffect(() => { if (groupRef.current) groupRef.current.focus(); }, []);
  return (
    <div ref={groupRef} role="group" aria-label={message} tabIndex={-1} className="ui-confirm" onKeyDown={(e) => {
      if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); onConfirm(); }
      if (e.key === 'Escape') { onCancel(); }
    }}>
      <span>{message}</span>
      <button type="button" onClick={onConfirm} className="ui-btn ui-btn-danger">Confirmar</button>
      <button type="button" onClick={onCancel} className="ui-btn ui-btn-text">Cancelar</button>
    </div>
  );
}
