import Link from 'next/link';

/** items: [{label, href?}] en orden; el último es la página actual (aria-current="page", sin enlace). */
export function Breadcrumbs({ items }: { items: { label: string; href?: string }[] }) {
  return (
    <nav aria-label="Migas de pan" className="ui-crumbs">
      <ol>{items.map((it, i) => {
        const last = i === items.length - 1;
        return <li key={i}>{last || !it.href ? <span aria-current={last ? 'page' : undefined}>{it.label}</span> : <Link href={it.href}>{it.label}</Link>}</li>;
      })}</ol>
    </nav>
  );
}
