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
  it('conserva horario permitido y acción recomendada del servidor', () => {
    render(<ProblemMessage error={new ApiError({ type: 'u', title: 'Horario', status: 409, code: 'FUERA_DE_HORARIO', mensaje: 'No puedes iniciar ahora.',
      horario_inicio: '2026-10-20T13:00:00Z', horario_fin: '2026-10-20T15:00:00Z', accion_recomendada: 'Contacta al responsable.' }, 409)} />);
    expect(screen.getByRole('alert')).toHaveTextContent('FUERA_DE_HORARIO: No puedes iniciar ahora.');
    expect(screen.getByRole('alert')).toHaveTextContent('Horario permitido:');
    expect(screen.getByRole('alert')).toHaveTextContent('8:00');
    expect(screen.getByRole('alert')).toHaveTextContent('10:00');
    expect(screen.getByRole('alert')).toHaveTextContent('Acción recomendada: Contacta al responsable.');
  });
});
