package com.wikicook.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wikicook.auth.api.AuditWriter;
import com.wikicook.auth.domain.AuditLog;
import com.wikicook.auth.dto.AuditLogDto;
import com.wikicook.auth.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Writes and reads the audit trail (FR-025, NFR-008). Writing joins the caller's transaction, so an action and its
 * audit row commit or roll back together. The {@code details} keys {@code email} and {@code displayName} are refused at
 * any depth: the log must survive the deletion of the account it talks about (NFR-009), so it may only hold ids and
 * hashes. Reading is for Administrators: newest first, filtered, paged; nothing here updates or deletes.
 */
@Service
public class AuditService implements AuditWriter {

    private static final Set<String> FORBIDDEN_KEYS = Set.of("email", "displayname");
    private static final TypeReference<Map<String, Object>> DETAILS = new TypeReference<>() {
    };

    private final AuditLogRepository repository;
    private final Clock clock;
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public AuditService(AuditLogRepository repository, Clock clock, JdbcTemplate jdbc, ObjectMapper json) {
        this.repository = repository;
        this.clock = clock;
        this.jdbc = jdbc;
        this.json = json;
    }

    @Override
    @Transactional
    public void write(String action, Long actorId, String targetType, Long targetId, Map<String, Object> details) {
        rejectPersonalData(details);
        repository.save(new AuditLog(actorId, action, targetType, targetId, details, currentIp(), clock.instant()));
    }

    /** Filter of {@link #search}; every field is optional. */
    public record Filter(String action, Long targetId, Instant from, Instant to) {
    }

    /** One page of entries, newest first; reads one row more than asked to know whether another page follows. */
    @Transactional(readOnly = true)
    public AuditLogDto.Page search(Filter filter, int page, int size) {
        StringBuilder sql = new StringBuilder("SELECT id, actor_id, action, target_type, target_id, "
                + "details::text AS details, host(ip_address) AS ip, created_at FROM audit_logs WHERE true");
        List<Object> args = new ArrayList<>();
        if (filter.action() != null && !filter.action().isBlank()) {
            sql.append(" AND action = ?");
            args.add(filter.action());
        }
        if (filter.targetId() != null) {
            sql.append(" AND target_id = ?");
            args.add(filter.targetId());
        }
        if (filter.from() != null) {
            sql.append(" AND created_at >= ?");
            args.add(Timestamp.from(filter.from()));
        }
        if (filter.to() != null) {
            sql.append(" AND created_at <= ?");
            args.add(Timestamp.from(filter.to()));
        }
        sql.append(" ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?");
        args.add(size + 1);
        args.add((long) page * size);

        List<AuditLogDto> rows = jdbc.query(sql.toString(), (rs, i) -> new AuditLogDto(rs.getLong("id"),
                (Long) rs.getObject("actor_id"), rs.getString("action"), rs.getString("target_type"),
                (Long) rs.getObject("target_id"), parse(rs.getString("details")), rs.getString("ip"),
                rs.getTimestamp("created_at").toInstant()), args.toArray());
        boolean hasNext = rows.size() > size;
        return new AuditLogDto.Page(hasNext ? rows.subList(0, size) : rows, page, size, hasNext);
    }

    private Map<String, Object> parse(String details) {
        if (details == null) {
            return null;
        }
        try {
            return json.readValue(details, DETAILS);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("unreadable audit details", e);
        }
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
