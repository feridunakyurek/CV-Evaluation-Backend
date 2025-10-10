package com.cvanalyzer.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT) // HTTP 409
public class EvaluationAlreadyExistsException extends RuntimeException {
    public EvaluationAlreadyExistsException(String message) {
        super(message);
    }
}