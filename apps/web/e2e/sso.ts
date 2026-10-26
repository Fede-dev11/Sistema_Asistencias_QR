import { expect, type Page, type PlaywrightWorkerArgs } from '@playwright/test';

/** Ingresa por la cuenta universitaria; si ya existe sesión SSO, el servicio no pide contraseña. */
export async function ssoLogin(page: Page, email: string, password = process.env.DEV_SEED_PASSWORD) {
  if (!password) throw new Error('Falta DEV_SEED_PASSWORD');
  await page.goto('/login');
  await page.getByRole('button', { name: 'Ingresar con cuenta universitaria' }).click();
  const form = page.getByLabel('Correo', { exact: true });
  const signedIn = page.getByRole('heading', { name: 'Sesión iniciada' });
  await expect(form.or(signedIn)).toBeVisible();
  if (await form.isVisible()) {
    await form.fill(email);
    await page.getByLabel('Contraseña', { exact: true }).fill(password);
    await page.getByRole('button', { name: 'Ingresar', exact: true }).click();
  }
  await expect(signedIn).toBeVisible();
}

/** Token de la API por el flujo CAS completo; la cuenta pasa a proveedor CAS y el inicio local deja de servir. */
export async function ssoToken(playwright: PlaywrightWorkerArgs['playwright'], email: string, password = process.env.DEV_SEED_PASSWORD) {
  if (!password) throw new Error('Falta DEV_SEED_PASSWORD');
  const api = process.env.API_URL ?? 'http://127.0.0.1:18080/api/v1';
  const config = (await (await fetch(`${api}/auth/sso-config`)).json()) as { login_url: string; web_service: string };
  const sso = await playwright.request.newContext({ maxRedirects: 0 });
  try {
    const form = await (await sso.get(`${config.login_url}?service=${encodeURIComponent(config.web_service)}`)).text();
    const csrf = /name="_csrf" value="([^"]+)"/.exec(form)?.[1] ?? ''; // token enmascarado que Spring pinta en el formulario
    const res = await sso.post(config.login_url, { form: { correo: email, password, service: config.web_service, _csrf: csrf } });
    const ticket = new URL(res.headers().location ?? 'http://x/').searchParams.get('ticket');
    if (!ticket) throw new Error(`SSO rechazó el ingreso (HTTP ${res.status()})`);
    const cas = await fetch(`${api}/auth/cas`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ ticket, service: config.web_service }) });
    if (!cas.ok) throw new Error(`/auth/cas respondió ${cas.status}`);
    return ((await cas.json()) as { access_token: string }).access_token;
  } finally {
    await sso.dispose();
  }
}
