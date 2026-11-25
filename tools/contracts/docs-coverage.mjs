// Verifica que cada operación del contrato tenga documentación mínima para Swagger UI.
// Falla (exit 1) si falta resumen, descripción, etiqueta, respuestas o descripción de parámetros.
// Los ejemplos y las descripciones de cuerpos/esquemas se informan como advertencias.
import SwaggerParser from '@apidevtools/swagger-parser';
import { fileURLToPath } from 'node:url';

const ruta = new URL('../../specs/001-asistencia-qr-dinamico/contracts/openapi.yaml', import.meta.url);
const spec = await SwaggerParser.dereference(fileURLToPath(ruta));
const metodos = ['get', 'post', 'put', 'patch', 'delete'];

const errores = [];
const advertencias = [];
let operaciones = 0;

const tieneEjemplo = (nodo) => {
  if (Array.isArray(nodo)) return nodo.some(tieneEjemplo);
  if (nodo && typeof nodo === 'object') {
    if ('example' in nodo || 'examples' in nodo) return true;
    return Object.values(nodo).some(tieneEjemplo);
  }
  return false;
};

for (const [rutaApi, item] of Object.entries(spec.paths ?? {})) {
  for (const metodo of metodos) {
    const op = item[metodo];
    if (!op) continue;
    operaciones += 1;
    const id = `${metodo.toUpperCase()} ${rutaApi}`;
    if (!op.summary) errores.push(`${id}: falta summary`);
    if (!op.description) errores.push(`${id}: falta description`);
    if (!op.tags?.length) errores.push(`${id}: falta tags`);
    if (!op.responses || Object.keys(op.responses).length === 0) errores.push(`${id}: falta responses`);
    for (const parametro of [...(item.parameters ?? []), ...(op.parameters ?? [])]) {
      if (!parametro.description) errores.push(`${id}: parámetro ${parametro.name} sin description`);
    }
    if (op.requestBody && !op.requestBody.description) {
      advertencias.push(`${id}: requestBody sin description`);
    }
    if (!tieneEjemplo(op)) advertencias.push(`${id}: sin ejemplos explícitos (Swagger los genera del esquema)`);
  }
}

const esquemas = Object.entries(spec.components?.schemas ?? {});
const esquemasSinDescripcion = esquemas.filter(([, esquema]) => !esquema.description).map(([nombre]) => nombre);

console.log(`Contrato: ${operaciones} operaciones, ${esquemas.length} esquemas.`);
if (advertencias.length) {
  console.log(`Advertencias (${advertencias.length}):`);
  for (const advertencia of advertencias.slice(0, 20)) console.log(`  - ${advertencia}`);
  if (advertencias.length > 20) console.log(`  … y ${advertencias.length - 20} más.`);
}
if (esquemasSinDescripcion.length) {
  console.log(`Esquemas sin description (${esquemasSinDescripcion.length}): ${esquemasSinDescripcion.slice(0, 10).join(', ')}${esquemasSinDescripcion.length > 10 ? '…' : ''}`);
}
if (errores.length) {
  console.error(`\nDocumentación incompleta (${errores.length}):`);
  for (const error of errores) console.error(`  - ${error}`);
  process.exit(1);
}
console.log('Cobertura documental mínima: OK.');
