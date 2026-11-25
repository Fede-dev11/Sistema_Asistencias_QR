'use client';

export const METHODS = [['SOLO_QR', 'Solo QR'], ['QR_Y_GPS', 'QR y GPS'], ['SOLO_GPS', 'Solo GPS'], ['NINGUNO', 'Ninguno']] as const;

export function MethodSelect({ value, onChange }: { value: string; onChange: (value: string) => void }) {
  return (
    <>
      <label>Método de verificación
        <select value={value} onChange={e => onChange(e.target.value)}>
          {METHODS.map(([v, label]) => <option key={v} value={v}>{label}</option>)}
        </select>
      </label>
      {value === 'NINGUNO' && <p role="note">Este método no comprueba presencia: cualquier participante autorizado podrá registrarse mientras la sesión esté activa.</p>}
    </>
  );
}
