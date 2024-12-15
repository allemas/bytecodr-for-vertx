package com.byteprofile;


import com.byteprofile.tools.BytecodrClassLoader;
import com.byteprofile.tools.FlamegraphWritter;
import com.byteprofile.tools.JFRStreamer;
import com.byteprofile.tools.Store;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.semconv.ResourceAttributes;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingStream;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Constructor;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.util.jar.JarFile;

import static net.bytebuddy.matcher.ElementMatchers.*;

import io.opentelemetry.sdk.OpenTelemetrySdk;

import io.opentelemetry.sdk.resources.Resource;

public class Premain {

    public static void premain(String agentArgs, Instrumentation inst) throws URISyntaxException, IOException, InterruptedException {
        buildOtel();
          JFRStreamer.stream(1);
        //  FlamegraphWritter.write(5);

        new AgentBuilder.Default()
                .with(AgentBuilder.InstallationListener.StreamWriting.toSystemError())
                .ignore(none())
                .with(AgentBuilder.Listener.StreamWriting.toSystemError().withTransformationsOnly())
                .disableClassFormatChanges()
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .type(named("io.vertx.ext.web.impl.RouteImpl"))
                .transform(new AgentBuilder.Transformer.ForAdvice()
                        .advice(ElementMatchers.named("handler")
                                .and(takesArgument(0, named("io.vertx.core.Handler")
                                        )
                                ), "com.byteprofile.HandlerVisitorCallSite")
                )
                .installOn(inst);
    }


    private static void buildOtel() throws InterruptedException {
        Resource resource = Resource.getDefault().toBuilder().put(ResourceAttributes.SERVICE_NAME, "APM4me")
                .put(ResourceAttributes.SERVICE_VERSION, "0.1.1")
                .build();
        ;
        SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(OtlpGrpcSpanExporter.builder().build()))
                .setResource(resource)
                .build();

        SdkMeterProvider sdkMeterProvider =
                SdkMeterProvider.builder()
                        .setResource(resource)
                        .registerMetricReader(
                                PeriodicMetricReader.builder(OtlpGrpcMetricExporter.getDefault())
                                        .setInterval(Duration.ofMillis(100))
                                        .build())
                        .build();

        SdkLoggerProvider loggerProvider = SdkLoggerProvider.builder()
                .addLogRecordProcessor(
                        BatchLogRecordProcessor.builder(
                                        OtlpGrpcLogRecordExporter.builder()
                                                .setEndpoint("http://0.0.0.0:4317")
                                                .build())
                                .build())
                .build();

        OpenTelemetrySdk.builder()
                .setMeterProvider(sdkMeterProvider)
                .setTracerProvider(sdkTracerProvider)
                .setLoggerProvider(loggerProvider)
                .setPropagators(ContextPropagators.create(TextMapPropagator
                        .composite(W3CTraceContextPropagator.getInstance(), W3CBaggagePropagator.getInstance())))
                .buildAndRegisterGlobal();
        Thread.sleep(150);
    }
}

