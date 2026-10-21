package ec.edu.unl.geoasist.identidades.persistencia;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "personas")
public class PersonaEntity {
  @Id private UUID id;

  @Column(name = "cedula_normalizada", nullable = false, unique = true, length = 10)
  private String cedula;

  @Column(nullable = false, length = 200)
  private String nombres;

  @Column(nullable = false, length = 20)
  private String tipo;

  protected PersonaEntity() {}
}
