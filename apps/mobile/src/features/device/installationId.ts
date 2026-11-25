import { randomUUID } from 'expo-crypto';
import * as SecureStore from 'expo-secure-store';

const KEY = 'geoasist-installation-id';
const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

let pending: Promise<string> | null = null;

export function getInstallationId(): Promise<string> {
  pending ??= loadOrCreate().catch(error => {
    pending = null;
    throw error;
  });
  return pending;
}

async function loadOrCreate(): Promise<string> {
  const stored = await SecureStore.getItemAsync(KEY);
  if (stored && UUID.test(stored)) return stored;
  const created = randomUUID();
  await SecureStore.setItemAsync(KEY, created);
  return created;
}

export function resetInstallationIdCacheForTests(): void {
  pending = null;
}
