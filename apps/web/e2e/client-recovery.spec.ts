import { expect, test } from '@playwright/test';
import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { ssoLogin } from './sso';

test('recupera QR activo al autenticarse otra vez y logout elimina la sesión local', async ({ page }) => {
  test.setTimeout(90_000);
  const fixture = JSON.parse(execFileSync('python3', [fileURLToPath(new URL('../../../tests/fixtures/create-activity.py', import.meta.url)), 'EVENTO'], { encoding: 'utf8' })) as { activity: string; session: string };
  async function login() {
    await ssoLogin(page, 'encargado@example.test');
    await page.getByRole('link', { name: 'Gestionar actividades' }).click();
    await page.getByLabel('Actividad', { exact: true }).selectOption(fixture.activity);
    await page.getByLabel('Código de sesión', { exact: true }).fill(fixture.session);
  }
  await login();
  await page.getByRole('button', { name: 'Iniciar sesión', exact: true }).click();
  await expect(page.getByAltText('Código QR de asistencia de la sesión activa')).toBeVisible();
  await login();
  await page.getByRole('button', { name: 'Mostrar QR activo' }).click();
  await expect(page.getByAltText('Código QR de asistencia de la sesión activa')).toBeVisible();
  await expect(page.getByText('Sesión activa recuperada. QR disponible.')).toBeVisible();
  await page.getByRole('link', { name: 'UNL · GeoAsist' }).click();
  await page.getByRole('link', { name: 'Ingresar con credenciales' }).click();
  await page.getByRole('button', { name: 'Cerrar sesión', exact: true }).click();
  await expect(page.getByRole('button', { name: 'Ingresar con cuenta universitaria' })).toBeVisible();
});
