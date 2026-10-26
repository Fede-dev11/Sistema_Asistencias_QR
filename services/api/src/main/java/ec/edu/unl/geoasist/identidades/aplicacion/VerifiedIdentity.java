package ec.edu.unl.geoasist.identidades.aplicacion;

import java.util.List;

public record VerifiedIdentity(String correo, String cedula, String nombres, List<String> roles, List<String> carreras, boolean activo) {}
