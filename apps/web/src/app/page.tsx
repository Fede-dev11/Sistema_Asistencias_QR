import Link from 'next/link';
export default function Home() {
 return <section><p className="eyebrow">UNIVERSIDAD NACIONAL DE LOJA</p><h1>Tu asistencia,<br/>en el momento.</h1><p>Accede con tu cuenta para gestionar clases o registrar tu asistencia. Los invitados autorizados usan su cédula en el formulario del evento.</p><nav><Link href="/login">Ingresar con credenciales</Link><Link href="/asistencia">Registrar asistencia</Link><Link href="/sesiones">Mis actividades</Link><Link href="/reportes">Consultar asistencias</Link><Link href="/admin/personas">Administración</Link></nav></section>;
}
