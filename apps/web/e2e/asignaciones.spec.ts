import { expect, test } from '@playwright/test';
import { execFileSync } from 'node:child_process';
import { ssoLogin } from './sso';

/** Crea un segundo director de carrera candidato (no es aún director de la carrera 1) para probar asignar/reemplazar. */
function seedSecondDirector() {
  const sqlText = `
INSERT INTO personas VALUES ('00000000-0000-0000-0000-000000000110', '0100000090', 'Persona ficticia 11', 'INSTITUCIONAL') ON CONFLICT DO NOTHING;
INSERT INTO cuentas VALUES ('00000000-0000-0000-0000-000000000210', '00000000-0000-0000-0000-000000000110', 'DEV', 'director-2@example.test', 'x', true) ON CONFLICT DO NOTHING;
INSERT INTO roles_cuenta VALUES ('00000000-0000-0000-0000-000000000210', 'DIRECTOR_CARRERA') ON CONFLICT DO NOTHING;
`;
  execFileSync('docker', ['exec', '-i', process.env.GEOASIST_DB_CONTAINER ?? 'geoasist-db-1', 'psql', '-v', 'ON_ERROR_STOP=1', '-U', 'geoasist', '-d', 'geoasist'], { input: sqlText, encoding: 'utf8', stdio: ['pipe', 'ignore', 'inherit'] });
}

test('administrador asigna, reemplaza y retira directores de una carrera', async ({ page }) => {
  test.setTimeout(120_000);
  seedSecondDirector();
  await ssoLogin(page, 'admin@dev.geoasist.local');

  await page.goto('/admin/asignaciones');
  await expect(page.getByRole('heading', { name: 'Asignaciones académicas' })).toBeVisible();

  await page.getByRole('tab', { name: 'Directores de carrera' }).click();

  // Busca y selecciona la carrera 1 sin recorrer listas completas.
  await page.getByLabel('Buscar carrera').fill('CARRERA-1');
  await page.getByRole('row', { name: 'Seleccionar Carrera ficticia 1' }).click();

  // La carrera ya tiene un director vigente (persona 104, director-carrera@example.test).
  await expect(page.getByRole('cell', { name: 'Persona ficticia 5', exact: true })).toBeVisible();

  // Asigna un segundo director válido solo para esta carrera.
  await page.getByRole('button', { name: 'Asignar director' }).click();
  await expect(page.getByRole('dialog', { name: 'Asignar director' })).toBeVisible();
  await page.getByLabel('Buscar director').fill('director-2');
  await page.getByRole('button', { name: 'Asignar director-2@example.test como director' }).click();
  await expect(page.getByText(/¿Asignar a Persona ficticia 11 como director de Carrera ficticia 1\?/)).toBeVisible();
  await page.getByRole('button', { name: 'Confirmar' }).click();
  await expect(page.getByText(/Persona ficticia 11 fue asignado como director/)).toBeVisible();

  // Reemplaza el director vigente de la carrera.
  await page.getByRole('button', { name: 'Reemplazar Persona ficticia 5' }).click();
  await expect(page.getByRole('dialog', { name: 'Reemplazar director' })).toBeVisible();
  await page.getByLabel('Buscar director').fill('director-2');
  await page.getByRole('button', { name: 'Reemplazar director-2@example.test como director' }).click();
  await expect(page.getByText(/¿Reemplazar a Persona ficticia 5 por Persona ficticia 11 como director de Carrera ficticia 1\?/)).toBeVisible();
  await page.getByRole('button', { name: 'Confirmar' }).click();
  await expect(page.getByText(/Persona ficticia 11 reemplaza a Persona ficticia 5/)).toBeVisible();

  // Retira el director.
  await page.getByRole('button', { name: 'Retirar Persona ficticia 11' }).click();
  await expect(page.getByRole('dialog', { name: 'Retirar director' })).toBeVisible();
  await page.getByRole('button', { name: 'Confirmar retiro' }).click();
  await expect(page.getByText(/Persona ficticia 11 fue retirado de Carrera ficticia 1/)).toBeVisible();
});

test('administrador filtra en cascada carrera → paralelo → clase → persona y asigna responsable', async ({ page }) => {
  test.setTimeout(120_000);
  await ssoLogin(page, 'admin@dev.geoasist.local');

  await page.goto('/admin/asignaciones');
  await expect(page.getByRole('heading', { name: 'Asignaciones académicas' })).toBeVisible();
  await page.getByRole('tab', { name: 'Responsables de clase' }).click();

  // Carrera 1
  await page.getByLabel('Buscar carrera').fill('CARRERA-1');
  await page.getByRole('row', { name: 'Seleccionar Carrera ficticia 1' }).click();

  // Paralelo 11 (código A, 2 matrículas vigentes)
  await page.getByLabel('Paralelo').selectOption('A · 2 matriculados');

  // Filtra por materia/clase
  await page.getByLabel('Materia o clase').fill('Actividad ficticia 1');
  await expect(page.getByRole('cell', { name: 'Actividad ficticia 1', exact: true })).toBeVisible();

  // Gestiona el responsable de la clase
  await page.getByRole('button', { name: 'Gestionar responsables de Actividad ficticia 1' }).click();
  await expect(page.getByRole('dialog', { name: 'Responsable de Actividad ficticia 1' })).toBeVisible();
  await expect(page.getByText(/Responsable actual:/)).toBeVisible();

  // Solo aparecen docentes/directores válidos para la clase seleccionada.
  // Cambia el responsable al director de carrera (persona 104, director-carrera@example.test).
  await page.getByLabel('Buscar persona').fill('director-carrera');
  await page.getByRole('button', { name: 'Asignar director-carrera@example.test como responsable' }).click();
  await expect(page.getByText(/Responsable de Actividad ficticia 1 actualizado a Persona ficticia 5/)).toBeVisible();
});
