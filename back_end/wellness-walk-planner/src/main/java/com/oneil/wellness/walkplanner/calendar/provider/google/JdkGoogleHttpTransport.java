package com.oneil.wellness.walkplanner.calendar.provider.google;
import java.net.URI;import java.net.http.*;import java.util.Map;import org.springframework.stereotype.Component;
@Component
public class JdkGoogleHttpTransport implements GoogleHttpTransport{
 private final GoogleCalendarConfiguration config; public JdkGoogleHttpTransport(GoogleCalendarConfiguration c){config=c;}
 public Response exchange(String method,URI uri,Map<String,String> headers,String body){try{HttpClient client=HttpClient.newBuilder().connectTimeout(config.getConnectionTimeout()).followRedirects(HttpClient.Redirect.NEVER).build();var builder=HttpRequest.newBuilder(uri).timeout(config.getReadTimeout());headers.forEach(builder::header);builder.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body));var response=client.send(builder.build(),HttpResponse.BodyHandlers.ofString());return new Response(response.statusCode(),response.body());}catch(java.net.http.HttpTimeoutException e){throw new GoogleProviderException("TIMEOUT","Google Calendar request timed out.");}catch(Exception e){throw new GoogleProviderException("UNAVAILABLE","Google Calendar is unavailable.");}}
}
