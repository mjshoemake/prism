package com.prism.common.exceptions;

import java.io.Serial;

public class ParameterValueRequiredException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ParameterValueRequiredException(String message) {
        super(message);
    }
}
