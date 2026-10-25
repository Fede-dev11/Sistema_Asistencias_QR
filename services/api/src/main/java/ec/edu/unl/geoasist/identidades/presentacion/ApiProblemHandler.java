package ec.edu.unl.geoasist.identidades.presentacion;

import ec.edu.unl.geoasist.compartido.aplicacion.ApiFailure;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.*;

@RestControllerAdvice
public class ApiProblemHandler {
  public static Map<String, Object> body(ApiFailure e) {
    return ec.edu.unl.geoasist.compartido.aplicacion.ProblemDetails.of(e);
  }

  @ExceptionHandler(ApiFailure.class)
  public ResponseEntity<Map<String, Object>> failure(ApiFailure e) {
    return ResponseEntity.status(e.status())
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .cacheControl(CacheControl.noStore())
        .headers(
            h -> {
              if (e.status() == 429) h.set("Retry-After", "60");
            })
        .body(body(e));
  }

  @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
  public ResponseEntity<Map<String, Object>> tooLarge(Exception e) {
    return failure(new ApiFailure(413, "ARCHIVO_DEMASIADO_GRANDE", "El archivo supera 2 MB."));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> fields(MethodArgumentNotValidException e) {
    String detail =
        e.getBindingResult().getFieldErrors().stream()
            .map(x -> x.getField() + ": " + x.getDefaultMessage())
            .collect(Collectors.joining("; "));
    return failure(
        new ApiFailure(400, "SOLICITUD_INVALIDA", "Revisa los campos indicados.", detail));
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class,
    IllegalArgumentException.class,
    ServletRequestBindingException.class,
    HandlerMethodValidationException.class
  })
  public ResponseEntity<Map<String, Object>> invalid(Exception e) {
    return failure(ApiFailure.invalid());
  }
}
