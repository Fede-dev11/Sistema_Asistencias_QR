import { useState, type ReactNode } from 'react';
import { useRouter, useSearchParams, usePathname } from 'next/navigation';

type Tab = { id: string; label: string; content: ReactNode };

export function Tabs({ tabs, param }: { tabs: Tab[]; param?: string }) {
  const router = useRouter(); const searchParams = useSearchParams(); const pathname = usePathname();
  const [active, setActive] = useState(() => {
    if (param) return searchParams.get(param) ?? tabs[0]?.id ?? '';
    return tabs[0]?.id ?? '';
  });
  const select = (id: string) => { setActive(id); if (param && router) void router.replace(`${pathname}?${param}=${id}`, { scroll: false }); };
  const tab = tabs.find(t => t.id === active) ?? tabs[0];
  return <div className="ui-tabs"><div role="tablist">
    {tabs.map(t => <button key={t.id} role="tab" aria-selected={t.id === active} aria-controls={`tab-${t.id}`} onClick={() => select(t.id)}>{t.label}</button>)}
  </div>{tab && <div id={`tab-${tab.id}`} role="tabpanel">{tab.content}</div>}</div>;
}
