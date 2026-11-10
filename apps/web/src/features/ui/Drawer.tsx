import { useCallback, useEffect, useRef, type ReactNode } from 'react';

export function Drawer({ title, open = false, onClose, children }: { title: string; open?: boolean; onClose?: () => void; children: ReactNode }) {
  const backdropRef = useRef<HTMLDivElement>(null);
  const prev = useRef<HTMLElement | null>(null);
  const handleClose = useCallback(() => { onClose?.(); }, [onClose]);
  useEffect(() => {
    if (!open) return;
    prev.current = document.activeElement as HTMLElement;
    backdropRef.current?.focus();
    return () => { prev.current?.focus(); };
  }, [open]);
  if (!open) return null;
  return (
    <div ref={backdropRef} className="ui-drawer-backdrop" role="dialog" aria-modal="true" aria-label={title} onClick={(e) => { if (e.target === backdropRef.current) handleClose(); }} onKeyDown={(e) => { if (e.key === 'Escape') handleClose(); }}>
      <div className="ui-drawer">
        <div className="ui-drawer-head"><h2>{title}</h2><button type="button" onClick={handleClose} className="ui-btn ui-btn-text">Cerrar</button></div>
        {children}
      </div>
    </div>
  );
}
