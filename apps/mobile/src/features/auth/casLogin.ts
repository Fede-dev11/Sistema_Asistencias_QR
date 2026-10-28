import * as Crypto from 'expo-crypto';
import * as WebBrowser from 'expo-web-browser';
import type { components } from '../../generated/api';
import { request } from '../../services/api';
import { saveSession, type AccountSession } from './session';

type SsoConfiguration = components['schemas']['SsoConfiguration'];

export type CasLoginResult =
 | { status: 'success'; session: AccountSession }
 | { status: 'cancelado' };

function createNonce(): string {
 return Array.from(Crypto.getRandomBytes(16), byte=>byte.toString(16).padStart(2,'0')).join('');
}

export async function loginWithCas(): Promise<CasLoginResult> {
 const config=await request<SsoConfiguration>('/auth/sso-config',{authentication:'none'});
 const nonce=createNonce();
 const service=`${config.mobile_service_prefix}?n=${nonce}`;
 const result=await WebBrowser.openAuthSessionAsync(
  `${config.login_url}?service=${encodeURIComponent(service)}`,
  config.mobile_service_prefix,
 );
 if(result.type!=='success')return {status:'cancelado'};

 const returned=new URL(result.url);
 if(returned.searchParams.get('n')!==nonce)throw new Error('Inicio de sesión no válido');
 const ticket=returned.searchParams.get('ticket');
 if(!ticket)throw new Error('Inicio de sesión no válido');

 const session=await request<AccountSession>('/auth/cas',{
  method:'POST',
  authentication:'none',
  body:JSON.stringify({ticket,service}),
 });
 await saveSession(session);
 return {status:'success',session};
}
