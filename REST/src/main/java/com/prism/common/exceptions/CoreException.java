package com.prism.common.exceptions;

import java.io.Serial;

public class CoreException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public CoreException(String message) {
        super(message);
    }

    public CoreException(String message, Throwable e) {
        super(message, e);
    }

}
