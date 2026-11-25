import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { MethodSelect } from './MethodSelect';

afterEach(cleanup);

describe('selector de método', () => {
  it('advierte que NINGUNO no comprueba presencia', () => {
    const change = vi.fn();
    const { rerender } = render(<MethodSelect value="SOLO_QR" onChange={change} />);
    expect(screen.queryByRole('note')).toBeNull();
    fireEvent.change(screen.getByLabelText('Método de verificación'), { target: { value: 'NINGUNO' } });
    expect(change).toHaveBeenCalledWith('NINGUNO');
    rerender(<MethodSelect value="NINGUNO" onChange={change} />);
    expect(screen.getByRole('note')).toHaveTextContent('no comprueba presencia');
  });
});
