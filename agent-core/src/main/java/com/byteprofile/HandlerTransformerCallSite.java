package com.byteprofile;

import io.vertx.core.Handler;
import io.vertx.ext.web.impl.RoutingContextImpl;
import net.bytebuddy.asm.Advice;

public class HandlerTransformerCallSite {
    @Advice.OnMethodEnter
    public static void onEnter(
            @Advice.Argument(value = 0, readOnly = false) Handler<RoutingContextImpl> handler) {
        handler = HandlerWrapper.wrap(handler);
    }
}