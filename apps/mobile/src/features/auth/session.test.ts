import { jest, test, expect } from '@jest/globals';
import * as SecureStore from 'expo-secure-store';
import { restore, logout } from './session';
import { request, setBearer } from '../../services/api';
jest.mock('expo-secure-store',()=>({getItemAsync:jest.fn(),setItemAsync:jest.fn(),deleteItemAsync:jest.fn()}));
jest.mock('../../services/api',()=>({request:jest.fn(),setBearer:jest.fn()}));
test('elimina la sesión caducada del almacenamiento seguro',async()=>{
 jest.mocked(SecureStore.getItemAsync).mockResolvedValue(JSON.stringify({access_token:'test-token',vence_en:'2000-01-01T00:00:00Z',roles:[]}));
 expect(await restore()).toBeNull();expect(SecureStore.deleteItemAsync).toHaveBeenCalledWith('geoasist-account-session');expect(setBearer).toHaveBeenCalledWith(null);
});
test('el cierre borra sesión local aunque falle la conexión',async()=>{
 jest.mocked(request).mockRejectedValue(new Error('sin red'));await expect(logout()).rejects.toThrow('sin red');expect(SecureStore.deleteItemAsync).toHaveBeenCalledWith('geoasist-account-session');
});
