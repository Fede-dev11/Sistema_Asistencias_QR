// Fechas (YYYY-MM-DD) que genera el horario; refleja WeeklySchedule.expand() del servidor.
export const MAX_SESSIONS = 200;

export function previewDates(days: number[], from: string, to: string): string[] {
  const out: string[] = [];
  if (days.length === 0 || !from || !to || to < from) return out;
  const end = new Date(`${to}T00:00:00Z`);
  for (let d = new Date(`${from}T00:00:00Z`); d <= end && out.length <= MAX_SESSIONS; d.setUTCDate(d.getUTCDate() + 1)) {
    const iso = d.getUTCDay() === 0 ? 7 : d.getUTCDay();
    if (days.includes(iso)) out.push(d.toISOString().slice(0, 10));
  }
  return out;
}
