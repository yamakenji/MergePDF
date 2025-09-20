package jp.goodenough;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.pdfbox.multipdf.PDFMergerUtility;

/**
 * Command-line PDF merger.
 *
 * <p>Constraints supported:
 * - Accept multiple PDF files as inputs.
 * - Accept directories; when a directory is specified, recursively collect all PDFs under it.
 * - Allow specifying output filename with -o option.
 * - If a single directory is specified without -o, use the directory name as the output file name
 *   (created in the current working directory).
 *
 * <p>Usage examples:
 * - java -jar target/MergePDF-1.0-SNAPSHOT-shaded.jar -o merged.pdf a.pdf b.pdf
 * - java -jar target/MergePDF-1.0-SNAPSHOT-shaded.jar /path/to/dir
 */
public class Main {

    public static void main(String[] args) {
        int exit = new Main().run(args);
        if (exit != 0) {
            System.exit(exit);
        }
    }

    int run(String[] args) {
        if (args == null || args.length == 0) {
            printUsage();
            return 1;
        }

        String output = null;
        List<String> inputs = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("-o".equals(arg) || "--output".equals(arg)) {
                if (i + 1 >= args.length) {
                    System.err.println("Error: -o/--output requires a filename argument.");
                    return 2;
                }
                output = args[++i];
            } else if ("-h".equals(arg) || "--help".equals(arg)) {
                printUsage();
                return 0;
            } else {
                inputs.add(arg);
            }
        }

        if (inputs.isEmpty()) {
            System.err.println("Error: No input files or directories provided.");
            printUsage();
            return 2;
        }

        // Resolve inputs to a list of PDF file paths (recursively for directories)
        List<Path> pdfs;
        try {
            pdfs = resolveInputs(inputs);
        } catch (IOException e) {
            System.err.println("Error while scanning inputs: " + e.getMessage());
            return 3;
        }

        if (pdfs.isEmpty()) {
            System.err.println("Error: No PDF files found in the given inputs.");
            return 4;
        }

        // Determine output path
        Path outputPath;
        if (output != null) {
            outputPath = Paths.get(output);
        } else if (inputs.size() == 1 && Files.isDirectory(Paths.get(inputs.get(0)))) {
            String dirName = Paths.get(inputs.get(0)).getFileName().toString();
            outputPath = Paths.get(dirName + ".pdf");
        } else {
            System.err.println("Error: Output filename must be specified with -o when not providing a single directory.");
            printUsage();
            return 5;
        }

        try {
            mergePdfs(pdfs, outputPath);
            System.out.println("Merged " + pdfs.size() + " PDF(s) into: " + outputPath);
            return 0;
        } catch (IOException e) {
            System.err.println("Failed to merge PDFs: " + e.getMessage());
            return 6;
        }
    }

    private static void printUsage() {
        System.out.println("Usage: java -jar MergePDF.jar [-o OUTPUT.pdf] <FILE_or_DIR> [<FILE_or_DIR> ...]");
        System.out.println("  -o, --output  Specify output PDF filename.\n"
                + "               If a single directory is provided and -o is omitted,\n"
                + "               the directory name will be used as the output filename in the current directory.");
        System.out.println("Examples:");
        System.out.println("  java -jar MergePDF.jar -o merged.pdf a.pdf b.pdf");
        System.out.println("  java -jar MergePDF.jar /path/to/dir");
    }

    private static List<Path> resolveInputs(List<String> inputs) throws IOException {
        List<Path> collected = new ArrayList<>();
        for (String in : inputs) {
            Path p = Paths.get(in);
            if (!Files.exists(p)) {
                throw new IOException("Path does not exist: " + p);
            }
            if (Files.isDirectory(p)) {
                Files.walkFileTree(p, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        if (isPdf(file)) {
                            collected.add(file.toAbsolutePath().normalize());
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            } else if (Files.isRegularFile(p)) {
                if (isPdf(p)) {
                    collected.add(p.toAbsolutePath().normalize());
                } else {
                    System.err.println("Warning: Skipping non-PDF file: " + p);
                }
            }
        }
        // Deterministic order: alphabetical by normalized absolute path
        Collections.sort(collected, Comparator.comparing(Path::toString, String.CASE_INSENSITIVE_ORDER));
        return collected;
    }

    private static boolean isPdf(Path file) {
        String name = file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        if (dot < 0) return false;
        String ext = name.substring(dot + 1);
        return "pdf".equalsIgnoreCase(ext);
    }

    /** Merge input PDFs into the output file. */
    private static void mergePdfs(List<Path> inputs, Path output) throws IOException {
        Objects.requireNonNull(inputs, "inputs");
        Objects.requireNonNull(output, "output");
        Files.createDirectories(output.toAbsolutePath().getParent() == null
                ? Paths.get(".")
                : output.toAbsolutePath().getParent());

        PDFMergerUtility merger = new PDFMergerUtility();
        merger.setDestinationFileName(output.toString());

        // Add sources as InputStreams to ensure file handles are managed
        List<InputStream> streams = new ArrayList<>();
        try {
            for (Path in : inputs) {
                InputStream is = Files.newInputStream(in);
                streams.add(is);
                merger.addSource(is);
            }
            merger.mergeDocuments(null);
        } finally {
            // Close all streams
            for (InputStream is : streams) {
                try {
                    if (is != null) is.close();
                } catch (IOException ignore) {
                    // best-effort close
                }
            }
        }
    }
}