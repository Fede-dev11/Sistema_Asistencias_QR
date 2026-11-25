import { describe, expect, it } from 'vitest';
import { validDate, validDateTimeLocal } from './dateValidation';

describe('dateValidation', () => {
  it('rechaza fechas imposibles y acepta fechas reales', () => {
    expect(validDate('2026-02-29')).toBe(false);
    expect(validDate('2026-02-28')).toBe(true);
  });

  it('valida fecha y hora local completas', () => {
    expect(validDateTimeLocal('2026-11-02T08:00')).toBe(true);
    expect(validDateTimeLocal('2026-11-02')).toBe(false);
    expect(validDateTimeLocal('2026-11-02T25:00')).toBe(false);
  });
});
