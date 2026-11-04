import { expect, test } from '@playwright/test';
import { createActivity } from './data';
import { ssoLogin, ssoToken } from './sso';

const API = process.env.API_URL ?? 'http://127.0.0.1:18080/api/v1';

test('director institucional consulta estadísticas por carrera', async ({ page, request, playwright }) => {
  // Una sesión cerrada propia garantiza que la tabla tenga al menos una fila.
  const fixture = createActivity('CLASE');
  const owner = await ssoToken(playwright, 'docente@example.test');
  for (const kind of ['start', 'close']) {
    const res = await request.post(`${API}/sessions/${fixture.session}/${kind}`, { headers: { Authorization: `Bearer ${owner}`, 'Idempotency-Key': crypto.randomUUID() } });
    expect(res.ok()).toBeTruthy();
  }
  await ssoLogin(page, 'director@example.test');
  await page.getByRole('link', { name: 'Estadísticas', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Estadísticas', exact: true })).toBeVisible();
  const careers = page.getByRole('region', { name: 'Estadísticas por carrera' });
  await careers.getByRole('button', { name: 'Consultar' }).click();
  await expect(careers.getByRole('table')).toBeVisible();
});
