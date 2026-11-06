package com.lumora.sms.enums;
/**
 * Roles are deliberately separate even when one role extends another. This lets
 * Spring Security assign least-privilege dashboard and module access.
 */
public enum UserRoleType { SUPER_ADMIN, ADMINISTRATOR, PRINCIPAL, DEPUTY_PRINCIPAL, REGISTRAR, BURSAR, TEACHER, CLASS_TEACHER, STUDENT, PARENT, LIBRARIAN }
