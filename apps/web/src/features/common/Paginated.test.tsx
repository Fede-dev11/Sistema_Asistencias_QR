import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { Paginated } from './Paginated';

afterEach(cleanup);
const base = { total: 45, size: 20, renderItem: (s: string) => s };
describe('Paginated', () => {
  it('pagina con Anterior/Siguiente y muestra el total', () => {
    const onPage = vi.fn();
    render(<Paginated {...base} items={['a', 'b']} page={1} onPage={onPage} />);
    expect(screen.getByText(/45 en total · página 2 de 3/)).toBeTruthy();
    fireEvent.click(screen.getByRole('button', { name: 'Anterior' })); expect(onPage).toHaveBeenCalledWith(0);
    fireEvent.click(screen.getByRole('button', { name: 'Siguiente' })); expect(onPage).toHaveBeenCalledWith(2);
  });
  it('deshabilita los extremos, y muestra carga y vacío', () => {
    const { rerender } = render(<Paginated {...base} items={['a']} page={0} onPage={vi.fn()} />);
    expect(screen.getByRole('button', { name: 'Anterior' })).toBeDisabled();
    rerender(<Paginated {...base} items={null} page={0} onPage={vi.fn()} />); expect(screen.getByRole('status')).toHaveTextContent('Cargando');
    rerender(<Paginated {...base} total={0} items={[]} page={0} onPage={vi.fn()} empty="Nada." />); expect(screen.getByText('Nada.')).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Siguiente' })).toBeDisabled();
  });
});
