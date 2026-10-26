import openapiTS, { astToString } from 'openapi-typescript';
import SwaggerParser from '@apidevtools/swagger-parser';
import { mkdir, readFile, writeFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import { buildDocs } from './docs.mjs';
const schema = new URL('../../specs/001-asistencia-qr-dinamico/contracts/openapi.yaml', import.meta.url);
const types = astToString(await openapiTS(schema));
const docs = JSON.stringify(buildDocs(await SwaggerParser.parse(fileURLToPath(schema))), null, 2) + '\n';
const outputs = [
  ['../../apps/web/src/generated/api.ts', types],
  ['../../apps/mobile/src/generated/api.ts', types],
  ['../../apps/web/src/generated/api-docs.json', docs],
];
for (const [file, output] of outputs) {
  const destination = new URL(file, import.meta.url);
  if (process.argv.includes('--check')) {
    if (await readFile(destination, 'utf8') !== output) throw new Error(`Generado desactualizado: ${file}`);
  } else { await mkdir(new URL('.', destination), { recursive: true }); await writeFile(destination, output); }
}
console.log('Tipos y documentación sincronizados.');
