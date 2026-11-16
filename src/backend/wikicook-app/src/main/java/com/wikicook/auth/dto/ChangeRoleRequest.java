package com.wikicook.auth.dto;

import com.wikicook.common.security.Role;
import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequest(@NotNull Role role) {
}
