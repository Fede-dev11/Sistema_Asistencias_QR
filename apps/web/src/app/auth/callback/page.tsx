import { Suspense } from 'react';
import { CasCallback } from '../../../features/auth/CasCallback';
export default function CallbackPage() { return <Suspense fallback={<p role="status">Verificando acceso…</p>}><CasCallback/></Suspense>; }
