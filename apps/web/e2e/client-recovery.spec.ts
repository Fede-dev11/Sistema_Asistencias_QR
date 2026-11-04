import { expect, test } from '@playwright/test';
import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { ssoLogin } from './sso';

test('recupera QR activo al autenticarse otra vez y logout elimina la sesión local', async ({ page }) => {
  test.setTimeout(90_000);
  const fixture = JSON.parse(execFileSync('python3', [fileURLToPath(new URL('../../../tests/fixtures/create-activity.py', import.meta.url)), 'EVENTO'], { encoding: 'utf8' })) as { activity: string; session: string };
  const projection = `/sesiones/${fixture.session}/proyectar`;
  await ssoLogin(page, 'encargado@example.test');
  await page.goto(`/sesiones/${fixture.session}`); // la sesión vuelve sola por re-autenticación silenciosa
  await page.getByRole('button', { name: 'Iniciar y proyectar QR' }).click();
  await expect(page.getByAltText('Código QR de asistencia de la sesión activa')).toBeVisible();
  await page.goto(projection); // recarga completa: el QR activo se recupera
  await expect(page.getByAltText('Código QR de asistencia de la sesión activa')).toBeVisible();
  await page.goto('/');
  await page.getByRole('link', { name: 'Ingresar con credenciales' }).click();
  await page.getByRole('button', { name: 'Cerrar sesión', exact: true }).click();
  await expect(page.getByRole('button', { name: 'Ingresar con cuenta universitaria' })).toBeVisible();
});
