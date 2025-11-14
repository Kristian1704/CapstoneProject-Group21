import med.supply.system.util.MetadataManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.zip.ZipFile;

public class MetadataManagerTest {
    public static void main(String[] args) {
        System.out.println("Running MetadataManager tests...");

        try {
            testAppendCreatesMetadataRow();
            testEscapeWithCommaAndQuotes();
            testMoveFileAndAppendMetadata();
            testDeleteFileAndAppendMetadata();
            testArchiveZipCreatesArchiveAndMetadata();

            System.out.println(" All MetadataManager tests finished.");
        } catch (AssertionError e) {
            System.err.println("MetadataManager test failed: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println(" IOException during MetadataManager tests: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ---------- TEST 1 ----------
    private static void testAppendCreatesMetadataRow() throws IOException {
        Path meta = Files.createTempFile("meta_test_", ".csv");
        Path f = Files.createTempFile("sample_", ".txt");

        MetadataManager.append(meta, f, "LOG", "created");

        String content = Files.readString(meta);
        assert content.contains("LOG") : "Metadata row missing kind";
        assert content.contains("created") : "Metadata row missing notes";

        System.out.println("Test 1 passed  (append creates metadata row)");
    }

    // ---------- TEST 2 ----------
    private static void testEscapeWithCommaAndQuotes() throws IOException {
        Path meta = Files.createTempFile("meta_escape_", ".csv");
        Path f = Files.createTempFile("file_with_,quotes", ".txt");

        MetadataManager.append(meta, f, "TYPE", "note, with \"quotes\"");
        String content = Files.readString(meta, StandardCharsets.UTF_8);

        // Expect double quotes around fields containing commas/quotes
        assert content.contains("\"") : "Escaping did not add quotes";
        assert content.contains("\"\"") : "Escaping did not double inner quotes";

        System.out.println("Test 2 passed  (escape handles commas/quotes)");
    }

    // ---------- TEST 3 ----------
    private static void testMoveFileAndAppendMetadata() throws IOException {
        Path source = Files.createTempFile("move_source_", ".txt");
        Files.writeString(source, "move test", StandardCharsets.UTF_8);
        Path target = Files.createTempFile("move_target_", ".txt");
        Files.deleteIfExists(target); // ensure it’s clean
        Path meta = Files.createTempFile("meta_move_", ".csv");

        MetadataManager.move(source, target, meta);

        assert Files.exists(target) : "File not moved successfully";
        String metaContent = Files.readString(meta);
        assert metaContent.contains("MOVE") : "Metadata missing MOVE entry";

        System.out.println("Test 3 passed  (move and append metadata)");
    }

    // ---------- TEST 4 ----------
    private static void testDeleteFileAndAppendMetadata() throws IOException {
        Path file = Files.createTempFile("delete_me_", ".txt");
        Files.writeString(file, "delete test");
        Path meta = Files.createTempFile("meta_delete_", ".csv");

        MetadataManager.delete(file, meta);

        assert !Files.exists(file) : "File not deleted";
        String metaContent = Files.readString(meta);
        assert metaContent.contains("DELETE") : "Metadata missing DELETE entry";

        System.out.println("Test 4 passed (delete and append metadata)");
    }

    // ---------- TEST 5 ----------
    private static void testArchiveZipCreatesArchiveAndMetadata() throws IOException {
        Path sourceDir = Files.createTempDirectory("archive_src_");
        Path file1 = Files.writeString(sourceDir.resolve("a.txt"), "A content");
        Path file2 = Files.writeString(sourceDir.resolve("b.txt"), "B content");

        Path zipTarget = Files.createTempFile("archive_out_", ".zip");
        Files.deleteIfExists(zipTarget); // ensure clean
        Path meta = Files.createTempFile("meta_archive_", ".csv");

        Path resultZip = MetadataManager.archiveZip(sourceDir, zipTarget, meta);

        assert Files.exists(resultZip) : "Zip archive not created";
        try (ZipFile zip = new ZipFile(resultZip.toFile())) {
            assert zip.getEntry("a.txt") != null : "a.txt missing in archive";
            assert zip.getEntry("b.txt") != null : "b.txt missing in archive";
        }

        String metaContent = Files.readString(meta);
        assert metaContent.contains("ARCHIVE") : "Metadata missing ARCHIVE entry";

        System.out.println("Test 5 passed  (archiveZip creates zip + metadata)");
    }
}
