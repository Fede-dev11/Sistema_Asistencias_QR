'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import Image from 'next/image';
import { useRouter } from 'next/navigation';
import QRCode from 'qrcode';
import type { components } from '../../generated/api';
import { ApiError, request } from '../../lib/api/client';
import { ProblemMessage } from '../common/ProblemMessage';
import { Button, ButtonLink } from '../ui/Button';
import { ConfirmInline } from '../ui/ConfirmInline';
import { loadSession, type SessionInfo } from './loadSession';

type Qr = components['schemas']['Qr'];

/** Pantalla de aula: QR grande, cuenta regresiva, asistentes en vivo. Sin chrome de la app (ver `.projection` en globals.css). */
export function ProjectionView({ id }: { id: string }) {
  const router = useRouter();
  const [info, setInfo] = useState<SessionInfo | null>(null);
  const [ended, setEnded] = useState(false);
  const [image, setImage] = useState('');
  const [left, setLeft] = useState(0);
  const remaining = useRef(0);
  const [present, setPresent] = useState(0);
  const [error, setError] = useState<unknown>(null);
  const [confirming, setConfirming] = useState(false);
  const [note, setNote] = useState('');
  const active = info?.session.estado === 'ACTIVA' && !ended;

  useEffect(() => { loadSession(id).then(setInfo, setError); }, [id]);

  const fetchQr = useCallback(async () => {
    try {
      const qr = await request<Qr>(`/sessions/${id}/qr`);
      setImage(await QRCode.toDataURL(`geoasist://asistencia#qr=${encodeURIComponent(qr.qr_token)}`, { width: 640, margin: 2, errorCorrectionLevel: 'M' }));
      remaining.current = qr.vigencia_segundos; setLeft(qr.vigencia_segundos); setError(null);
    } catch (e) {
      setError(e);
      if (e instanceof ApiError && [403, 409, 410].includes(e.status)) setEnded(true);
    }
  }, [id]);

  useEffect(() => { // cada segundo descuenta; al llegar a 0 pide otro QR (5 s de espera si falla)
    if (!active) return;
    const tick = () => {
      if (remaining.current <= 0) { remaining.current = 5; void fetchQr(); } else { remaining.current -= 1; setLeft(remaining.current); }
    };
    const first = setTimeout(tick, 0), t = setInterval(tick, 1000);
    return () => { clearTimeout(first); clearInterval(t); };
  }, [active, fetchQr]);
  useEffect(() => {
    if (!active) return;
    const poll = () => request<{ total: number }>(`/sessions/${id}/attendances?size=1&estado=CONFIRMADA`).then(p => setPresent(p.total), () => undefined);
    void poll();
    const t = setInterval(poll, 5000);
    return () => clearInterval(t);
  }, [active, id]);

  async function fullscreen() {
    setNote('');
    try { await document.documentElement.requestFullscreen(); }
    catch { setNote('El navegador no permitió la pantalla completa. Usa F11.'); }
  }
  async function close() {
    setConfirming(false);
    try { await request(`/sessions/${id}/close`, { method: 'POST', headers: { 'Idempotency-Key': crypto.randomUUID() } }); router.push(`/sesiones/${id}?tab=resumen`); }
    catch (e) { setError(e); }
  }

  return <div className="projection">
    <header>
      <h1>{info ? `${info.activity.nombre}${info.space ? ` · ${info.space}` : ''}` : 'Sesión'}</h1>
      {active && <div><Button variant="secondary" onClick={() => void fullscreen()}>Pantalla completa</Button> <Button variant="danger" onClick={() => setConfirming(true)}>Cerrar sesión</Button></div>}
    </header>
    {confirming && <ConfirmInline danger message="¿Cerrar la sesión? Ya no se admitirán registros." confirmLabel="Cerrar sesión" onConfirm={() => void close()} onCancel={() => setConfirming(false)} />}
    {note && <p role="status">{note}</p>}
    <ProblemMessage error={error} />
    {info && !active && <div className="panel"><p role="status">Esta sesión no está activa ({info.session.estado.toLowerCase()}).</p><ButtonLink href={`/sesiones/${id}`}>Volver a la sesión</ButtonLink></div>}
    {active && <>
      <div className="projection-qr">{image ? <Image unoptimized src={image} alt="Código QR de asistencia de la sesión activa" width={640} height={640} /> : <p role="status">Generando QR…</p>}</div>
      <footer><span>Nuevo código en {left} s</span><span aria-live="polite">{present} presentes</span></footer>
    </>}
  </div>;
}
