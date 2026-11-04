import { expect, test, type APIRequestContext, type Page } from '@playwright/test';
import { createActivity, sql } from './data';
import { ssoLogin, ssoToken } from './sso';

const API = process.env.API_URL ?? 'http://127.0.0.1:18080/api/v1';
const ABSENT = '00000000-0000-0000-0000-000000000106';

test.use({ viewport: { width: 1366, height: 850 } });
test.setTimeout(120_000);

// El SSO limita a 5 ingresos por minuto por correo: el docente ingresa una sola vez y comparte la página.
let docente: Page;
let owner: string;
test.describe.configure({ mode: 'serial' });
test.beforeAll(async ({ browser, playwright }) => {
  owner = await ssoToken(playwright, 'docente@example.test');
  docente = await browser.newPage({ baseURL: test.info().project.use.baseURL, viewport: { width: 1366, height: 850 } });
  await ssoLogin(docente, 'docente@example.test');
});
test.afterAll(async () => { await docente.close(); });

/** Los clics se cuentan en el propio flujo: cada `click()` pasa por aquí. */
function counter() {
  let n = 0;
  return { click: async (target: { click(): Promise<void> }) => { await target.click(); n += 1; }, get count() { return n; } };
}

async function startAndClose(request: APIRequestContext, token: string, session: string, kinds: string[]) {
  for (const kind of kinds) {
    const res = await request.post(`${API}/sessions/${session}/${kind}`, { headers: { Authorization: `Bearer ${token}`, 'Idempotency-Key': crypto.randomUUID() } });
    expect(res.ok()).toBeTruthy();
  }
}

/** Botones de acción dentro de `main` con los paneles cerrados; las pestañas (`role=tab`) son navegación y no cuentan. */
async function mainButtons(page: Page, path: string) {
  await page.goto(path);
  await page.waitForLoadState('networkidle');
  await expect(page.locator('main')).toBeVisible();
  return page.locator('main button:visible:not([role=tab])').count();
}

test('SC-001 desde Hoy, un clic inicia la sesión y abre la proyección con el QR', async () => {
  const page = docente;
  const fixture = createActivity('CLASE');
  await page.goto('/hoy');
  const c = counter();
  await c.click(page.locator('li', { hasText: fixture.name }).getByRole('button', { name: 'Iniciar y proyectar QR' }));
  await expect(page).toHaveURL(new RegExp(`/sesiones/${fixture.session}/proyectar$`));
  await expect(page.getByAltText('Código QR de asistencia de la sesión activa')).toBeVisible();
  console.log(`SC-001 clics=${c.count} (meta 1)`);
  expect(c.count).toBeLessThanOrEqual(1);
});

test('SC-002 recargar una página interna conserva la sesión sin clics', async () => {
  const page = docente;
  await page.goto('/actividades');
  await expect(page.getByRole('heading', { name: 'Actividades', exact: true })).toBeVisible();
  await page.reload();
  await expect(page.getByRole('heading', { name: 'Actividades', exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Salir' })).toBeVisible();
  await expect(page).toHaveURL(/\/actividades$/);
  console.log('SC-002 clics=0 (meta 0), misma página tras recargar');
});

test('SC-003 justificar una ausencia en dos clics desde el resumen de la sesión cerrada', async ({ request }) => {
  const page = docente;
  const fixture = createActivity('CLASE');
  sql(`INSERT INTO estudiantes_autorizados VALUES ('${fixture.activity}','${ABSENT}',true);`);
  await startAndClose(request, owner, fixture.session, ['start', 'close']);
  await page.goto(`/sesiones/${fixture.session}?tab=resumen`);
  const absences = page.getByRole('region', { name: 'Ausencias' });
  const c = counter();
  await c.click(absences.getByRole('button', { name: /^Justificar / }).first());
  await absences.getByLabel('Razón de justificación').fill('Certificado médico');
  await c.click(absences.getByRole('button', { name: 'Confirmar justificación' }));
  await expect(absences.getByText('Justificada')).toHaveCount(1);
  console.log(`SC-003 clics=${c.count} (meta 2)`);
  expect(c.count).toBeLessThanOrEqual(2);
});

test('SC-004 el inicio del director muestra los paralelos bajo umbral sin clics', async ({ page, request }) => {
  const fixture = createActivity('CLASE');
  // El paralelo A de la carrera 1 (matriculado 102, ausente) queda en 0 %: bajo el umbral.
  sql(`INSERT INTO actividad_paralelos VALUES ('${fixture.activity}','00000000-0000-0000-0000-000000000001','00000000-0000-0000-0000-000000000011');`);
  await startAndClose(request, owner, fixture.session, ['start', 'close']);
  await ssoLogin(page, 'director@example.test');
  await expect(page).toHaveURL(/\/inicio$/);
  const summary = page.getByText(/paralelos están por debajo/);
  await expect(summary).toBeVisible();
  console.log(`SC-004 clics=0 (meta 0): "${(await summary.first().innerText()).trim()}"`);
});

/** Deja al docente sin actividades previas (las pasa a otro-docente, que ninguna prueba usa para listar, para no saturar a los demás) para medir con un conjunto conocido. */
const DOCENTE = '00000000-0000-0000-0000-000000000200';
const soloNuevas = () => sql(`UPDATE actividades SET responsable_id='00000000-0000-0000-0000-000000000205' WHERE responsable_id='${DOCENTE}';`);

test('SC-005 altura de Actividades con 10 actividades', async () => {
  const page = docente;
  soloNuevas();
  for (let i = 0; i < 10; i++) createActivity('CLASE');
  await page.goto('/actividades');
  await page.waitForLoadState('networkidle');
  const rows = await page.locator('main tbody tr').count();
  expect(rows).toBe(10);
  const height = await page.evaluate(() => document.documentElement.scrollHeight);
  console.log(`SC-005 alto=${height}px con ${rows} actividades (meta ≤ 900)`);
  expect(height).toBeLessThanOrEqual(900);
});

test('SC-006 docente: botones por pantalla', async ({ request }) => {
  const page = docente;
  soloNuevas();
  createActivity('CLASE');
  const closed = createActivity('CLASE');
  sql(`INSERT INTO estudiantes_autorizados VALUES ('${closed.activity}','${ABSENT}',true);`);
  await startAndClose(request, owner, closed.session, ['start', 'close']);
  const measured: Record<string, number> = {};
  for (const [name, path] of [['hoy', '/hoy'], ['actividades', '/actividades'], ['actividades/[id]', `/actividades/${closed.activity}`], ['sesiones/[id] cerrada', `/sesiones/${closed.session}`], ['estadisticas', '/estadisticas'], ['reportes', '/reportes']] as const) {
    measured[name] = await mainButtons(page, path);
  }
  console.log(`SC-006 docente botones=${JSON.stringify(measured)} (meta ≤ 8)`);
  for (const [name, n] of Object.entries(measured)) expect(n, name).toBeLessThanOrEqual(8);
});

test('SC-006 administrador: botones por pantalla', async ({ page }) => {
  await ssoLogin(page, 'admin@dev.geoasist.local');
  const measured: Record<string, number> = {};
  for (const name of ['personas', 'carreras', 'espacios', 'importar']) measured[name] = await mainButtons(page, `/admin/${name}`);
  console.log(`SC-006 admin botones=${JSON.stringify(measured)} (meta ≤ 8)`);
  for (const [name, n] of Object.entries(measured)) expect(n, name).toBeLessThanOrEqual(8);
});
