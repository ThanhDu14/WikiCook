package com.wikicook.devstub;

import com.wikicook.common.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** The user from the {@value #HEADER} header, or the configured one; there is no login yet. */
public class StubCurrentUser implements CurrentUser {

    public static final String HEADER = "X-Dev-User-Id";

    private final long defaultUserId;
    private final DevUserProvisioner provisioner;

    public StubCurrentUser(long defaultUserId, DevUserProvisioner provisioner) {
        this.defaultUserId = defaultUserId;
        this.provisioner = provisioner;
    }

    @Override
    public long id() {
        String header = header();
        long id = header == null ? defaultUserId : parse(header);
        provisioner.ensure(id);
        return id;
    }

    private static String header() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servlet) {
            HttpServletRequest request = servlet.getRequest();
            return request.getHeader(HEADER);
        }
        return null;
    }

    private static long parse(String header) {
        try {
            long id = Long.parseLong(header.strip());
            if (id > 0) {
                return id;
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
