import { expect, test } from '@playwright/test';
import { createActivity, sql } from './data';
import { ssoLogin } from './sso';

test('docente elige actividad y sesión, inicia, registra manual, cierra y justifica', async ({ page }) => {
  test.setTimeout(90_000);
  const fixture = createActivity('CLASE');
  // 106 queda autorizado y sin asistencia: será la ausencia a justificar.
  sql(`INSERT INTO estudiantes_autorizados VALUES ('${fixture.activity}','00000000-0000-0000-0000-000000000106',true);`);
  await ssoLogin(page, 'docente@example.test');
  await page.getByRole('link', { name: 'Sesiones' }).click();
  await page.getByLabel('Actividad', { exact: true }).selectOption(fixture.activity);
  await page.getByRole('button', { name: 'Elegir' }).click();
  await page.getByRole('button', { name: 'Iniciar sesión', exact: true }).click();
  await expect(page.getByAltText('Código QR de asistencia de la sesión activa')).toBeVisible();

  const manual = page.locator('form', { has: page.getByRole('heading', { name: 'Asistencia manual' }) });
  await manual.getByRole('combobox').selectOption('00000000-0000-0000-0000-000000000102');
  await manual.getByRole('textbox').fill('Llegó sin teléfono');
  await manual.getByRole('button', { name: 'Registrar asistencia' }).click();
  await expect(page.getByText('Asistencia manual registrada.')).toBeVisible();

  await page.getByRole('button', { name: 'Cerrar sesión', exact: true }).click();
  await expect(page.getByText('Sesión cerrada. Ya no se admiten nuevos registros.', { exact: true })).toBeVisible();

  const absences = page.getByRole('region', { name: 'Ausencias' });
  await expect(absences.getByText(/— AUSENTE/)).toHaveCount(1);
  await absences.getByRole('button', { name: /^Justificar / }).click();
  await absences.getByLabel('Razón de justificación').fill('Certificado médico');
  await absences.getByRole('button', { name: 'Confirmar justificación' }).click();
  await expect(absences.getByText(/— AUSENTE/)).toHaveCount(0);
  await expect(absences.getByText(/— JUSTIFICAD/)).toHaveCount(1);
});
