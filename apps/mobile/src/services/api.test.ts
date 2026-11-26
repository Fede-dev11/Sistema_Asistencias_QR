import { afterEach, expect, jest, test } from '@jest/globals';
import { ApiError, request } from './api';

afterEach(() => { jest.restoreAllMocks(); });

test('preserva los detalles de horario y la acción recomendada del backend', async () => {
  const problem = { type: 'urn:unl:problem:fuera-de-horario', title: 'Horario', status: 409,
    code: 'FUERA_DE_HORARIO', mensaje: 'No puedes iniciar ahora.', accion_recomendada: 'Espera el horario permitido.',
    horario_inicio: '2026-10-20T13:00:00Z', horario_fin: '2026-10-20T15:00:00Z', hora_servidor: '2026-10-20T12:00:00Z' };
  jest.spyOn(globalThis, 'fetch').mockResolvedValue({ ok: false, status: 409, json: async () => problem } as Response);
  try {
    await request('/sessions/example/start', { method: 'POST' });
    throw new Error('Debía rechazar la respuesta.');
  } catch (error) {
    expect(error).toBeInstanceOf(ApiError);
    expect((error as ApiError).problem).toEqual(problem);
    expect((error as Error).message).toBe(problem.mensaje);
  }
});

test('normaliza errores de otro formato sin ocultar el mensaje disponible', async () => {
  jest.spyOn(globalThis, 'fetch').mockResolvedValue({ ok: false, status: 503, json: async () => ({ message: 'Servicio no disponible.' }) } as Response);
  await expect(request('/sessions')).rejects.toMatchObject({ status: 503, problem: { code: 'HTTP_503', mensaje: 'Servicio no disponible.' } });
});
