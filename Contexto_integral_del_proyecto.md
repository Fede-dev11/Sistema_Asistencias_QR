Contexto integral del proyecto
Sistema híbrido de control de asistencia mediante QR dinámico y validación geoespacial
Documento base de visión, alcance, reglas de negocio y especificación inicial (SDD) | Versión 1.0 | 9 de octubre de 2026
Estado: propuesta académica en definición. Las tecnologías y parámetros indicados como recomendados no representan implementaciones terminadas.
1. Resumen ejecutivo
El proyecto consiste en desarrollar un sistema de registro y verificación de asistencia a clases y eventos, orientado a instituciones educativas y organizadores. Un docente u organizador configura una actividad y activa una sesión que muestra un código QR temporal, renovado de forma periódica (referencia inicial: cada 30 segundos). El participante lo escanea desde una aplicación móvil, preferida, o una aplicación web, y el servidor verifica vigencia, identidad y proximidad a la ubicación autorizada. El objetivo es reducir registros fraudulentos y proporcionar información auditable, no garantizar certeza absoluta de presencia física.
El sistema atenderá estudiantes existentes en una base institucional y participantes externos sin registro previo. Los invitados podrán proporcionar sus datos sin crear una cuenta permanente y, opcionalmente, crear una cuenta para visualizar posteriormente su historial, previa verificación de identidad o contacto.
2. Problema y justificación
En métodos tradicionales de control de asistencia, la firma manual consume tiempo y puede sufrir suplantación; los QR estáticos o de larga duración pueden compartirse a distancia. Un QR rotativo limita su reutilización temporal, pero no evita que alguien envíe una imagen del código en tiempo real. La geolocalización añade una segunda señal, aunque en interiores la exactitud puede ser insuficiente para distinguir aulas próximas y pueden existir ubicaciones simuladas. Por ello se plantea un sistema multicontrol, configurable, transparente y evaluable.
3. Objetivos
3.1 Objetivo general
Diseñar e implementar un sistema híbrido de gestión y verificación de asistencia a clases y eventos que integre códigos QR temporales, autenticación de participantes y validación geoespacial configurable, con trazabilidad de resultados.
3.2 Objetivos específicos
•	Gestionar espacios, eventos, sesiones, horarios y responsables desde un panel administrativo.
•	Generar y validar QR temporales asociados a una sesión activa.
•	Permitir la asistencia de estudiantes autenticados y participantes invitados con o sin cuenta.
•	Validar proximidad mediante PostgreSQL/PostGIS considerando la precisión reportada del dispositivo.
•	Prevenir duplicados, documentar intentos fallidos y habilitar revisión justificada de casos inciertos.
•	Presentar historial personal y reportes para organizadores, y evaluar falsos positivos, falsos negativos y usabilidad.
4. Usuarios, roles y canales
Actor	Funciones principales
Administrador	Gestiona roles, instituciones (si se incluye), políticas globales, espacios y auditoría.
Docente / organizador	Crea actividades, configura sesión, zona, duración y QR; consulta y corrige incidencias con justificación.
Estudiante	Accede con cuenta académica; escanea QR, verifica asistencia y consulta historial.
Invitado sin cuenta	Proporciona información mínima; verifica contacto cuando corresponda; registra participación temporal.
Invitado con cuenta	Inicia sesión, registra asistencia y consulta su historial acumulado.
Canales: (1) app móvil React Native/Expo como experiencia primaria; (2) sitio web Next.js para administración y asistencia alternativa mediante cámara y geolocalización del navegador. Ambas interfaces consumen la misma API y comparten las reglas de validación. Las verificaciones específicas del dispositivo pueden variar por canal.
5. Conceptos de dominio y alcance
•	Espacio: lugar físico reutilizable (aula, laboratorio, auditorio, coliseo, estadio o espacio abierto), representado como punto y radio o, para áreas irregulares, polígono.
•	Evento: actividad académica o pública de nivel organizativo; puede contener una o varias sesiones.
•	Sesión: ventana concreta para controlar asistencia, con horario, estado, ubicación, método y políticas de validación.
•	Asistencia: resultado individual de validaciones por persona y sesión, con estado y evidencia mínima.
•	Intento: cada solicitud de registro y su resultado (aceptada, rechazada o pendiente de revisión).
Dentro del alcance inicial: autenticación, gestión básica de espacios y actividades, QR temporal, registro web/móvil, PostGIS, invitados, asistencias e informes. Fuera del alcance mínimo: integración obligatoria con el sistema académico real, reconocimiento facial, ubicación infalible en interiores, despliegue multisede a gran escala y pagos/certificados complejos. Pueden quedar como mejoras posteriores.
6. Flujos funcionales
6.1 Configuración de clase o evento
1.	El responsable se autentica y crea el evento, indicando título, descripción, fecha y tipo de actividad.
2.	Selecciona un espacio previamente registrado o define uno nuevo mediante coordenadas y zona autorizada.
3.	Configura sesión, ventana de entrada, radio o polígono, reglas para invitados y eventual control de permanencia.
4.	Inicia la sesión; el backend activa el mecanismo de generación del token temporal y el panel muestra su QR.
5.	Consulta en tiempo real las asistencias y finaliza la sesión cuando corresponda.
6.2 Asistencia de estudiante
6.	El estudiante inicia sesión en la app móvil o web.
7.	Escanea un QR vigente; el cliente envía el token y solicita una ubicación actual con precisión estimada.
8.	La API verifica identidad, sesión activa, horario, vigencia del token y ausencia de asistencia duplicada.
9.	PostGIS evalúa la distancia o pertenencia geográfica y la política trata mediciones imprecisas.
10.	Se registra asistencia, rechazo o revisión, y se presenta una explicación comprensible.
6.3 Asistencia de invitado
11.	El invitado escanea el QR o abre un enlace de acceso autorizado.
12.	Si no tiene cuenta, ingresa datos mínimos (por ejemplo nombre y correo o teléfono; documento solo cuando sea necesario).
13.	Opcionalmente confirma su contacto mediante código de un solo uso según política del evento.
14.	El servidor emite una autorización temporal ligada al intento de registro, para que la expiración del QR no interrumpa el formulario.
15.	Al confirmar se revisan nuevamente los límites de la sesión y la ubicación; se registra el resultado.
16.	Si luego crea una cuenta, el sistema vincula las asistencias anteriores solo tras comprobar la identidad o la titularidad del contacto.
6.4 Revisión y permanencia
El organizador puede solicitar un segundo control durante o al final de actividades extensas. Se diferencian entrada registrada, asistencia completa, parcial o pendiente de revisión. Toda decisión manual debe indicar responsable, fecha, razón y cambios realizados.
7. Seguridad y prevención de fraude
Amenaza / error	Control propuesto	Limitación
Reenvío del QR	Token temporal de 30 s; verificación en servidor; sesión y horario	El token puede compartirse mientras siga vigente.
Reutilización / duplicado	Restricción única persona-sesión; operación transaccional e idempotencia	Requiere identificación coherente entre invitados.
Usuario remoto	Ubicación actual + área permitida, verificada en backend	GPS reportado puede falsearse.
Aula contigua / piso distinto	Medición de precisión y estado incierto; segunda comprobación opcional	GPS no resuelve proximidad de interiores con fiabilidad.
Suplantación de cuenta	Autenticación, verificación de contacto, protección de sesiones	La cuenta no garantiza presencia del titular.
Salida anticipada	Control complementario de permanencia, si se configura	Exige interacción adicional.
Modificación irregular	Bitácora de auditoría, permisos y justificación	El control institucional sigue siendo necesario.
Implementación sugerida del QR: token aleatorio criptográficamente seguro, asociado a un identificador de sesión, instante de emisión, caducidad y estado; mantener su validación en servidor. No colocar datos personales o secretos en la imagen. Muchos participantes pueden usar el mismo token durante su vigencia: no debe invalidarse por el primer escaneo. Emplear rate limiting y HTTPS.
Para invitados, la autorización emitida después de un escaneo válido debe tener expiración propia, ser de un solo propósito, no permitir trasladar una asistencia a otra persona y no impedir las comprobaciones finales. El registro solo se confirma cuando se completan las validaciones finales.
8. Geolocalización y evaluación espacial
Al registrar un espacio se almacena su geometría en SRID 4326. Para distancias en metros se recomienda usar el tipo geography o las transformaciones correctas. ST_DWithin permite evaluar la cercanía entre un punto de asistencia y el punto central de la zona. Para polígonos se pueden emplear funciones de contención o intersección. La tolerancia configurada depende del tipo de instalación y pruebas reales.
Espacio	Valor inicial para pruebas	Observación
Aula/laboratorio	15–25 m	No discrimina con certeza aulas o pisos.
Auditorio	30–60 m	Ajustar según edificio y errores medidos.
Coliseo	60–120 m	Valorar polígono en lugar de círculo.
Estadio	100–200 m	Preferir delimitación real del recinto.
Espacio abierto	Personalizado	Configurar según perímetro autorizado.
Los radios son valores tentativos para experimentación, NO garantías de precisión. El cliente envía coordenadas, timestamp y exactitud estimada; el backend aplica umbral de antigüedad y exactitud configurable. Cuando la incertidumbre se cruza con el límite de la zona, debe repetir la medición o pasar a revisión. La ubicación reportada por el dispositivo no constituye prueba criptográfica de presencia.
9. Reglas de negocio iniciales
RN-01. Una persona tiene como máximo una asistencia confirmada por sesión.
RN-02. Solo se admiten solicitudes para sesiones activas y dentro de la ventana permitida.
RN-03. La validación de caducidad usa la hora del servidor.
RN-04. Las políticas geográficas se definen por sesión o se heredan del espacio y pueden ajustarse por autorización.
RN-05. La precisión insuficiente o ubicación desactualizada no debe aceptarse automáticamente.
RN-06. Los invitados pueden registrar asistencia sin cuenta, pero el historial posterior requiere vinculación verificada.
RN-07. La pertenencia al listado de estudiantes se determina por un identificador institucional, cuando exista integración.
RN-08. Los registros fallidos quedan auditados con información mínima, evitando una recopilación excesiva de coordenadas.
RN-09. Los organizadores no pueden alterar asistencias sin trazabilidad.
RN-10. El cierre de sesión suspende nuevos registros incluso si un QR recién emitido aún no ha caducado.
RN-11. El segundo control de permanencia es opcional y no debe aplicarse sin aviso al participante.
RN-12. Todo resultado de validación debe comunicar estado y motivo legible al usuario.
10. Requisitos funcionales (borrador)
RF-01. Crear y administrar cuentas, perfiles y roles.
RF-02. Importar o registrar estudiantes institucionales, si se dispone de datos.
RF-03. Registrar y reutilizar espacios geográficos.
RF-04. Crear eventos y sesiones con horarios y políticas.
RF-05. Emitir QR dinámicos mientras la sesión está abierta.
RF-06. Escanear y validar QR en web y app móvil.
RF-07. Solicitar ubicación y evaluar proximidad mediante PostGIS.
RF-08. Registrar invitados sin cuenta y permitir cuenta opcional.
RF-09. Vincular historial de invitados con verificación de titularidad.
RF-10. Permitir consulta de asistencia e historial personal.
RF-11. Gestionar revisión y rectificación auditada.
RF-12. Exportar informes de asistencias por evento, sesión y estado.
RF-13. Registrar intentos, rechazos y duplicados.
RF-14. Configurar segundo control de permanencia.
11. Requisitos no funcionales (propuesta medible)
•	Seguridad: HTTPS, control de acceso por roles, validación de entradas, protección de tokens y límites de peticiones.
•	Integridad: unicidad de asistencia por sesión y persona; transacciones ante solicitudes concurrentes.
•	Rendimiento: definir en pruebas una meta de respuesta p95 para validación (objetivo inicial por evaluar: < 2 s sin contar adquisición de GPS).
•	Disponibilidad: tratar pérdida de red con estados y reintentos sin crear registros duplicados.
•	Usabilidad y accesibilidad: mensajes claros, botones legibles y alternativa operativa cuando falte cámara o permiso de ubicación.
•	Privacidad: minimización de datos y geolocalización puntual, periodos de conservación, consentimiento/aviso e información para titulares.
•	Reproducibilidad: README con instalación, variables de entorno de ejemplo, migraciones y ejecución local.
•	Mantenibilidad: pruebas automatizadas, linters, convenciones de commits y separación de responsabilidades.
12. Arquitectura y tecnologías propuestas
Componente	Tecnología propuesta	Responsabilidad
Móvil	React Native + Expo	Escaneo, permisos, ubicación, historial y perfil.
Web	Next.js + TypeScript	Panel de organizador, visualización QR y acceso web de participantes.
Backend	Spring Boot (Java 21) o Laravel	API, autenticación, reglas de negocio y validaciones.
Datos	PostgreSQL + PostGIS	Usuarios, sesiones, geometrías, asistencias, auditoría.
Temporal (opcional)	Redis	Apoyo para tokens temporales y rate limiting.
Infraestructura	Docker Compose en desarrollo	Servicios reproducibles e integración local.
Decisión pendiente: escoger un único framework backend con el equipo. La arquitectura se propone modular por dominios (identidades, eventos, espacios, QR, validación, asistencia, reportes), evitando colocar reglas críticas únicamente en el cliente.
13. Modelo conceptual de datos
•	personas (id, tipo, nombre, identificador opcional, datos de contacto).
•	usuarios (id, persona_id, credenciales/identidad externa, estado); roles y usuarios_roles.
•	estudiantes (persona_id, codigo_institucional y atributos académicos).
•	espacios (id, nombre, tipo, geometría, parámetros predeterminados).
•	eventos (id, organizador_id, título, tipo y metadatos).
•	sesiones (id, evento_id, espacio_id, inicio, fin, estado, política y radio).
•	tokens_qr (id, sesion_id, hash_token, emitido, expira); alternativamente tokens verificables sin persistir cada rotación.
•	asistencias (id, sesion_id, persona_id, estado, registrado_en y validación).
•	intentos_asistencia (id, sesion_id, persona_id o identificador temporal, resultado, razón, timestamp y metadatos mínimos).
•	verificaciones_contacto / invitaciones y auditoria_cambios.
Índices sugeridos: UNIQUE(sesion_id, persona_id) en asistencias confirmadas, índices para tokens activos y GiST sobre geometría geoespacial. Revisar la identificación de invitados para que no se creen duplicados triviales por variaciones de nombre.
14. API preliminar (propuesta, no contrato definitivo)
Método y ruta	Propósito
POST /auth/login	Inicio de sesión.
GET /spaces	Consulta de espacios.
POST /events	Creación de evento.
POST /events/{id}/sessions	Creación de sesión.
POST /sessions/{id}/start	Apertura de sesión.
GET /sessions/{id}/qr	QR temporal (solo autorizado).
POST /attendance/attempts	Enviar token, contexto e información de ubicación.
POST /guests/pre-register	Identificación preliminar de invitado.
GET /me/attendances	Historial personal.
GET /sessions/{id}/attendances	Reporte para responsable autorizado.
Las rutas y esquemas deberán detallarse en OpenAPI una vez definido el backend y el proceso exacto de autenticación de invitados.
15. Organización del repositorio y GitFlow simplificado
Estrategia sugerida: main como rama estable; develop como rama de integración; feature/* para trabajo individual o por módulo; fix/* para correcciones en desarrollo; hotfix/* para fallos urgentes de la rama estable. Integrar por Pull Request, revisiones y pruebas. Evitar commits directos a main. Cada integrante debe configurar nombre y correo de Git para disponer de commits atribuibles.
Estructura inicial orientativa: /apps/web, /apps/mobile, /services/api, /infra, /docs, /specs, /.specify y README.md. Puede adaptarse según las convenciones reales de Spec Kit. No afirmar que estas carpetas ya existen sin crearlas en el repositorio.
16. SDD / GitHub Spec Kit
Se propone un enfoque Specification-Driven Development. Antes de implementar una funcionalidad se define su comportamiento observable y sus criterios de aceptación; luego se aclaran ambigüedades, se elabora un plan técnico y se dividen tareas.
Artefacto / actividad	Propósito
constitution	Principios no negociables del proyecto: seguridad, calidad, pruebas, privacidad, arquitectura y forma de trabajo.
specify	Especificación funcional: usuarios, historias, requisitos, escenarios y criterios verificables.
clarify	Resolver ambigüedades: invitado sin cuenta, precisión GPS, expiración de QR, políticas de permanencia.
plan	Decisiones arquitectónicas, componentes, API, modelo de datos, dependencias y estrategia de pruebas.
Primeras especificaciones sugeridas: 001-autenticacion-identidades, 002-espacios-eventos, 003-qr-dinamico, 004-validacion-geografica, 005-invitados, 006-reportes-auditoria. El orden puede variar por dependencias.
17. Plan de implementación sugerido
Fase	Entregables
0. Preparación	GitHub, ramas, primeros commits por integrante, README, .env.example, estructura SDD y proyecto base que arranca.
1. Núcleo funcional	Roles, usuarios, eventos/espacios, sesiones, QR temporal y escaneo.
2. Asistencia híbrida	Web y móvil, invitados, registro transaccional e historial.
3. Geoespacial	PostGIS, configuración de radio/polígono, precisión y resultados inciertos.
4. Calidad y evaluación	Seguridad, auditoría, reportes, pruebas de carga básicas y experimentos antifraude.
18. Matriz inicial de pruebas
Caso	Resultado esperado
Participante válido, QR activo y zona válida	Registro correcto una sola vez.
QR caducado	Intento rechazado con motivo.
QR compartido a distancia	Rechazo si ubicación reportada cae fuera de la zona según política.
Dos solicitudes simultáneas de la misma persona	Un único registro confirmado.
GPS poco preciso en aula	Nueva medición o revisión, sin aceptación automática.
Participante en aula contigua	Documentar limitación y resultado medido.
GPS simulado	Evaluar detección y limitaciones, sin prometer prevención absoluta.
Invitado crea cuenta después	Historial asociado tras verificación.
Sesión finalizada	No admite nuevas asistencias.
Interrupción de red	Reintento controlado, sin duplicar.
19. Riesgos, supuestos y decisiones pendientes
•	Riesgo alto: falsos negativos por precisión insuficiente en interiores; mitigación: medición real y ruta de revisión.
•	Riesgo alto: suplantación o GPS simulado; mitigación: autenticación, análisis de comportamiento, señales adicionales y auditoría.
•	Riesgo medio: fricción de registro de invitados y caducidad del QR; mitigación: credencial temporal y preinscripción opcional.
•	Riesgo medio: falta de conectividad en eventos masivos; mitigación: mensajes claros, reintentos idempotentes y capacidad de revisión.
•	Pendiente: elegir backend definitivo, número de integrantes, institución objetivo, forma de importar estudiantes, servicios de correo/OTP y hosting.
•	Pendiente: definir obligatoriedad del documento de identidad de invitados, política de retención y base legal aplicable.
•	Pendiente: determinar si se habilitan polígonos desde la primera versión o únicamente radios y si habrá comprobación de permanencia.
20. Criterios de aceptación de la práctica universitaria inicial
•	Existe repositorio remoto de GitHub con main estable, develop si corresponde y convención documentada de feature/*.
•	Cada integrante dispone de al menos un commit identificable y verificable.
•	Otro integrante puede clonar y ejecutar el esqueleto del sistema únicamente con instrucciones escritas en README.
•	El repositorio contiene .gitignore, .env.example sin secretos, archivos de instalación y dependencias reproducibles.
•	Constan artefactos SDD para constitution, specify, clarify y plan, o sus equivalentes según Spec Kit.
•	Las primeras historias incluyen criterios comprobables, no solo descripciones generales.
21. Glosario
QR dinámico: código cuyo token o contenido válido cambia periódicamente. Geocerca: área digital que delimita ubicaciones autorizadas. PostGIS: extensión de PostgreSQL para almacenar y consultar datos espaciales. Precisión GPS: estimación probabilística de incertidumbre del posicionamiento. Falso positivo: asistencia aceptada incorrectamente. Falso negativo: asistencia real rechazada incorrectamente. SDD: desarrollo guiado por especificaciones. Idempotencia: una misma operación repetida no genera resultados adicionales inesperados.
22. Nota final de alcance
Este documento reúne las decisiones y propuestas discutidas hasta la fecha y sirve como contexto común para docentes, integrantes del equipo y herramientas de asistencia al desarrollo. Es una línea base de análisis, no evidencia de implementación. Los parámetros tentativos, las tecnologías opcionales y las rutas API están sujetos a validación y ajuste documentado.
