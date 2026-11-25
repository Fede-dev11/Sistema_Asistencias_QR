import { cleanup, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it } from 'vitest';
import { ApiError } from '../../lib/api/client';
import { ProblemMessage } from './ProblemMessage';

afterEach(cleanup);
describe('ProblemMessage', () => {
  it('muestra code y mensaje de ApiError', () => {
    render(<ProblemMessage error={new ApiError({ type: 'u', title: 't', status: 409, code: 'CONFLICTO', mensaje: 'Ya existe.' }, 409)} />);
    expect(screen.getByRole('alert')).toHaveTextContent('CONFLICTO: Ya existe.');
  });
  it('muestra el mensaje de otros errores y nada sin error', () => {
    const { rerender, container } = render(<ProblemMessage error={new Error('Sin red.')} />);
    expect(screen.getByRole('alert')).toHaveTextContent('Sin red.');
    rerender(<ProblemMessage error={null} />); expect(container).toBeEmptyDOMElement();
  });
});
