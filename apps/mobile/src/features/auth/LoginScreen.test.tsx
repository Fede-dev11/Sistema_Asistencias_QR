import { beforeEach, jest, test, expect } from '@jest/globals';
import { render, fireEvent, waitFor } from '@testing-library/react-native';
import { LoginScreen } from './LoginScreen';
import { loginWithCas } from './casLogin';
import { login } from './session';
jest.mock('./casLogin',()=>({ loginWithCas:jest.fn() }));
jest.mock('./session',()=>({ login:jest.fn() }));
const session={access_token:'test-token',token_type:'Bearer' as const,vence_en:'2099-01-01T00:00:00Z',roles:['ESTUDIANTE' as const]};
beforeEach(()=>{jest.clearAllMocks();});
test('el ingreso universitario es la acción principal y entrega la sesión',async()=>{
 jest.mocked(loginWithCas).mockResolvedValue({status:'success',session});const onLogin=jest.fn();const ui=await render(<LoginScreen onLogin={onLogin}/>);
 await fireEvent.press(ui.getByLabelText('Ingresar con cuenta universitaria'));
 await waitFor(()=>expect(onLogin).toHaveBeenCalledWith(session));expect(ui.getByLabelText('Ingresar con credenciales locales')).toBeTruthy();
});
test('muestra el estado cancelado sin crear sesión',async()=>{
 jest.mocked(loginWithCas).mockResolvedValue({status:'cancelado'});const onLogin=jest.fn();const ui=await render(<LoginScreen onLogin={onLogin}/>);
 await fireEvent.press(ui.getByLabelText('Ingresar con cuenta universitaria'));
 await waitFor(()=>expect(ui.getByText('Inicio de sesión cancelado.')).toBeTruthy());expect(onLogin).not.toHaveBeenCalled();
});
test('expone estados accesibles de carga y error universitario',async()=>{
 let rejectLogin:(reason:Error)=>void=()=>undefined;
 jest.mocked(loginWithCas).mockImplementation(()=>new Promise((_,reject)=>{rejectLogin=reject;}));const ui=await render(<LoginScreen onLogin={jest.fn()}/>);
 await fireEvent.press(ui.getByLabelText('Ingresar con cuenta universitaria'));
 expect(ui.getByLabelText('Conectando con la universidad')).toBeDisabled();
 rejectLogin(new Error('SSO no disponible.'));
 await waitFor(()=>expect(ui.getByRole('alert')).toHaveTextContent('SSO no disponible.'));
});
test('rechazo de credenciales muestra motivo y no cambia la sesión',async()=>{
 jest.mocked(login).mockRejectedValue(new Error('Credenciales inválidas.'));const onLogin=jest.fn();const ui=await render(<LoginScreen onLogin={onLogin}/>);
 await fireEvent.changeText(ui.getByLabelText('Correo'),'ficticio@example.test');await fireEvent.changeText(ui.getByLabelText('Contraseña'),'incorrecto');await fireEvent.press(ui.getByLabelText('Ingresar con credenciales locales'));
 await waitFor(()=>expect(ui.getByRole('alert')).toBeTruthy());expect(onLogin).not.toHaveBeenCalled();expect(ui.getByText('Credenciales inválidas.')).toBeTruthy();
});
