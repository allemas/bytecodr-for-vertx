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
import io.vertx.ext.web.impl.RoutingContextImpl;

/**
 * This is used to wrap Vert.x Handlers to provide nice user-friendly SERVER span names
 */
public final class HandlerWrapper implements Handler<RoutingContextImpl> {
    private final Handler<RoutingContextImpl> delegate;
    //    private final Handler<HttpServerRequest> delegate;
    private Context context;

    private HandlerWrapper(Handler<RoutingContextImpl> delegate, Context currentContext) {
        this.delegate = delegate;
        this.context = currentContext;
    }


    public static Handler<RoutingContextImpl> wrap(Handler<RoutingContextImpl> handler) {
        if (handler != null && !(handler instanceof RoutingContextImpl)) {
            Context current = Context.current();
            handler = new HandlerWrapper(handler, current);
        }
        return handler;
    }

    @Override
    public void handle(RoutingContextImpl httpServerRequest) {
        TextMapPropagator propagator = GlobalOpenTelemetry.getPropagators().getTextMapPropagator();
        Tracer tracer = GlobalOpenTelemetry.getTracer("APM-Vert.x");

        Context extractedContext = propagator.extract(context, httpServerRequest.request(), ByteProfilerTextMapGetter.create());
        Span span = SpanFactory.create(tracer, httpServerRequest.request(), extractedContext)
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            propagator.inject(Context.current(), httpServerRequest.request(), (req, key, value) -> req.headers().add(key, value));
            httpServerRequest.response().endHandler(v -> {
                span.end();
            });

            this.delegate.handle(httpServerRequest);
        }
    }
}
