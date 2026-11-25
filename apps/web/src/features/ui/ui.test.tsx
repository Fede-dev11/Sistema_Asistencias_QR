import { useState } from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { Button, ButtonLink } from './Button';
import { StatusTag } from './StatusTag';
import { EmptyState } from './EmptyState';
import { ConfirmInline } from './ConfirmInline';
import { Breadcrumbs } from './Breadcrumbs';
import { Tabs } from './Tabs';
import { Drawer } from './Drawer';
import { Stepper } from './Stepper';

afterEach(cleanup);
const replace = vi.fn();
let query = '';
vi.mock('next/navigation', () => ({
  useRouter: () => ({ replace }),
  usePathname: () => '/x',
  useSearchParams: () => new URLSearchParams(query),
}));

describe('Button', () => {
  it('aplica variante y reenvía props nativas', () => {
    const onClick = vi.fn();
    render(<Button variant="danger" onClick={onClick}>Borrar</Button>);
    const b = screen.getByRole('button', { name: 'Borrar' });
    expect(b).toHaveClass('ui-btn-danger');
    fireEvent.click(b);
    expect(onClick).toHaveBeenCalled();
  });
  it('loading deshabilita y marca aria-busy', () => {
    render(<Button loading>Guardando…</Button>);
    const b = screen.getByRole('button', { name: 'Guardando…' });
    expect(b).toBeDisabled();
    expect(b).toHaveAttribute('aria-busy', 'true');
  });
  it('ButtonLink es un enlace con la variante', () => {
    render(<ButtonLink href="/a" variant="secondary">Ir</ButtonLink>);
    expect(screen.getByRole('link', { name: 'Ir' })).toHaveClass('ui-btn-secondary');
  });
});

describe('StatusTag, EmptyState, Breadcrumbs', () => {
  it('StatusTag muestra siempre el texto', () => {
    render(<><StatusTag status="PENDIENTE_REVISION" /><StatusTag status="ACTIVA" /><StatusTag status="RARO" /></>);
    expect(screen.getByText(/Pendiente de revisión/)).toBeInTheDocument();
    expect(screen.getByText(/Activa/)).toBeInTheDocument();
    expect(screen.getByText(/RARO/)).toBeInTheDocument();
  });
  it('EmptyState con acción', () => {
    render(<EmptyState title="Sin datos" text="Nada aún" action={<button>Crear</button>} />);
    expect(screen.getByRole('heading', { name: 'Sin datos' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Crear' })).toBeInTheDocument();
  });
  it('Breadcrumbs marca la página actual', () => {
    render(<Breadcrumbs items={[{ label: 'Inicio', href: '/' }, { label: 'Actividad' }]} />);
    expect(screen.getByRole('navigation', { name: 'Migas de pan' })).toBeInTheDocument();
    expect(screen.getByText('Actividad')).toHaveAttribute('aria-current', 'page');
    expect(screen.getByRole('link', { name: 'Inicio' })).toBeInTheDocument();
  });
});

describe('ConfirmInline', () => {
  it('enfoca la fila, confirma y Escape cancela', () => {
    const onConfirm = vi.fn(), onCancel = vi.fn();
    render(<ConfirmInline message="¿Cancelar la actividad?" onConfirm={onConfirm} onCancel={onCancel} />);
    const row = screen.getByRole('group', { name: '¿Cancelar la actividad?' });
    expect(row).toHaveFocus();
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar' }));
    expect(onConfirm).toHaveBeenCalled();
    fireEvent.keyDown(row, { key: 'Escape' });
    expect(onCancel).toHaveBeenCalledTimes(1);
  });
});

describe('Tabs', () => {
  const tabs = [{ id: 'a', label: 'A', content: 'panel A' }, { id: 'b', label: 'B', content: 'panel B' }, { id: 'c', label: 'C', content: 'panel C' }];
  it('flechas cambian y enfocan la pestaña, con vuelta circular', () => {
    render(<Tabs tabs={tabs} />);
    const list = screen.getByRole('tablist');
    fireEvent.keyDown(list, { key: 'ArrowRight' });
    expect(screen.getByRole('tab', { name: 'B' })).toHaveAttribute('aria-selected', 'true');
    expect(screen.getByRole('tab', { name: 'B' })).toHaveFocus();
    expect(screen.getByRole('tabpanel')).toHaveTextContent('panel B');
    fireEvent.keyDown(list, { key: 'ArrowLeft' });
    fireEvent.keyDown(list, { key: 'ArrowLeft' });
    expect(screen.getByRole('tab', { name: 'C' })).toHaveAttribute('aria-selected', 'true');
  });
  it('sincroniza con searchParams', () => {
    query = 'tab=b';
    render(<Tabs tabs={tabs} param="tab" />);
    expect(screen.getByRole('tabpanel')).toHaveTextContent('panel B');
    fireEvent.click(screen.getByRole('tab', { name: 'C' }));
    expect(replace).toHaveBeenCalledWith('/x?tab=c', { scroll: false });
    query = '';
  });
});

describe('Drawer', () => {
  function Host({ onClosed }: { onClosed?: () => void }) {
    const [open, setOpen] = useState(false);
    return <><button onClick={() => setOpen(true)}>Abrir</button>
      {open && <Drawer title="Detalle" onClose={() => { setOpen(false); onClosed?.(); }}><input aria-label="campo" /></Drawer>}</>;
  }
  it('es diálogo modal etiquetado, atrapa el foco y devuelve el foco', () => {
    render(<Host />);
    const opener = screen.getByRole('button', { name: 'Abrir' });
    opener.focus();
    fireEvent.click(opener);
    expect(screen.getByRole('dialog', { name: 'Detalle' })).toHaveAttribute('aria-modal', 'true');
    const close = screen.getByRole('button', { name: 'Cerrar' });
    const input = screen.getByLabelText('campo');
    expect(close).toHaveFocus();
    fireEvent.keyDown(close, { key: 'Tab', shiftKey: true }); // desde el primero salta al último
    expect(input).toHaveFocus();
    input.focus();
    fireEvent.keyDown(input, { key: 'Tab' }); // desde el último vuelve al primero
    expect(close).toHaveFocus();
    fireEvent.keyDown(close, { key: 'Escape' });
    expect(screen.queryByRole('dialog')).toBeNull();
    expect(opener).toHaveFocus();
  });
});

describe('Stepper', () => {
  it('marca el paso actual y navega', () => {
    const onNext = vi.fn(), onPrev = vi.fn();
    const { rerender } = render(<Stepper steps={['Datos', 'Fechas', 'Revisar']} current={0} onPrev={onPrev} onNext={onNext} />);
    expect(screen.getByText(/Datos/).closest('li')).toHaveAttribute('aria-current', 'step');
    expect(screen.queryByRole('button', { name: 'Anterior' })).toBeNull();
    fireEvent.click(screen.getByRole('button', { name: 'Siguiente' }));
    expect(onNext).toHaveBeenCalled();
    rerender(<Stepper steps={['Datos', 'Fechas', 'Revisar']} current={2} onPrev={onPrev} onNext={onNext} />);
    expect(screen.queryByRole('button', { name: 'Siguiente' })).toBeNull();
    fireEvent.click(screen.getByRole('button', { name: 'Anterior' }));
    expect(onPrev).toHaveBeenCalled();
  });
});
