package com.wikicook.common.security;

/**
 * The four roles of spec 010 FR-020, lowest first. A higher role includes every right of the lower ones (the
 * {@code RoleHierarchy} in {@code SecurityConfig}); the Contributor badge is data, not a right.
 */
public enum Role {
    MEMBER,
    CONTRIBUTOR,
    MODERATOR,
    ADMINISTRATOR;

    /** Spring authority name, {@code ROLE_} + name. */
    public String authority() {
        return "ROLE_" + name();
    }

    /** True when this role is strictly above {@code other}. */
    public boolean outranks(Role other) {
        return ordinal() > other.ordinal();
    }
}
