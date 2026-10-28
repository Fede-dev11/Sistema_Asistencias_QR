package ec.edu.unl.sso.compartido;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/** Secretos y tickets: aleatorios de 32 bytes; en BD solo se guarda su SHA-256 hexadecimal. */
public final class Secrets {
  private static final SecureRandom RANDOM = new SecureRandom();

  private Secrets() {}

  public static String random() {
    byte[] b = new byte[32];
    RANDOM.nextBytes(b);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
  }

  public static String sha256Hex(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  private static final String TEMP_ALPHABET =
      "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";

  /** Contraseña temporal de 16 caracteres (sin símbolos ambiguos). */
  public static String temporaryPassword() {
    StringBuilder sb = new StringBuilder(16);
    for (int i = 0; i < 16; i++)
      sb.append(TEMP_ALPHABET.charAt(RANDOM.nextInt(TEMP_ALPHABET.length())));
    return sb.toString();
  }
}
