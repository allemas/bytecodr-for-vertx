package com.byteprofile.instrumentation;

import com.byteprofile.HandlerWrapper;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;

import java.util.ArrayList;


public class HandlerVisitorCallSite {
    @Advice.OnMethodEnter
    public static void onEnter(
            @Advice.Argument(value = 0, readOnly = false) Handler<RoutingContext> handler
    ) {
        System.out.println("ENTER the method: ");
        try {
            System.out.println("-->" + handler.getClass().getClassLoader());
            handler = new HandlerWrapper(handler);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Advice.OnMethodExit
    public static void onExit(@Advice.Origin("#t.#m") String method) {
        System.out.print("Exiting method: " + method);
        System.out.print('\n');

    }

}
//https://stackoverflow.com/questions/78833310/how-can-i-replace-a-static-field-access-with-a-new-generate-method
//https://groups.google.com/g/byte-buddy/c/eudEFl9euJo
//https://stackoverflow.com/questions/34523361/unable-to-instrument-apache-httpclient-using-javaagent-for-spring-boot-uber-jar