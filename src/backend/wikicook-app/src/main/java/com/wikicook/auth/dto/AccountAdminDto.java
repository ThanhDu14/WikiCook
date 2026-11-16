package com.wikicook.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.wikicook.auth.api.AccountAdminView;
import com.wikicook.auth.api.AccountAdminView.ActiveSanction;
import com.wikicook.common.security.AccountStatus;
import com.wikicook.common.security.Role;

/** REST shape of {@link AccountAdminView}; {@code email} is left out of the JSON when it is null (NFR-009). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AccountAdminDto(long id, String displayName, String email, Role role, AccountStatus status,
        boolean emailVerified, ActiveSanction activeSanction) {

    public static AccountAdminDto from(AccountAdminView view) {
        return new AccountAdminDto(view.id(), view.displayName(), view.email(), view.role(), view.status(),
                view.emailVerified(), view.activeSanction());
    }
}
