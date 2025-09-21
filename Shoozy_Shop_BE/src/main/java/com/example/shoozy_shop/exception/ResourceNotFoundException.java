package com.example.shoozy_shop.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, Object resourceId) {
        super(String.format("Can't not found %s with ID: %s", resourceName, resourceId));
    }
}
