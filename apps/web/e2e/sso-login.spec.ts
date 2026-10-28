import { expect, test } from '@playwright/test';

test('ingreso por SSO en menos de 30 s y segundo ingreso sin contraseña', async ({ page }) => {
  const password = process.env.DEV_SEED_PASSWORD;
  if (!password) throw new Error('Falta DEV_SEED_PASSWORD');
  const started = Date.now();
  await page.goto('/login');
  await page.getByRole('button', { name: 'Ingresar con cuenta universitaria' }).click();
  await page.getByLabel('Correo', { exact: true }).fill('docente@example.test');
  await page.getByLabel('Contraseña', { exact: true }).fill(password);
  await page.getByRole('button', { name: 'Ingresar', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Sesión iniciada' })).toBeVisible();
  await expect(page.getByRole('link', { name: 'Gestionar actividades' })).toBeVisible();
  const elapsed = Date.now() - started;
  console.log(`SC-001 ingreso SSO de punta a punta: ${elapsed} ms`);
  expect(elapsed).toBeLessThan(30_000);

  // Cierra solo la sesión de GeoAsist; la sesión SSO (cookie del servicio) sigue abierta.
  await page.getByRole('button', { name: 'Cerrar sesión', exact: true }).click();
  await page.getByRole('button', { name: 'Ingresar con cuenta universitaria' }).click();
  await expect(page.getByRole('heading', { name: 'Sesión iniciada' })).toBeVisible();
  await expect(page.getByLabel('Contraseña', { exact: true })).toHaveCount(0);
});
