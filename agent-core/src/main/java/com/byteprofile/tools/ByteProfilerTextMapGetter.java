package com.byteprofile.tools;

import io.opentelemetry.context.propagation.TextMapGetter;
import io.vertx.core.http.HttpServerRequest;

public class ByteProfilerTextMapGetter implements TextMapGetter<HttpServerRequest> {
    @Override
    public Iterable<String> keys(HttpServerRequest carrier) {
        return carrier.headers().names();
    }

    @Override
    public String get(HttpServerRequest carrier, String key) {
        if (carrier == null || key == null) {
            return null;
        }
        return carrier.getHeader(key);
    }

    public static ByteProfilerTextMapGetter create() {
        return new ByteProfilerTextMapGetter();
    }
}
