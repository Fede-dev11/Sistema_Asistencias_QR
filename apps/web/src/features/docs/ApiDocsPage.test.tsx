import { cleanup, fireEvent, render, screen, within } from '@testing-library/react';
import { afterEach, describe, expect, it } from 'vitest';
import docs from '../../generated/api-docs.json';
import { ApiDocsPage } from './ApiDocsPage';

afterEach(cleanup);

describe('documentación de la API', () => {
  it('lista todas las operaciones del contrato agrupadas por funcionalidad', () => {
    render(<ApiDocsPage />);
    expect(screen.getByText(`${docs.ops.length} de ${docs.ops.length}`)).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Autenticación y cuenta' })).toBeInTheDocument();
    expect(screen.getAllByText('/auth/login').length).toBeGreaterThan(0);
  });

  it('agrupa por rol y la búsqueda filtra por ruta o campo', () => {
    render(<ApiDocsPage />);
    fireEvent.click(screen.getByRole('button', { name: 'Por rol' }));
    const admin = screen.getByRole('region', { name: 'Administrador' });
    expect(within(admin).getAllByText(/^\/admin\//).length).toBeGreaterThan(0);

    fireEvent.change(screen.getByLabelText('Buscar endpoint'), { target: { value: '/auth/login' } });
    expect(screen.getByText(`1 de ${docs.ops.length}`)).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText('Buscar endpoint'), { target: { value: 'zzz-sin-resultado' } });
    expect(screen.getByText(/Ningún endpoint coincide/)).toBeInTheDocument();
  });

  it('el filtro por método oculta los demás', () => {
    render(<ApiDocsPage />);
    for (const m of ['GET', 'PUT', 'PATCH', 'DELETE']) {
      const b = screen.queryByRole('button', { name: m });
      if (b) fireEvent.click(b);
    }
    const posts = docs.ops.filter((o) => o.method === 'POST').length;
    expect(screen.getByText(`${posts} de ${docs.ops.length}`)).toBeInTheDocument();
  });
});
