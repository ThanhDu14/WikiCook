package com.wikicook.search.port;

import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Dev stand-in: the viewer is the {@value #HEADER} header, and no header means guest. Unlike the shared
 * {@code StubCurrentUser} it never falls back to the default dev user, because a guest must see unfiltered results
 * (020 acceptance 3.4).
 */
public class DevHeaderSearchViewer implements SearchViewerPort {

    public static final String HEADER = "X-Dev-User-Id";

    @Override
    public Optional<Long> currentUserId() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes servlet)) {
            return Optional.empty();
        }
        String value = servlet.getRequest().getHeader(HEADER);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            long id = Long.parseLong(value.strip());
            if (id > 0) {
                return Optional.of(id);
            }
        } catch (NumberFormatException e) {
            // reported below
        }
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                HEADER + " must be a positive number");
        problem.setProperty("code", "VALIDATION_ERROR");
        throw new ErrorResponseException(HttpStatus.BAD_REQUEST, problem, null);
    }
}
