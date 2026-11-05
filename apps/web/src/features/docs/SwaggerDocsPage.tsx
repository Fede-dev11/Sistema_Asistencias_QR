'use client';
import { useEffect, useRef } from 'react';
import 'swagger-ui-dist/swagger-ui.css';
import { DocsNav } from './DocsNav';

const apiBase = (process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080/api/v1').replace(/\/$/, '');

// El contrato declara el servidor relativo `/api/v1`; "Try it out" se redirige al backend configurado.
const toApi = (req: { url: string }) => {
  const same = `${window.location.origin}/api/v1`;
  if (req.url.startsWith(same)) req.url = apiBase + req.url.slice(same.length);
  return req;
};

// swagger-ui-dist trae su propio React y se monta fuera del árbol de la app: sin avisos de modo estricto de la librería.
export function SwaggerDocsPage() {
  const node = useRef<HTMLDivElement>(null);
  useEffect(() => {
    let alive = true;
    void import('swagger-ui-dist/swagger-ui-es-bundle.js').then(({ default: SwaggerUIBundle }) => {
      if (alive && node.current) SwaggerUIBundle({ domNode: node.current, url: '/openapi.yaml', persistAuthorization: false, requestInterceptor: toApi });
    });
    return () => { alive = false; if (node.current) node.current.innerHTML = ''; }; // eslint-disable-line react-hooks/exhaustive-deps -- el nodo es fijo
  }, []);
  return (
    <div className="swagger-docs">
      <DocsNav />
      <div data-testid="swagger-container" ref={node} />
    </div>
  );
}
