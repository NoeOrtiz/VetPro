package veterinaria.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordSecurityUtil {

    private static final String PREFIX_PBKDF2 = "PBKDF2$";
    private static final String PREFIX_SHA256 = "SHA256$";
    private static final int DEFAULT_ITERATIONS = 65536;
    private static final int SALT_LENGTH_BYTES = 16;
    private static final int KEY_LENGTH_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordSecurityUtil() {
    }

    public static String normalizeForPersist(String plainOrStored) {
        if (plainOrStored == null || plainOrStored.trim().isEmpty()) {
            return plainOrStored;
        }
        if (isStrongHash(plainOrStored)) {
            return plainOrStored;
        }
        return hashPassword(plainOrStored);
    }

    public static boolean matches(String rawPassword, String storedPassword) {
        if (storedPassword == null || rawPassword == null) {
            return false;
        }

        if (isPbkdf2Hash(storedPassword)) {
            return verifyPbkdf2(rawPassword, storedPassword);
        }

        if (isLegacySha256Hash(storedPassword)) {
            return storedPassword.equals(hashLegacySha256(rawPassword));
        }

        return storedPassword.equals(rawPassword);
    }

    public static boolean needsMigration(String storedPassword) {
        return storedPassword != null && !isPbkdf2Hash(storedPassword);
    }

    public static boolean isHashed(String value) {
        return isPbkdf2Hash(value) || isLegacySha256Hash(value);
    }

    public static String hashPassword(String rawPassword) {
        if (rawPassword == null) {
            return null;
        }
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        RANDOM.nextBytes(salt);
        byte[] hash = pbkdf2(rawPassword.toCharArray(), salt, DEFAULT_ITERATIONS, KEY_LENGTH_BITS);
        return PREFIX_PBKDF2
                + DEFAULT_ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    public static String generateRecoveryCode(int digits) {
        if (digits <= 0) {
            throw new IllegalArgumentException("digits debe ser mayor a 0");
        }
        int bound = (int) Math.pow(10, digits);
        int min = (int) Math.pow(10, digits - 1);
        int value = RANDOM.nextInt(bound - min) + min;
        return String.valueOf(value);
    }

    private static boolean isStrongHash(String value) {
        return isPbkdf2Hash(value);
    }

    private static boolean isPbkdf2Hash(String value) {
        return value != null && value.startsWith(PREFIX_PBKDF2);
    }

    private static boolean isLegacySha256Hash(String value) {
        return value != null && value.startsWith(PREFIX_SHA256);
    }

    private static boolean verifyPbkdf2(String rawPassword, String storedPassword) {
        try {
            String[] parts = storedPassword.split("\\$");
            if (parts.length != 4) {
                return false;
            }
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[3]);
            byte[] calculatedHash = pbkdf2(rawPassword.toCharArray(), salt, iterations, expectedHash.length * 8);
            return MessageDigest.isEqual(expectedHash, calculatedHash);
        } catch (Exception ex) {
            return false;
        }
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLengthBits) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLengthBits);
            SecretKeyFactory skf;
            try {
                skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            } catch (NoSuchAlgorithmException ex) {
                skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            }
            return skf.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            throw new IllegalStateException("No se pudo aplicar PBKDF2", ex);
        }
    }

    private static String hashLegacySha256(String rawPassword) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            return PREFIX_SHA256 + toHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("No se pudo aplicar SHA-256", ex);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
