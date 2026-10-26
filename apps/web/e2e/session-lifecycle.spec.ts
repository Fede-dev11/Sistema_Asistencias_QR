import { test, expect } from '@playwright/test';
import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { ssoLogin } from './sso';
test('responsable configura invitados, proyecta QR y cierra actividad',async({page})=>{
 test.setTimeout(90000);
 const fixture=JSON.parse(execFileSync('python3',[fileURLToPath(new URL('../../../tests/fixtures/create-activity.py',import.meta.url)),'EVENTO'],{encoding:'utf8'})) as {activity:string;session:string};
 let qrResponses=0;let firstToken:string|undefined;let serverRotated=false;let scriptErrors=0;
 page.on('pageerror',()=>{scriptErrors++;});
 page.on('response',async response=>{if(response.url().endsWith(`/sessions/${fixture.session}/qr`)&&response.ok()){qrResponses++;const body=await response.json() as {qr_token:string};if(firstToken&&body.qr_token!==firstToken)serverRotated=true;firstToken??=body.qr_token;}});
 await ssoLogin(page,'encargado@example.test');
 await page.getByRole('link',{name:'Gestionar actividades'}).click();await page.getByLabel('Actividad',{exact:true}).selectOption(fixture.activity);await page.getByLabel('Código de sesión',{exact:true}).fill('00000000-0000-0000-0000-000000000401');await page.getByRole('button',{name:'Iniciar sesión',exact:true}).click();await expect(page.getByRole('main').getByRole('alert')).toContainText('No tienes autorización');await page.getByLabel('Código de sesión',{exact:true}).fill(fixture.session);
 await page.getByLabel('Tiempo del formulario de invitados (segundos)',{exact:true}).fill('120');await page.getByLabel('Habilitar invitados sin cuenta').check();await page.getByRole('button',{name:'Guardar configuración'}).click();await expect(page.getByRole('status')).toContainText('Configuración guardada');
 await page.getByLabel('Listado de invitados',{exact:true}).fill(JSON.stringify({invitados:[{cedula:'1701234567',nombres:'Invitado ficticio de prueba'}]}));await page.getByRole('button',{name:'Reemplazar listado'}).click();await expect(page.getByRole('status')).toContainText('Listado guardado: 1');
 await page.getByRole('button',{name:'Iniciar sesión',exact:true}).click();await expect(page.getByAltText('Código QR de asistencia de la sesión activa')).toBeVisible();await expect(page.getByText('Sesión activa. QR disponible.',{exact:true})).toBeVisible();
 const image=page.getByAltText('Código QR de asistencia de la sesión activa');const initialSrc=await image.getAttribute('src');if(!initialSrc)throw new Error('Falta QR');
 try{await expect.poll(()=>serverRotated,{timeout:40000}).toBe(true);await expect.poll(async()=>await image.getAttribute('src')!==initialSrc,{timeout:10000}).toBe(true);}catch{throw new Error(`No se observó renovación: respuestas QR=${qrResponses}, servidor renovó=${serverRotated}, errores JS=${scriptErrors}`);}
 await page.getByRole('button',{name:'Cerrar sesión',exact:true}).click();await expect(page.getByText('Sesión cerrada. Ya no se admiten nuevos registros.',{exact:true})).toBeVisible();await expect(page.getByAltText('Código QR de asistencia de la sesión activa')).toHaveCount(0);
 page.once('dialog',dialog=>dialog.accept());await page.getByRole('button',{name:'Cerrar actividad definitivamente'}).click();await expect(page.getByText('Actividad cerrada definitivamente. Se inició el plazo de conservación de cinco años.',{exact:true})).toBeVisible();
});
