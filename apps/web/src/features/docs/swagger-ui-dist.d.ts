// Tipos mínimos de swagger-ui-dist (el paquete no trae declaraciones); solo lo que usa SwaggerDocsPage.
declare module 'swagger-ui-dist/swagger-ui-es-bundle.js' {
  type Request = { url: string };
  const SwaggerUIBundle: (options: {
    domNode: HTMLElement;
    url: string;
    persistAuthorization?: boolean;
    requestInterceptor?: (req: Request) => Request;
  }) => unknown;
  export default SwaggerUIBundle;
}
declare module 'swagger-ui-dist/swagger-ui.css';
