import { Button } from './Button';

/** steps: etiquetas; current: índice activo (controlado por el padre); onPrev/onNext: callbacks (Anterior se oculta en el primer paso, Siguiente en el último); nextDisabled: bloquea Siguiente. */
export function Stepper({ steps, current, onPrev, onNext, nextDisabled = false }:
  { steps: string[]; current: number; onPrev: () => void; onNext: () => void; nextDisabled?: boolean }) {
  return (
    <div className="ui-stepper">
      <ol>{steps.map((s, i) => (
        <li key={s} aria-current={i === current ? 'step' : undefined} data-done={i < current || undefined}>
          <span aria-hidden="true">{i < current ? '✓' : i + 1}</span> {s}
        </li>
      ))}</ol>
      <div className="ui-stepper-nav">
        {current > 0 && <Button variant="secondary" onClick={onPrev}>Anterior</Button>}
        {current < steps.length - 1 && <Button onClick={onNext} disabled={nextDisabled}>Siguiente</Button>}
      </div>
    </div>
  );
}
