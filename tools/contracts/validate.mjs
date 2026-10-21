import { fileURLToPath } from 'node:url';
import SwaggerParser from '@apidevtools/swagger-parser';
import Ajv from 'ajv';
import addFormats from 'ajv-formats';
const source = fileURLToPath(new URL('../../specs/001-asistencia-qr-dinamico/contracts/openapi.yaml', import.meta.url));
const api = await SwaggerParser.validate(source);
const resolved = await SwaggerParser.dereference(source);
const ajv = new Ajv({ strict: false, validateSchema: false }); addFormats(ajv);
let count = 0;
for (const item of Object.values(resolved.paths)) for (const op of Object.values(item)) {
  if (!op?.responses) continue;
  for (const response of Object.values(op.responses)) for (const media of Object.values(response.content ?? {})) {
    if (media.example !== undefined && media.schema) {
      const validate = ajv.compile(media.schema);
      if (!validate(media.example)) throw new Error(JSON.stringify(validate.errors));
      count++;
    }
  }
}
console.log(`OpenAPI ${api.openapi}: válido; ${count} ejemplos verificados.`);
