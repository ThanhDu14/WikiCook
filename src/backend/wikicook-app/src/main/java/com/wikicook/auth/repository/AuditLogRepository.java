package com.wikicook.auth.repository;

import com.wikicook.auth.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

/** Append-only: the table rejects UPDATE and DELETE, and nothing here deletes. */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
