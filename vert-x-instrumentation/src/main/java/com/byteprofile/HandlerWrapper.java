package com.byteprofile;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * This is used to wrap Vert.x Handlers to provide nice user-friendly SERVER span names
 */
public final class HandlerWrapper implements Handler<RoutingContext> {

    private final Handler<RoutingContext> handler;

    public HandlerWrapper(Handler<RoutingContext> handler) {
        System.out.println("ENTER IN HandlerWrapper");
        this.handler = handler;
    }

    @Override
    public void handle(RoutingContext context) {

        handler.handle(context);

    }
}
