import * as SecureStore from 'expo-secure-store';
import type { components } from '../../generated/api';
import { request, setBearer } from '../../services/api';
export type AccountSession = components['schemas']['LoginResult'];
const KEY = 'geoasist-account-session';
export async function saveSession(session: AccountSession): Promise<void> {
 await SecureStore.setItemAsync(KEY,JSON.stringify(session));setBearer(session.access_token);
}
export async function login(correo: string, password: string): Promise<AccountSession> {
 const session = await request<AccountSession>('/auth/login', {method:'POST', authentication:'none', body:JSON.stringify({correo,password})});
 await saveSession(session);return session;
}
export async function restore(): Promise<AccountSession | null> {
 const stored=await SecureStore.getItemAsync(KEY);if(!stored)return null;
 try {const session=JSON.parse(stored) as AccountSession;if(typeof session.access_token!=='string'||!Array.isArray(session.roles)||!(Date.parse(session.vence_en)>Date.now()))throw new Error('Caducada');setBearer(session.access_token);return session;}catch{await clear();return null;}
}
export async function clear(){setBearer(null);await SecureStore.deleteItemAsync(KEY);}
export async function logout(){try{await request<void>('/auth/logout',{method:'POST'});}finally{await clear();}}
