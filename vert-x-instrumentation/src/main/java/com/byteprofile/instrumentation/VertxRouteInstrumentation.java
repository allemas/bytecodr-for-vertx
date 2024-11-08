package com.byteprofile.instrumentation;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

import java.security.ProtectionDomain;

public class VertxRouteInstrumentation implements AgentBuilder.Transformer {
    @Override
    public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader, JavaModule javaModule, ProtectionDomain protectionDomain) {
        return builder
                .visit(Advice.to(LogAllCallsites.class)
                        .on(ElementMatchers.isMethod()
                                .and(ElementMatchers.nameContains("handle"))
                        )
                )
                ;


    }


    public static VertxRouteInstrumentation build() {
        return new VertxRouteInstrumentation();
    }


}
