package com.oneil.wellness.walkplanner.calendar.provider.google;
import java.net.URI; import java.util.Map;
public interface GoogleHttpTransport {Response exchange(String method,URI uri,Map<String,String> headers,String body);record Response(int status,String body) {}}
