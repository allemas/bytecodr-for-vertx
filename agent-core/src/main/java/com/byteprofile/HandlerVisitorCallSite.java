package com.byteprofile;

import io.vertx.core.Handler;
import io.vertx.ext.web.impl.RoutingContextImpl;
import net.bytebuddy.asm.Advice;

public class HandlerVisitorCallSite {
    @Advice.OnMethodEnter
    public static void onEnter(
            @Advice.Argument(value = 0, readOnly = false) Handler<RoutingContextImpl> handler) {
        handler = HandlerWrapper.wrap(handler);
    }
}


//https://stackoverflow.com/questions/78833310/how-can-i-replace-a-static-field-access-with-a-new-generate-method
//https://groups.google.com/g/byte-buddy/c/eudEFl9euJo
//https://stackoverflow.com/questions/34523361/unable-to-instrument-apache-httpclient-using-javaagent-for-spring-boot-uber-jar