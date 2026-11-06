package com.lumora.sms.security;

import com.lumora.sms.exception.BusinessRuleViolationException;
import java.util.Locale;
import java.util.Set;

/**
 * What a password chosen by a person must look like: short enough for a teacher
 * to remember and type on a phone, but not one that is easily guessed.
 */
public final class PasswordRules {

    /** Passwords anyone could guess, including the demo password printed in the README. */
    private static final Set<String> TOO_COMMON = Set.of(
            "lumora@123", "password", "password1", "password123", "12345678", "123456789",
            "qwerty123", "admin123", "admin@123", "school123", "welcome1", "kenya123");

    private PasswordRules() {
    }

    /** Throws with a message a person can act on when the password is not acceptable. */
    public static void check(String password, String username) {
        if (password == null || password.length() < 8) {
            throw new BusinessRuleViolationException("The password must be at least 8 characters long.");
        }
        if (password.length() > 100) {
            throw new BusinessRuleViolationException("The password must be at most 100 characters long.");
        }
        if (password.chars().noneMatch(Character::isLetter) || password.chars().noneMatch(Character::isDigit)) {
            throw new BusinessRuleViolationException("The password must contain both letters and numbers.");
        }
        String lower = password.toLowerCase(Locale.ROOT);
        if (TOO_COMMON.contains(lower)) {
            throw new BusinessRuleViolationException("That password is too easy to guess. Please choose another.");
        }
        if (username != null && username.length() >= 3 && lower.contains(username.toLowerCase(Locale.ROOT))) {
            throw new BusinessRuleViolationException("The password must not contain the username.");
        }
    }
}
