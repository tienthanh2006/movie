import javax.tools.*;
import com.sun.source.util.JavacTask;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

public class ParseAllJava {
    public static void main(String[] args) throws Exception {
        var javaFiles = Files.walk(Path.of(args[0])).filter(p -> p.toString().endsWith(".java"))
            .filter(p -> p.toString().contains("src/main/java")
                || p.toString().contains("src/test/java")
                || p.toString().contains("tests/offline")).toList();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager manager = compiler.getStandardFileManager(diagnostics, null, null)) {
            var files = manager.getJavaFileObjectsFromPaths(javaFiles);
            JavacTask task = (JavacTask) compiler.getTask(null, manager, diagnostics, List.of("-proc:none"), null, files);
            long count = 0;
            for (var tree : task.parse()) count++;
            var errors = diagnostics.getDiagnostics().stream()
                .filter(d -> d.getKind() == Diagnostic.Kind.ERROR).toList();
            errors.forEach(System.err::println);
            if (errors.size() > 0) System.exit(1);
            System.out.println("PASS parsed " + count + " Java source files with javac parser (no dependency resolution)");
        }
    }
}
