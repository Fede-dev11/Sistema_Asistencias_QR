// Contraste WCAG de los pares de tokens de globals.css (T019). Uso: node tools/contracts/contrast.mjs
const lum = (h) => { const c = [1, 3, 5].map((i) => parseInt(h.slice(i, i + 2), 16) / 255).map((v) => (v <= .03928 ? v / 12.92 : ((v + .055) / 1.055) ** 2.4)); return .2126 * c[0] + .7152 * c[1] + .0722 * c[2]; };
const ratio = (a, b) => { const [x, y] = [lum(a), lum(b)].sort((p, q) => q - p); return (x + .05) / (y + .05); };
const T = { ink: '#18302a', brand: '#146b50', danger: '#a33726', warn: '#8a5a00', info: '#1c5ca3', muted: '#4f665b', paper: '#f5f6f1', surface: '#ffffff', white: '#ffffff',
  okSoft: '#e9f4ec', warnSoft: '#fbf1dc', dangerSoft: '#fff1eb', infoSoft: '#e6eefa', get: '#1e7448', put: '#8f5a0c', patch: '#6a43a6', del: '#ad342c' };
const pairs = [['ink', 'paper'], ['ink', 'surface'], ['brand', 'paper'], ['brand', 'surface'], ['white', 'brand'], ['danger', 'paper'], ['danger', 'surface'], ['warn', 'paper'], ['warn', 'surface'],
  ['info', 'surface'], ['muted', 'paper'], ['muted', 'surface'], ['brand', 'okSoft'], ['warn', 'warnSoft'], ['danger', 'dangerSoft'], ['info', 'infoSoft'], ['ink', 'okSoft'],
  ['get', 'surface'], ['info', 'surface'], ['put', 'surface'], ['patch', 'surface'], ['del', 'surface']];
let bad = 0;
for (const [f, b] of pairs) { const r = ratio(T[f], T[b]); if (r < 4.5) bad++; console.log(`${f} sobre ${b}\t${r.toFixed(2)}\t${r >= 4.5 ? 'OK' : 'FALLA'}`); }
process.exit(bad ? 1 : 0);
