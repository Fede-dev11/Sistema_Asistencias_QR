import { expect, test, type APIRequestContext, type Page } from '@playwright/test';
import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const API = process.env.API_URL ?? 'http://127.0.0.1:18080/api/v1';
const PASSWORD = process.env.DEV_SEED_PASSWORD;

async function account(api: APIRequestContext, email: string) {
  if (!PASSWORD) throw new Error('Falta DEV_SEED_PASSWORD');
  const response = await api.post(`${API}/auth/login`, { data: { correo: email, password: PASSWORD } });
  expect(response.ok()).toBeTruthy();
  return ((await response.json()) as { access_token: string }).access_token;
}

async function login(page: Page, email: string) {
  if (!PASSWORD) throw new Error('Falta DEV_SEED_PASSWORD');
  await page.goto('/login');
  await page.getByLabel('Correo', { exact: true }).fill(email);
  await page.getByLabel('Contraseña', { exact: true }).fill(PASSWORD);
  await page.getByRole('button', { name: 'Ingresar', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Sesión iniciada' })).toBeVisible();
}

test('responsable exporta y directores solo leen su ámbito', async ({ page, request }) => {
  test.setTimeout(90_000);
  const fixture = JSON.parse(
    execFileSync('python3', [fileURLToPath(new URL('../../../tests/fixtures/create-activity.py', import.meta.url)), 'EVENTO', 'ABIERTO'], { encoding: 'utf8' }),
  ) as { activity: string; session: string; name: string };
  const owner = await account(request, 'encargado@example.test');
  const student = await account(request, 'estudiante@example.test');
  const start = await request.post(`${API}/sessions/${fixture.session}/start`, { headers: { Authorization: `Bearer ${owner}`, 'Idempotency-Key': crypto.randomUUID() } });
  expect(start.ok()).toBeTruthy();
  const qrResponse = await request.get(`${API}/sessions/${fixture.session}/qr`, { headers: { Authorization: `Bearer ${owner}` } });
  const qr = ((await qrResponse.json()) as { qr_token: string }).qr_token;
  const attendance = await request.post(`${API}/attendance/attempts`, {
    headers: { Authorization: `Bearer ${student}`, 'Idempotency-Key': crypto.randomUUID() },
    data: { qr_token: qr, dispositivo_id: crypto.randomUUID() },
  });
  expect(attendance.status()).toBe(201);

  await login(page, 'encargado@example.test');
  await page.getByRole('link', { name: 'Consultar asistencias' }).click();
  await page.getByLabel('Actividad').selectOption(fixture.activity);
  await page.getByLabel('Sesión').fill(fixture.session);
  await page.getByRole('button', { name: 'Consultar', exact: true }).click();
  await expect(page.getByText('Persona ficticia 3')).toBeVisible();
  await expect(page.getByText('Asistencia registrada.')).toBeVisible();
  const download = page.waitForEvent('download');
  await page.getByRole('button', { name: 'Descargar CSV' }).click();
  expect((await download).suggestedFilename()).toBe('asistencias.csv');
  await page.getByRole('button', { name: 'Consultar auditoría' }).click();
  await expect(page.getByText('EXPORTAR_ASISTENCIAS')).toBeVisible();

  await page.reload();
  await login(page, 'director@example.test');
  await page.getByRole('link', { name: 'Consultar asistencias' }).click();
  await page.getByLabel('Actividad').selectOption(fixture.activity);
  await page.getByLabel('Sesión').fill(fixture.session);
  await page.getByRole('button', { name: 'Consultar', exact: true }).click();
  await expect(page.getByText('Persona ficticia 3')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Descargar CSV' })).toHaveCount(0);

  const other = await account(request, 'otro-docente@example.test');
  const denied = await request.get(`${API}/sessions/${fixture.session}/attendances`, { headers: { Authorization: `Bearer ${other}` } });
  expect(denied.status()).toBe(403);
});
