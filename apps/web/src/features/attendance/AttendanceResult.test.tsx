import { cleanup, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it } from 'vitest';
import { AttendanceResult } from './AttendanceResult';

afterEach(cleanup);

describe('resultado de asistencia', () => {
  it.each([
    ['CONFIRMADA', 'Asistencia registrada.'],
    ['REPETIR', 'Toma una nueva medición.'],
    ['PENDIENTE_REVISION', 'El encargado revisará el intento.'],
    ['RECHAZADA', 'No perteneces al listado.'],
  ] as const)('muestra estado %s y su motivo', (estado, mensaje) => {
    render(<AttendanceResult estado={estado} mensaje={mensaje} />);
    expect(screen.getByRole(estado === 'CONFIRMADA' ? 'status' : 'alert')).toHaveTextContent(
      estado.replaceAll('_', ' '),
    );
    expect(screen.getByText(mensaje)).toBeInTheDocument();
  });
});
