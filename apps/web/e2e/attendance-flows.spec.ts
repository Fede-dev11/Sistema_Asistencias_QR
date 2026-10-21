import { test, expect, type APIRequestContext, type Page } from '@playwright/test';
import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const API = process.env.API_URL ?? 'http://127.0.0.1:18080/api/v1';
const PASSWORD = process.env.DEV_SEED_PASSWORD;

type Fixture = { activity: string; session: string; name: string };

function fixture(mode: 'CLASE' | 'EVENTO'): Fixture {
  return JSON.parse(
    execFileSync(
      'python3',
      [fileURLToPath(new URL('../../../tests/fixtures/create-activity.py', import.meta.url)), mode],
      { encoding: 'utf8' },
    ),
  ) as Fixture;
}

async function account(api: APIRequestContext, email: string): Promise<string> {
  if (!PASSWORD) throw new Error('Falta DEV_SEED_PASSWORD de pruebas');
  const response = await api.post(`${API}/auth/login`, {
    data: { correo: email, password: PASSWORD },
  });
  expect(response.ok()).toBeTruthy();
  return ((await response.json()) as { access_token: string }).access_token;
}

async function command(
  api: APIRequestContext,
  bearer: string,
  path: string,
  method: 'post' | 'put',
  data?: unknown,
) {
  const response = await api[method](`${API}${path}`, {
    headers: {
      Authorization: `Bearer ${bearer}`,
      'Idempotency-Key': crypto.randomUUID(),
    },
    data,
  });
  expect(response.ok(), `${method.toUpperCase()} ${path}: ${await response.text()}`).toBeTruthy();
  return response;
}

async function prepare(
  api: APIRequestContext,
  mode: 'CLASE' | 'EVENTO',
  guests: Array<{ cedula: string; nombres: string }> = [],
) {
  const item = fixture(mode);
  const owner = await account(api, mode === 'CLASE' ? 'docente@example.test' : 'encargado@example.test');
  if (mode === 'EVENTO') {
    await command(api, owner, `/activities/${item.activity}/guest-settings`, 'put', {
      habilitado: true,
      tolerancia_invitado_segundos: 120,
    });
    await command(api, owner, `/activities/${item.activity}/guest-list`, 'put', { invitados: guests });
  }
  await command(api, owner, `/sessions/${item.session}/start`, 'post');
  const qr = await api.get(`${API}/sessions/${item.session}/qr`, {
    headers: { Authorization: `Bearer ${owner}` },
  });
  expect(qr.ok()).toBeTruthy();
  return { ...item, owner, qr: ((await qr.json()) as { qr_token: string }).qr_token };
}

