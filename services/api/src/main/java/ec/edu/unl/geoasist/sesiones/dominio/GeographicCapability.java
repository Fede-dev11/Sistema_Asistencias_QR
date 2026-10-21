package ec.edu.unl.geoasist.sesiones.dominio;

import java.util.UUID;

public interface GeographicCapability {
  boolean available();

  boolean configured(UUID session);
}
