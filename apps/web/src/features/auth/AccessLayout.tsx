import Image from 'next/image';
import type { ReactNode } from 'react';
import styles from './access.module.css';

/** Marco común para portada y acceso, inspirado en la identidad de Stitch. */
export function AccessLayout({ children }: { children: ReactNode }) {
  return <section className={styles.access}>
    <div className={styles.identity}>
      <div className={styles.crest}><Image src="/identidad/escudo-unl.png" alt="Escudo de la Universidad Nacional de Loja" width={104} height={104} priority /></div>
      <span className={styles.institution}>Universidad Nacional de Loja</span>
      <p className={styles.brand}>UNL · GeoAsist</p>
      <p className={styles.subtitle}>Sistema de asistencia académica</p>
    </div>
    <div className={styles.card}>{children}</div>
    <p className={styles.footer}>Universidad Nacional de Loja · GeoAsist</p>
  </section>;
}
