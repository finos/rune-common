package com.regnosys.rosetta.common.util;

public class PathException extends RuntimeException {

    public PathException(String message) {
        super(message);
    }

    public PathException(String message, Throwable cause) {
        super(message, cause);
    }
}
