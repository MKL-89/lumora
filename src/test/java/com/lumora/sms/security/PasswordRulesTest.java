package com.lumora.sms.security;

import com.lumora.sms.exception.BusinessRuleViolationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordRulesTest {

    @Test
    void acceptsAReasonablePassword() {
        assertDoesNotThrow(() -> PasswordRules.check("maziwa2mala", "jkamau"));
    }

    @Test
    void refusesShortLettersOnlyCommonAndUsernamePasswords() {
        assertThrows(BusinessRuleViolationException.class, () -> PasswordRules.check("ab12", "x"));
        assertThrows(BusinessRuleViolationException.class, () -> PasswordRules.check("onlyletters", "x"));
        assertThrows(BusinessRuleViolationException.class, () -> PasswordRules.check("12345678", "x"));
        assertThrows(BusinessRuleViolationException.class, () -> PasswordRules.check("Lumora@123", "x"));
        assertThrows(BusinessRuleViolationException.class, () -> PasswordRules.check("jkamau2026", "jkamau"));
    }
}
