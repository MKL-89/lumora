package com.lumora.sms.dto;

import com.lumora.sms.enums.UserRoleType;

/** A role together with how many accounts currently hold it. */
public record RoleUsage(UserRoleType role, String description, long accounts) {

    /** The authority string Spring Security checks for this role. */
    public String authority() {
        return "ROLE_" + role.name();
    }

    public String label() {
        return role.name().charAt(0) + role.name().substring(1).toLowerCase().replace('_', ' ');
    }
}
