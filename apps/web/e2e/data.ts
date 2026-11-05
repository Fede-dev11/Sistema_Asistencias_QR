import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

export type Fixture = { activity: string; session: string; name: string };

/** Actividad y sesión ficticias con tests/fixtures/create-activity.py (usa GEOASIST_DB_CONTAINER). */
export function createActivity(mode: 'CLASE' | 'EVENTO', access: 'CERRADO' | 'ABIERTO' = 'CERRADO'): Fixture {
  const script = fileURLToPath(new URL('../../../tests/fixtures/create-activity.py', import.meta.url));
  return JSON.parse(execFileSync('python3', [script, mode, access], { encoding: 'utf8' })) as Fixture;
}

/** SQL directo para preparar datos que ningún endpoint crea (solo entorno local/CI). */
export function sql(statement: string) {
  execFileSync('docker', ['exec', '-i', process.env.GEOASIST_DB_CONTAINER ?? 'geoasist-db-1', 'psql', '-v', 'ON_ERROR_STOP=1', '-U', 'geoasist', '-d', 'geoasist'], { input: statement, encoding: 'utf8', stdio: ['pipe', 'ignore', 'inherit'] });
}
