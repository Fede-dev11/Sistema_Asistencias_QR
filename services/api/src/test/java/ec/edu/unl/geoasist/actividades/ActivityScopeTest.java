package ec.edu.unl.geoasist.actividades;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import ec.edu.unl.geoasist.actividades.aplicacion.ActivityAuthorization;
import ec.edu.unl.geoasist.actividades.dominio.Activity;
import ec.edu.unl.geoasist.actividades.persistencia.ActivityRepository;
import ec.edu.unl.geoasist.compartido.aplicacion.ApiFailure;
import ec.edu.unl.geoasist.compartido.aplicacion.NominalAccessGuard;
import ec.edu.unl.geoasist.identidades.dominio.UserSession;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class ActivityScopeTest {
  final UUID owner = UUID.randomUUID(), other = UUID.randomUUID(), activityId = UUID.randomUUID();
  final Activity activity =
      new Activity(activityId, "Ficticia", "CLASE", owner, "ABIERTA", null, null);
  final ActivityRepository repo = mock(ActivityRepository.class);
  final ActivityAuthorization policy =
      new ActivityAuthorization(repo, new NominalAccessGuard(Clock.systemUTC()));

  UserSession user(UUID account, String... roles) {
    return new UserSession(UUID.randomUUID(), account, UUID.randomUUID(), Set.of(roles));
  }

  @Test
  void effectiveScopeMatrix() {
    when(repo.find(activityId)).thenReturn(activity);
    assertTrue(policy.canRead(user(owner, "DOCENTE"), activity));
    assertTrue(policy.canRead(user(owner, "ENCARGADO"), activity));
    assertFalse(policy.canRead(user(other, "DOCENTE"), activity));
    assertFalse(policy.canRead(user(other, "ENCARGADO"), activity));
    assertTrue(policy.canRead(user(other, "DIRECTOR_INSTITUCIONAL"), activity));
    assertFalse(policy.canRead(user(other, "DIRECTOR_CARRERA"), activity));
    when(repo.directs(other, activityId)).thenReturn(true);
    assertTrue(policy.canRead(user(other, "DIRECTOR_CARRERA"), activity));
    assertThrows(
        ApiFailure.class, () -> policy.write(user(other, "DIRECTOR_INSTITUCIONAL"), activityId));
    assertThrows(ApiFailure.class, () -> policy.write(user(other, "DIRECTOR_CARRERA"), activityId));
    assertThrows(ApiFailure.class, () -> policy.write(user(owner, "DIRECTOR_CARRERA"), activityId));
    assertEquals(
        activity, policy.write(user(owner, "DOCENTE", "DIRECTOR_INSTITUCIONAL"), activityId));
    assertFalse(policy.canRead(user(owner, "ESTUDIANTE"), activity));
  }
}
