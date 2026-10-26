'use client';
import { useMemo, useState } from 'react';
import docs from '../../generated/api-docs.json';

type Op = (typeof docs.ops)[number];
type Field = { name: string; type: string; required: boolean; desc: string; rules: string; in?: string };
const METHODS = ['GET', 'POST', 'PUT', 'PATCH', 'DELETE'] as const;
const roleLabel = Object.fromEntries(docs.roles.map((r) => [r.key, r.label]));
const searchText = new Map(
  docs.ops.map((o) => [o, [o.method, o.path, o.summary, o.desc, o.note ?? '', ...o.roles.map((r) => roleLabel[r]),
    ...o.params.map((p) => p.name), ...(o.body?.fields.map((f) => `${f.name} ${f.desc}`) ?? [])].join(' ').toLowerCase()]),
);

function Fields({ rows, title, showIn }: { rows: Field[]; title: string; showIn?: boolean }) {
  return (
    <div className="docs-table">
      <table>
        <caption className="docs-caption">{title}</caption>
        <thead><tr><th scope="col">Nombre</th><th scope="col">Tipo</th><th scope="col">Detalle</th></tr></thead>
        <tbody>
          {rows.map((f) => (
            <tr key={`${f.in ?? ''}${f.name}`}>
              <td className="docs-name">{f.name}{f.required && <abbr title="obligatorio" className="docs-req"> *</abbr>}{showIn && <small>{f.in}</small>}</td>
              <td className="docs-type">{f.type}</td>
              <td>{f.desc}{f.rules && <span className="docs-rules">{f.rules}</span>}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function Operation({ op, open }: { op: Op; open: boolean }) {
  return (
    <details className="docs-op" open={open || undefined}>
      <summary>
        <span className={`docs-method docs-${op.method.toLowerCase()}`}>{op.method}</span>
        <code className="docs-path">{op.path}</code>
        <span className="docs-summary">{op.summary}
          <span className="docs-roles">{op.roles.map((r) => <span key={r} className="docs-role">{roleLabel[r]}</span>)}</span>
        </span>
      </summary>
      <div className="docs-body">
        {op.desc && <p>{op.desc}</p>}
        {op.note && <p className="docs-note">{op.note}</p>}
        {op.params.length > 0 && <Fields rows={op.params} title="Parámetros" showIn />}
        {op.body && (op.body.fields.length > 0
          ? <Fields rows={op.body.fields} title={`Cuerpo · ${op.body.contentType}${op.body.schema ? ` · ${op.body.schema}` : ''}`} />
          : <p>Cuerpo {op.body.contentType} sin campos declarados.</p>)}
        <dl className="docs-responses">
          {op.responses.map((r) => (
            <div key={r.code}><dt className={Number(r.code) < 400 ? 'docs-ok' : 'docs-err'}>{r.code}</dt><dd>{r.desc}{r.schema && <> <code>{r.schema}</code></>}</dd></div>
          ))}
        </dl>
      </div>
    </details>
  );
}

export function ApiDocsPage() {
  const [view, setView] = useState<'group' | 'role'>('group');
  const [query, setQuery] = useState('');
  const [methods, setMethods] = useState<Set<string>>(new Set(METHODS));

  const visible = useMemo(() => {
    const terms = query.trim().toLowerCase().split(/\s+/).filter(Boolean);
    return docs.ops.filter((o) => methods.has(o.method) && terms.every((t) => searchText.get(o)!.includes(t)));
  }, [query, methods]);
  const sections = (view === 'group'
    ? docs.groups.map((g) => ({ ...g, ops: visible.filter((o) => o.group === g.key) }))
    : docs.roles.map((r) => ({ ...r, ops: visible.filter((o) => o.roles.includes(r.key)) }))).filter((s) => s.ops.length > 0);
  const expand = query.trim() !== '' && visible.length <= 6;

  function toggleMethod(m: string) {
    const next = new Set(methods);
    if (next.has(m)) next.delete(m); else next.add(m);
    setMethods(next.size ? next : new Set(METHODS));
  }

  return (
    <section className="docs">
      <p className="eyebrow">REST {docs.server} · contrato {docs.version}</p>
      <h1>Documentación de la API</h1>
      <p>{docs.ops.length} operaciones generadas desde el contrato OpenAPI que valida el CI. Envía el token de <code>POST /auth/login</code> como <code>Authorization: Bearer …</code>; los <code>POST</code> que crean registros aceptan <code>Idempotency-Key</code>; los errores llegan en formato Problem Details con un <code>code</code> estable.</p>

      <div className="docs-bar">
        <div className="docs-seg" role="group" aria-label="Agrupar por">
          <button type="button" aria-pressed={view === 'group'} onClick={() => setView('group')}>Por funcionalidad</button>
          <button type="button" aria-pressed={view === 'role'} onClick={() => setView('role')}>Por rol</button>
        </div>
        <input type="search" aria-label="Buscar endpoint" placeholder="Buscar ruta, acción o campo (ej. cedula, qr, csv)" value={query} onChange={(e) => setQuery(e.target.value)} />
        <div className="docs-filter" role="group" aria-label="Filtrar por método">
          {METHODS.filter((m) => docs.ops.some((o) => o.method === m)).map((m) => (
            <button key={m} type="button" aria-pressed={methods.has(m)} className={`docs-method docs-${m.toLowerCase()}`} onClick={() => toggleMethod(m)}>{m}</button>
          ))}
        </div>
        <span className="docs-count" aria-live="polite">{visible.length} de {docs.ops.length}</span>
      </div>

      <div className="docs-layout">
        <ul className="docs-toc" aria-label="Secciones">
          {sections.map((s) => <li key={s.key}><a href={`#docs-${s.key}`}>{s.label}<span>{s.ops.length}</span></a></li>)}
        </ul>
        <div className="docs-list">
          {sections.length === 0 && <p className="docs-empty">Ningún endpoint coincide con “{query}”. Prueba otra palabra o activa más métodos.</p>}
          {sections.map((s) => (
            <section key={s.key} id={`docs-${s.key}`} className="docs-group" aria-labelledby={`docs-h-${s.key}`}>
              <h2 id={`docs-h-${s.key}`}>{s.label}</h2>
              <p>{s.desc}</p>
              {s.ops.map((op) => <Operation key={op.id} op={op} open={expand} />)}
            </section>
          ))}
          <section className="docs-group" aria-labelledby="docs-h-codes">
            <h2 id="docs-h-codes">Códigos de error</h2>
            <p>Valores de <code>code</code> que aparecen en el contrato. Decide qué mostrar al usuario según el código, no según el texto.</p>
            <ul className="docs-codes">{docs.codes.map((c) => <li key={c}><code>{c}</code></li>)}</ul>
          </section>
        </div>
      </div>
    </section>
  );
}
