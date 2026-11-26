package ec.edu.unl.geoasist.compartido.aplicacion;

public final class RecommendedAction {
  private RecommendedAction() {}

  public static String forCode(String code) {
    return switch (code) {
      case "FUERA_DE_HORARIO" ->
          "Realiza la operación dentro del horario permitido; si ya terminó, contacta al responsable.";
      case "UBICACION_REQUERIDA" ->
          "Permite el acceso a la ubicación, activa el GPS y vuelve a medir.";
      case "UBICACION_INVALIDA", "UBICACION_ANTIGUA" ->
          "Obtén una nueva medición de ubicación y vuelve a enviar.";
      case "PRECISION_INSUFICIENTE", "UBICACION_INCIERTA" ->
          "Acércate a un lugar con mejor señal GPS y vuelve a medir.";
      case "REVISION_UBICACION" -> "Espera la revisión del responsable de la sesión.";
      case "FUERA_DEL_ESPACIO" -> "Acércate al espacio de la sesión y vuelve a medir.";
      case "CONFIGURACION_GPS_REQUERIDA", "CAPACIDAD_GPS_NO_DISPONIBLE" ->
          "Revisa la configuración geográfica de la sesión antes de iniciarla.";
      case "TOKEN_INVALIDO", "TOKEN_VENCIDO", "FORMULARIO_EXPIRADO" ->
          "Escanea el QR actual de la sesión.";
      case "SESION_INACTIVA", "ESTADO_INCOMPATIBLE" ->
          "Actualiza la sesión y consulta su estado con el responsable.";
      default -> "Revisa los datos e inténtalo de nuevo; si persiste, contacta al responsable.";
    };
  }
}
