package med.supply.system.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class MetadataManager {
    public static void append(Path metadataCsv, Path filePath, String kind, String notes) throws IOException {
        String row = String.join(",", escape(filePath.toString()), escape(Instant.now().toString()),
                escape(kind), escape(notes)) + "\n";
        Files.writeString(metadataCsv, row, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
    }

    private static String escape(String s) {
        if (s.contains(",") || s.contains("\"")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    public static void move(Path from, Path to, Path metaIndex) throws IOException {
        Files.createDirectories(to.getParent());
        Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
        append(metaIndex, to, "MOVE", "moved from " + from);
    }

    public static void delete(Path file, Path metaIndex) throws IOException {
        Files.deleteIfExists(file);
        append(metaIndex, file, "DELETE", "deleted");
    }

    public static Path archiveZip(Path sourceDir, Path zipTarget, Path metaIndex) throws IOException {
        Files.createDirectories(zipTarget.getParent());
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipTarget))) {
            try (var stream = Files.walk(sourceDir)) {
                for (Path p : (Iterable<Path>) stream::iterator) {
                    if (Files.isRegularFile(p)) {
                        String entryName = sourceDir.relativize(p).toString().replace('\\', '/');
                        zos.putNextEntry(new ZipEntry(entryName));
                        Files.copy(p, zos);
                        zos.closeEntry();
                    }
                }
            }
        }
        append(metaIndex, zipTarget, "ARCHIVE", "archived " + sourceDir);
        return zipTarget;
    }
}
