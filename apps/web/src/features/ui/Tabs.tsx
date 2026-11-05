'use client';
import { useId, useRef, useState, type KeyboardEvent, type ReactNode } from 'react';
import { usePathname, useRouter, useSearchParams } from 'next/navigation';

type Tab = { id: string; label: string; content: ReactNode };

/** tabs: [{id,label,content}]; param: clave de searchParams donde se guarda la pestaña activa (sin param, estado local); initial: id inicial. Flechas, Inicio y Fin cambian de pestaña. */
export function Tabs({ tabs, param, initial }: { tabs: Tab[]; param?: string; initial?: string }) {
  const base = useId();
  const router = useRouter(); const pathname = usePathname(); const search = useSearchParams();
  const [local, setLocal] = useState(initial ?? tabs[0]?.id);
  const fromUrl = param ? search?.get(param) : null;
  const active = tabs.find((t) => t.id === (fromUrl ?? local))?.id ?? tabs[0]?.id;
  const refs = useRef<Record<string, HTMLButtonElement | null>>({});
  const select = (id: string) => {
    setLocal(id);
    if (param) { const q = new URLSearchParams(search?.toString()); q.set(param, id); router.replace(`${pathname}?${q}`, { scroll: false }); }
  };
  const onKey = (e: KeyboardEvent) => {
    const i = tabs.findIndex((t) => t.id === active);
    const n = e.key === 'ArrowRight' ? (i + 1) % tabs.length : e.key === 'ArrowLeft' ? (i - 1 + tabs.length) % tabs.length
      : e.key === 'Home' ? 0 : e.key === 'End' ? tabs.length - 1 : -1;
    if (n < 0) return;
    e.preventDefault(); select(tabs[n].id); refs.current[tabs[n].id]?.focus();
  };
  return (
    <div className="ui-tabs">
      <div role="tablist" onKeyDown={onKey}>
        {tabs.map((t) => (
          <button key={t.id} ref={(el) => { refs.current[t.id] = el; }} role="tab" type="button" id={`${base}-t-${t.id}`}
            aria-selected={t.id === active} aria-controls={`${base}-p-${t.id}`} tabIndex={t.id === active ? 0 : -1} onClick={() => select(t.id)}>{t.label}</button>
        ))}
      </div>
      {tabs.map((t) => t.id === active && (
        <div key={t.id} role="tabpanel" id={`${base}-p-${t.id}`} aria-labelledby={`${base}-t-${t.id}`} tabIndex={0}>{t.content}</div>
      ))}
    </div>
  );
}
