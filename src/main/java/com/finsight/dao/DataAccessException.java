package com.finsight.dao;

/** Wraps {@link java.sql.SQLException} so callers are not forced to catch checked exceptions. */
public class DataAccessException extends RuntimeException {

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
