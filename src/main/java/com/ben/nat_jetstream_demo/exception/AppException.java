package com.ben.nat_jetstream_demo.exception;

public class AppException extends RuntimeException {
    private final String messageKey;
    private final Object[] args;

    public AppException(String messageKey) {
        this(messageKey, (Object[]) null);
    }

    public AppException(String messageKey, Object... args) {
        super(messageKey);
        this.messageKey = messageKey;
        this.args = args;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public Object[] getArgs() {
        return args;
    }
}
