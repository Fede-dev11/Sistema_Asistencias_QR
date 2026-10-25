'use client';

import 'leaflet/dist/leaflet.css';
import { type FormEvent, useEffect, useRef, useState } from 'react';
import type { components } from '../../generated/api';
import { request } from '../../lib/api/client';

export type Space = components['schemas']['Space'];

const UNL: [number, number] = [-4.0328, -79.2026];

export function SpaceForm({ space, onSaved }: { space?: Space; onSaved?: (space: Space) => void }) {
  const [codigo, setCodigo] = useState(space?.codigo ?? '');
  const [nombre, setNombre] = useState(space?.nombre ?? '');
  const [tipo, setTipo] = useState(space?.tipo ?? 'AULA');
  const [latitud, setLatitud] = useState(space?.latitud?.toString() ?? '');
  const [longitud, setLongitud] = useState(space?.longitud?.toString() ?? '');
  const [radio, setRadio] = useState((space?.radio_defecto_m ?? 20).toString());
  const [precision, setPrecision] = useState((space?.precision_defecto_m ?? 30).toString());
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const mapNode = useRef<HTMLDivElement>(null);
  const place = useRef<(lat: number, lng: number) => void>(() => {});

  // El mapa es una ayuda: marcar un punto rellena los campos numéricos, que siguen siendo editables.
  useEffect(() => {
    let map: import('leaflet').Map | undefined;
    let cancelled = false;
    void import('leaflet').then(L => {
      if (cancelled || !mapNode.current) return;
      const lat0 = Number(latitud), lng0 = Number(longitud);
      const start: [number, number] = latitud !== '' && longitud !== '' && Number.isFinite(lat0) && Number.isFinite(lng0) ? [lat0, lng0] : UNL;
      map = L.map(mapNode.current).setView(start, 17);
      L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution: '&copy; OpenStreetMap contributors',
      }).addTo(map);
      let marker: import('leaflet').CircleMarker | undefined;
      place.current = (lat, lng) => {
        if (!map) return;
        if (marker) marker.setLatLng([lat, lng]);
        else marker = L.circleMarker([lat, lng], { radius: 8 }).addTo(map);
      };
      if (start !== UNL) place.current(start[0], start[1]);
      map.on('click', event => {
        const lat = Number(event.latlng.lat.toFixed(6)), lng = Number(event.latlng.lng.toFixed(6));
        setLatitud(String(lat));
        setLongitud(String(lng));
        place.current(lat, lng);
      });
    });
    return () => {
      cancelled = true;
      map?.remove();
    };
    // Solo se monta una vez; los cambios posteriores de los campos no recrean el mapa.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function moveMarker(lat: string, lng: string) {
    const a = Number(lat), b = Number(lng);
    if (lat !== '' && lng !== '' && Math.abs(a) <= 90 && Math.abs(b) <= 180) place.current(a, b);
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    setError('');
    if ((latitud === '') !== (longitud === '')) {
      setError('Indica latitud y longitud juntas, o deja ambas vacías.');
      return;
    }
    setBusy(true);
    try {
      const common = {
        nombre,
        tipo: tipo as Space['tipo'],
        radio_defecto_m: Number(radio),
        precision_defecto_m: Number(precision),
        ...(latitud !== '' ? { latitud: Number(latitud), longitud: Number(longitud) } : {}),
      };
      const saved = space
        ? await request<Space>(`/spaces/${space.id}`, { method: 'PATCH', body: JSON.stringify(common) })
        : await request<Space>('/spaces', { method: 'POST', body: JSON.stringify({ codigo, ...common }) });
      onSaved?.(saved);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'No se guardó el espacio.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <form className="panel" onSubmit={submit}>
      <h2>{space ? 'Editar espacio' : 'Nuevo espacio'}</h2>
      <label>Código<input aria-label="Código" required maxLength={40} disabled={!!space} value={codigo} onChange={e => setCodigo(e.target.value)} /></label>
      <label>Nombre<input aria-label="Nombre" required maxLength={200} value={nombre} onChange={e => setNombre(e.target.value)} /></label>
      <label>Tipo
        <select aria-label="Tipo" value={tipo} onChange={e => setTipo(e.target.value as Space['tipo'])}>
          {['AULA', 'LABORATORIO', 'AUDITORIO', 'EXTERIOR', 'OTRO'].map(t => <option key={t}>{t}</option>)}
        </select>
      </label>
      <div
        ref={mapNode}
        role="application"
        aria-label="Mapa para marcar el centro del espacio. Alternativa: escribe latitud y longitud en los campos siguientes."
        style={{ height: 280 }}
      />
      <label>Latitud<input aria-label="Latitud" type="number" min="-90" max="90" step="any" value={latitud} onChange={e => { setLatitud(e.target.value); moveMarker(e.target.value, longitud); }} /></label>
      <label>Longitud<input aria-label="Longitud" type="number" min="-180" max="180" step="any" value={longitud} onChange={e => { setLongitud(e.target.value); moveMarker(latitud, e.target.value); }} /></label>
      <label>Radio por defecto (m)<input aria-label="Radio por defecto (m)" type="number" min="0.01" step="0.01" required value={radio} onChange={e => setRadio(e.target.value)} /></label>
      <label>Precisión por defecto (m)<input aria-label="Precisión por defecto (m)" type="number" min="0.01" step="0.01" required value={precision} onChange={e => setPrecision(e.target.value)} /></label>
      <button disabled={busy}>Guardar espacio</button>
      {error && <p role="alert">{error}</p>}
    </form>
  );
}
