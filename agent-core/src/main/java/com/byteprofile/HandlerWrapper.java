package com.byteprofile;

import com.byteprofile.tools.ByteProfilerTextMapGetter;
import com.byteprofile.tools.SpanFactory;
import com.sun.tools.attach.VirtualMachine;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;

import java.lang.instrument.Instrumentation;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * This is used to wrap Vert.x Handlers to provide nice user-friendly SERVER span names
 */
public final class HandlerWrapper implements Handler<HttpServerRequest> {
    private final Handler<HttpServerRequest> delegate;
    private Context context;

    private HandlerWrapper(Handler<HttpServerRequest> delegate, Context currentContext) {
        this.delegate = delegate;
        this.context = currentContext;
    }


    public static Handler<HttpServerRequest> wrap(Handler<HttpServerRequest> handler) {
        Context current = Context.current();
        if (handler != null && !(handler instanceof HandlerWrapper)) {
            if (current == Context.root()) {
                Tracer tracer = GlobalOpenTelemetry.getTracer("APM-Vert.x");
                Span span = tracer.spanBuilder("vertx.handleRequest.root").startSpan();
                Context contextRoot = Context.root().with(span);

                handler = new HandlerWrapper(handler, contextRoot);
                span.end();
            } else {
                handler = new HandlerWrapper(handler, current);
            }
        }
        return handler;
    }

    @Override
    public void handle(HttpServerRequest httpServerRequest) {
        TextMapPropagator propagator = GlobalOpenTelemetry.getPropagators().getTextMapPropagator();
        Tracer tracer = GlobalOpenTelemetry.getTracer("APM-Vert.x");

        Context extractedContext = propagator.extract(context, httpServerRequest, ByteProfilerTextMapGetter.create());
        Span span = SpanFactory.create(tracer, httpServerRequest, extractedContext).startSpan();

        try (Scope ignore = extractedContext.makeCurrent()) {
            Context childContext = context.with(span);
            propagator.inject(childContext, httpServerRequest, (req, key, value) -> req.headers().add(key, value));

            this.delegate.handle(httpServerRequest);
        } finally {
            span.end();
        }
    }
}
