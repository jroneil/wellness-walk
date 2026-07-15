package com.oneil.wellness.walkplanner.zip.client;

import java.io.IOException;
import java.net.http.HttpRequest;

@FunctionalInterface
interface ZipLookupTransport {

    ZipLookupHttpResponse send(HttpRequest request) throws IOException, InterruptedException;
}
