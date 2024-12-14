package com.byteprofile.tools;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Store bootstrap, here we will store all StacktraceElement, count them and create data struct for flamegraph generation
 */
public class Store {
    private final int MAX_FLAMEGRAPH_DEPTH = 40;

    private static class Node {

        private final String method;
        private final Map<String, Node> children = new HashMap<>();
        private long samples = 0;


        public Node(String name) {
            method = name;
        }


        private Node getChild(String method) {
            return children.computeIfAbsent(method, Node::new);
        }

        private void addTrace(List<String> trace, int end) {
            samples++;
            if (end > 0) {
                getChild(trace.get(end)).addTrace(trace, end - 1);
            }
        }

        public void addTrace(List<String> trace) {
            addTrace(trace, trace.size() - 1);
        }


        /**
         * Write in d3-flamegraph format
         */
        private void writeAsJson(PrintStream s, int maxDepth) {
            s.printf("{ \"name\": \"%s\", \"value\": %d, \"children\": [", method, samples);
            if (maxDepth > 1) {
                for (Node child : children.values()) {
                    child.writeAsJson(s, maxDepth - 1);
                    s.print(",");
                }
            }
            s.print("]}");
        }

        public void writeAsHTML(PrintStream s, int maxDepth) {
            s.print("""
                    <head>
                      <link rel="stylesheet" type="text/css" href="https://cdn.jsdelivr.net/npm/d3-flame-graph@4.1.3/dist/d3-flamegraph.css">
                      <link rel="stylesheet" type="text/css" href="misc/d3-flamegraph.css">
                    </head>
                    <body>
                      <div id="chart"></div>
                      <script type="text/javascript" src="https://d3js.org/d3.v7.js"></script>
                      <script type="text/javascript" src="misc/d3.v7.js"></script>
                      <script type="text/javascript" src="https://cdn.jsdelivr.net/npm/d3-flame-graph@4.1.3/dist/d3-flamegraph.js"></script>
                      <script type="text/javascript" src="misc/d3-flamegraph.js"></script>
                      <script type="text/javascript">
                      var chart = flamegraph().width(window.innerWidth);
                      d3.select("#chart").datum(""");
            writeAsJson(s, maxDepth);
            s.print("""
                    ).call(chart);
                      window.onresize = () => chart.width(window.innerWidth);
                      </script>
                    </body>
                    """);
        }

    }


    Node rootNode = new Node("root");

    String flamegraphPath;

    public Store(String flamegraphpath) {
        flamegraphPath = flamegraphpath;
    }

    public Store() {
    }

    private final Map<String, Long> methodOnTopSampleCount = new HashMap<>();
    public final Map<String, Long> methodSampleCount = new HashMap<>();

    private long totalSampleCount = 0;


    public void addSample(StackTraceElement[] stackTraceElements) {
        List<String> flattenStackStraces = Arrays.stream(stackTraceElements).map(this::flattenStackTraceElement).toList();
        rootNode.addTrace(flattenStackStraces);

        flattenStackStraces.forEach(flattenStackStrace -> {
            methodSampleCount.put(flattenStackStrace,
                    methodSampleCount.getOrDefault(flattenStackStrace, 0L) + 1L);
        });
    }

    private StackTraceElementReadable formatStackTracename(StackTraceElement element) {
        return new StackTraceElementReadable(String.format("%s.%s", element.getClassName(), element.getMethodName()));
    }

    private String flattenStackTraceElement(StackTraceElement stackTraceElement) {
        // call intern to safe some memory
        return (stackTraceElement.getClassName() + "." + stackTraceElement.getMethodName()).intern();
    }

    private record StackTraceElementReadable(String fullName) {
    }


    public void createFlameGraphFile() {
        try (OutputStream os = new BufferedOutputStream(java.nio.file.Files.newOutputStream(Path.of(flamegraphPath)))) {
            PrintStream s = new PrintStream(os);
            rootNode.writeAsHTML(s, MAX_FLAMEGRAPH_DEPTH);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
