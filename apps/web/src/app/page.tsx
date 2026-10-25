import Link from 'next/link';
export default function Home() {
 return <section><p className="eyebrow">UNIVERSIDAD NACIONAL DE LOJA</p><h1>Tu asistencia,<br/>en el momento.</h1><p>Registra tu asistencia desde la app GeoAsist. La web queda disponible para responsables y administradores.</p><nav><Link href="/login">Ingresar con credenciales</Link><Link href="/actividades">Mis clases</Link><Link href="/sesiones">Mis actividades</Link><Link href="/reportes">Consultar asistencias</Link><Link href="/admin/personas">Administración</Link></nav></section>;
}
