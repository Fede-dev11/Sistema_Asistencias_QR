import { describe, it, expect, vi, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor, cleanup } from '@testing-library/react';
import { SessionProvider } from './SessionProvider';
import { LoginForm } from './LoginForm';
afterEach(() => { cleanup(); vi.unstubAllGlobals(); });
const sinSso = { ok: false, status: 404, json: async () => ({ type:'urn:test',title:'x',status:404,code:'NO_ENCONTRADO',mensaje:'Sin SSO.' }) };
const sso = { ok: true, status: 200, json: async () => ({ login_url:'https://sso.example.test/cas/login', logout_url:'https://sso.example.test/cas/logout', web_service:'https://web.example.test/auth/callback', mobile_service_prefix:'geoasist://' }) };
const sesion = { ok:true,status:200,json:async () => ({ access_token:'test-opaque-token',token_type:'Bearer',vence_en:new Date(Date.now()+60000).toISOString(),roles:['ESTUDIANTE'] }) };
describe('acceso sin persistir credenciales', () => {
 it('muestra el motivo de rechazo', async () => {
  vi.stubGlobal('fetch', vi.fn().mockImplementation(async (url: string) => url.endsWith('/auth/sso-config') ? sinSso : { ok: false, status: 401, json: async () => ({ type:'urn:test',title:'No autorizado',status:401,code:'NO_AUTENTICADO',mensaje:'Credenciales inválidas.' }) }));
  render(<SessionProvider><LoginForm /></SessionProvider>);
  fireEvent.change(await screen.findByLabelText('Correo'), { target:{ value:'ficticio@example.test' } }); fireEvent.change(screen.getByLabelText('Contraseña'), { target:{ value:'incorrecto' } });
  fireEvent.submit(screen.getByRole('button', { name:'Ingresar' }).closest('form')!);
  await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('Credenciales inválidas.'));
 });
 it('conserva el bearer solo en memoria y cierra sesión', async () => {
  const local = vi.spyOn(Storage.prototype, 'setItem');
  const fetcher = vi.fn().mockImplementation(async (url: string) => url.endsWith('/auth/sso-config') ? sinSso : url.endsWith('/auth/login') ? sesion : { ok:true,status:204 });vi.stubGlobal('fetch',fetcher);
  render(<SessionProvider><LoginForm /></SessionProvider>);await screen.findByLabelText('Correo');fireEvent.submit(screen.getByRole('button', { name:'Ingresar' }).closest('form')!);
  await screen.findByRole('button', {name:'Cerrar sesión'});expect(local).not.toHaveBeenCalled();
  fireEvent.click(screen.getByRole('button', {name:'Cerrar sesión'}));await screen.findByRole('button', {name:'Ingresar'});
  const logout = fetcher.mock.calls.find(c => String(c[0]).endsWith('/auth/logout'))!;
  expect(new Headers(logout[1].headers).get('Authorization')).toBe('Bearer test-opaque-token');local.mockRestore();
 });
 it('con SSO disponible ofrece solo el botón universitario y navega con service codificado', async () => {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(sso));
  const loc = { href: '' }; vi.stubGlobal('location', loc);
  render(<SessionProvider><LoginForm /></SessionProvider>);
  fireEvent.click(await screen.findByRole('button', { name:'Ingresar con cuenta universitaria' }));
  expect(loc.href).toBe('https://sso.example.test/cas/login?service=' + encodeURIComponent('https://web.example.test/auth/callback'));
  expect(screen.queryByLabelText('Contraseña')).toBeNull();
 });
 it('tras cerrar sesión ofrece cerrar también la sesión universitaria', async () => {
  const fetcher = vi.fn().mockImplementation(async (url: string) => url.endsWith('/auth/sso-config') ? sinSso : url.endsWith('/auth/login') ? sesion : { ok:true,status:200,json:async () => ({ sso_logout_url:'https://sso.example.test/cas/logout?service=x' }) });vi.stubGlobal('fetch',fetcher);
  render(<SessionProvider><LoginForm /></SessionProvider>);await screen.findByLabelText('Correo');fireEvent.submit(screen.getByRole('button', { name:'Ingresar' }).closest('form')!);
  fireEvent.click(await screen.findByRole('button', {name:'Cerrar sesión'}));
  expect(await screen.findByRole('link', {name:'Cerrar también la sesión universitaria'})).toHaveAttribute('href','https://sso.example.test/cas/logout?service=x');
 });
});
