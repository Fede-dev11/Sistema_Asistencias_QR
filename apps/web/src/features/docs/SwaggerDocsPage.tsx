'use client';
import dynamic from 'next/dynamic';
import Link from 'next/link';
import 'swagger-ui-react/swagger-ui.css';

const SwaggerUI = dynamic(() => import('swagger-ui-react'), { ssr: false });
const apiBase = (process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080/api/v1').replace(/\/$/, '');

// El contrato declara el servidor relativo `/api/v1`; "Try it out" se redirige al backend configurado.
type Interceptor = NonNullable<React.ComponentProps<typeof SwaggerUI>['requestInterceptor']>;
const toApi: Interceptor = (req) => {
  const same = `${window.location.origin}/api/v1`;
  if (req.url.startsWith(same)) req.url = apiBase + req.url.slice(same.length);
  return req;
};

export function SwaggerDocsPage() {
  return (
    <main className="swagger-docs">
      <p><Link href="/documentacion">Documentación por rol</Link></p>
      <div data-testid="swagger-container">
        <SwaggerUI url="/openapi.yaml" persistAuthorization={false} requestInterceptor={toApi} />
      </div>
    </main>
  );
}
