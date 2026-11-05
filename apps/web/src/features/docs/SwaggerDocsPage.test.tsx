import { cleanup, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';

vi.mock('swagger-ui-react/swagger-ui.css', () => ({}));
vi.mock('swagger-ui-react', () => ({ default: ({ url }: { url: string }) => <div data-testid="swagger-mock">{url}</div> }));

import { SwaggerDocsPage } from './SwaggerDocsPage';

afterEach(cleanup);

describe('Swagger UI', () => {
  it('monta el contenedor con el contrato público y enlaza a la documentación por rol', async () => {
    render(<SwaggerDocsPage />);
    expect(screen.getByTestId('swagger-container')).toBeInTheDocument();
    expect(await screen.findByTestId('swagger-mock')).toHaveTextContent('/openapi.yaml');
    expect(screen.getByRole('link', { name: 'Documentación por rol' })).toHaveAttribute('href', '/documentacion');
  });
});
