package com.prism.api.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;

import org.junit.jupiter.api.Test;

class TemporaryPasswordsTests {

    @Test
    void hasTheReadableFormat() {
        for (int i = 0; i < 1_000; i++) {
            var password = TemporaryPasswords.generate();
            assertTrue(password.matches("[A-Za-z0-9]{4}-[A-Za-z0-9]{4}-[A-Za-z0-9]{4}"), password);
            password.replace("-", "").chars().forEach(c ->
                    assertTrue(TemporaryPasswords.ALPHABET.indexOf(c) >= 0, "unexpected character in " + password));
        }
    }

    @Test
    void containsUpperLowerAndDigit() {
        for (int i = 0; i < 1_000; i++) {
            var password = TemporaryPasswords.generate();
            assertTrue(password.chars().anyMatch(Character::isUpperCase), password);
            assertTrue(password.chars().anyMatch(Character::isLowerCase), password);
            assertTrue(password.chars().anyMatch(Character::isDigit), password);
        }
    }

    @Test
    void neverUsesLookAlikeCharacters() {
        for (int i = 0; i < 1_000; i++) {
            var password = TemporaryPasswords.generate();
            assertTrue(password.chars().noneMatch(c -> "0O1lI".indexOf(c) >= 0), password);
        }
    }

    @Test
    void isRandom() {
        var seen = new HashSet<String>();
        for (int i = 0; i < 10_000; i++) {
            seen.add(TemporaryPasswords.generate());
        }
        assertEquals(10_000, seen.size());
    }
}
