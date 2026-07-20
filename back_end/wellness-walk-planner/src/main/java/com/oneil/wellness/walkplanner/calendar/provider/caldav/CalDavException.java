package com.oneil.wellness.walkplanner.calendar.provider.caldav;

public class CalDavException extends RuntimeException {
    private final String code;
    public CalDavException(String code, String message) { super(message); this.code = code; }
    public CalDavException(String code, String message, Throwable cause) { super(message, cause); this.code = code; }
    public String getCode() { return code; }
}
