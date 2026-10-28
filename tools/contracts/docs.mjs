// Datos de /documentacion en la web, derivados del contrato OpenAPI (fuente única).
export const GROUPS = [
  ['auth', 'Autenticación y cuenta', 'Inicio de sesión, renovación, perfil y contraseña propia.'],
  ['admin', 'Administración académica', 'Sección simulada del sistema académico: personas, cuentas, carreras, paralelos e importación CSV.'],
  ['spaces', 'Espacios', 'Aulas y lugares compartidos con coordenadas y tolerancia geográfica por defecto.'],
  ['activities', 'Clases y eventos', 'Crear y gestionar clases con horario, eventos cerrados o abiertos, listas autorizadas e invitados.'],
  ['sessions', 'Sesiones y QR', 'Ciclo de cada sesión: programar, iniciar, proyectar el QR, configurar GPS y cerrar.'],
  ['attendance', 'Registro de asistencia', 'Lo que usa la app móvil para que un participante con cuenta registre su asistencia.'],
  ['guest-registrations', 'Invitados y eventos abiertos', 'Flujo sin cuenta: invitados precargados en eventos cerrados y autorregistro en eventos abiertos.'],
  ['attempts', 'Revisión de intentos', 'Decisión del responsable sobre intentos pendientes por ubicación imprecisa.'],
  ['corrections', 'Correcciones y justificaciones', 'Asistencia manual, justificación de ausencias y solicitudes del estudiante.'],
  ['stats', 'Estadísticas e historial', 'Ausencias, porcentajes por estudiante, paralelo y carrera, e historial propio.'],
].map(([key, label, desc]) => ({ key, label, desc }));

export const ROLES = [
  ['pub', 'Público', 'Sin autenticación.'],
  ['inv', 'Invitado sin cuenta', 'Persona externa: cédula en el listado (evento cerrado) o autorregistro (evento abierto).'],
  ['cta', 'Cualquier cuenta', 'Cualquier usuario autenticado, sea cual sea su rol.'],
  ['est', 'Estudiante / participante', 'Registra su asistencia desde la app y consulta su historial.'],
  ['doc', 'Docente', 'Responsable de clases; también puede crear eventos.'],
  ['enc', 'Encargado', 'Responsable de eventos.'],
  ['dir', 'Directores', 'Director de carrera (su carrera) e institucional (todo). Solo lectura.'],
  ['adm', 'Administrador', 'Gestiona la sección académica simulada.'],
].map(([key, label, desc]) => ({ key, label, desc }));

const RESP = ['doc', 'enc'];
const READ = ['doc', 'enc', 'dir'];
const SCOPE = 'Directores: solo lectura dentro de su ámbito.';

// El contrato no declara roles; se toman de las specs 001–003 y de la autorización del backend.
export function rolesFor(method, path) {
  if (['/auth/login', '/auth/cas', '/auth/sso-config'].includes(path)) return [['pub'], null];
  if (['/auth/logout', '/auth/renew', '/me'].includes(path)) return [['cta'], null];
  if (path.startsWith('/admin/')) return [['adm'], null];
  if (['/guest-registrations', '/guest-registrations/submit'].includes(path) || path.startsWith('/events/open/')) return [['inv'], null];
  if (['/attendance/attempts', '/me/active-sessions', '/me/attendances', '/me/stats'].includes(path) || path.startsWith('/me/absences/')) return [['est'], null];
  if (path === '/me/parallels') return [RESP, null];
  if (path.startsWith('/spaces')) return [['doc', 'enc', 'adm'], method === 'PATCH' ? 'Solo el creador del espacio o un administrador puede editarlo.' : null];
  if (path.startsWith('/stats/') || /\/(stats|absences|audit)$/.test(path)) return [READ, SCOPE];
  if (method === 'GET' && (['/activities', '/activities/{activityId}'].includes(path) || path.endsWith('/attendances'))) return [READ, SCOPE];
  if (method === 'GET' && /\/(guest-list|guest-settings|schedule|geolocation)$/.test(path)) return [READ, SCOPE];
  if (path.endsWith('/schedule') || path.includes('/students')) return [['doc'], 'Solo clases; el responsable es el docente que la creó.'];
  if (path === '/activities' && method === 'POST') return [RESP, 'CLASE: solo DOCENTE. EVENTO: DOCENTE o ENCARGADO.'];
  return [RESP, 'Solo el responsable de la actividad.'];
}

