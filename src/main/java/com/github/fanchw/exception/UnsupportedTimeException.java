package com.github.fanchw.exception;

public class UnsupportedTimeException extends RuntimeException {
    public UnsupportedTimeException() {
    }

    public UnsupportedTimeException(String message) {
        super(message);
    }
}
