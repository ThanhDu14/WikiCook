package com.wikicook.auth.controller;

import com.wikicook.auth.api.AccountAdminFacade;
import com.wikicook.auth.dto.AccountAdminDto;
import com.wikicook.auth.dto.BanRequest;
import com.wikicook.auth.dto.ChangeRoleRequest;
import com.wikicook.auth.dto.ReinstateRequest;
import com.wikicook.auth.dto.SuspendRequest;
import com.wikicook.common.security.AdministratorOnly;
import com.wikicook.common.security.CurrentAccount;
import com.wikicook.common.security.ModeratorOnly;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Account administration (contracts/openapi.yaml, tag {@code admin}). The role annotation is the first gate; the
 * hierarchy, self-action, re-authentication and last-Administrator rules live in {@link AccountAdminFacade}. Spec 100's
 * screens call the same facade.
 */
@RestController
@RequestMapping("/api/v1/admin/users")
public class AccountAdminController {

    private final AccountAdminFacade admin;
    private final CurrentAccount current;

    public AccountAdminController(AccountAdminFacade admin, CurrentAccount current) {
        this.admin = admin;
        this.current = current;
    }

    @GetMapping("/{id}")
    @AdministratorOnly
    public AccountAdminDto view(@PathVariable long id) {
        return AccountAdminDto.from(admin.view(current.id(), id));
    }

    @PatchMapping("/{id}/role")
    @AdministratorOnly
    public AccountAdminDto changeRole(@PathVariable long id, @Valid @RequestBody ChangeRoleRequest request) {
        return AccountAdminDto.from(admin.changeRole(current.id(), id, request.role()));
    }

    @PostMapping("/{id}/suspension")
    @ModeratorOnly
    public AccountAdminDto suspend(@PathVariable long id, @Valid @RequestBody SuspendRequest request) {
        return AccountAdminDto.from(admin.suspend(current.id(), id, request.reason(), request.endsAt()));
    }

    @PostMapping("/{id}/ban")
    @AdministratorOnly
    public AccountAdminDto ban(@PathVariable long id, @Valid @RequestBody BanRequest request) {
        return AccountAdminDto.from(admin.ban(current.id(), id, request.reason()));
    }

    @PostMapping("/{id}/reinstatement")
    @AdministratorOnly
    public AccountAdminDto reinstate(@PathVariable long id,
            @Valid @RequestBody(required = false) ReinstateRequest request) {
        return AccountAdminDto.from(admin.reinstate(current.id(), id, request == null ? null : request.reason()));
    }
}
