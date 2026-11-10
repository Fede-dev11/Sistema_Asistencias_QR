type Item = { label: string; href?: string };
export function Breadcrumbs({ items }: { items: Item[] }) {
  return <nav aria-label="Migas de pan" className="ui-crumbs"><ol>
    {items.map((item, i) => <li key={i || item.label}><a aria-current={i === items.length - 1 ? 'page' : undefined} href={item.href}>{item.label}</a></li>)}
  </ol></nav>;
}
