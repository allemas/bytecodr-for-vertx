package com.byteprofile;

import com.byteprofile.instrumentation.HandlerVisitorCallSite;
import com.byteprofile.instrumentation.LogAllCallsites;
import io.vertx.core.Handler;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.asm.MemberSubstitution;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.TypeResolutionStrategy;
import net.bytebuddy.dynamic.VisibilityBridgeStrategy;
import net.bytebuddy.dynamic.loading.*;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import net.bytebuddy.implementation.auxiliary.AuxiliaryType;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.matcher.StringMatcher;
import net.bytebuddy.utility.JavaModule;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;

import static net.bytebuddy.agent.builder.AgentBuilder.RedefinitionStrategy.REDEFINITION;
import static net.bytebuddy.agent.builder.AgentBuilder.RedefinitionStrategy.RETRANSFORMATION;
import static net.bytebuddy.matcher.ElementMatchers.*;

public class InstrumentFactory {

    public InstrumentFactory(ClassLoader c, ClassLoader parent, Instrumentation loadedInstrumentation) throws IOException, NoSuchMethodException {

        new AgentBuilder
                .Default()
                .with(
                        new AgentBuilder.Listener.Filtering(
                                new StringMatcher("com.byteprofile", StringMatcher.Mode.CONTAINS),
                                AgentBuilder.Listener.StreamWriting.toSystemOut())
                )
                .with(
                        new AgentBuilder.Listener.Filtering(
                                new StringMatcher("io.vertx.core.Handler", StringMatcher.Mode.CONTAINS),
                                AgentBuilder.Listener.StreamWriting.toSystemOut())

                )

                .with(
                        new AgentBuilder.Listener.Filtering(
                                new StringMatcher("io.vertx.ext.web.impl.RouteImpl", StringMatcher.Mode.CONTAINS),
                                AgentBuilder.Listener.StreamWriting.toSystemOut())

                )

                .disableClassFormatChanges()
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
//                .with(new RedefinitionDiscoveryStrategy())
                .with(AgentBuilder.DescriptionStrategy.Default.POOL_ONLY)
                .with(AgentBuilder.InjectionStrategy.UsingUnsafe.INSTANCE)

                .type(named("io.vertx.ext.web.impl.RouteImpl"))
                .transform(new AgentBuilder.Transformer.ForAdvice()
                        .include(c)
                        .advice(ElementMatchers.named("handler")
                                .and(takesArgument(0, named("io.vertx.core.Handler")
                                )), "com.byteprofile.instrumentation.HandlerVisitorCallSite")
                        .auxiliary(List.of("com.byteprofile.HandlerWrapper","io.vertx.core.Handler", "com.byteprofile.instrumentation.HandlerVisitorCallSite"))

                )
                .installOn(loadedInstrumentation);


    }

    private static final Map<String, List<Runnable>> CLASS_LOAD_CALLBACKS = new HashMap<>();

    private static class ClassLoadListener extends AgentBuilder.Listener.Adapter {
        @Override
        public void onComplete(
                String typeName, ClassLoader classLoader, JavaModule javaModule, boolean b) {
            synchronized (CLASS_LOAD_CALLBACKS) {
                List<Runnable> callbacks = CLASS_LOAD_CALLBACKS.get(typeName);
                if (callbacks != null) {
                    for (Runnable callback : callbacks) {
                        callback.run();
                    }
                }
            }
        }
    }

    private static class RedefinitionDiscoveryStrategy
            implements AgentBuilder.RedefinitionStrategy.DiscoveryStrategy {
        private static final AgentBuilder.RedefinitionStrategy.DiscoveryStrategy delegate =
                AgentBuilder.RedefinitionStrategy.DiscoveryStrategy.Reiterating.INSTANCE;

        @Override
        public Iterable<Iterable<Class<?>>> resolve(Instrumentation instrumentation) {
            // filter out our agent classes and injected helper classes
            return () ->
                    streamOf(delegate.resolve(instrumentation))
                            .map(RedefinitionDiscoveryStrategy::filterClasses)
                            .iterator();
        }

        private static Iterable<Class<?>> filterClasses(Iterable<Class<?>> classes) {
            return () -> streamOf(classes).filter(c -> !isIgnored(c)).iterator();
        }

        private static <T> Stream<T> streamOf(Iterable<T> iterable) {
            return StreamSupport.stream(iterable.spliterator(), false);
        }

        private static boolean isIgnored(Class<?> c) {
            ClassLoader cl = c.getClassLoader();
            if (cl instanceof ByteArrayClassLoader) {
                System.out.println(c.getName());
                System.out.println("xxxx");
                return true;
            }
            // ignore generate byte buddy helper class
            if (c.getName().startsWith("java.lang.ClassLoader$ByteBuddyAccessor$")) {
                return true;
            }

            return true;
        }
    }
}
