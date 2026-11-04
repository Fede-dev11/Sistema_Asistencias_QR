import Link from 'next/link';
import type { ComponentProps, ReactNode } from 'react';

export type ButtonVariant = 'primary' | 'secondary' | 'text' | 'danger';
const cls = (v: ButtonVariant, extra?: string) => `ui-btn ui-btn-${v}${extra ? ` ${extra}` : ''}`;

/** variant: primary|secondary|text|danger (def. primary). loading: deshabilita, aria-busy y muestra el texto de `children`. Resto: props nativas de button. */
export function Button({ variant = 'primary', loading = false, className, disabled, children, type = 'button', ...rest }:
  { variant?: ButtonVariant; loading?: boolean } & ComponentProps<'button'>) {
  return <button {...rest} type={type} className={cls(variant, className)} disabled={disabled || loading} aria-busy={loading || undefined}>{children}</button>;
}

/** Enlace de Next con el aspecto de Button. variant como en Button; resto: props de Link (href obligatorio). */
export function ButtonLink({ variant = 'primary', className, children, ...rest }:
  { variant?: ButtonVariant; children: ReactNode } & ComponentProps<typeof Link>) {
  return <Link {...rest} className={cls(variant, className)}>{children}</Link>;
}
