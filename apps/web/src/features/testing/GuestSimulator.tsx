'use client';

import { useEffect, useState, type FormEvent } from 'react';
import type { components } from '../../generated/api';
import { request } from '../../lib/api/client';
import { ProblemMessage } from '../common/ProblemMessage';
import { createTestDeviceId, loadTestDeviceId, saveTestDeviceId } from './testDevice';

type Method = components['schemas']['OpenEvent']['metodo_verificacion'];
type OpenEvent = components['schemas']['OpenEvent'];
type Start = components['schemas']['GuestStartResult'];
type Receipt = components['schemas']['AttendanceReceipt'] | components['schemas']['PendingReceipt'];
const usesQr = (method: Method) => method.includes('QR');

export function GuestSimulator() {
  const [sessionId, setSessionId] = useState('');
  const [event, setEvent] = useState<OpenEvent | null>(null);
  const [method, setMethod] = useState<Method>('SOLO_QR');
  const [qrToken, setQrToken] = useState('');
  const [registration, setRegistration] = useState<Start | null>(null);
  const [cedula, setCedula] = useState('');
  const [names, setNames] = useState('');
  const [deviceId, setDeviceId] = useState('');
  const [withLocation, setWithLocation] = useState(false);
  const [latitude, setLatitude] = useState('');
  const [longitude, setLongitude] = useState('');
  const [precision, setPrecision] = useState('');
  const [result, setResult] = useState<Receipt | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => { void Promise.resolve().then(() => setDeviceId(loadTestDeviceId())); }, []);
  if (process.env.NEXT_PUBLIC_HERRAMIENTAS_PRUEBA !== 'true') return null;

  async function findEvent(event_: FormEvent) {
    event_.preventDefault(); setLoading(true); setError(null); setEvent(null); setRegistration(null); setResult(null);
    try { const found = await request<OpenEvent>(`/events/open/${encodeURIComponent(sessionId)}`, { authentication: 'none' }); setEvent(found); setMethod(found.metodo_verificacion); }
    catch (caught) { setError(caught); }
    finally { setLoading(false); }
  }

  async function start(event_: FormEvent) {
    event_.preventDefault(); setLoading(true); setError(null); setRegistration(null); setResult(null);
    try {
      const body = usesQr(method) ? { qr_token: qrToken } : { sesion_id: sessionId };
      setRegistration(await request<Start>('/guest-registrations', { method: 'POST', authentication: 'none', body: JSON.stringify(body) }));
    } catch (caught) { setError(caught); }
    finally { setLoading(false); }
  }

  async function submit(event_: FormEvent) {
    event_.preventDefault(); if (!registration) return; setLoading(true); setError(null); setResult(null);
    try {
      const body: components['schemas']['GuestSubmitRequest'] = {
        registro_invitado_token: registration.registro_invitado_token, cedula,
        ...(registration.evento_abierto ? { nombres: names } : {}), dispositivo_id: saveTestDeviceId(deviceId),
        ...(withLocation ? { ubicacion: { latitud: Number(latitude), longitud: Number(longitude), precision_m: Number(precision), medido_en: new Date().toISOString() } } : {})
      };
      setResult(await request<Receipt>('/guest-registrations/submit', {
        method: 'POST', authentication: 'none', headers: { 'Idempotency-Key': crypto.randomUUID() }, body: JSON.stringify(body)
      }));
    } catch (caught) { setError(caught); }
    finally { setLoading(false); }
  }

  return <section>
    <p role="status">Herramienta de pruebas: en producción el registro de asistencia es desde la app.</p>
    <h1>Simulador de invitado</h1>
    <form onSubmit={findEvent}><h2>Consultar evento abierto</h2><label>ID de sesión<input required value={sessionId} onChange={e => setSessionId(e.target.value)} /></label><button disabled={loading} type="submit">Consultar evento</button></form>
    {event && <section className="panel" aria-live="polite"><h2>{event.nombre}</h2><p>{event.espacio ?? 'Sin espacio'} · {event.metodo_verificacion}</p></section>}
    <form onSubmit={start}><h2>Iniciar registro</h2>
      {!event && <label>Método de verificación<select value={method} onChange={e => setMethod(e.target.value as Method)}><option>SOLO_QR</option><option>QR_Y_GPS</option><option>SOLO_GPS</option><option>NINGUNO</option></select></label>}
      {usesQr(method) && <label>Token QR<input required value={qrToken} onChange={e => setQrToken(e.target.value)} /></label>}
      <button disabled={loading || (!usesQr(method) && !sessionId)} type="submit">Abrir formulario</button>
    </form>
    {registration && <form onSubmit={submit}><h2>Datos del invitado</h2>
      <label>Cédula<input required value={cedula} onChange={e => setCedula(e.target.value)} /></label>
      {registration.evento_abierto && <label>Nombres<input required value={names} onChange={e => setNames(e.target.value)} /></label>}
      <label>Dispositivo de prueba<input required value={deviceId} onChange={e => { setDeviceId(e.target.value); saveTestDeviceId(e.target.value); }} /></label>
      <button type="button" onClick={() => setDeviceId(createTestDeviceId())}>Nuevo dispositivo</button>
      <fieldset><legend>Ubicación manual</legend><label><input type="checkbox" checked={withLocation} onChange={e => setWithLocation(e.target.checked)} /> Incluir ubicación</label>
        {withLocation && <div className="grid"><label>Latitud<input required type="number" step="any" value={latitude} onChange={e => setLatitude(e.target.value)} /></label><label>Longitud<input required type="number" step="any" value={longitude} onChange={e => setLongitude(e.target.value)} /></label><label>Precisión (m)<input required min="0" type="number" step="any" value={precision} onChange={e => setPrecision(e.target.value)} /></label></div>}
      </fieldset><button disabled={loading} type="submit">Enviar asistencia</button>
    </form>}
    {result && <section className="panel" aria-live="polite"><h2>Resultado</h2><p><strong>{result.resultado}</strong>{'code' in result ? ` · ${result.code}` : ''}: {result.mensaje}</p></section>}
    <ProblemMessage error={error} />
  </section>;
}
