package com.liteapi.exception;

/**
 * Represents an error response from the LiteAPI, including the HTTP status code
 * and a human-readable message suitable for display to the user.
 */
public class ApiException extends RuntimeException {

    private final int statusCode;

    public ApiException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public ApiException(String message) {
        super(message);
        this.statusCode = -1;
    }

    public ApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
