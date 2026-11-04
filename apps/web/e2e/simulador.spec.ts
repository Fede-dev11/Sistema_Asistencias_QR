import { expect, test } from '@playwright/test';
import { createActivity } from './data';
import { ssoLogin, ssoToken } from './sso';

const API = process.env.API_URL ?? 'http://127.0.0.1:18080/api/v1';

test('simulador: mismo dispositivo en dos estudiantes e invitado en evento abierto', async ({ browser, request, playwright }) => {
  test.setTimeout(150_000);
  const fixture = createActivity('EVENTO', 'ABIERTO');
  const owner = await ssoToken(playwright, 'encargado@example.test');
  const start = await request.post(`${API}/sessions/${fixture.session}/start`, { headers: { Authorization: `Bearer ${owner}`, 'Idempotency-Key': crypto.randomUUID() } });
  expect(start.ok()).toBeTruthy();
  const qrToken = async () => ((await (await request.get(`${API}/sessions/${fixture.session}/qr`, { headers: { Authorization: `Bearer ${owner}` } })).json()) as { qr_token: string }).qr_token;
  const baseURL = process.env.WEB_URL ?? 'http://127.0.0.1:3000';
  const device = crypto.randomUUID();

  /** Un estudiante en su propio contexto (sesión SSO aparte) registra con el dispositivo de prueba dado. */
  async function registerAs(email: string) {
    const context = await browser.newContext({ baseURL });
    try {
      const page = await context.newPage();
      await ssoLogin(page, email);
      await page.getByRole('link', { name: 'Simulador de participante' }).click();
      await page.getByLabel('Sesión', { exact: true }).selectOption(fixture.session);
      await page.getByLabel('Token QR').fill(await qrToken());
      await page.getByLabel('Dispositivo de prueba').fill(device);
      await page.getByRole('button', { name: 'Registrar asistencia' }).click();
      const result = page.locator('section.panel', { has: page.getByRole('heading', { name: 'Resultado' }) });
      await expect(result).toBeVisible({ timeout: 15_000 });
      return await result.innerText();
    } finally {
      await context.close();
    }
  }

  expect(await registerAs('estudiante@example.test')).toContain('REGISTRADA');
  expect(await registerAs('otro-estudiante@example.test')).toContain('DISPOSITIVO_YA_REGISTRADO');

  const guest = await (await browser.newContext({ baseURL })).newPage();
  await guest.goto('/pruebas/invitado');
  await guest.getByLabel('ID de sesión').fill(fixture.session);
  await guest.getByRole('button', { name: 'Consultar evento' }).click();
  await expect(guest.getByRole('heading', { name: fixture.name })).toBeVisible();
  await guest.getByLabel('Token QR').fill(await qrToken());
  await guest.getByRole('button', { name: 'Abrir formulario' }).click();
  await guest.getByLabel('Cédula').fill('1710034065');
  await guest.getByLabel('Nombres').fill('Invitado ficticio simulador');
  await guest.getByLabel('Dispositivo de prueba').fill(crypto.randomUUID());
  await guest.getByRole('button', { name: 'Enviar asistencia' }).click();
  await expect(guest.locator('section', { has: guest.getByRole('heading', { name: 'Resultado' }) })).toContainText('REGISTRADA');
});
