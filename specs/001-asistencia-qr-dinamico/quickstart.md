# Quickstart de validación: 001-asistencia-qr-dinamico

Guía para comprobar la funcionalidad de punta a punta una vez implementada.
Los comandos exactos de arranque se documentarán en el README cuando se elija el backend.

1. Levantar la base de datos y la API: `docker compose -f infra/docker-compose.yml up -d`.
2. Cargar datos semilla: un docente, un estudiante, un espacio "Aula 101" (radio 20 m) y una
   sesión *programada*.
3. Como docente, en la web: iniciar la sesión y verificar que el QR cambia cada 30 s.
4. Como estudiante, en la app Expo: escanear el QR dentro del aula → "Asistencia registrada".
5. Volver a escanear → "Ya tenía una asistencia registrada en esta sesión".
6. Esperar más de 40 s y enviar un token antiguo (prueba de API) → `TOKEN_VENCIDO`.
7. Enviar una ubicación a 200 m del aula → `FUERA_DE_ZONA`.
8. Enviar precisión 80 m dos veces → `repetir` y luego `pendiente_revision`; aprobar como docente.
9. Cerrar la sesión y enviar el token actual → `SESION_INACTIVA`.
10. Exportar CSV y comprobar estados y motivos.
