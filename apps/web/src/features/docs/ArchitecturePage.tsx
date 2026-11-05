'use client';
import { useEffect, useState } from 'react';
import { EmptyState } from '../ui/EmptyState';
import { Tabs } from '../ui/Tabs';
import { DocsNav } from './DocsNav';

const BASE = '/arquitectura';
// Páginas Quarto con diagramas C4 en Mermaid: se muestran tal cual (se dibujan en el navegador).
const PAGES = [
  { id: 'contexto', label: 'C4 · Contexto', file: '01-contexto.html' },
  { id: 'contenedores', label: 'C4 · Contenedores', file: '02-contenedores.html' },
  { id: 'componentes', label: 'C4 · Componentes', file: '03-componentes.html' },
  { id: 'modulos', label: 'Módulos', file: 'modulos/index.html' },
];

const title = (svg: string) => svg.replace(/\.svg$/, '').replace(/-/g, ' ');

function Gallery({ files }: { files: string[] }) {
  if (!files.length) return <p>No hay diagramas en esta sección.</p>;
  return (
    <div className="arq-gallery">
      {files.map(f => (
        <figure key={f}>
          {/* eslint-disable-next-line @next/next/no-img-element -- SVG estático generado por PlantUML; next/image no aporta nada aquí */}
          <a href={`${BASE}/_generated/${f}`} target="_blank" rel="noopener"><img src={`${BASE}/_generated/${f}`} alt={`Diagrama ${title(f)}`} loading="lazy" /></a>
          <figcaption>{title(f)}</figcaption>
        </figure>
      ))}
    </div>
  );
}

export function ArchitecturePage() {
  const [svgs, setSvgs] = useState<string[] | null | undefined>(undefined); // undefined: cargando; null: sitio no publicado
  useEffect(() => {
    let alive = true;
    fetch(`${BASE}/diagramas.json`).then(r => (r.ok ? r.json() : null)).then(d => { if (alive) setSvgs(Array.isArray(d) ? d : null); }, () => { if (alive) setSvgs(null); });
    return () => { alive = false; };
  }, []);

  const frame = (p: (typeof PAGES)[number]) => (
    <>
      <p><a href={`${BASE}/${p.file}`} target="_blank" rel="noopener">Abrir en el sitio Quarto</a></p>
      <iframe className="arq-frame" src={`${BASE}/${p.file}`} title={p.label} loading="lazy" />
    </>
  );

  return (
    <div className="docs">
      <DocsNav />
      <h1>Arquitectura y diagramas</h1>
      <p>Diagramas C4, clases de cada módulo y modelo de datos, tomados del sitio Quarto de <code>docs/arquitectura</code>.</p>
      {svgs === undefined && <p role="status">Cargando diagramas…</p>}
      {svgs === null && <EmptyState title="La documentación de arquitectura no está publicada en la web" text="Ejecuta npm run docs:arquitectura en apps/web (requiere Quarto, Python y Java) y recarga la página." />}
      {svgs && (
        <Tabs param="vista" tabs={[
          ...PAGES.slice(0, 3).map(p => ({ id: p.id, label: p.label, content: frame(p) })),
          { id: 'clases', label: 'Clases (C4 · Código)', content: <Gallery files={svgs.filter(f => !f.startsWith('datos-'))} /> },
          { id: 'datos', label: 'Modelo de datos', content: <Gallery files={svgs.filter(f => f.startsWith('datos-'))} /> },
          { id: PAGES[3].id, label: PAGES[3].label, content: frame(PAGES[3]) },
        ]} />
      )}
    </div>
  );
}
