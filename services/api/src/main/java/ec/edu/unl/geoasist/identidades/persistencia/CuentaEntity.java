package ec.edu.unl.geoasist.identidades.persistencia;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cuentas")
public class CuentaEntity {
  @Id private UUID id;

  @Column(name = "persona_id", nullable = false, unique = true)
  private UUID personaId;

  @Column(nullable = false, length = 50)
  private String proveedor;

  @Column(nullable = false, length = 254, unique = true)
  private String identificador;

  @Column(name = "password_hash", columnDefinition = "text")
  private String passwordHash;

  @Column(nullable = false)
  private boolean activa;

  @Column(name = "sincronizada_en")
  private Instant sincronizadaEn;

  protected CuentaEntity() {}
}
