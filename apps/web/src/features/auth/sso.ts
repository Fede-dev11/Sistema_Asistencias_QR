// Indicador (nunca el token) de que esta pestaña ya ingresó por SSO: permite pedir un ticket nuevo sin formulario.
export const SSO_FLAG = 'geoasist-sso';
export const setSsoFlag = (on: boolean) => { try { if (on) sessionStorage.setItem(SSO_FLAG, '1'); else sessionStorage.removeItem(SSO_FLAG); } catch { /* almacenamiento bloqueado: sin re-autenticación silenciosa */ } };
export const hasSsoFlag = () => { try { return sessionStorage.getItem(SSO_FLAG) === '1'; } catch { return false; } };

/** Solo rutas internas: empieza con "/", no con "//" ni "/\", sin esquema ni caracteres de control. */
export function safeDestino(raw: string | null | undefined): string | null {
  if (!raw || !raw.startsWith('/') || raw.startsWith('//') || /[\u0000-\u001f\\]/.test(raw)) return null;
  return raw === '/auth/callback' || raw.startsWith('/auth/callback?') ? null : raw;
}

/** `service` registrado en el SSO + `destino` en su consulta (el SSO acepta consulta sobre el prefijo registrado). */
export const serviceFor = (webService: string, destino: string | null) => destino ? `${webService}?destino=${encodeURIComponent(destino)}` : webService;

const has = (roles: readonly string[], ...r: string[]) => roles.some(x => r.includes(x));
/** Inicio de cada rol; '/login' = sin inicio web (estudiante sin herramientas de prueba). */
export function homeFor(roles: readonly string[]): string {
  if (has(roles, 'DOCENTE', 'ENCARGADO')) return '/hoy';
  if (has(roles, 'DIRECTOR_INSTITUCIONAL', 'DIRECTOR_CARRERA', 'ADMINISTRADOR')) return '/inicio';
  return process.env.NEXT_PUBLIC_HERRAMIENTAS_PRUEBA === 'true' ? '/pruebas/participante' : '/login';
}
