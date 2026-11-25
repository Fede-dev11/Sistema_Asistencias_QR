import type { Metadata } from 'next';
import { SwaggerDocsPage } from '../../../features/docs/SwaggerDocsPage';

export const metadata: Metadata = { title: 'Swagger UI · UNL GeoAsist' };

export default function SwaggerPage() {
  return <SwaggerDocsPage />;
}
