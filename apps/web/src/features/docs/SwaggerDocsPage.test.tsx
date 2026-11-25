import { cleanup, render, screen, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';

const { bundle } = vi.hoisted(() => ({ bundle: vi.fn() })); // vi.mock se eleva al inicio del archivo
vi.mock('swagger-ui-dist/swagger-ui.css', () => ({}));
vi.mock('swagger-ui-dist/swagger-ui-es-bundle.js', () => ({ default: bundle }));
vi.mock('next/navigation', () => ({ usePathname: () => '/documentacion/swagger' }));

import { SwaggerDocsPage } from './SwaggerDocsPage';

afterEach(cleanup);

describe('Swagger UI', () => {
  it('monta Swagger con el contrato público, redirige "Try it out" a la API y enlaza las demás secciones', async () => {
    render(<SwaggerDocsPage />);
    const container = screen.getByTestId('swagger-container');
    await waitFor(() => expect(bundle).toHaveBeenCalled());
    const options = bundle.mock.calls[0][0];
    expect(options.domNode).toBe(container);
    expect(options.url).toBe('/openapi.yaml');
    expect(options.persistAuthorization).toBe(false);
    expect(options.requestInterceptor({ url: `${window.location.origin}/api/v1/me` }).url).toBe('http://localhost:8080/api/v1/me');
    expect(screen.getByRole('link', { name: 'API por rol' })).toHaveAttribute('href', '/documentacion');
    expect(screen.getByRole('link', { name: 'Sitio Quarto completo' })).toHaveAttribute('href', '/arquitectura/index.html');
  });
});
