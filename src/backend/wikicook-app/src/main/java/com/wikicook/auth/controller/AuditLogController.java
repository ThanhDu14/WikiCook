package com.wikicook.auth.controller;

import com.wikicook.auth.dto.AuditLogDto;
import com.wikicook.auth.service.AuditService;
import com.wikicook.auth.service.AuditService.Filter;
import com.wikicook.common.security.AdministratorOnly;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only view of the audit trail for Administrators (FR-025, NFR-008). It has no other method on purpose: the trail
 * cannot be edited or deleted through the API, and the table refuses it too.
 */
@RestController
@RequestMapping("/api/v1/admin/audit-logs")
public class AuditLogController {

    private final AuditService audit;

    public AuditLogController(AuditService audit) {
        this.audit = audit;
    }

    @GetMapping
    @AdministratorOnly
    public AuditLogDto.Page list(
            @RequestParam(required = false) @Size(max = 50) String action,
            @RequestParam(required = false) Long targetId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return audit.search(new Filter(action, targetId, from, to), page, size);
    }
}
