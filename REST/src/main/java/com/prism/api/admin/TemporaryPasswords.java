package com.prism.api.admin;

import java.security.SecureRandom;

/**
 * Generates random temporary passwords for new users and password resets.
 * <p>
 * Format {@code xxxx-xxxx-xxxx}: 12 characters from an alphabet without look-alikes (no 0/O, 1/l/I),
 * so an administrator can read one out to a user. That is about 68 bits of randomness; each password
 * contains at least one upper-case letter, one lower-case letter and one digit.
 */
public final class TemporaryPasswords {

    private static final String UPPER = "ABCDEFGHJKMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghjkmnpqrstuvwxyz";
    private static final String DIGITS = "23456789";
    static final String ALPHABET = UPPER + LOWER + DIGITS;

    static final int GROUPS = 3;
    static final int GROUP_LENGTH = 4;

    private static final SecureRandom RANDOM = new SecureRandom();

    private TemporaryPasswords() {
    }

    public static String generate() {
        while (true) {
            var password = new StringBuilder(GROUPS * (GROUP_LENGTH + 1));
            for (int group = 0; group < GROUPS; group++) {
                if (group > 0) {
                    password.append('-');
                }
                for (int i = 0; i < GROUP_LENGTH; i++) {
                    password.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
                }
            }
            var candidate = password.toString();
            if (containsAny(candidate, UPPER) && containsAny(candidate, LOWER) && containsAny(candidate, DIGITS)) {
                return candidate;
            }
        }
    }

    private static boolean containsAny(String text, String characters) {
        return text.chars().anyMatch(c -> characters.indexOf(c) >= 0);
    }
}
