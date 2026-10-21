'use client';

import { BrowserQRCodeReader, type IScannerControls } from '@zxing/browser';
import Link from 'next/link';
import { type FormEvent, useEffect, useRef, useState } from 'react';
import type { components } from '../../generated/api';
import { ApiError, request } from '../../lib/api/client';
import { useSession } from '../auth/SessionProvider';
import { AttendanceResult, type AttendanceState } from './AttendanceResult';
import {
  clearGuestResume,
  createGuestResume,
  loadGuestResume,
  renewGuestIdempotency,
  saveGuestResume,
  type GuestResume,
} from './guestResume';
import { captureLocation, type AttendanceLocation } from './location';

type GuestStart = components['schemas']['GuestStartResult'];
type Receipt = components['schemas']['AttendanceReceipt'];
type Pending = components['schemas']['PendingReceipt'];
type Feedback = { estado: AttendanceState; mensaje: string; code?: string };

function qrFrom(value: string): string | null {
  const trimmed = value.trim();
  try {
    const url = new URL(trimmed);
    const token = new URLSearchParams(url.hash.slice(1)).get('qr');
    return token && token.length >= 32 ? token : null;
  } catch {
    return trimmed.length >= 32 && trimmed.length <= 512 ? trimmed : null;
  }
}

function feedback(value: Receipt | Pending): Feedback {
  if (value.resultado === 'CONFIRMADA') return { estado: 'CONFIRMADA', mensaje: value.mensaje };
  return { estado: value.resultado, mensaje: value.mensaje, code: value.code };
}

