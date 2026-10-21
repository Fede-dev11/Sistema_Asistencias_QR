package ec.edu.unl.geoasist.actividades.persistencia;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "actividades")
public class ActividadEntity {
  @Id private UUID id;

  @Column(nullable = false, length = 200)
  private String nombre;

  @Column(nullable = false, length = 10)
  private String modalidad;

  @Column(name = "responsable_id", nullable = false)
  private UUID responsableId;

  @Column(nullable = false, length = 10)
  private String estado;

  @Column(name = "cerrada_en")
  private Instant cerradaEn;

  @Column(name = "anonimizar_en")
  private Instant anonimizarEn;

  protected ActividadEntity() {}
}
