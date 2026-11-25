import { beforeEach, expect, jest, test } from '@jest/globals';
import * as Crypto from 'expo-crypto';
import * as SecureStore from 'expo-secure-store';
import * as WebBrowser from 'expo-web-browser';
import { request, setBearer } from '../../services/api';
import { loginWithCas } from './casLogin';

jest.mock('expo-crypto',()=>({getRandomBytes:jest.fn()}));
jest.mock('expo-secure-store',()=>({setItemAsync:jest.fn()}));
jest.mock('expo-web-browser',()=>({openAuthSessionAsync:jest.fn()}));
jest.mock('../../services/api',()=>({request:jest.fn(),setBearer:jest.fn()}));

const nonce='000102030405060708090a0b0c0d0e0f';
const prefix='geoasist://auth/callback';
const service=`${prefix}?n=${nonce}`;
const session={access_token:'account-token',token_type:'Bearer' as const,vence_en:'2099-01-01T00:00:00Z',roles:['ESTUDIANTE' as const]};

beforeEach(()=>{
 jest.clearAllMocks();
 jest.mocked(Crypto.getRandomBytes).mockReturnValue(Uint8Array.from({length:16},(_,index)=>index));
 jest.mocked(request).mockResolvedValueOnce({login_url:'http://localhost:8081/cas/login',logout_url:'http://localhost:8081/cas/logout',web_service:'http://localhost:3000/auth/callback',mobile_service_prefix:prefix});
});

test('genera el nonce, lo verifica y guarda la sesión canjeada',async()=>{
 jest.mocked(WebBrowser.openAuthSessionAsync).mockResolvedValue({type:'success',url:`${service}&ticket=ST-secret`} as WebBrowser.WebBrowserAuthSessionResult);
 jest.mocked(request).mockResolvedValueOnce(session);

 await expect(loginWithCas()).resolves.toEqual({status:'success',session});

 expect(Crypto.getRandomBytes).toHaveBeenCalledWith(16);
 expect(WebBrowser.openAuthSessionAsync).toHaveBeenCalledWith(`http://localhost:8081/cas/login?service=${encodeURIComponent(service)}`,prefix);
 expect(SecureStore.setItemAsync).toHaveBeenCalledWith('geoasist-account-session',JSON.stringify(session));
 expect(setBearer).toHaveBeenCalledWith(session.access_token);
});

test('descarta un retorno con un nonce ajeno sin canjearlo',async()=>{
 jest.mocked(WebBrowser.openAuthSessionAsync).mockResolvedValue({type:'success',url:`${prefix}?n=otro&ticket=ST-secret`} as WebBrowser.WebBrowserAuthSessionResult);

 await expect(loginWithCas()).rejects.toThrow('Inicio de sesión no válido');

 expect(request).toHaveBeenCalledTimes(1);
 expect(SecureStore.setItemAsync).not.toHaveBeenCalled();
});

test('la cancelación deja la app sin una sesión nueva',async()=>{
 jest.mocked(WebBrowser.openAuthSessionAsync).mockResolvedValue({type:'cancel' as WebBrowser.WebBrowserResultType});

 await expect(loginWithCas()).resolves.toEqual({status:'cancelado'});

 expect(request).toHaveBeenCalledTimes(1);
 expect(SecureStore.setItemAsync).not.toHaveBeenCalled();
 expect(setBearer).not.toHaveBeenCalled();
});

test('canjea el ticket con exactamente el mismo service enviado al SSO',async()=>{
 jest.mocked(WebBrowser.openAuthSessionAsync).mockResolvedValue({type:'success',url:`${service}&ticket=ST-secret`} as WebBrowser.WebBrowserAuthSessionResult);
 jest.mocked(request).mockResolvedValueOnce(session);

 await loginWithCas();

 expect(request).toHaveBeenNthCalledWith(2,'/auth/cas',{
  method:'POST',authentication:'none',body:JSON.stringify({ticket:'ST-secret',service}),
 });
});