export function buildDocs(spec) {
  const comp = spec.components;
  const deref = (o) => {
    while (o && o.$ref) { const [kind, name] = o.$ref.split('/').slice(-2); o = comp[kind][name]; }
    return o ?? {};
  };
  const refName = (o) => (o?.$ref ? o.$ref.split('/').pop() : null);
  const typeOf = (raw) => {
    const s = deref(raw);
    const t = s.type ?? (s.properties ? 'object' : '');
    if (t === 'array') return 'lista de ' + typeOf(s.items ?? {});
    if (refName(raw) && t === 'object') return refName(raw);
    return (t + (s.format ? ` (${s.format})` : '')) || refName(raw) || '';
  };
  const rules = (raw) => {
    const s = deref(raw);
    const bits = [];
    if (s.enum) bits.push(s.enum.filter((v) => v !== null).join(' | '));
    for (const [k, label] of [['minLength', 'mín. long.'], ['maxLength', 'máx. long.'], ['minimum', 'mín.'], ['maximum', 'máx.'],
      ['minItems', 'mín. elementos'], ['maxItems', 'máx. elementos'], ['default', 'por defecto'], ['pattern', 'patrón']])
      if (s[k] !== undefined) bits.push(`${label} ${s[k]}`);
    if (s.nullable) bits.push('puede ser null');
    return bits.join('; ');
  };
  const fields = (raw, prefix = '', depth = 0) => {
    const s = deref(raw);
    if (s.allOf) return s.allOf.flatMap((part) => fields(part, prefix, depth));
    const required = new Set(s.required ?? []);
    return Object.entries(s.properties ?? {}).flatMap(([name, prop]) => {
      const p = deref(prop);
      const row = { name: prefix + name, type: typeOf(prop), required: required.has(name), desc: (p.description ?? '').trim(), rules: rules(prop) };
      if (depth < 1 && p.properties) return [row, ...fields(prop, `${prefix}${name}.`, depth + 1)];
      if (depth < 1 && p.type === 'array' && deref(p.items).properties) return [row, ...fields(p.items, `${prefix}${name}[].`, depth + 1)];
      return [row];
    });
  };
  const groupOf = { '/me/parallels': 'activities' };
  const ops = [];
  for (const [path, item] of Object.entries(spec.paths)) {
    for (const method of ['get', 'post', 'put', 'patch', 'delete']) {
      const op = item[method];
      if (!op) continue;
      const M = method.toUpperCase();
      const params = [...(item.parameters ?? []), ...(op.parameters ?? [])].map(deref).map((p) => ({
        name: p.name, in: p.in, required: Boolean(p.required), type: typeOf(p.schema ?? {}), desc: (p.description ?? '').trim(), rules: rules(p.schema ?? {}),
      }));
      let body = null;
      if (op.requestBody) {
        const [contentType, media] = Object.entries(deref(op.requestBody).content)[0];
        body = { contentType, schema: refName(media.schema) ?? typeOf(media.schema ?? {}), fields: fields(media.schema ?? {}) };
      }
      const responses = Object.entries(op.responses ?? {}).map(([code, r]) => {
        const res = deref(r);
        const media = res.content ? Object.values(res.content)[0] : null;
        return { code: String(code), desc: (res.description ?? '').trim(), schema: media?.schema ? refName(media.schema) ?? typeOf(media.schema) : null };
      });
      const [roles, note] = rolesFor(M, path);
      ops.push({ id: op.operationId ?? `${M} ${path}`, method: M, path, summary: op.summary ?? '', desc: (op.description ?? '').trim(),
        group: groupOf[path] ?? op.tags?.[0] ?? 'otros', roles, note, params, body, responses });
    }
  }
  const known = new Set(GROUPS.map((g) => g.key));
  const unknown = ops.filter((o) => !known.has(o.group)).map((o) => `${o.method} ${o.path}`);
  if (unknown.length) throw new Error(`Operaciones sin grupo de documentación: ${unknown.join(', ')}`);

  const codes = new Set((comp.schemas.Problem?.properties?.code?.description ?? '').match(/\b[A-Z][A-Z_]{3,}[A-Z]\b/g) ?? []);
  const walk = (node) => {
    if (Array.isArray(node)) node.forEach(walk);
    else if (node && typeof node === 'object')
      for (const [k, v] of Object.entries(node)) { if (k === 'code' && typeof v === 'string' && /^[A-Z][A-Z_]+$/.test(v)) codes.add(v); walk(v); }
  };
  walk(spec);
  return { version: spec.info.version, server: spec.servers?.[0]?.url ?? '/api/v1', groups: GROUPS, roles: ROLES, ops, codes: [...codes].sort() };
}
