import type { Metadata } from 'next';
import { Suspense } from 'react';
import { ArchitecturePage } from '../../../features/docs/ArchitecturePage';

export const metadata: Metadata = { title: 'Arquitectura y diagramas · UNL GeoAsist' };

export default function ArchitectureDocsPage() {
  return <Suspense><ArchitecturePage /></Suspense>;
}
