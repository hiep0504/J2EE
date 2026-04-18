package com.example.Backend_J2EE.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {

    @Test
    void hashProducesEncodedValueThatMatchesOriginalPassword() {
        String encoded = PasswordHasher.hash("secret-password");

        assertTrue(encoded.startsWith("pbkdf2$"));
        assertTrue(PasswordHasher.matches("secret-password", encoded));
        assertFalse(PasswordHasher.matches("different-password", encoded));
    }

    @Test
    void hashProducesDifferentEncodingsForSamePasswordDueToRandomSalt() {
        String first = PasswordHasher.hash("secret-password");
        String second = PasswordHasher.hash("secret-password");

        assertNotEquals(first, second);
        assertTrue(PasswordHasher.matches("secret-password", first));
        assertTrue(PasswordHasher.matches("secret-password", second));
    }

    @Test
    void matchesSupportsLegacyPlainTextPasswords() {
        assertTrue(PasswordHasher.matches("plain-text", "plain-text"));
        assertFalse(PasswordHasher.matches("plain-text", "other-text"));
    }

    @Test
    void matchesReturnsFalseForNullInputs() {
        assertFalse(PasswordHasher.matches(null, "plain-text"));
        assertFalse(PasswordHasher.matches("plain-text", null));
        assertFalse(PasswordHasher.matches(null, null));
    }

    @Test
    void matchesReturnsFalseForInvalidPbkdf2Format() {
        assertFalse(PasswordHasher.matches("secret", "pbkdf2$120000$only-three-parts"));
        assertFalse(PasswordHasher.matches("secret", "pbkdf2$120000$abc$def$extra"));
    }

    @Test
    void matchesReturnsFalseForInvalidIterationValue() {
        assertFalse(PasswordHasher.matches("secret", "pbkdf2$not-a-number$c2FsdA==$aGFzaA=="));
    }

    @Test
    void matchesReturnsFalseForInvalidBase64Payload() {
        assertFalse(PasswordHasher.matches("secret", "pbkdf2$120000$%%%$%%%"));
    }

    @Test
    void matchesReturnsFalseForWrongPasswordAgainstEncodedHash() {
        String encoded = PasswordHasher.hash("secret-password");

        assertFalse(PasswordHasher.matches("wrong-password", encoded));
    }

    @Test
    void hashRejectsNullInput() {
        assertThrows(IllegalArgumentException.class, () -> PasswordHasher.hash(null));
    }
}