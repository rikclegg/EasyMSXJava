package com.bloomberg.emsx.samples;

@SuppressWarnings("serial")
public class ServiceException extends Exception {
    public ServiceException(String message) {
        super(message);
    }
}