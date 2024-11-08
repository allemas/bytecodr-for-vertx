package com.byteprofile.instrumentation;

import net.bytebuddy.asm.Advice;

public class LogAllCallsites {
    @Advice.OnMethodEnter(inline = false)
    public static void onEnter(
            @Advice.This Object thisObject,
            @Advice.Origin String origin,
            @Advice.Origin("#t #m") String detaildOrigin
    ) {
        System.out.print("ENTER method: " + detaildOrigin);

        System.out.print(detaildOrigin);
        System.out.print(" ");
        System.out.print('\n');
    }

    @Advice.OnMethodExit
    public static void onExit(@Advice.Origin("#t.#m") String method) {
        // Informations sur la sortie de la méthode
        System.out.print("Exiting method: " + method);
        System.out.print('\n');
        System.out.print('\n');

    }

}
//https://stackoverflow.com/questions/78833310/how-can-i-replace-a-static-field-access-with-a-new-generate-method
//https://groups.google.com/g/byte-buddy/c/eudEFl9euJo
//https://stackoverflow.com/questions/34523361/unable-to-instrument-apache-httpclient-using-javaagent-for-spring-boot-uber-jar