import { randomUUID } from 'expo-crypto';
import * as SecureStore from 'expo-secure-store';
import type { components } from '../../generated/api';

type GuestStart = components['schemas']['GuestStartResult'];
export type GuestResume = {
  token: string;
  inicioEn: string;
  venceEn: string;
  idempotencyKey: string;
  deviceId: string;
};

const KEY = 'geoasist-guest-resume';
const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

function valid(value: unknown): value is GuestResume {
  if (typeof value !== 'object' || value === null) return false;
  const item = value as Record<string, unknown>;
  return (
    Object.keys(item).sort().join(',') === 'deviceId,idempotencyKey,inicioEn,token,venceEn' &&
    typeof item.token === 'string' &&
    item.token.length >= 32 &&
    item.token.length <= 512 &&
    typeof item.inicioEn === 'string' &&
    Number.isFinite(Date.parse(item.inicioEn)) &&
    typeof item.venceEn === 'string' &&
    Number.isFinite(Date.parse(item.venceEn)) &&
    Date.parse(item.venceEn) > Date.parse(item.inicioEn) &&
    typeof item.idempotencyKey === 'string' &&
    UUID.test(item.idempotencyKey) &&
    typeof item.deviceId === 'string' && UUID.test(item.deviceId)
  );
}

export function createGuestResume(start: GuestStart, deviceId: string): GuestResume {
  return {
    token: start.registro_invitado_token,
    inicioEn: start.inicio_en,
    venceEn: start.vence_en,
    idempotencyKey: randomUUID(),
    deviceId,
  };
}

export function renewGuestIdempotency(value: GuestResume): GuestResume {
  return { ...value, idempotencyKey: randomUUID() };
}

export async function saveGuestResume(value: GuestResume): Promise<void> {
  if (!valid(value)) throw new Error('Capacidad temporal inválida.');
  await SecureStore.setItemAsync(KEY, JSON.stringify(value));
}

export async function loadGuestResume(): Promise<GuestResume | null> {
  const raw = await SecureStore.getItemAsync(KEY);
  if (!raw) return null;
  try {
    const value: unknown = JSON.parse(raw);
    if (!valid(value)) throw new Error('Formato inválido');
    return value;
  } catch {
    await clearGuestResume();
    return null;
  }
}

export async function clearGuestResume(): Promise<void> {
  await SecureStore.deleteItemAsync(KEY);
}
