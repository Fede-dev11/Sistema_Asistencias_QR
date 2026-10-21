package ec.edu.unl.geoasist.compartido.aplicacion;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OpaqueSecrets {
  private final SecureRandom random = new SecureRandom();
  private final byte[] key;

  public OpaqueSecrets(@Value("${geoasist.hmac-key}") String key) {
    if (key.length() < 32)
      throw new IllegalArgumentException("GEOASIST_HMAC_KEY requiere al menos 32 caracteres.");
    this.key = key.getBytes(StandardCharsets.UTF_8);
  }

  public String issue() {
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  public static String hash(String secret) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  public String hmac(String input) {
    try {
      Mac m = Mac.getInstance("HmacSHA256");
      m.init(new SecretKeySpec(key, "HmacSHA256"));
      return HexFormat.of().formatHex(m.doFinal(input.getBytes(StandardCharsets.UTF_8)));
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException(e);
    }
  }
}
