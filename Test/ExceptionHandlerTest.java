import med.supply.system.exception.ExceptionHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public class ExceptionHandlerTest {
    public static void main(String[] args) {
        System.out.println("Running ExceptionHandler tests...");

        try {
            testMoveLogFile();
            testReadFirstLine();
            testScheduleTaskChainedException();
            testCustomHandlerWrapsCause();

            System.out.println("All ExceptionHandler tests finished.");
        } catch (AssertionError e) {
            System.err.println(" Assertion failed: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println(" Exception during tests: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ---------- TEST 1 ----------
    private static void testMoveLogFile() throws Exception {
        Path src = Files.createTempFile("move_src_", ".log");
        Files.writeString(src, "Log content");
        Path dst = Files.createTempFile("move_dst_", ".log");
        Files.deleteIfExists(dst);

        ExceptionHandler.moveLogFile(src, dst);
        assert Files.exists(dst) : "File not moved correctly";
        assert !Files.exists(src) : "Source file still exists";

        // test exception case — move from nonexistent file
        Path fakeSrc = Paths.get("nonexistent_file.log");
        try {
            ExceptionHandler.moveLogFile(fakeSrc, dst);
            assert false : "Expected exception not thrown for missing source";
        } catch (Exception e) {
            assert e.getMessage().contains("Failed to move log file") : "Wrong exception message";
        }

        System.out.println("Test 1 passed  (moveLogFile)");
    }

    // ---------- TEST 2 ----------
    private static void testReadFirstLine() throws Exception {
        Path f = Files.createTempFile("read_test_", ".txt");
        Files.writeString(f, "First line\nSecond line", StandardCharsets.UTF_8);

        String line = ExceptionHandler.readFirstLine(f);
        assert line.equals("First line") : "Did not read correct first line";

        // test exception case — missing file
        Path fake = Paths.get("no_such_file.txt");
        try {
            ExceptionHandler.readFirstLine(fake);
            assert false : "Expected exception not thrown for missing file";
        } catch (Exception e) {
            assert e.getMessage().contains("Failed to read file") : "Wrong exception message";
        }

        System.out.println("Test 2 passed  (readFirstLine)");
    }

    // ---------- TEST 3 ----------
    private static void testScheduleTaskChainedException() {
        try {
            ExceptionHandler.scheduleTask("T001", "Van_A");
            assert false : "Expected chained exception not thrown";
        } catch (Exception e) {
            assert e.getMessage().contains("Failed to schedule task") : "Wrong top-level message";
            assert e.getCause() instanceof IllegalStateException : "Cause should be IllegalStateException";
            assert e.getCause().getMessage().contains("Database connection failed(This is for testing chaining exception)") : "Cause message mismatch";
            System.out.println("Test 3 passed  (scheduleTask chained exception)");
        }
    }

    // ---------- TEST 4 ----------
    private static void testCustomHandlerWrapsCause() {
        Exception base = new IllegalArgumentException("Underlying reason");
        try {
            ExceptionHandler.handleInvalidTaskStatus("INVALID", base);
            assert false : "Expected exception not thrown from custom handler";
        } catch (Exception e) {
            assert e.getMessage().contains("Invalid task status entered") : "Missing top-level handler message";
            assert e.getCause() == base : "Cause not preserved in custom handler";
            System.out.println("Test 4 passed  (custom handler wraps cause)");
        }
    }


}
 