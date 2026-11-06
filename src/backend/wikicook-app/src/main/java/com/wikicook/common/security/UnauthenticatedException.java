package com.wikicook.common.security;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/** No signed-in user: 401 ProblemDetail with {@code code = UNAUTHENTICATED} (031 research R16). */
public class UnauthenticatedException extends ErrorResponseException {

    public UnauthenticatedException() {
        super(HttpStatus.UNAUTHORIZED, problem(), null);
    }

    private static ProblemDetail problem() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Sign in first");
        problem.setProperty("code", "UNAUTHENTICATED");
        return problem;
    }
}
