package com.acme.assistant.api.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(WebExchangeBindException.class)
    ProblemDetail validation(WebExchangeBindException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Invalid request");
        problem.setDetail(ex.getAllErrors().getFirst().getDefaultMessage());
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail illegalArgument(IllegalArgumentException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Invalid request");
        problem.setDetail(ex.getMessage());
        return problem;
    }

    @ExceptionHandler(IllegalStateException.class)
    ProblemDetail illegalState(IllegalStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE);
        problem.setTitle("Assistant unavailable");
        problem.setDetail(ex.getMessage());
        return problem;
    }
}