export function AttendancePage() {
  const { session } = useSession();
  const video = useRef<HTMLVideoElement>(null);
  const controls = useRef<IScannerControls | null>(null);
  const initialFragment = useRef<string | null>(null);
  const [qrToken, setQrToken] = useState('');
  const [guest, setGuest] = useState<GuestResume | null>(null);
  const [cedula, setCedula] = useState('');
  const [accountKey, setAccountKey] = useState('');
  const [accountLocation, setAccountLocation] = useState<AttendanceLocation | null>(null);
  const [guestLocation, setGuestLocation] = useState<AttendanceLocation | null>(null);
  const [busy, setBusy] = useState(false);
  const [camera, setCamera] = useState(false);
  const [feedbackState, setFeedbackState] = useState<Feedback | null>(null);

  useEffect(() => {
    if (initialFragment.current === null) initialFragment.current = window.location.hash;
    const fragment = initialFragment.current;
    const receiveFragment = (value: string) => {
      const token = new URLSearchParams(value.slice(1)).get('qr');
      if (token && token.length >= 32 && token.length <= 512) {
        setQrToken(token);
        setAccountKey('');
        setAccountLocation(null);
        setFeedbackState(null);
      }
      if (value) {
        window.history.replaceState(null, '', `${window.location.pathname}${window.location.search}`);
      }
    };
    queueMicrotask(() => {
      setGuest(loadGuestResume());
      receiveFragment(fragment);
    });
    const changed = () => receiveFragment(window.location.hash);
    window.addEventListener('hashchange', changed);
    return () => {
      window.removeEventListener('hashchange', changed);
      controls.current?.stop();
    };
  }, []);

  function acceptScan(value: string) {
    const token = qrFrom(value);
    if (!token) {
      setFeedbackState({ estado: 'RECHAZADA', mensaje: 'El código leído no contiene un QR válido.' });
      return;
    }
    controls.current?.stop();
    controls.current = null;
    setCamera(false);
    setQrToken(token);
    setAccountKey(crypto.randomUUID());
    setAccountLocation(null);
    setFeedbackState(null);
  }

  async function startCamera() {
    setFeedbackState(null);
    setCamera(true);
    try {
      const reader = new BrowserQRCodeReader();
      controls.current = await reader.decodeFromVideoDevice(undefined, video.current ?? undefined, result => {
        if (result) acceptScan(result.getText());
      });
    } catch {
      setCamera(false);
      setFeedbackState({ estado: 'RECHAZADA', mensaje: 'No se pudo usar la cámara. Revisa su permiso e inténtalo otra vez.' });
    }
  }

  async function submitAccount(location = accountLocation, newPayload = false) {
    if (!qrToken || !session) return;
    const key = newPayload ? crypto.randomUUID() : accountKey || crypto.randomUUID();
    setAccountKey(key);
    setBusy(true);
    setFeedbackState(null);
    try {
      const result = await request<Receipt | Pending>('/attendance/attempts', {
        method: 'POST',
        headers: { 'Idempotency-Key': key },
        body: JSON.stringify({ qr_token: qrToken, ...(location ? { ubicacion: location } : {}) }),
      });
      setFeedbackState(feedback(result));
    } catch (error) {
      setFeedbackState({
        estado: 'RECHAZADA',
        mensaje: error instanceof Error ? error.message : 'No se pudo registrar. Reintenta con la misma operación.',
      });
    } finally {
      setBusy(false);
    }
  }

  async function locateAccount() {
    setBusy(true);
    try {
      const location = await captureLocation();
      setAccountLocation(location);
      await submitAccount(location, true);
    } catch (error) {
      setFeedbackState({ estado: 'REPETIR', mensaje: error instanceof Error ? error.message : 'No se obtuvo la ubicación.' });
    } finally {
      setBusy(false);
    }
  }

  async function openGuest() {
    if (!qrToken) return;
    setBusy(true);
    setFeedbackState(null);
    try {
      const result = await request<GuestStart>('/guest-registrations', {
        method: 'POST',
        authentication: 'none',
        body: JSON.stringify({ qr_token: qrToken }),
      });
      const resume = createGuestResume(result);
      saveGuestResume(resume);
      setGuest(resume);
    } catch (error) {
      setFeedbackState({
        estado: 'RECHAZADA',
        mensaje: error instanceof Error ? error.message : 'No se pudo abrir el formulario de invitado.',
      });
    } finally {
      setBusy(false);
    }
  }

  async function submitGuest(event?: FormEvent, location = guestLocation, newPayload = false) {
    event?.preventDefault();
    if (!guest) return;
    const current = newPayload ? renewGuestIdempotency(guest) : guest;
    if (newPayload) {
      saveGuestResume(current);
      setGuest(current);
    }
    setBusy(true);
    setFeedbackState(null);
    try {
      const result = await request<Receipt | Pending>('/guest-registrations/submit', {
        method: 'POST',
        authentication: 'none',
        headers: { 'Idempotency-Key': current.idempotencyKey },
        body: JSON.stringify({ registro_invitado_token: current.token, cedula, ...(location ? { ubicacion: location } : {}) }),
      });
      setFeedbackState(feedback(result));
      if (result.resultado === 'CONFIRMADA') {
        clearGuestResume();
        setGuest(null);
        setCedula('');
      }
    } catch (error) {
      if (error instanceof ApiError && (error.status === 401 || error.status === 410)) {
        clearGuestResume();
        setGuest(null);
      }
      setFeedbackState({
        estado: 'RECHAZADA',
        mensaje: error instanceof Error ? error.message : 'No se pudo registrar. Reintenta con la misma operación.',
      });
    } finally {
      setBusy(false);
    }
  }

  async function locateGuest() {
    setBusy(true);
    try {
      const location = await captureLocation();
      setGuestLocation(location);
      await submitGuest(undefined, location, true);
    } catch (error) {
      setFeedbackState({ estado: 'REPETIR', mensaje: error instanceof Error ? error.message : 'No se obtuvo la ubicación.' });
    } finally {
      setBusy(false);
    }
  }

  function discardGuest() {
    clearGuestResume();
    setGuest(null);
    setCedula('');
    setGuestLocation(null);
  }

  return (
    <section>
      <p className="eyebrow">REGISTRO DE ASISTENCIA</p>
      <h1>Escanea el QR de la sesión</h1>
      <p>El QR cambia periódicamente. El formulario invitado conserva el plazo que fijó el servidor al abrirlo.</p>

      {!guest && (
        <section className="panel">
          <button type="button" disabled={busy || camera} onClick={() => void startCamera()}>
            {camera ? 'Cámara activa…' : 'Abrir cámara'}
          </button>
          {camera && <video ref={video} className="scanner-preview" muted playsInline aria-label="Vista de la cámara para escanear QR" />}
          {qrToken && <p role="status">QR leído. Elige cómo registrar la asistencia.</p>}
          {qrToken && session && (
            <button type="button" disabled={busy} onClick={() => void submitAccount()}>
              Registrar con mis credenciales
            </button>
          )}
          {qrToken && !session && <p>Si tienes cuenta institucional, <Link href="/login">ingresa con tus credenciales</Link> y vuelve a escanear.</p>}
          {qrToken && (
            <button type="button" disabled={busy} onClick={() => void openGuest()}>
              Continuar como invitado sin cuenta
            </button>
          )}
        </section>
      )}

      {guest && (
        <form onSubmit={event => void submitGuest(event)}>
          <h2>Invitado autorizado</h2>
          <p>Este formulario vence según la hora del servidor: {new Date(guest.venceEn).toLocaleString('es-EC')}.</p>
          <label>
            Cédula
            <input
              aria-label="Cédula"
              inputMode="numeric"
              autoComplete="off"
              pattern="[0-9]{10}"
              minLength={10}
              maxLength={10}
              required
              value={cedula}
              onChange={event => setCedula(event.target.value.replace(/[^0-9]/g, '').slice(0, 10))}
            />
          </label>
          <button disabled={busy || !/^[0-9]{10}$/.test(cedula)}>Enviar asistencia</button>{' '}
          <button type="button" disabled={busy} onClick={discardGuest}>Escanear otro QR</button>
        </form>
      )}

      {feedbackState && <AttendanceResult {...feedbackState} />}
      {feedbackState?.estado === 'REPETIR' && session && !guest && (
        <div><button type="button" disabled={busy} onClick={() => void locateAccount()}>Obtener ubicación y reintentar</button>{' '}<button type="button" disabled={busy} onClick={() => void submitAccount(null, true)}>Solicitar revisión sin ubicación</button></div>
      )}
      {feedbackState?.estado === 'REPETIR' && guest && (
        <div><button type="button" disabled={busy} onClick={() => void locateGuest()}>Obtener ubicación y reintentar</button>{' '}<button type="button" disabled={busy} onClick={() => void submitGuest(undefined, null, true)}>Solicitar revisión sin ubicación</button></div>
      )}
    </section>
  );
}
