import openapiTS, { astToString } from 'openapi-typescript';
import { mkdir, readFile, writeFile } from 'node:fs/promises';
const schema = new URL('../../specs/001-asistencia-qr-dinamico/contracts/openapi.yaml', import.meta.url);
const output = astToString(await openapiTS(schema));
for (const app of ['web', 'mobile']) {
  const destination = new URL(`../../apps/${app}/src/generated/api.ts`, import.meta.url);
  if (process.argv.includes('--check')) {
    if (await readFile(destination, 'utf8') !== output) throw new Error(`Tipos desactualizados: ${app}`);
  } else { await mkdir(new URL('.', destination), { recursive: true }); await writeFile(destination, output); }
}
console.log('Tipos de web y móvil sincronizados.');
