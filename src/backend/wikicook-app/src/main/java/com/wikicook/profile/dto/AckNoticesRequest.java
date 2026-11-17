package com.wikicook.profile.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

/**
 * {@code POST /me/dietary-profile/notices/ack}: the notices the person has read and closed (FR-019). The contract fixes
 * no upper bound; 100 keeps the {@code IN} list small and is far above what the form shows at once (it closes one
 * notice per click). An id that is not the caller's is ignored by the service, so it is not checked here.
 *
 * @param ids {@code dietary_notices.id}, at least one
 */
public record AckNoticesRequest(
        @NotEmpty @Size(max = 100, message = "TOO_MANY_IDS") Set<@NotNull Long> ids) {
}
