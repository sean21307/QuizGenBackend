package com.example.quizgenbackend.generator;

import javax.tools.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;

public class Executor {
    public static String compileAndExecute(String code) {
        if (!code.contains("class")) {
            code = "public class DynamicCode {\n" +
                    "    public static void main(String[] args) {\n" +
                    code +
                    "    }\n" +
                    "}\n";
        }



        // Prepare the compiler
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);

        // Prepare the source file
        JavaFileObject sourceFile = new DynamicJavaSourceFile("DynamicCode", code);

        // redirect standard output to capture the output
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);
        PrintStream originalOut = System.out;

        // set the new output
        System.setOut(printStream);

        // Compile the source file
        Iterable<? extends JavaFileObject> compilationUnits = Arrays.asList(sourceFile);
        JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, null, null, compilationUnits);
        boolean success = task.call();


        // Check for compilation errors
        if (!success) {
            for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                System.err.println(diagnostic.getMessage(null));
            }
            return null;
        }

        // Load and execute the compiled class
        try {
            URLClassLoader classLoader = new URLClassLoader(new URL[]{new File(".").toURI().toURL()});
            Class<?> dynamicClass = classLoader.loadClass("DynamicCode");

            // Run the main method in a separate thread
            Thread thread = new Thread(() -> {
                try {
                    dynamicClass.getMethod("main", String[].class).invoke(null, (Object) new String[]{});
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            thread.start();
            thread.join(); // Wait for the thread to finish

            // Capture output
            printStream.flush(); // Ensure all output is written to the stream
            String result = outputStream.toString().trim(); // Capture output
            outputStream.close(); // Close the output stream
            System.setOut(originalOut);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Represents a Java source file in memory
    static class DynamicJavaSourceFile extends SimpleJavaFileObject {
        private final String code;

        DynamicJavaSourceFile(String name, String code) {
            super(URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }
}