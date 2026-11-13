package com.wikicook.auth.service;

import com.wikicook.auth.api.AuditWriter;
import com.wikicook.auth.domain.AuditLog;
import com.wikicook.auth.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Clock;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Writes the audit trail (FR-025, NFR-008). Joins the caller's transaction, so an action and its audit row commit or
 * roll back together. The {@code details} keys {@code email} and {@code displayName} are refused at any depth: the
 * log must survive the deletion of the account it talks about (NFR-009), so it may only hold ids and hashes.
 */
@Service
public class AuditService implements AuditWriter {

    private static final Set<String> FORBIDDEN_KEYS = Set.of("email", "displayname");

    private final AuditLogRepository repository;
    private final Clock clock;

    public AuditService(AuditLogRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void write(String action, Long actorId, String targetType, Long targetId, Map<String, Object> details) {
        rejectPersonalData(details);
        repository.save(new AuditLog(actorId, action, targetType, targetId, details, currentIp(), clock.instant()));
    }

    private static void rejectPersonalData(Map<?, ?> details) {
        if (details == null) {
            return;
        }
        details.forEach((key, value) -> {
            if (key != null && FORBIDDEN_KEYS.contains(key.toString().toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("audit details must not contain " + key);
            }
            if (value instanceof Map<?, ?> nested) {
                rejectPersonalData(nested);
            }
        });
    }

    private static InetAddress currentIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            try {
                return InetAddress.getByName(request.getRemoteAddr());
            } catch (UnknownHostException e) {
                return null;
            }
        }
        return null;
    }
}
