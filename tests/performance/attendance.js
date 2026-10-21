// Ejecutar con Node 22 y .env local; no registrar tokens, cuerpos ni identidades.
import { execFileSync } from 'node:child_process';
import { writeFileSync } from 'node:fs';
import { cpus, totalmem, release, platform } from 'node:os';
import { fileURLToPath } from 'node:url';

const API = process.env.API_URL ?? 'http://127.0.0.1:18081/api/v1';
if (!process.env.DEV_SEED_PASSWORD) throw new Error('Falta DEV_SEED_PASSWORD ficticia');
const fixture = JSON.parse(execFileSync('python3', [fileURLToPath(new URL('../fixtures/create-performance.py', import.meta.url))], { encoding: 'utf8' }));
async function call(path, token, body) {
  const response = await fetch(API + path, {
    method: body === undefined ? 'GET' : 'POST',
    headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}), 'Content-Type': 'application/json', 'Idempotency-Key': crypto.randomUUID() },
    body: body === undefined ? undefined : JSON.stringify(body),
    signal: AbortSignal.timeout(20_000),
  });
  const value = await response.json();
  if (!response.ok) throw new Error(`Operación falló: estado ${response.status}`);
  return value;
}
async function login(email) {
  return (await call('/auth/login', null, { correo: email, password: process.env.DEV_SEED_PASSWORD })).access_token;
}
const owner = await login('encargado@example.test');
const participants = [];
for (const email of fixture.emails) participants.push(await login(email));
for (const session of fixture.sessions) await call(`/sessions/${session}/start`, owner, {});
const durations = [];
const statuses = {};
let errors = 0;
let maximumLocks = 0;
let lockSamples = 0;
const start = performance.now();
const lockTimer = setInterval(() => {
  try {
    const blocked = Number(execFileSync('docker', ['exec', 'geoasist-db-1', 'psql', '-U', 'geoasist', '-d', 'geoasist', '-Atc', "SELECT count(*) FROM pg_stat_activity WHERE datname=current_database() AND wait_event_type='Lock'"], { encoding: 'utf8' }).trim());
    maximumLocks = Math.max(maximumLocks, blocked);
    lockSamples++;
  } catch { errors++; }
}, 5000);
try {
  // 40 ráfagas de 50 solicitudes en 600 s: 200 intentos/min; alternar cinco sesiones.
  for (let round = 0; round < 40; round++) {
    const delay = start + round * 15_000 - performance.now();
    if (delay > 0) await new Promise(resolve => setTimeout(resolve, delay));
    const tokens = await Promise.all(fixture.sessions.map(async session => (await call(`/sessions/${session}/qr`, owner)).qr_token));
    await Promise.all(participants.map(async (bearer, index) => {
      const begin = performance.now();
      try {
        const response = await fetch(API + '/attendance/attempts', {
          method: 'POST',
          headers: { Authorization: `Bearer ${bearer}`, 'Content-Type': 'application/json', 'Idempotency-Key': crypto.randomUUID() },
          body: JSON.stringify({ qr_token: tokens[(index + round) % 5] }),
          signal: AbortSignal.timeout(20_000),
        });
        await response.json();
        durations.push(performance.now() - begin);
        statuses[response.status] = (statuses[response.status] ?? 0) + 1;
        if (![200, 201].includes(response.status)) errors++;
      } catch { errors++; }
    }));
    if ((round + 1) % 4 === 0) process.stdout.write(`Minuto ${(round + 1) / 4}/10: ${durations.length} intentos medidos.\n`);
  }
  const remaining = start + 600_000 - performance.now();
  if (remaining > 0) await new Promise(resolve => setTimeout(resolve, remaining));
} finally { clearInterval(lockTimer); }
durations.sort((a,b) => a-b);
const p95 = durations[Math.ceil(durations.length * 0.95)-1];
const report = {
  date_utc: new Date().toISOString(), elapsed_seconds: (performance.now()-start)/1000,
  runtime: process.version, os: `${platform()} ${release()}`, cpu: cpus()[0]?.model,
  logical_cpus: cpus().length, memory_gib: Math.round(totalmem()/1024**3*10)/10,
  network: 'HTTP loopback; API y PostgreSQL Docker en el mismo host',
  profile: '50 personas ficticias, 5 sesiones EVENTO, GPS OFF, 50 concurrentes cada 15s, 10min',
  attempts: durations.length, statuses, errors, p95_http_ms: p95,
  max_http_ms: durations.at(-1), max_db_lock_waiters_sampled: maximumLocks, lock_samples: lockSamples,
  scan_to_result_seconds: null,
  limitation: 'HTTP medido incluye red loopback y servidor. No mide cámara, GPS, red de aula ni capacidad institucional.',
};
writeFileSync(fileURLToPath(new URL('../../docs/validation/rendimiento-laboratorio.json', import.meta.url)), JSON.stringify(report, null, 2)+'\n');
console.log(JSON.stringify(report, null, 2));
if (durations.length !== 2000 || errors !== 0 || p95 >= 2000) process.exitCode = 1;
