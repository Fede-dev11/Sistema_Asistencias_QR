import { expect, test } from '@playwright/test';
import { ssoLogin } from './sso';

test('administrador registra evidencia GPS de un espacio y abre los miembros de un paralelo', async ({ page }) => {
  await ssoLogin(page, 'admin@dev.geoasist.local');
  await page.getByRole('navigation', { name: 'Principal' }).getByRole('link', { name: 'Espacios' }).click();
  await page.getByRole('row', { name: /^Editar espacio / }).first().click();
  await page.getByRole('tab', { name: 'Evidencias GPS' }).click();
  const reference = `Medición e2e ${crypto.randomUUID().slice(0, 8)}`;
  const form = page.getByRole('form', { name: 'Registrar evidencia' });
  await form.getByLabel('Referencia').fill(reference);
  await form.getByLabel('Realizada en').fill('2026-01-15T10:00');
  await form.getByLabel('Falsos positivos (0 a 1)').fill('0.02');
  await form.getByLabel('Falsos negativos (0 a 1)').fill('0.05');
  await form.getByRole('button', { name: 'Registrar evidencia' }).click();
  await expect(page.getByText('Evidencia registrada.')).toBeVisible();
  await expect(page.getByRole('cell', { name: reference })).toBeVisible();

  await page.getByRole('dialog').getByRole('button', { name: 'Cerrar' }).click();
  await page.getByRole('navigation', { name: 'Principal' }).getByRole('link', { name: 'Carreras y paralelos' }).click();
  await page.getByRole('row', { name: /^Seleccionar / }).first().click();
  await page.getByRole('row', { name: /^Gestionar paralelo / }).first().click();
  await expect(page.getByRole('region', { name: 'Matriculados' })).toBeVisible();
  await page.getByRole('tab', { name: 'Docentes' }).click();
  await expect(page.getByRole('region', { name: 'Docentes' })).toBeVisible();
});
