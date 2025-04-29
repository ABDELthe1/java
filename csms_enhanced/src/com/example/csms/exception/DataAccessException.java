package com.example.csms.exception;

// Exception personnalisée pour les erreurs d'accès aux données
public class DataAccessException extends RuntimeException {

    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}