// Cobertura web (SC-001 de 006): una operación está cubierta si algún archivo de apps/web/src
// menciona sus segmentos literales en orden (los {param} se ignoran), con hasta 40 caracteres entre ellos.
// Heurística informal: indica dónde buscar, no prueba que la pantalla funcione.
import { mkdirSync, readFileSync, readdirSync, writeFileSync } from 'node:fs';
import { dirname, join, relative, resolve, sep } from 'node:path';
import { fileURLToPath } from 'node:url';
import { GROUPS } from './docs.mjs';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..', '..');
const srcDir = join(root, 'apps', 'web', 'src');
const out = join(root, 'docs', 'validation', 'cobertura-web.md');

const { ops } = JSON.parse(readFileSync(join(srcDir, 'generated', 'api-docs.json'), 'utf8'));

const sources = readdirSync(srcDir, { recursive: true })
  .filter((f) => /\.(ts|tsx)$/.test(f) && !/\.test\./.test(f) && !f.split(sep).includes('generated'))
  .sort()
  .map((f) => [`apps/web/src/${f.split(sep).join('/')}`, readFileSync(join(srcDir, f), 'utf8')]);

// Lookarounds evitan que "me" coincida dentro de "name" o "message".
const escape = (s) => s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
// Además, si la ruta termina en un tramo variable en el código (`/sessions/${id}/${kind}` con kind 'start'|'close'),
// basta que el archivo tenga el prefijo literal (todos los segmentos menos el último) y el último segmento como
// cadena entre comillas o como `/segmento` dentro de una plantilla.
const word = (s) => `(?<![A-Za-z0-9])${escape(s)}(?![A-Za-z0-9])`;
function matcher(path) {
  const literals = path.split('/').filter((s) => s && !(s.startsWith('{') && s.endsWith('}')));
  if (literals.length === 0) return null;
  const direct = new RegExp(literals.map(word).join('[\\s\\S]{0,40}'));
  const last = literals.at(-1);
  const prefix = literals.length > 1 ? new RegExp(literals.slice(0, -1).map(word).join('[\\s\\S]{0,40}')) : null;
  const tail = new RegExp(`['"\`]${escape(last)}['"\`]|/${word(last)}`);
  return (text) => direct.test(text) || (prefix !== null && prefix.test(text) && tail.test(text));
}

const rows = ops.map((op) => {
  const test = matcher(op.path);
  const file = test ? sources.find(([, text]) => test(text))?.[0] ?? null : null;
  return { ...op, file };
});

const covered = rows.filter((r) => r.file).length;
const keys = [...new Set([...GROUPS.map((g) => g.key), ...ops.map((o) => o.group)])];

const lines = [
  '# Cobertura web de operaciones de la API (006 · T022)',
  '',
  'Generado con `npm run coverage:web` en `tools/contracts` desde `apps/web/src/generated/api-docs.json`. No editar a mano.',
  '',
  `**Cubiertas: ${covered} de ${ops.length} operaciones.**`,
  '',
  'Criterio: el archivo de `apps/web/src` (sin `generated/` ni `*.test.*`) menciona los segmentos literales de la ruta en orden, con hasta 40 caracteres entre ellos; o el prefijo literal más el último segmento como cadena o `/segmento` (tramo final variable).',
];

for (const key of keys) {
  const group = rows.filter((r) => r.group === key);
  if (group.length === 0) continue;
  const label = GROUPS.find((g) => g.key === key)?.label ?? key;
  lines.push('', `## ${label} (\`${key}\`)`, '', '| Método | Ruta | Roles | Cubierta | Primer archivo |', '|---|---|---|---|---|');
  for (const r of group) {
    lines.push(`| ${r.method} | \`${r.path}\` | ${r.roles.join(', ')} | ${r.file ? 'Sí' : 'No'} | ${r.file ? `\`${r.file}\`` : '—'} |`);
  }
}

mkdirSync(dirname(out), { recursive: true });
writeFileSync(out, lines.join('\n') + '\n', 'utf8');
console.log(`Cubiertas: ${covered} de ${ops.length} operaciones -> ${relative(root, out)}`);
