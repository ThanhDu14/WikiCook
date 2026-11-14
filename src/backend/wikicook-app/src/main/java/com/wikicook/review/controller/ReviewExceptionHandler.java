package com.wikicook.review.controller;

import com.wikicook.review.exception.CooksnapProcessingException;
import com.wikicook.review.exception.RateLimitExceededException;
import com.wikicook.review.exception.ReportAlreadyExistsException;
import com.wikicook.review.exception.ReviewAlreadyExistsException;
import com.wikicook.review.exception.ReviewNotFoundException;
import com.wikicook.review.exception.ReviewUnderModerationException;
import com.wikicook.review.exception.SelfHelpfulVoteForbiddenException;
import com.wikicook.review.exception.SelfReportForbiddenException;
import com.wikicook.review.exception.SelfReviewForbiddenException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ReviewExceptionHandler {

    @ExceptionHandler(SelfReviewForbiddenException.class)
    public ProblemDetail handleSelfReviewForbidden(SelfReviewForbiddenException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setTitle("Forbidden");
        problem.setType(URI.create("https://wikicook.com/errors/self-review-forbidden"));
        return problem;
    }

    @ExceptionHandler(SelfHelpfulVoteForbiddenException.class)
    public ProblemDetail handleSelfHelpfulVoteForbidden(SelfHelpfulVoteForbiddenException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setTitle("Forbidden");
        problem.setType(URI.create("https://wikicook.com/errors/self-upvote-forbidden"));
        return problem;
    }

    @ExceptionHandler(SelfReportForbiddenException.class)
    public ProblemDetail handleSelfReportForbidden(SelfReportForbiddenException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setTitle("Forbidden");
        problem.setType(URI.create("https://wikicook.com/errors/self-report-forbidden"));
        return problem;
    }

    @ExceptionHandler(ReviewAlreadyExistsException.class)
    public ProblemDetail handleReviewAlreadyExists(ReviewAlreadyExistsException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Conflict");
        problem.setType(URI.create("https://wikicook.com/errors/review-already-exists"));
        return problem;
    }

    @ExceptionHandler(ReviewUnderModerationException.class)
    public ProblemDetail handleReviewUnderModeration(ReviewUnderModerationException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Conflict");
        problem.setType(URI.create("https://wikicook.com/errors/review-under-moderation"));
        return problem;
    }

    @ExceptionHandler(ReportAlreadyExistsException.class)
    public ProblemDetail handleReportAlreadyExists(ReportAlreadyExistsException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Conflict");
        problem.setType(URI.create("https://wikicook.com/errors/report-already-exists"));
        return problem;
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ProblemDetail handleRateLimitExceeded(RateLimitExceededException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
        problem.setTitle("Too Many Requests");
        problem.setType(URI.create("https://wikicook.com/errors/rate-limit-exceeded"));
        return problem;
    }

    @ExceptionHandler(ReviewNotFoundException.class)
    public ProblemDetail handleReviewNotFound(ReviewNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Not Found");
        problem.setType(URI.create("https://wikicook.com/errors/review-not-found"));
        return problem;
    }

    @ExceptionHandler(CooksnapProcessingException.class)
    public ProblemDetail handleCooksnapProcessing(CooksnapProcessingException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Bad Request");
        problem.setType(URI.create("https://wikicook.com/errors/cooksnap-processing-error"));
        return problem;
    }
}