async function login(page: Page, email: string) {
  if (!PASSWORD) throw new Error('Falta DEV_SEED_PASSWORD de pruebas');
  await page.goto('/login');
  await page.getByLabel('Correo', { exact: true }).fill(email);
  await page.getByLabel('Contraseña', { exact: true }).fill(PASSWORD);
  await page.getByRole('button', { name: 'Ingresar', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Sesión iniciada' })).toBeVisible();
}

async function scan(page: Page, qr: string) {
  const attendanceLink = page.getByRole('link', { name: 'Registrar asistencia', exact: true });
  if (await attendanceLink.isVisible().catch(() => false)) {
    await attendanceLink.click();
    await expect(page).toHaveURL(/\/asistencia$/);
    await page.evaluate(value => {
      window.location.hash = `qr=${encodeURIComponent(value)}`;
    }, qr);
  } else if (new URL(page.url()).pathname === '/asistencia') {
    await page.evaluate(value => {
      window.location.hash = `qr=${encodeURIComponent(value)}`;
    }, qr);
  } else {
    await page.goto(`/asistencia#qr=${encodeURIComponent(qr)}`);
  }
  await expect(page).not.toHaveURL(/#qr=/);
  await expect(page.getByText('QR leído. Elige cómo registrar la asistencia.')).toBeVisible();
}

test('clase, evento e invitados aplican autorización, plazo recuperable y GPS apagado', async ({
  page,
  request,
}) => {
  test.setTimeout(150_000);
  const guestAllowed = '7012345678';
  const event = await prepare(request, 'EVENTO', [
    { cedula: guestAllowed, nombres: 'Invitada ficticia E2E' },
    { cedula: '0000000008', nombres: 'Cuenta inactiva ficticia' },
  ]);
  const classroom = await prepare(request, 'CLASE');
  const attendanceBodies: unknown[] = [];
  page.on('request', req => {
    if (req.method() === 'POST' && /attendance|guest-registrations\/submit/.test(req.url())) {
      const body = req.postDataJSON() as unknown;
      attendanceBodies.push(body);
    }
  });

  await login(page, 'estudiante@example.test');
  await scan(page, classroom.qr);
  await page.getByRole('button', { name: 'Registrar con mis credenciales' }).click();
  await expect(page.getByRole('status').filter({ hasText: 'CONFIRMADA' })).toBeVisible();

  await page.reload();
  await login(page, 'otro-estudiante@example.test');
  const refreshedClassQr = await request.get(`${API}/sessions/${classroom.session}/qr`, {
    headers: { Authorization: `Bearer ${classroom.owner}` },
  });
  await scan(page, ((await refreshedClassQr.json()) as { qr_token: string }).qr_token);
  await page.getByRole('button', { name: 'Registrar con mis credenciales' }).click();
  await expect(page.locator('.attendance-result[role="alert"]')).toContainText('autorización');

  await page.reload();
  await login(page, 'otro-estudiante@example.test');
  await scan(page, event.qr);
  await page.getByRole('button', { name: 'Registrar con mis credenciales' }).click();
  await expect(page.getByRole('status').filter({ hasText: 'CONFIRMADA' })).toBeVisible();

  await page.reload();
  const currentEventQr = await request.get(`${API}/sessions/${event.session}/qr`, {
    headers: { Authorization: `Bearer ${event.owner}` },
  });
  await scan(page, ((await currentEventQr.json()) as { qr_token: string }).qr_token);
  let openings = 0;
  page.on('request', req => {
    if (req.method() === 'POST' && req.url().endsWith('/guest-registrations')) openings += 1;
  });
  await page.getByRole('button', { name: 'Continuar como invitado sin cuenta' }).click();
  await expect(page.getByLabel('Cédula')).toBeVisible();
  expect(openings).toBe(1);
  await page.reload();
  await expect(page.getByLabel('Cédula')).toBeVisible();
  expect(openings).toBe(1);
  await page.getByLabel('Cédula').fill(guestAllowed);
  await page.getByRole('button', { name: 'Enviar asistencia' }).click();
  await expect(page.getByRole('status').filter({ hasText: 'CONFIRMADA' })).toBeVisible();

  const blockedQr = await request.get(`${API}/sessions/${event.session}/qr`, {
    headers: { Authorization: `Bearer ${event.owner}` },
  });
  await scan(page, ((await blockedQr.json()) as { qr_token: string }).qr_token);
  await page.getByRole('button', { name: 'Continuar como invitado sin cuenta' }).click();
  await page.getByLabel('Cédula').fill('0000000008');
  await page.getByRole('button', { name: 'Enviar asistencia' }).click();
  await expect(page.locator('.attendance-result[role="alert"]')).toContainText('credenciales');
  await page.getByRole('button', { name: 'Escanear otro QR' }).click();

  const outsideQr = await request.get(`${API}/sessions/${event.session}/qr`, {
    headers: { Authorization: `Bearer ${event.owner}` },
  });
  await scan(page, ((await outsideQr.json()) as { qr_token: string }).qr_token);
  await page.getByRole('button', { name: 'Continuar como invitado sin cuenta' }).click();
  await page.getByLabel('Cédula').fill('7099999999');
  await page.getByRole('button', { name: 'Enviar asistencia' }).click();
  await expect(page.locator('.attendance-result[role="alert"]')).toContainText('autorización');

  for (const body of attendanceBodies) {
    expect(JSON.stringify(body)).not.toMatch(/ubicacion|latitud|longitud|precision_m|medido_en/i);
  }
});

test('un formulario abierto sobrevive al QR vencido y el cierre cancela otro formulario', async ({
  page,
  request,
}) => {
  test.setTimeout(150_000);
  const first = await prepare(request, 'EVENTO', [
    { cedula: '7112345678', nombres: 'Invitado plazo ficticio' },
  ]);
  await scan(page, first.qr);
  await page.getByRole('button', { name: 'Continuar como invitado sin cuenta' }).click();
  await page.getByLabel('Cédula').fill('7112345678');
  await page.waitForTimeout(41_000);
  await page.getByRole('button', { name: 'Enviar asistencia' }).click();
  await expect(page.getByRole('status').filter({ hasText: 'CONFIRMADA' })).toBeVisible();

  const second = await prepare(request, 'EVENTO', [
    { cedula: '7212345678', nombres: 'Invitado cierre ficticio' },
  ]);
  await scan(page, second.qr);
  await page.getByRole('button', { name: 'Continuar como invitado sin cuenta' }).click();
  await page.getByLabel('Cédula').fill('7212345678');
  await command(request, second.owner, `/sessions/${second.session}/close`, 'post');
  await page.getByRole('button', { name: 'Enviar asistencia' }).click();
  await expect(page.locator('.attendance-result[role="alert"]')).toContainText('no admite nuevos registros');
});
