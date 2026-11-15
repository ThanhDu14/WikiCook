package com.wikicook.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.wikicook.auth.domain.AuditLog;
import com.wikicook.auth.repository.AuditLogRepository;
import java.net.InetAddress;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** FR-025, NFR-009: one audit row per action, never with an email or a display name. */
class AuditServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-13T05:00:00Z");

    private final AuditLogRepository repository = mock(AuditLogRepository.class);
    private final AuditService service = new AuditService(repository, Clock.fixed(NOW, ZoneOffset.UTC),
            mock(org.springframework.jdbc.core.JdbcTemplate.class), new com.fasterxml.jackson.databind.ObjectMapper());

    @AfterEach
    void clear() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void writesTheActionWithActorTargetDetailsAndTime() {
        service.write("ROLE_CHANGED", 7L, "USER", 9L, Map.of("from", "MEMBER", "to", "MODERATOR"));

        ArgumentCaptor<AuditLog> saved = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(saved.capture());
        AuditLog log = saved.getValue();
        assertThat(log.getAction()).isEqualTo("ROLE_CHANGED");
        assertThat(log.getActorId()).isEqualTo(7L);
        assertThat(log.getTargetType()).isEqualTo("USER");
        assertThat(log.getTargetId()).isEqualTo(9L);
        assertThat(log.getDetails()).containsEntry("from", "MEMBER").containsEntry("to", "MODERATOR");
        assertThat(log.getCreatedAt()).isEqualTo(NOW);
        assertThat(log.getIpAddress()).isNull();
    }

    @Test
    void recordsTheIpOfTheCurrentRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.9");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        service.write("SIGNED_OUT", 7L, "USER", 7L, null);

        ArgumentCaptor<AuditLog> saved = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getIpAddress()).isEqualTo(InetAddress.getByName("203.0.113.9"));
    }

    @Test
    void refusesEmailAndDisplayNameInDetailsAtAnyDepth() {
        assertThatThrownBy(() -> service.write("X", 1L, "USER", 2L, Map.of("email", "an@mail.com")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("email");
        assertThatThrownBy(() -> service.write("X", 1L, "USER", 2L, Map.of("DisplayName", "An")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.write("X", 1L, "USER", 2L, Map.of("nested", Map.of("Email", "a@b.c"))))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(repository);
    }

    @Test
    void emailHashIsAllowed() {
        service.write("SIGN_IN_FAILED", null, "USER", null, Map.of("reason", "BAD_CREDENTIALS", "emailHash", "ab12"));

        verify(repository).save(org.mockito.ArgumentMatchers.any(AuditLog.class));
    }
}
