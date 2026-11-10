package ec.edu.unl.geoasist.identidades.aplicacion;

public interface IdentityProvider {
  VerifiedIdentity verifyTicket(String ticket, String service);

  boolean isActive(String correo);
}
