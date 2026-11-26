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
import styles from './sessions.module.css';

type Qr = components['schemas']['Qr'];

/** Pantalla de aula: QR grande, cuenta regresiva, asistentes en vivo. Sin chrome de la app (ver `.projection` en globals.css). */
export function ProjectionView({ id }: { id: string }) {
  const router = useRouter();
  const [info, setInfo] = useState<SessionInfo | null>(null);
  const [ended, setEnded] = useState(false);
  const [image, setImage] = useState('');
  const [left, setLeft] = useState(0);
  const remaining = useRef(0);
  const validUntil = useRef(0);
  const refreshAt = useRef(0);
  const fetching = useRef(false);
  const [present, setPresent] = useState(0);
  const [attendanceError, setAttendanceError] = useState<unknown>(null);
  const [error, setError] = useState<unknown>(null);
  const [confirming, setConfirming] = useState(false);
  const [note, setNote] = useState('');
  const active = info?.session.estado === 'ACTIVA' && !ended;

  useEffect(() => { loadSession(id).then(setInfo, setError); }, [id]);

  const fetchQr = useCallback(async () => {
    if (fetching.current) return;
    fetching.current = true;
    const requestedAt = performance.now();
    try {
      const qr = await request<Qr>(`/sessions/${id}/qr`);
      const rendered = await QRCode.toDataURL(`geoasist://asistencia#qr=${encodeURIComponent(qr.qr_token)}`, { width: 640, margin: 2, errorCorrectionLevel: 'M' });
      // El servidor define la validez; el reloj monotónico solo descuenta el tiempo transcurrido.
      const serverNow = Date.parse(qr.hora_servidor);
      const expiresIn = Date.parse(qr.vence_en) - serverNow;
      const rotatesIn = Date.parse(qr.emitido_en) + qr.rotacion_segundos * 1000 - serverNow;
      validUntil.current = requestedAt + expiresIn;
      refreshAt.current = requestedAt + Math.min(rotatesIn, expiresIn);
      if (!Number.isFinite(expiresIn) || !Number.isFinite(rotatesIn) || performance.now() >= validUntil.current) {
        throw new Error('El QR ya venció. Vuelve a cargar la sesión para obtener el actual.');
      }
      setImage(rendered);
      remaining.current = Math.max(0, Math.ceil((refreshAt.current - performance.now()) / 1000));
      setLeft(remaining.current); setError(null);
    } catch (e) {
      setImage('');
      setError(e);
      refreshAt.current = performance.now() + 5000;
      if (e instanceof ApiError && [403, 409, 410].includes(e.status)) setEnded(true);
    } finally {
      fetching.current = false;
    }
  }, [id]);

  useEffect(() => { // cada segundo descuenta; al llegar a 0 pide otro QR (5 s de espera si falla)
    if (!active) return;
    const tick = () => {
      if (performance.now() >= validUntil.current) setImage('');
      remaining.current = Math.max(0, Math.ceil((refreshAt.current - performance.now()) / 1000)); setLeft(remaining.current);
      if (remaining.current <= 0) void fetchQr();
    };
    const first = setTimeout(tick, 0), t = setInterval(tick, 1000);
    return () => { clearTimeout(first); clearInterval(t); };
  }, [active, fetchQr]);
  useEffect(() => {
    if (!active) return;
    const poll = () => request<{ total: number }>(`/sessions/${id}/attendances?size=1&estado=CONFIRMADA`).then(p => {
      setPresent(p.total); setAttendanceError(null);
    }, setAttendanceError);
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

  return <div className={`projection ${styles.projectionShell}`}>
    <header className={styles.projectionHeader}>
      <div>
        <p className={styles.projectionKicker}>Proyección de asistencia · UNL GeoAsist</p>
        <h1 className={styles.projectionTitle}>{info ? `${info.activity.nombre}${info.space ? ` · ${info.space}` : ''}` : 'Sesión'}</h1>
        <p className={styles.projectionMeta}>{info ? `${new Date(info.session.inicio).toLocaleTimeString('es-EC', { timeZone: 'America/Guayaquil', hour: '2-digit', minute: '2-digit' })} - ${new Date(info.session.fin).toLocaleTimeString('es-EC', { timeZone: 'America/Guayaquil', hour: '2-digit', minute: '2-digit' })}` : 'Cargando sesión…'}</p>
      </div>
      {active && <div className={styles.projectionActions}><Button variant="secondary" onClick={() => void fullscreen()}>Pantalla completa</Button> <Button variant="danger" onClick={() => setConfirming(true)}>Cerrar sesión</Button></div>}
    </header>

    {confirming && <ConfirmInline danger message="¿Cerrar la sesión? Ya no se admitirán registros." confirmLabel="Cerrar sesión" onConfirm={() => void close()} onCancel={() => setConfirming(false)} />}
    {note && <p role="status">{note}</p>}
    <ProblemMessage error={error} />
    <ProblemMessage error={attendanceError} />

    {info && !active && <div className={styles.inactiveCard}>
      <p role="status">Esta sesión no está activa ({info.session.estado.toLowerCase()}).</p>
      <div><ButtonLink href={`/sesiones/${id}`}>Volver a la sesión</ButtonLink></div>
    </div>}

    {active && <>
      <div className={styles.projectionBody}>
        <div className={`projection-qr ${styles.qrFrame}`}>
          {image
            ? <Image unoptimized src={image} alt="Código QR de asistencia de la sesión activa" width={640} height={640} />
            : <div className={styles.qrLoading} role="status">Generando QR…</div>}
        </div>
      </div>
      <footer className={styles.projectionFooter}>
        <span>Nuevo código en {left} s</span>
        <span aria-live="polite">{present} presentes</span>
      </footer>
    </>}
  </div>;
}
