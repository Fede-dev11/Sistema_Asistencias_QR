'use client';
import { useEffect, useId, useRef, type KeyboardEvent, type ReactNode } from 'react';
import { Button } from './Button';

const FOCUSABLE = 'a[href],button:not([disabled]),input:not([disabled]),select:not([disabled]),textarea:not([disabled]),[tabindex]:not([tabindex="-1"])';

/** Panel lateral modal (render condicional del padre: `{open && <Drawer/>}`). title: nombre accesible; onClose: Escape o botón Cerrar. Foco atrapado y devuelto al elemento que lo abrió. */
export function Drawer({ title, onClose, children }: { title: string; onClose: () => void; children: ReactNode }) {
  const titleId = useId();
  const ref = useRef<HTMLDivElement>(null);
  useEffect(() => {
    const opener = document.activeElement as HTMLElement | null;
    ref.current?.querySelector<HTMLElement>(FOCUSABLE)?.focus();
    return () => opener?.focus();
  }, []);
  const onKey = (e: KeyboardEvent) => {
    if (e.key === 'Escape') { e.stopPropagation(); onClose(); return; }
    if (e.key !== 'Tab') return;
    const f = [...ref.current!.querySelectorAll<HTMLElement>(FOCUSABLE)];
    const first = f[0], last = f[f.length - 1];
    if (e.shiftKey && document.activeElement === first) { e.preventDefault(); last.focus(); }
    else if (!e.shiftKey && document.activeElement === last) { e.preventDefault(); first.focus(); }
  };
  return (
    <div className="ui-drawer-backdrop">
      <div ref={ref} role="dialog" aria-modal="true" aria-labelledby={titleId} className="ui-drawer" onKeyDown={onKey}>
        <div className="ui-drawer-head"><h2 id={titleId}>{title}</h2><Button variant="text" onClick={onClose}>Cerrar</Button></div>
        {children}
      </div>
    </div>
  );
}
