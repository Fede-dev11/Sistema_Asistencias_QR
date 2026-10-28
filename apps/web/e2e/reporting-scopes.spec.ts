import { expect, test } from '@playwright/test';
import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { ssoLogin, ssoToken } from './sso';

const API = process.env.API_URL ?? 'http://127.0.0.1:18080/api/v1';

test('responsable exporta y directores solo leen su ámbito', async ({ page, request, playwright }) => {
  test.setTimeout(90_000);
  const fixture = JSON.parse(
    execFileSync('python3', [fileURLToPath(new URL('../../../tests/fixtures/create-activity.py', import.meta.url)), 'EVENTO', 'ABIERTO'], { encoding: 'utf8' }),
  ) as { activity: string; session: string; name: string };
  const owner = await ssoToken(playwright, 'encargado@example.test');
  const student = await ssoToken(playwright, 'estudiante@example.test');
  const start = await request.post(`${API}/sessions/${fixture.session}/start`, { headers: { Authorization: `Bearer ${owner}`, 'Idempotency-Key': crypto.randomUUID() } });
  expect(start.ok()).toBeTruthy();
  const qrResponse = await request.get(`${API}/sessions/${fixture.session}/qr`, { headers: { Authorization: `Bearer ${owner}` } });
  const qr = ((await qrResponse.json()) as { qr_token: string }).qr_token;
  const attendance = await request.post(`${API}/attendance/attempts`, {
    headers: { Authorization: `Bearer ${student}`, 'Idempotency-Key': crypto.randomUUID() },
    data: { qr_token: qr, dispositivo_id: crypto.randomUUID() },
  });
  expect(attendance.status()).toBe(201);

  await ssoLogin(page, 'encargado@example.test');
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
  await page.context().clearCookies(); // cierra la sesión SSO del encargado
  await ssoLogin(page, 'director@example.test');
  await page.getByRole('link', { name: 'Consultar asistencias' }).click();
  await page.getByLabel('Actividad').selectOption(fixture.activity);
  await page.getByLabel('Sesión').fill(fixture.session);
  await page.getByRole('button', { name: 'Consultar', exact: true }).click();
  await expect(page.getByText('Persona ficticia 3')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Descargar CSV' })).toHaveCount(0);

  const other = await ssoToken(playwright, 'otro-docente@example.test');
  const denied = await request.get(`${API}/sessions/${fixture.session}/attendances`, { headers: { Authorization: `Bearer ${other}` } });
  expect(denied.status()).toBe(403);
});
