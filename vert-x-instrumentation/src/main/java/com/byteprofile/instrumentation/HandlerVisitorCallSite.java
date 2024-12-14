package com.byteprofile.instrumentation;

import com.byteprofile.HandlerWrapper;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;

import javax.imageio.spi.ServiceRegistry;
import java.util.ArrayList;


public class HandlerVisitorCallSite {
    @Advice.OnMethodEnter
    public static void onEnter(
            @Advice.Argument(value = 0, readOnly = false) Handler<HttpServerRequest> handler
    ) {

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(LoggingSpanExporter.create()))
                .build();

        OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .buildAndRegisterGlobal();



        System.out.println("ENTER the method: ");
        try {
            handler = HandlerWrapper.wrap(handler);
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