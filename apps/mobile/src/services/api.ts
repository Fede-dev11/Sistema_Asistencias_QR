import type { components } from '../generated/api';
export type Problem = components['schemas']['Problem'];
export class ApiError extends Error {
 constructor(public readonly problem: Problem, public readonly status: number) { super(problem.mensaje); }
}
let bearer: string | null = null;
const unauthorized=new Set<()=>void>();
export function onUnauthorized(callback:()=>void){unauthorized.add(callback);return ()=>{unauthorized.delete(callback);};}
export function setBearer(value: string | null) { bearer = value; }
const base = process.env.EXPO_PUBLIC_API_URL ?? 'http://localhost:8080/api/v1';
export type ApiRequestInit = RequestInit & { authentication?: 'account' | 'none' };
export async function request<T>(path: string, options: ApiRequestInit = {}): Promise<T> {
 const { authentication = 'account', ...fetchOptions } = options;
 const bearerUsed = authentication === 'account' ? bearer : null;
 const headers = new Headers(options.headers);
 if (options.body) headers.set('Content-Type', 'application/json');
 if (bearerUsed) headers.set('Authorization', `Bearer ${bearerUsed}`);
 const response = await fetch(base + path, { ...fetchOptions, headers, credentials: 'omit', cache: 'no-store' });
 if (!response.ok) {
  let problem: Problem;
  try {
   const raw = await response.json() as Partial<Problem> & { message?: string };
   const mensaje = raw.mensaje ?? raw.message ?? 'No se pudo completar la operación.';
   problem = { ...raw, type: raw.type ?? 'urn:unl:problem:api-error', title: raw.title ?? mensaje, status: response.status, code: raw.code ?? `HTTP_${response.status}`, mensaje };
  } catch { problem = { type: 'urn:unl:problem:red', title: 'Respuesta no disponible', status: response.status, code: 'RESPUESTA_INVALIDA', mensaje: 'No se pudo leer la respuesta. Reintenta con la misma operación.' }; }
  if (response.status === 401 && bearerUsed && bearer === bearerUsed) { setBearer(null); unauthorized.forEach(callback=>callback()); }
  throw new ApiError(problem, response.status);
 }
 if (response.status === 204) return undefined as T;
 return await response.json() as T;
}
