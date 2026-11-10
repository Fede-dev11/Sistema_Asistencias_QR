const STORAGE_KEY = 'geoasist-dispositivo-prueba';

function newUuid() {
  return crypto.randomUUID();
}

export function loadTestDeviceId() {
  const stored = sessionStorage.getItem(STORAGE_KEY);
  if (stored) return stored;
  const created = newUuid();
  sessionStorage.setItem(STORAGE_KEY, created);
  return created;
}

export function saveTestDeviceId(value: string) {
  sessionStorage.setItem(STORAGE_KEY, value);
  return value;
}

export function createTestDeviceId() {
  return saveTestDeviceId(newUuid());
}
