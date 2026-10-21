import { describe, it, expect, vi, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor, cleanup } from '@testing-library/react';
import { SessionProvider } from './SessionProvider';
import { LoginForm } from './LoginForm';
afterEach(() => { cleanup(); vi.unstubAllGlobals(); });
describe('acceso sin persistir credenciales', () => {
 it('muestra el motivo de rechazo', async () => {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: false, status: 401, json: async () => ({ type:'urn:test',title:'No autorizado',status:401,code:'NO_AUTENTICADO',mensaje:'Credenciales inválidas.' }) }));
  render(<SessionProvider><LoginForm /></SessionProvider>);
  fireEvent.change(screen.getByLabelText('Correo'), { target:{ value:'ficticio@example.test' } }); fireEvent.change(screen.getByLabelText('Contraseña'), { target:{ value:'incorrecto' } });
  fireEvent.submit(screen.getByRole('button', { name:'Ingresar' }).closest('form')!);
  await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('Credenciales inválidas.'));
 });
 it('conserva el bearer solo en memoria y cierra sesión', async () => {
  const local = vi.spyOn(Storage.prototype, 'setItem');
  const fetcher = vi.fn().mockResolvedValueOnce({ ok:true,status:200,json:async () => ({ access_token:'test-opaque-token',token_type:'Bearer',vence_en:new Date(Date.now()+60000).toISOString(),roles:['ESTUDIANTE'] }) }).mockResolvedValueOnce({ok:true,status:204});vi.stubGlobal('fetch',fetcher);
  render(<SessionProvider><LoginForm /></SessionProvider>);fireEvent.submit(screen.getByRole('button', { name:'Ingresar' }).closest('form')!);
  await screen.findByRole('button', {name:'Cerrar sesión'});expect(local).not.toHaveBeenCalled();
  fireEvent.click(screen.getByRole('button', {name:'Cerrar sesión'}));await screen.findByRole('button', {name:'Ingresar'});
  expect(new Headers(fetcher.mock.calls[1][1].headers).get('Authorization')).toBe('Bearer test-opaque-token');local.mockRestore();
 });
});
