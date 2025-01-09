package io.kestra.plugin.langchain.exceptions;

import java.io.Serial;

public class ApiKeyNotFoundException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    public ApiKeyNotFoundException(Throwable e) {
        super(e);
    }

    public ApiKeyNotFoundException(String message) {
        super(message);
    }

    public ApiKeyNotFoundException(String message, Throwable e) {
        super(message, e);
    }
}

