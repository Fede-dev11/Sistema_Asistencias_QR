package ec.edu.unl.geoasist.actividades.dominio;

import java.time.Instant;
import java.util.UUID;

public record Activity(
    UUID id,
    String name,
    String mode,
    UUID responsible,
    String state,
    Instant closedAt,
    Instant anonymizeAt) {}
