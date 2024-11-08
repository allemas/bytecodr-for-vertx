package com.byteprofile;

//import io.vertx.core.Context;

import io.vertx.core.Handler;

public class HandlerWrapper<T> implements Handler<T> {
    private final Handler<T> delegate;
//    private final Context context;

    private HandlerWrapper(Handler<T> delegate) {
        this.delegate = delegate;
        System.out.println("Hello !");
        //  this.context = context;
    }

    public static <T> Handler<T> wrap(Handler<T> handler) {
        //     Context current = Context.current();
        handler = new HandlerWrapper<>(handler);
        return handler;
    }

    @Override
    public void handle(T t) {

        delegate.handle(t);
    }
}
