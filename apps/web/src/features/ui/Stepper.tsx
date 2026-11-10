export function Stepper({ steps, current, onPrev, onNext }: { steps: string[]; current: number; onPrev?: () => void; onNext?: () => void }) {
  return (
    <div className="ui-stepper"><ol>
      {steps.map((label, i) => <li key={i} aria-current={i === current ? 'step' : undefined}><span>{i + 1}</span> {label}</li>)}
    </ol><div className="ui-stepper-nav">
      {onPrev && <button type="button" onClick={onPrev} disabled={current === 0} className="ui-btn ui-btn-secondary">Anterior</button>}
      {onNext && <button type="button" onClick={onNext} disabled={current === steps.length - 1} className="ui-btn ui-btn-primary">Siguiente</button>}
    </div></div>
  );
}
