import { cleanup, fireEvent, render, screen, within } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { ArchitecturePage } from './ArchitecturePage';

let search = new URLSearchParams();
vi.mock('next/navigation', () => ({
  usePathname: () => '/documentacion/arquitectura',
  useRouter: () => ({ replace: (url: string) => { search = new URLSearchParams(url.split('?')[1]); } }),
  useSearchParams: () => search,
}));

afterEach(() => { cleanup(); vi.unstubAllGlobals(); search = new URLSearchParams(); });

describe('ArchitecturePage', () => {
  it('muestra el C4 en la página Quarto y separa clases y modelo de datos', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: true, json: async () => ['actividades.svg', 'datos-auth.svg'] }));
    const { rerender } = render(<ArchitecturePage />);
    const frame = await screen.findByTitle('C4 · Contexto');
    expect(frame.getAttribute('src')).toBe('/arquitectura/01-contexto.html');
    expect(screen.getByRole('link', { name: 'Sitio Quarto completo' }).getAttribute('href')).toBe('/arquitectura/index.html');
    expect(screen.getByRole('link', { name: 'Arquitectura y diagramas' }).getAttribute('aria-current')).toBe('page');

    fireEvent.click(screen.getByRole('tab', { name: 'Clases (C4 · Código)' }));
    rerender(<ArchitecturePage />);
    const panel = screen.getByRole('tabpanel');
    expect(within(panel).getByAltText('Diagrama actividades')).toBeTruthy();
    expect(within(panel).queryByAltText('Diagrama datos auth')).toBeNull();
  });

  it('explica cómo publicar el sitio si no existe', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: false, json: async () => null }));
    render(<ArchitecturePage />);
    expect(await screen.findByText(/no está publicada en la web/)).toBeTruthy();
    expect(screen.queryByRole('tablist')).toBeNull();
  });
});
