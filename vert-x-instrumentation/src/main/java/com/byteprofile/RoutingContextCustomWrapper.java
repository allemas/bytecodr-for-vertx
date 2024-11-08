package com.byteprofile;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class RoutingContextCustomWrapper implements Handler<RoutingContext> {

    Handler<RoutingContext> handler;

    public RoutingContextCustomWrapper(Handler<RoutingContext> handler) {
        handler = handler;
    }


    @Override
    public void handle(RoutingContext routingContext) {
        Class<?> classHer = routingContext.getClass();
        System.out.println(classHer.getSuperclass());
    }
}
