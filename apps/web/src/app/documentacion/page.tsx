import type { Metadata } from 'next';
import { ApiDocsPage } from '../../features/docs/ApiDocsPage';

export const metadata: Metadata = { title: 'Documentación de la API · UNL GeoAsist' };

export default function DocumentationPage() {
  return <ApiDocsPage />;
}
