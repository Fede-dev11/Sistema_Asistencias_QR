export interface paths {
    "/auth/login": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * Iniciar sesión con credenciales
         * @description Proveedor local semilla solo en desarrollo; CAS detrás del proveedor de identidad queda pendiente de confirmación institucional. Bearer opaco revocable respaldado en PostgreSQL; sin JWT ni almacenamiento web persistente.
         */
        post: operations["login"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/auth/logout": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * Revocar sesión autenticada
         * @description Revoca en PostgreSQL el bearer presentado. No registrar su valor.
         */
        post: operations["logout"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/activities": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Descubrir actividades autorizadas
         * @description Lista paginada y filtrada en servidor. RESPONSABLE solo descubre sus actividades; DIRECTOR_INSTITUCIONAL todas; DIRECTOR_CARRERA solo las vinculadas a su carrera y paralelos. Los filtros restringen el ámbito autorizado y nunca lo amplían. Una carrera fuera del ámbito devuelve 403. Excluir actividades cuyo plazo nominal de cinco años calendario ya venció; los agregados anónimos no se exponen aquí. Sin filtros, devolver solo actividades autorizadas.
         */
        get: operations["listActivities"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/activities/{activityId}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Consultar actividad
         * @description Lectura permitida a RESPONSABLE de su propia actividad, DIRECTOR_INSTITUCIONAL en todas y DIRECTOR_CARRERA solo en actividades vinculadas en servidor a su carrera y sus paralelos. No confiar en carrera enviada por el cliente. Al vencer cinco años calendario desde su cierre definitivo, la consulta nominal devuelve 410; estadísticas anónimas no se exponen aquí. El 410 de conservación aplica mientras el recurso nominal vencido sea reconocible; después de sanear/eliminar sus encabezados, devolver 404 sin conservar PII para mantener un 410.
         */
        get: operations["getActivity"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/activities/{activityId}/close": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * Cerrar definitivamente una actividad
         * @description Solo RESPONSABLE vinculado a esta actividad. Los directores no reciben facultades de modificación por su permiso de lectura. Cierre definitivo distinto del cierre de una sesión. Cierra las sesiones activas, invalida QR e inicios invitados y fija cerrada_en y anonimizar_en a cinco años calendario. No permite reabrir ni crear nuevas sesiones.
         */
        post: operations["closeActivity"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/activities/{activityId}/guest-settings": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * Configurar acceso de invitados
         * @description Solo RESPONSABLE vinculado a esta actividad. Los directores no reciben facultades de modificación por su permiso de lectura. Solo EVENTO abierto. Formulario exclusivo de personas sin cuenta precargadas. La tolerancia se copia al crear cada inicio; cambios posteriores no prolongan inicios ya emitidos. Deshabilitar impide nuevos envíos.
         */
        put: operations["setGuestSettings"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/activities/{activityId}/guest-list": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * Reemplazar listado de invitados
         * @description Solo RESPONSABLE vinculado a esta actividad. Los directores no reciben facultades de modificación por su permiso de lectura. Solo EVENTO abierto. Reemplazo atómico; validar todo antes de guardar. Cada cédula identifica una persona sin crear cuenta. No autorregistro. Una cuenta existente, incluso inactiva, exige credenciales y no habilita la vía invitada. El límite inicial de 1000 entradas es técnico y fijo en este contrato; cambiarlo requiere actualizar el contrato y su versión. No es una regla institucional. Eliminar una entrada no elimina asistencias ya registradas.
         */
        put: operations["replaceGuestList"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/sessions/{sessionId}/start": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * Iniciar sesión de asistencia
         * @description Solo RESPONSABLE vinculado a esta actividad. Los directores no reciben facultades de modificación por su permiso de lectura. Transición PROGRAMADA a ACTIVA solo si la actividad permanece abierta. Transacción coordinada con sus cierres.
         */
        post: operations["startSession"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/sessions/{sessionId}/close": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * Cerrar sesión de asistencia
         * @description Solo RESPONSABLE vinculado a esta actividad. Los directores no reciben facultades de modificación por su permiso de lectura. Invalida inmediatamente nuevos registros e inicios de invitados, aunque QR o formularios tengan plazo restante. No inicia por sí mismo los cinco años: el origen es el cierre definitivo de la actividad.
         */
        post: operations["closeSession"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/sessions/{sessionId}/qr": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Obtener QR vigente
         * @description Solo RESPONSABLE vinculado a esta actividad. Los directores no reciben facultades de modificación por su permiso de lectura. Solo sesión ACTIVA y actividad abierta. Rotación cada 30 segundos; cada token vence a los 40 segundos desde su emisión. El servidor comprueba la hora de recepción, no la hora declarada por el dispositivo. No incluye datos personales. Responder Cache-Control: no-store. Representación permitida del cliente: URL con fragmento #qr=token; nunca ruta o query.
         */
        get: operations["getSessionQr"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/sessions/{sessionId}/geolocation": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * Configurar GPS opcional
         * @description Solo RESPONSABLE vinculado a esta actividad. Los directores no reciben facultades de modificación por su permiso de lectura. Solo PROGRAMADA. Habilitar exige capacidad geográfica disponible y radio_m, precision_max_m y antiguedad_max_s válidos. El servidor determina el modo: piloto/revisión permitido; rechazo automático solo con evidencia de campo verificada, sin aceptar una declaración del cliente como prueba. Sin GPS no se solicitan permisos ni se almacenan coordenadas. Capacidad ausente: 409 CAPACIDAD_GPS_NO_DISPONIBLE.
         */
        put: operations["setSessionGeolocation"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/attendance/attempts": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * Registrar asistencia autenticada
         * @description Cuenta autenticada obligatoria. En CLASE comprobar vínculo autorizado a la clase; en EVENTO comprobar autorización del evento. No aceptar una cédula cliente para elegir identidad. Comprobar QR vigente, sesión activa, actividad abierta, GPS si corresponde y unicidad persona/sesión. El plazo exclusivo de invitados nunca aplica aquí. Con GPS deshabilitado, rechazar ubicación innecesaria sin guardarla.
         */
        post: operations["recordAuthenticatedAttendance"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/guest-registrations": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * Abrir formulario exclusivo de invitados
         * @description Sin cuenta autenticada; exige QR ordinario vigente, sesión ACTIVA, actividad EVENTO abierta y flujo invitado habilitado. Emite capacidad opaca asociada al formulario y a una sola persona al consumirse. inicio_en se fija al validar el QR en servidor; vence_en usa la tolerancia del evento tomada como instantánea. No extiende el QR ni la ventana de cuentas. No devuelve listado o datos personales. Rate limit obligatorio; no requiere Idempotency-Key. Recargar recupera el mismo inicio en sessionStorage de la pestaña sin volver a invocar esta operación ni extender vence_en. Un nuevo escaneo explícito de QR vigente puede abrir otro inicio, sin reiniciar el anterior.
         */
        post: operations["startGuestRegistration"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/guest-registrations/submit": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * Enviar cédula de invitado precargado
         * @description Sin bearer de cuenta; autenticar capacidad registro_invitado_token. Validar vencimiento con recepción del servidor, sesión/actividad abiertas y flujo habilitado. Volver a consultar existencia de cuenta y pertenencia actual al listado dentro de la transacción; cuenta existente: 403 USAR_CREDENCIALES, fuera del listado: 403 NO_AUTORIZADO. Usar exclusivamente datos precargados. No OTP, verificación presencial ni autorregistro. Consumir capacidad para una sola persona y registrar una sola asistencia. GPS según sesión. Recibo mínimo sin nombres, cédula ni contenido del listado. Vencimiento: 410 FORMULARIO_EXPIRADO. Con GPS apagado rechazar ubicación innecesaria sin conservarla. El comportamiento de identificación por listado es una excepción constitucional propuesta, pendiente de revisión.
         */
        post: operations["submitGuestAttendance"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/sessions/{sessionId}/attendances": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Consultar asistencias paginadas
         * @description Lectura permitida a RESPONSABLE de su propia actividad, DIRECTOR_INSTITUCIONAL en todas y DIRECTOR_CARRERA solo en actividades vinculadas en servidor a su carrera y sus paralelos. No confiar en carrera enviada por el cliente. Al vencer retención de la actividad: 410 sin datos nominales; estadísticas agregadas se definirán por separado. Una fila por persona identificada: confirmación prevalece; sin asistencia, último intento identificado. Excluir intentos sin persona identificada y cédulas desconocidas. Aplicar estado después de esta proyección. El 410 de conservación aplica mientras el recurso nominal vencido sea reconocible; después de sanear/eliminar sus encabezados, devolver 404 sin conservar PII para mantener un 410.
         */
        get: operations["listAttendances"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/sessions/{sessionId}/attendances/export": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Exportar asistencias CSV
         * @description Solo RESPONSABLE vinculado a esta actividad. Los directores no reciben facultades de modificación por su permiso de lectura. La lectura global o por carrera no concede exportación a los directores. Auditar la exportación. Al vencer retención: 410. No exportar secretos ni coordenadas exactas; neutralizar fórmulas en campos de texto. El 410 de conservación aplica mientras el recurso nominal vencido sea reconocible; después de sanear/eliminar sus encabezados, devolver 404 sin conservar PII para mantener un 410.
         */
        get: operations["exportAttendances"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/attempts/{attemptId}/review": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * Resolver intento pendiente
         * @description Solo RESPONSABLE vinculado a esta actividad. Los directores no reciben facultades de modificación por su permiso de lectura. Solo intento de su actividad pendiente de revisión. Razón no vacía, auditoría con responsable, fecha, anterior y nuevo. Aprobar para crear una asistencia requiere sesión ACTIVA y actividad abierta; si están cerradas devolver 409. Una aprobación no duplica asistencia. Rechazar una revisión no crea asistencia y puede resolver un pendiente después del cierre, mientras los datos sigan dentro de su conservación. Revisiones concurrentes y cierre comparten protocolo transaccional.
         */
        post: operations["reviewAttempt"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/activities/{activityId}/audit": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Consultar auditoría paginada
         * @description Lectura permitida a RESPONSABLE de su propia actividad, DIRECTOR_INSTITUCIONAL en todas y DIRECTOR_CARRERA solo en actividades vinculadas en servidor a su carrera y sus paralelos. No confiar en carrera enviada por el cliente. Incluye acciones administrativas, cambios manuales, configuración, listados y exportaciones con sus valores pertinentes. No devuelve credenciales, tokens ni ubicación exacta. Auditoría nominal sujeta al mismo vencimiento: 410; evidencia estadística anónima fuera de este endpoint. El 410 de conservación aplica mientras el recurso nominal vencido sea reconocible; después de sanear/eliminar sus encabezados, devolver 404 sin conservar PII para mantener un 410.
         */
        get: operations["listActivityAudit"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
}
export type webhooks = Record<string, never>;
export interface components {
    schemas: {
        Problem: {
            /**
             * Format: uri-reference
             * @example urn:unl:problem:session-closed
             */
            type: string;
            /** @example Sesión cerrada */
            title: string;
            status: number;
            /** @example La sesión ya no admite nuevos registros. */
            detail?: string;
            /** Format: uri-reference */
            instance?: string;
            /**
             * @description Código estable para el cliente; no contiene cédulas ni secretos. 002 añade DISPOSITIVO_REQUERIDO, DISPOSITIVO_YA_REGISTRADO, CEDULA_INVALIDA, ESPACIO_CODIGO_EXISTE, ESPACIO_SIN_UBICACION, METODO_NO_ADMITE_QR, QR_REQUERIDO, PARALELO_NO_ASIGNADO, SESION_NO_EDITABLE y HORARIO_DEMASIADO_LARGO.
             * @example SESION_CERRADA
             */
            code: string;
            /** @example La sesión está cerrada. */
            mensaje: string;
            /** Format: uuid */
            attempt_id?: string;
            correlation_id?: string;
        };
        /** @enum {string} */
        VerificationMethod: "QR_Y_GPS" | "SOLO_QR" | "SOLO_GPS" | "NINGUNO";
        /**
         * Format: uuid
         * @description Identificador de instalación generado por la app; el servidor solo guarda su HMAC.
         */
        DeviceId: string;
        Space: {
            /** Format: uuid */
            id: string;
            codigo: string;
            nombre: string;
            /** @enum {string} */
            tipo: "AULA" | "LABORATORIO" | "AUDITORIO" | "EXTERIOR" | "OTRO";
            latitud?: number;
            longitud?: number;
            radio_defecto_m: number;
            precision_defecto_m: number;
            editable: boolean;
        };
        ImportReport: {
            procesadas: number;
            creadas: number;
            actualizadas: number;
            sin_cambios: number;
            rechazadas: {
                fila: number;
                motivo: string;
            }[];
        };
        LoginRequest: {
            /** Format: email */
            correo: string;
            /** Format: password */
            password: string;
        };
        LoginResult: {
            /** @description Secreto opaco aleatorio. No incluirlo en URL, logs o telemetría. */
            access_token: string;
            /** @enum {string} */
            token_type: "Bearer";
            /** Format: date-time */
            vence_en: string;
            roles: ("DOCENTE" | "ENCARGADO" | "ESTUDIANTE" | "DIRECTOR_INSTITUCIONAL" | "DIRECTOR_CARRERA")[];
        };
        /** @description Los eventos pueden vincular varias carreras y paralelos. El servidor calcula los ámbitos mediante estos vínculos y responsable_id; los filtros de lectura no los sustituyen. */
        Activity: {
            /** Format: uuid */
            id: string;
            nombre: string;
            /** @enum {string} */
            tipo: "CLASE" | "EVENTO";
            /** @enum {string} */
            estado: "ABIERTA" | "CERRADA";
            /** Format: date-time */
            cerrada_en: string | null;
            /** Format: date-time */
            anonimizar_en: string | null;
            /** Format: uuid */
            responsable_id: string;
            carrera_ids: string[];
            paralelo_ids: string[];
        };
        ActivityPage: {
            items: components["schemas"]["Activity"][];
            page: number;
            size: number;
            total: number;
        };
        GuestSettings: {
            habilitado: boolean;
            /** @description Solo para el formulario de invitados sin cuenta precargados. No modifica la vigencia ordinaria del QR. */
            tolerancia_invitado_segundos: number;
        };
        GuestEntry: {
            /** @description Diez dígitos ASCII como cadena; conservar ceros iniciales. Identifica la entrada del listado, no acredita titularidad. */
            cedula: string;
            nombres: string;
        };
        GuestListReplacement: {
            /** @description Límite técnico inicial fijo de 1000 entradas en este contrato; cambiarlo requiere actualizar el contrato y su versión. No es una regla institucional. Reemplazo completo y atómico; rechazar cédulas duplicadas y cualquier fila inválida. */
            invitados: components["schemas"]["GuestEntry"][];
        };
        GuestListResult: {
            /** Format: uuid */
            activity_id: string;
            total: number;
            /** Format: date-time */
            actualizada_en: string;
        };
        Session: {
            /** Format: uuid */
            id: string;
            /** Format: uuid */
            activity_id: string;
            /** @enum {string} */
            estado: "PROGRAMADA" | "ACTIVA" | "CERRADA";
            /** Format: date-time */
            iniciada_en: string | null;
            /** Format: date-time */
            cerrada_en: string | null;
        };
        Qr: {
            /** @description Secreto QR opaco. Puede representarse en una URL con fragmento #qr=token, leído por el cliente y enviado solo en el cuerpo al backend. No incluirlo en ruta, query, logs o telemetría; retirar el fragmento tras leerlo. */
            qr_token: string;
            /** Format: date-time */
            emitido_en: string;
            /** Format: date-time */
            vence_en: string;
            /** @enum {integer} */
            rotacion_segundos: 30;
            /** @enum {integer} */
            vigencia_segundos: 40;
        };
        /** @description Al habilitar se requieren los tres parámetros; el servidor los valida conjuntamente. Si está apagada no se pide permiso ni se conserva ubicación. */
        GeolocationSettings: {
            /** @default false */
            habilitada: boolean;
            radio_m?: number;
            precision_max_m?: number;
            antiguedad_max_s?: number;
        };
        GeolocationStatus: {
            configuracion: components["schemas"]["GeolocationSettings"];
            capacidad_disponible: boolean;
            /** @enum {string} */
            modo_efectivo: "DESACTIVADA" | "PILOTO_REVISION" | "VALIDACION_AUTOMATICA";
            evidencia_campo_verificada: boolean;
        };
        Location: {
            latitud: number;
            longitud: number;
            precision_m: number;
            /** Format: date-time */
            medido_en: string;
        };
        AttendanceAttemptRequest: {
            /** @description Secreto QR opaco. Puede representarse en una URL con fragmento #qr=token, leído por el cliente y enviado solo en el cuerpo al backend. No incluirlo en ruta, query, logs o telemetría; retirar el fragmento tras leerlo. */
            qr_token: string;
            ubicacion?: components["schemas"]["Location"];
        };
        /** @description Recibo mínimo compartido por ambos flujos. Sin cédula, nombres ni datos del listado. */
        AttendanceReceipt: {
            /** Format: uuid */
            asistencia_id: string;
            /** Format: uuid */
            attempt_id: string;
            /** Format: uuid */
            session_id: string;
            /** @enum {string} */
            resultado: "CONFIRMADA";
            /** Format: date-time */
            registrada_en: string;
            mensaje: string;
        };
        PendingReceipt: {
            /** Format: uuid */
            attempt_id: string;
            /** @enum {string} */
            resultado: "REPETIR" | "PENDIENTE_REVISION";
            code: string;
            mensaje: string;
        };
        GuestStartRequest: {
            /** @description Secreto QR opaco. Puede representarse en una URL con fragmento #qr=token, leído por el cliente y enviado solo en el cuerpo al backend. No incluirlo en ruta, query, logs o telemetría; retirar el fragmento tras leerlo. */
            qr_token: string;
        };
        GuestStartResult: {
            /** @description Secreto opaco aleatorio. No incluirlo en URL, logs o telemetría. */
            registro_invitado_token: string;
            /** Format: date-time */
            inicio_en: string;
            /** Format: date-time */
            vence_en: string;
        };
        GuestSubmitRequest: {
            /** @description Secreto opaco aleatorio. No incluirlo en URL, logs o telemetría. */
            registro_invitado_token: string;
            cedula: string;
            ubicacion?: components["schemas"]["Location"];
        };
        /** @description Una fila por persona identificada en la sesión. Prevalece su asistencia confirmada; si no existe, mostrar su último intento identificado pendiente o rechazado. Nunca exponer intentos sin persona identificada ni cédulas desconocidas. El filtro estado se aplica después de esta proyección. */
        AttendanceRow: {
            /**
             * Format: uuid
             * @description UUID si existe asistencia confirmada; null para pendiente o rechazo sin asistencia.
             */
            asistencia_id: string | null;
            /** Format: uuid */
            persona_id: string;
            cedula: string;
            nombres: string;
            /** @enum {string} */
            estado: "CONFIRMADA" | "PENDIENTE_REVISION" | "RECHAZADA";
            /**
             * Format: date-time
             * @description Hora de confirmación; si no existe asistencia, hora del último intento identificado mostrado.
             */
            registrada_en: string;
            /** Format: uuid */
            attempt_id: string;
            /** @description Código estable del resultado; por ejemplo ASISTENCIA_CONFIRMADA o UBICACION_INCIERTA. */
            code: string;
            /** @description Motivo comprensible del resultado mostrado. */
            mensaje: string;
        };
        AttendancePage: {
            items: components["schemas"]["AttendanceRow"][];
            page: number;
            size: number;
            total: number;
        };
        ReviewRequest: {
            /** @enum {string} */
            decision: "APROBAR" | "RECHAZAR";
            razon: string;
        };
        ReviewResult: {
            /** Format: uuid */
            attempt_id: string;
            /** Format: uuid */
            asistencia_id: string | null;
            /** @enum {string} */
            decision: "APROBAR" | "RECHAZAR";
            /** Format: date-time */
            revisada_en: string;
        };
        /** @description Valores anteriores/nuevos pertinentes; no incluir credenciales, tokens, coordenadas exactas o copias completas de listados. */
        AuditValues: {
            estado?: string;
            decision?: string;
            configuracion_invitados?: components["schemas"]["GuestSettings"];
            configuracion_geografica?: components["schemas"]["GeolocationSettings"];
            cantidad_invitados?: number;
        };
        AuditRow: {
            /** Format: uuid */
            id: string;
            /** Format: uuid */
            activity_id: string;
            /** Format: uuid */
            responsable_id: string;
            accion: string;
            /** Format: date-time */
            ocurrida_en: string;
            razon?: string;
            /** @description Valores anteriores/nuevos pertinentes; no incluir credenciales, tokens, coordenadas exactas o copias completas de listados. */
            anterior: {
                estado?: string;
                decision?: string;
                configuracion_invitados?: components["schemas"]["GuestSettings"];
                configuracion_geografica?: components["schemas"]["GeolocationSettings"];
                cantidad_invitados?: number;
            } | null;
            /** @description Valores anteriores/nuevos pertinentes; no incluir credenciales, tokens, coordenadas exactas o copias completas de listados. */
            nuevo: {
                estado?: string;
                decision?: string;
                configuracion_invitados?: components["schemas"]["GuestSettings"];
                configuracion_geografica?: components["schemas"]["GeolocationSettings"];
                cantidad_invitados?: number;
            } | null;
        };
        AuditPage: {
            items: components["schemas"]["AuditRow"][];
            page: number;
            size: number;
            total: number;
        };
    };
    responses: {
        /** @description Datos mal formados o parámetros inválidos. */
        BadRequest: {
            headers: {
                [name: string]: unknown;
            };
            content: {
                /**
                 * @example {
                 *       "type": "urn:unl:problem:solicitud-invalida",
                 *       "title": "Datos mal formados o parámetros inválidos.",
                 *       "status": 400,
                 *       "code": "SOLICITUD_INVALIDA",
                 *       "mensaje": "Datos mal formados o parámetros inválidos."
                 *     }
                 */
                "application/problem+json": components["schemas"]["Problem"];
            };
        };
        /** @description Credenciales o bearer ausentes, inválidos, revocados o caducados. */
        Unauthorized: {
            headers: {
                [name: string]: unknown;
            };
            content: {
                /**
                 * @example {
                 *       "type": "urn:unl:problem:no-autenticado",
                 *       "title": "Credenciales o bearer ausentes, inválidos, revocados o caducados.",
                 *       "status": 401,
                 *       "code": "NO_AUTENTICADO",
                 *       "mensaje": "Credenciales o bearer ausentes, inválidos, revocados o caducados."
                 *     }
                 */
                "application/problem+json": components["schemas"]["Problem"];
            };
        };
        /** @description Permisos o ámbito insuficientes; en invitados USAR_CREDENCIALES si existe cuenta, NO_AUTORIZADO si no pertenece al listado. */
        Forbidden: {
            headers: {
                [name: string]: unknown;
            };
            content: {
                /**
                 * @example {
                 *       "type": "urn:unl:problem:no-autorizado",
                 *       "title": "Permisos o ámbito insuficientes",
                 *       "status": 403,
                 *       "code": "NO_AUTORIZADO",
                 *       "mensaje": "Permisos o ámbito insuficientes; en invitados USAR_CREDENCIALES si existe cuenta, NO_AUTORIZADO si no pertenece al listado."
                 *     }
                 */
                "application/problem+json": components["schemas"]["Problem"];
            };
        };
        /** @description Recurso inexistente. */
        NotFound: {
            headers: {
                [name: string]: unknown;
            };
            content: {
                /**
                 * @example {
                 *       "type": "urn:unl:problem:recurso-no-encontrado",
                 *       "title": "Recurso inexistente.",
                 *       "status": 404,
                 *       "code": "RECURSO_NO_ENCONTRADO",
                 *       "mensaje": "Recurso inexistente."
                 *     }
                 */
                "application/problem+json": components["schemas"]["Problem"];
            };
        };
        /** @description Estado incompatible o reutilización de Idempotency-Key con otra carga. */
        Conflict: {
            headers: {
                [name: string]: unknown;
            };
            content: {
                /**
                 * @example {
                 *       "type": "urn:unl:problem:estado-incompatible",
                 *       "title": "Estado incompatible o reutilización de Idempotency-Key con otra carga.",
                 *       "status": 409,
                 *       "code": "ESTADO_INCOMPATIBLE",
                 *       "mensaje": "Estado incompatible o reutilización de Idempotency-Key con otra carga."
                 *     }
                 */
                "application/problem+json": components["schemas"]["Problem"];
            };
        };
        /** @description Formulario vencido (FORMULARIO_EXPIRADO) o datos nominales ya sujetos a anonimización (DATOS_ANONIMIZADOS). En retención, 410 solo mientras el recurso vencido sea reconocible; tras sanear o eliminar el encabezado nominal, 404. */
        Gone: {
            headers: {
                [name: string]: unknown;
            };
            content: {
                /**
                 * @example {
                 *       "type": "urn:unl:problem:formulario-expirado",
                 *       "title": "Formulario vencido (FORMULARIO_EXPIRADO) o datos nominales ya sujetos a anonimización (DATOS_ANONIMIZADOS).",
                 *       "status": 410,
                 *       "code": "FORMULARIO_EXPIRADO",
                 *       "mensaje": "Formulario vencido (FORMULARIO_EXPIRADO) o datos nominales ya sujetos a anonimización (DATOS_ANONIMIZADOS)."
                 *     }
                 */
                "application/problem+json": components["schemas"]["Problem"];
            };
        };
        /** @description Intento rechazado por una regla de negocio; motivo legible y attempt_id cuando se registró el intento. */
        BusinessRejected: {
            headers: {
                [name: string]: unknown;
            };
            content: {
                /**
                 * @example {
                 *       "type": "urn:unl:problem:qr-expirado",
                 *       "title": "Intento rechazado por una regla de negocio",
                 *       "status": 422,
                 *       "code": "QR_EXPIRADO",
                 *       "mensaje": "Intento rechazado por una regla de negocio; motivo legible y attempt_id cuando se registró el intento."
                 *     }
                 */
                "application/problem+json": components["schemas"]["Problem"];
            };
        };
        /** @description Demasiadas solicitudes; límite técnico configurable por despliegue. */
        RateLimited: {
            headers: {
                /** @description Segundos antes de reintentar. */
                "Retry-After"?: number;
                [name: string]: unknown;
            };
            content: {
                /**
                 * @example {
                 *       "type": "urn:unl:problem:limite-solicitudes",
                 *       "title": "Demasiadas solicitudes",
                 *       "status": 429,
                 *       "code": "LIMITE_SOLICITUDES",
                 *       "mensaje": "Demasiadas solicitudes; límite técnico configurable por despliegue."
                 *     }
                 */
                "application/problem+json": components["schemas"]["Problem"];
            };
        };
    };
    parameters: {
        activityId: string;
        sessionId: string;
        attemptId: string;
        /** @description UUID por operación lógica. Misma identidad o capacidad invitada + operación + clave + carga canónica devuelve el resultado guardado tras revalidar autenticación y ámbito; carga distinta con la misma clave: 409. Nunca omite autorización ni crea una asistencia nueva después del cierre. */
        IdempotencyKey: string;
        Page: number;
        Size: number;
        AttendanceState: "CONFIRMADA" | "PENDIENTE_REVISION" | "RECHAZADA";
    };
    requestBodies: never;
    headers: never;
    pathItems: never;
}
export type $defs = Record<string, never>;
export interface operations {
    login: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["LoginRequest"];
            };
        };
        responses: {
            /** @description Sesión autenticada creada. */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    /**
                     * @example {
                     *       "access_token": "ejemplo-opaco-no-es-una-credencial-real-0001",
                     *       "token_type": "Bearer",
                     *       "vence_en": "2026-10-19T17:00:00Z",
                     *       "roles": [
                     *         "ESTUDIANTE"
                     *       ]
                     *     }
                     */
                    "application/json": components["schemas"]["LoginResult"];
                };
            };
            400: components["responses"]["BadRequest"];
            401: components["responses"]["Unauthorized"];
            429: components["responses"]["RateLimited"];
        };
    };
    logout: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description Sesión revocada. */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
            401: components["responses"]["Unauthorized"];
            429: components["responses"]["RateLimited"];
        };
    };
    listActivities: {
        parameters: {
            query?: {
                page?: components["parameters"]["Page"];
                size?: components["parameters"]["Size"];
                /** @description Restringir por tipo de actividad dentro del ámbito autorizado. */
                modalidad?: "CLASE" | "EVENTO";
                /** @description Restringir por vínculo de carrera verificado en servidor; no concede permisos. */
                carrera_id?: string;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description Página de actividades visibles, incluso vacía; sin datos de otros ámbitos. */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    /**
                     * @example {
                     *       "items": [],
                     *       "page": 0,
                     *       "size": 20,
                     *       "total": 0
                     *     }
                     */
                    "application/json": components["schemas"]["ActivityPage"];
                };
            };
            400: components["responses"]["BadRequest"];
            401: components["responses"]["Unauthorized"];
            403: components["responses"]["Forbidden"];
            429: components["responses"]["RateLimited"];
        };
    };
    getActivity: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                activityId: components["parameters"]["activityId"];
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description Actividad autorizada. */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Activity"];
                };
            };
            400: components["responses"]["BadRequest"];
            401: components["responses"]["Unauthorized"];
            403: components["responses"]["Forbidden"];
            404: components["responses"]["NotFound"];
            409: components["responses"]["Conflict"];
            410: components["responses"]["Gone"];
            429: components["responses"]["RateLimited"];
        };
    };
    closeActivity: {
        parameters: {
            query?: never;
            header: {
                /** @description UUID por operación lógica. Misma identidad o capacidad invitada + operación + clave + carga canónica devuelve el resultado guardado tras revalidar autenticación y ámbito; carga distinta con la misma clave: 409. Nunca omite autorización ni crea una asistencia nueva después del cierre. */
                "Idempotency-Key": components["parameters"]["IdempotencyKey"];
            };
            path: {
                activityId: components["parameters"]["activityId"];
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description Actividad cerrada y plazo de conservación calculado. */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Activity"];
                };
            };
            400: components["responses"]["BadRequest"];
            401: components["responses"]["Unauthorized"];
            403: components["responses"]["Forbidden"];
            404: components["responses"]["NotFound"];
            409: components["responses"]["Conflict"];
            410: components["responses"]["Gone"];
            429: components["responses"]["RateLimited"];
        };
    };
    setGuestSettings: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                activityId: components["parameters"]["activityId"];
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["GuestSettings"];
            };
        };
        responses: {
            /** @description Configuración validada. */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    /**
                     * @example {
                     *       "habilitado": true,
                     *       "tolerancia_invitado_segundos": 120
                     *     }
                     */
                    "application/json": components["schemas"]["GuestSettings"];
                };
            };
            400: components["responses"]["BadRequest"];
            401: components["responses"]["Unauthorized"];
            403: components["responses"]["Forbidden"];
            404: components["responses"]["NotFound"];
            409: components["responses"]["Conflict"];
            410: components["responses"]["Gone"];
            429: components["responses"]["RateLimited"];
        };
    };
    replaceGuestList: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                activityId: components["parameters"]["activityId"];
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["GuestListReplacement"];
            };
        };
        responses: {
            /** @description Listado reemplazado sin exponer su contenido. */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    /**
                     * @example {
                     *       "activity_id": "f12f0768-dc90-4f61-9ce9-9859fcd20d13",
                     *       "total": 2,
                     *       "actualizada_en": "2026-10-19T15:00:00Z"
                     *     }
                     */
                    "application/json": components["schemas"]["GuestListResult"];
                };
            };
            400: components["responses"]["BadRequest"];
            401: components["responses"]["Unauthorized"];
            403: components["responses"]["Forbidden"];
            404: components["responses"]["NotFound"];
            409: components["responses"]["Conflict"];
            410: components["responses"]["Gone"];
            429: components["responses"]["RateLimited"];
        };
    };
    startSession: {
        parameters: {
            query?: never;
            header: {
                /** @description UUID por operación lógica. Misma identidad o capacidad invitada + operación + clave + carga canónica devuelve el resultado guardado tras revalidar autenticación y ámbito; carga distinta con la misma clave: 409. Nunca omite autorización ni crea una asistencia nueva después del cierre. */
                "Idempotency-Key": components["parameters"]["IdempotencyKey"];
            };
            path: {
                sessionId: components["parameters"]["sessionId"];
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description Sesión activa. */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Session"];
                };
            };
            400: components["responses"]["BadRequest"];
            401: components["responses"]["Unauthorized"];
            403: components["responses"]["Forbidden"];
            404: components["responses"]["NotFound"];
            409: components["responses"]["Conflict"];
            410: components["responses"]["Gone"];
            429: components["responses"]["RateLimited"];
        };
    };
    closeSession: {
        parameters: {
            query?: never;
            header: {
                /** @description UUID por operación lógica. Misma identidad o capacidad invitada + operación + clave + carga canónica devuelve el resultado guardado tras revalidar autenticación y ámbito; carga distinta con la misma clave: 409. Nunca omite autorización ni crea una asistencia nueva después del cierre. */
                "Idempotency-Key": components["parameters"]["IdempotencyKey"];
            };
            path: {
                sessionId: components["parameters"]["sessionId"];
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description Sesión cerrada. */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Session"];
                };
            };
            400: components["responses"]["BadRequest"];
            401: components["responses"]["Unauthorized"];
            403: components["responses"]["Forbidden"];
            404: components["responses"]["NotFound"];
            409: components["responses"]["Conflict"];
            410: components["responses"]["Gone"];
            429: components["responses"]["RateLimited"];
        };
    };
    getSessionQr: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                sessionId: components["parameters"]["sessionId"];
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description QR actual. */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    /**
                     * @example {
                     *       "qr_token": "ejemplo-qr-opaco-no-es-un-secreto-real-0001",
                     *       "emitido_en": "2026-10-19T16:00:00Z",
                     *       "vence_en": "2026-10-19T16:00:40Z",
                     *       "rotacion_segundos": 30,
                     *       "vigencia_segundos": 40
                     *     }
                     */
                    "application/json": components["schemas"]["Qr"];
                };
            };
            400: components["responses"]["BadRequest"];
            401: components["responses"]["Unauthorized"];
            403: components["responses"]["Forbidden"];
            404: components["responses"]["NotFound"];
            409: components["responses"]["Conflict"];
            410: components["responses"]["Gone"];
            429: components["responses"]["RateLimited"];
        };
    };
    setSessionGeolocation: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                sessionId: components["parameters"]["sessionId"];
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["GeolocationSettings"];
            };
        };
        responses: {
            /** @description Configuración y modo efectivo. */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["GeolocationStatus"];
                };
            };
            400: components["responses"]["BadRequest"];
            401: components["responses"]["Unauthorized"];
            403: components["responses"]["Forbidden"];
            404: components["responses"]["NotFound"];
            409: components["responses"]["Conflict"];
            410: components["responses"]["Gone"];
            429: components["responses"]["RateLimited"];
        };
    };
    recordAuthenticatedAttendance: {
        parameters: {
            query?: never;
            header: {
                /** @description UUID por operación lógica. Misma identidad o capacidad invitada + operación + clave + carga canónica devuelve el resultado guardado tras revalidar autenticación y ámbito; carga distinta con la misma clave: 409. Nunca omite autorización ni crea una asistencia nueva después del cierre. */
                "Idempotency-Key": components["parameters"]["IdempotencyKey"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["AttendanceAttemptRequest"];
            };
        };
        responses: {
            /** @description Asistencia existente o repetición idempotente autorizada. */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    /**
                     * @example {
                     *       "asistencia_id": "f12f0768-dc90-4f61-9ce9-9859fcd20d13",
                     *       "attempt_id": "f12f0768-dc90-4f61-9ce9-9859fcd20d13",
                     *       "session_id": "f12f0768-dc90-4f61-9ce9-9859fcd20d13",
                     *       "resultado": "CONFIRMADA",
                     *       "registrada_en": "2026-10-19T16:00:30Z",
                     *       "mensaje": "Asistencia registrada."
                     *     }
                     */
                    "application/json": components["schemas"]["AttendanceReceipt"];
                };
            };
            /** @description Nueva asistencia confirmada. */
            201: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    /**
                     * @example {
                     *       "asistencia_id": "f12f0768-dc90-4f61-9ce9-9859fcd20d13",
                     *       "attempt_id": "f12f0768-dc90-4f61-9ce9-9859fcd20d13",
                     *       "session_id": "f12f0768-dc90-4f61-9ce9-9859fcd20d13",
                     *       "resultado": "CONFIRMADA",
                     *       "registrada_en": "2026-10-19T16:00:30Z",
                     *       "mensaje": "Asistencia registrada."
                     *     }
                     */
                    "application/json": components["schemas"]["AttendanceReceipt"];
                };
            };
            /** @description Repetir medición o esperar revisión. */
            202: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    /**
                     * @example {
                     *       "attempt_id": "f12f0768-dc90-4f61-9ce9-9859fcd20d13",
                     *       "resultado": "PENDIENTE_REVISION",
                     *       "code": "UBICACION_INCIERTA",
                     *       "mensaje": "El encargado revisará el intento."
                     *     }
                     */
                    "application/json": components["schemas"]["PendingReceipt"];
                };
            };
            400: components["responses"]["BadRequest"];
            401: components["responses"]["Unauthorized"];
            403: components["responses"]["Forbidden"];
            404: components["responses"]["NotFound"];
            409: components["responses"]["Conflict"];
            410: components["responses"]["Gone"];
            422: components["responses"]["BusinessRejected"];
            429: components["responses"]["RateLimited"];
        };
    };
    startGuestRegistration: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["GuestStartRequest"];
            };
        };
        responses: {
            /** @description Inicio temporal creado. */
            201: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    /**
                     * @example {
                     *       "registro_invitado_token": "ejemplo-formulario-opaco-no-real-00000001",
                     *       "inicio_en": "2026-10-19T16:00:20Z",
                     *       "vence_en": "2026-10-19T16:02:20Z"
                     *     }
                     */
                    "application/json": components["schemas"]["GuestStartResult"];
                };
            };
            400: components["responses"]["BadRequest"];
            403: components["responses"]["Forbidden"];
            404: components["responses"]["NotFound"];
            409: components["responses"]["Conflict"];
            422: components["responses"]["BusinessRejected"];
            429: components["responses"]["RateLimited"];
        };
    };
    submitGuestAttendance: {
        parameters: {
            query?: never;
            header: {
                /** @description UUID por operación lógica. Misma identidad o capacidad invitada + operación + clave + carga canónica devuelve el resultado guardado tras revalidar autenticación y ámbito; carga distinta con la misma clave: 409. Nunca omite autorización ni crea una asistencia nueva después del cierre. */
                "Idempotency-Key": components["parameters"]["IdempotencyKey"];
            };
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["GuestSubmitRequest"];
            };
        };
        responses: {
            /** @description Asistencia existente o replay autorizado por la misma capacidad y cédula. */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    /**
                     * @example {
                     *       "asistencia_id": "f12f0768-dc90-4f61-9ce9-9859fcd20d13",
                     *       "attempt_id": "f12f0768-dc90-4f61-9ce9-9859fcd20d13",
                     *       "session_id": "f12f0768-dc90-4f61-9ce9-9859fcd20d13",
                     *       "resultado": "CONFIRMADA",
                     *       "registrada_en": "2026-10-19T16:00:30Z",
                     *       "mensaje": "Asistencia registrada."
                     *     }
                     */
                    "application/json": components["schemas"]["AttendanceReceipt"];
                };
            };
            /** @description Nueva asistencia invitada confirmada. */
            201: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    /**
                     * @example {
                     *       "asistencia_id": "f12f0768-dc90-4f61-9ce9-9859fcd20d13",
                     *       "attempt_id": "f12f0768-dc90-4f61-9ce9-9859fcd20d13",
                     *       "session_id": "f12f0768-dc90-4f61-9ce9-9859fcd20d13",
                     *       "resultado": "CONFIRMADA",
                     *       "registrada_en": "2026-10-19T16:00:30Z",
                     *       "mensaje": "Asistencia registrada."
                     *     }
                     */
                    "application/json": components["schemas"]["AttendanceReceipt"];
                };
            };
            /** @description Repetición o revisión necesaria. */
            202: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    /**
                     * @example {
                     *       "attempt_id": "f12f0768-dc90-4f61-9ce9-9859fcd20d13",
                     *       "resultado": "REPETIR",
                     *       "code": "UBICACION_NO_DISPONIBLE",
                     *       "mensaje": "Vuelve a obtener tu ubicación."
                     *     }
                     */
                    "application/json": components["schemas"]["PendingReceipt"];
                };
            };
            400: components["responses"]["BadRequest"];
            401: components["responses"]["Unauthorized"];
            403: components["responses"]["Forbidden"];
            404: components["responses"]["NotFound"];
            409: components["responses"]["Conflict"];
            410: components["responses"]["Gone"];
            422: components["responses"]["BusinessRejected"];
            429: components["responses"]["RateLimited"];
        };
    };
    listAttendances: {
        parameters: {
            query?: {
                page?: components["parameters"]["Page"];
                size?: components["parameters"]["Size"];
                estado?: components["parameters"]["AttendanceState"];
            };
            header?: never;
            path: {
                sessionId: components["parameters"]["sessionId"];
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description Página de asistencias permitidas. */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["AttendancePage"];
                };
            };
            400: components["responses"]["BadRequest"];
            401: components["responses"]["Unauthorized"];
            403: components["responses"]["Forbidden"];
            404: components["responses"]["NotFound"];
            409: components["responses"]["Conflict"];
            410: components["responses"]["Gone"];
            429: components["responses"]["RateLimited"];
        };
    };
    exportAttendances: {
        parameters: {
            query?: {
                estado?: components["parameters"]["AttendanceState"];
            };
            header?: never;
            path: {
                sessionId: components["parameters"]["sessionId"];
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description CSV de asistencias del ámbito autorizado. Cache-Control: no-store. */
            200: {
                headers: {
                    /** @description attachment; filename="asistencias.csv" */
                    "Content-Disposition"?: string;
                    [name: string]: unknown;
                };
                content: {
                    /**
                     * @example asistencia_id,nombres,estado,registrada_en
                     *     00000000-0000-4000-8000-000000000001,Persona ficticia,CONFIRMADA,2026-10-19T16:00:30Z
                     */
                    "text/csv": string;
                };
            };
            400: components["responses"]["BadRequest"];
            401: components["responses"]["Unauthorized"];
            403: components["responses"]["Forbidden"];
            404: components["responses"]["NotFound"];
            409: components["responses"]["Conflict"];
            410: components["responses"]["Gone"];
            429: components["responses"]["RateLimited"];
        };
    };
    reviewAttempt: {
        parameters: {
            query?: never;
            header: {
                /** @description UUID por operación lógica. Misma identidad o capacidad invitada + operación + clave + carga canónica devuelve el resultado guardado tras revalidar autenticación y ámbito; carga distinta con la misma clave: 409. Nunca omite autorización ni crea una asistencia nueva después del cierre. */
                "Idempotency-Key": components["parameters"]["IdempotencyKey"];
            };
            path: {
                attemptId: components["parameters"]["attemptId"];
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["ReviewRequest"];
            };
        };
        responses: {
            /** @description Revisión aplicada o repetida de forma idempotente. */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ReviewResult"];
                };
            };
            400: components["responses"]["BadRequest"];
            401: components["responses"]["Unauthorized"];
            403: components["responses"]["Forbidden"];
            404: components["responses"]["NotFound"];
            409: components["responses"]["Conflict"];
            410: components["responses"]["Gone"];
            422: components["responses"]["BusinessRejected"];
            429: components["responses"]["RateLimited"];
        };
    };
    listActivityAudit: {
        parameters: {
            query?: {
                page?: components["parameters"]["Page"];
                size?: components["parameters"]["Size"];
            };
            header?: never;
            path: {
                activityId: components["parameters"]["activityId"];
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description Auditoría del ámbito permitido. */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["AuditPage"];
                };
            };
            400: components["responses"]["BadRequest"];
            401: components["responses"]["Unauthorized"];
            403: components["responses"]["Forbidden"];
            404: components["responses"]["NotFound"];
            409: components["responses"]["Conflict"];
            410: components["responses"]["Gone"];
            429: components["responses"]["RateLimited"];
        };
    };
}
