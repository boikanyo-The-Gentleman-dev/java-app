import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/** Salted PBKDF2 password hashing using only the JDK (no extra libraries). */
public final class PasswordUtil {
    private PasswordUtil() {}

    private static final int ITERATIONS = 120_000;
    private static final int KEY_BITS = 256;
    private static final String PREFIX = "pbkdf2$";

    public static String hash(String password) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        byte[] h = derive(password.toCharArray(), salt, ITERATIONS);
        return PREFIX + ITERATIONS + "$" + Base64.getEncoder().encodeToString(salt)
                + "$" + Base64.getEncoder().encodeToString(h);
    }

    public static boolean isHashed(String stored) {
        return stored != null && stored.startsWith(PREFIX);
    }

    /** Verifies a password. Old plain-text passwords still work (and get upgraded at login). */
    public static boolean verify(String password, String stored) {
        if (stored == null) return false;
        if (!isHashed(stored)) {
            return MessageDigest.isEqual(stored.getBytes(StandardCharsets.UTF_8),
                    password.getBytes(StandardCharsets.UTF_8));
        }
        try {
            String[] p = stored.split("\\$");
            if (p.length != 4) return false;
            int iterations = Integer.parseInt(p[1]);
            byte[] salt = Base64.getDecoder().decode(p[2]);
            byte[] expected = Base64.getDecoder().decode(p[3]);
            return MessageDigest.isEqual(expected, derive(password.toCharArray(), salt, iterations));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] derive(char[] password, byte[] salt, int iterations) {
        try {
            SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return f.generateSecret(new PBEKeySpec(password, salt, iterations, KEY_BITS)).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}