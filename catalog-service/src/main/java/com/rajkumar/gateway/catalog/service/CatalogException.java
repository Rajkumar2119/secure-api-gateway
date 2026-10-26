package com.rajkumar.gateway.catalog.service;

import org.springframework.http.HttpStatus;

public class CatalogException extends RuntimeException {

    private final HttpStatus status;

    private CatalogException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public static CatalogException notFound(String message) {
        return new CatalogException(HttpStatus.NOT_FOUND, message);
    }

    public static CatalogException conflict(String message) {
        return new CatalogException(HttpStatus.CONFLICT, message);
    }

    public static CatalogException badRequest(String message) {
        return new CatalogException(HttpStatus.BAD_REQUEST, message);
    }

    public HttpStatus getStatus() {
        return status;
    }
}
