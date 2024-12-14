package com.byteprofile;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;

/**
 * This is used to wrap Vert.x Handlers to provide nice user-friendly SERVER span names
 */
public final class HandlerWrapper implements Handler<HttpServerRequest> {

    private final Handler<HttpServerRequest> delegate;
    private final Context context;

    private HandlerWrapper(Handler<HttpServerRequest> delegate, Context context) {
        this.delegate = delegate;
        this.context = context;


    }


    public static Handler<HttpServerRequest> wrap(Handler<HttpServerRequest> handler) {
        Context current = Context.current();
        handler = HandlerWrapper.wrap(handler);
        return handler;
    }

    @Override
    public void handle(HttpServerRequest httpServerRequest) {
        try (Scope ignore = context.makeCurrent()) {
            this.delegate.handle(httpServerRequest);
        }
    }
}
