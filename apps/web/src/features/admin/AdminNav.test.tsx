import { cleanup, render, screen, within } from '@testing-library/react';
import { afterEach, expect, it, vi } from 'vitest';
import { AdminNav } from './AdminNav';

vi.mock('next/navigation', () => ({ usePathname: () => '/admin/espacios' }));
afterEach(cleanup);

it('permite volver al inicio y distingue la sección administrativa actual', () => {
  render(<AdminNav />);
  expect(screen.getByRole('link', { name: /Volver al inicio/ })).toHaveAttribute('href', '/inicio');
  const nav = within(screen.getByRole('navigation', { name: 'Secciones de administración' }));
  expect(nav.getByRole('link', { name: 'Espacios y geocercas' })).toHaveAttribute('aria-current', 'page');
  expect(nav.getByRole('link', { name: 'Personas y cuentas' })).toHaveAttribute('href', '/admin/personas');
  expect(nav.getByRole('link', { name: 'Carreras y paralelos' })).toHaveAttribute('href', '/admin/carreras');
  expect(nav.getByRole('link', { name: 'Importar listas' })).toHaveAttribute('href', '/admin/importar');
});
