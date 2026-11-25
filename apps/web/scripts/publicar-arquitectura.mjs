// Renderiza el sitio Quarto de docs/arquitectura y lo copia a public/arquitectura para servirlo desde la web.
// Requiere Quarto, Python y Java (PlantUML vía mvnw), igual que `quarto render` en docs/arquitectura.
import { execSync } from 'node:child_process';
import { cpSync, existsSync, readdirSync, rmSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const web = join(dirname(fileURLToPath(import.meta.url)), '..');
const docs = join(web, '..', '..', 'docs', 'arquitectura');
const target = join(web, 'public', 'arquitectura');

if (!process.argv.includes('--sin-render')) execSync('quarto render', { cwd: docs, stdio: 'inherit' });
const site = join(docs, '_site');
if (!existsSync(join(site, 'index.html'))) throw new Error(`No existe ${site}/index.html: ejecuta "quarto render" en docs/arquitectura.`);

rmSync(target, { recursive: true, force: true });
cpSync(site, target, { recursive: true });
// Índice de diagramas PlantUML para la galería de /documentacion/arquitectura.
const svgs = readdirSync(join(target, '_generated')).filter(f => f.endsWith('.svg')).sort();
writeFileSync(join(target, 'diagramas.json'), JSON.stringify(svgs, null, 2));
console.log(`Publicado en ${target} (${svgs.length} diagramas PlantUML).`);
