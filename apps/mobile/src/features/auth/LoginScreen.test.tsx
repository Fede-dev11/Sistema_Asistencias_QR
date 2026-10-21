import { jest, test, expect } from '@jest/globals';
import { render, fireEvent, waitFor } from '@testing-library/react-native';
import { LoginScreen } from './LoginScreen';
import { login } from './session';
jest.mock('./session',()=>({ login:jest.fn() }));
test('rechazo de credenciales muestra motivo y no cambia la sesión',async()=>{
 jest.mocked(login).mockRejectedValue(new Error('Credenciales inválidas.'));const onLogin=jest.fn();const ui=await render(<LoginScreen onLogin={onLogin}/>);
 await fireEvent.changeText(ui.getByLabelText('Correo'),'ficticio@example.test');await fireEvent.changeText(ui.getByLabelText('Contraseña'),'incorrecto');await fireEvent.press(ui.getByText('Ingresar'));
 await waitFor(()=>expect(ui.getByRole('alert')).toBeTruthy());expect(onLogin).not.toHaveBeenCalled();expect(ui.getByText('Credenciales inválidas.')).toBeTruthy();
});
