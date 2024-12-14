package com.byteprofile.tools;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.vertx.core.http.HttpServerRequest;

import java.util.stream.Collectors;

public class SpanFactory {
    public static SpanBuilder create(Tracer tracer, HttpServerRequest httpServerRequest, Context extractedContext) {

        SpanBuilder span = tracer.spanBuilder("vertx.handleRequest")
                .setAttribute("uri", httpServerRequest.uri())
                .setAttribute("absoluteUri", httpServerRequest.absoluteURI())
                .setAttribute("headers", httpServerRequest.headers().entries().stream()
                        .map(entry -> entry.getKey() + ": " + entry.getValue())
                        .collect(Collectors.joining(" ")));

        Span s = Span.fromContext(extractedContext);
        if (s.getClass().getName().equals("io.opentelemetry.sdk.trace.SdkSpan")) {
            System.out.println(extractedContext);
            span.setNoParent();
        } else
            span.setParent(Context.current().with(s));

        return span;
    }
}
