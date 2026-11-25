import { beforeEach, expect, jest, test } from '@jest/globals';
import * as SecureStore from 'expo-secure-store';
import { getInstallationId, resetInstallationIdCacheForTests } from './installationId';

jest.mock('expo-secure-store', () => ({ getItemAsync: jest.fn(), setItemAsync: jest.fn() }));
jest.mock('expo-crypto', () => ({ randomUUID: () => '11111111-1111-4111-8111-111111111111' }));

beforeEach(() => {
  jest.clearAllMocks();
  resetInstallationIdCacheForTests();
});

test('crea una sola vez y conserva el UUID en almacenamiento seguro', async () => {
  jest.mocked(SecureStore.getItemAsync).mockResolvedValue(null);
  jest.mocked(SecureStore.setItemAsync).mockResolvedValue();
  await expect(getInstallationId()).resolves.toBe('11111111-1111-4111-8111-111111111111');
  await expect(getInstallationId()).resolves.toBe('11111111-1111-4111-8111-111111111111');
  expect(SecureStore.setItemAsync).toHaveBeenCalledTimes(1);
});

test('reutiliza un identificador ya almacenado', async () => {
  jest.mocked(SecureStore.getItemAsync).mockResolvedValue('22222222-2222-4222-8222-222222222222');
  await expect(getInstallationId()).resolves.toBe('22222222-2222-4222-8222-222222222222');
  expect(SecureStore.setItemAsync).not.toHaveBeenCalled();
});
