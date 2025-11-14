import med.supply.system.util.*;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.List;

public class LogManagerTest {

    public static void main(String[] args) {
        System.out.println("Running LogManager tests...");
        PathsConfig cfg = new PathsConfig();
        try {
            cfg.ensure();
            LogManager logManager = new LogManager(cfg);

            testSystemLogging(logManager, cfg);
            testVehicleLogging(logManager, cfg);
            testChargingLogging(logManager, cfg);
            testFindByEquipment(logManager, cfg);
            testReadLog(logManager, cfg);

            System.out.println(" All LogManager tests finished.");
        } catch (Exception e) {
            System.err.println(" LogManager tests failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ---------- TEST 1 ----------
    private static void testSystemLogging(LogManager logManager, PathsConfig cfg) throws IOException {
        logManager.logSystem("System startup OK");
        Path expected = cfg.logsSystem.resolve(LocalDate.now().toString() + ".log");
        assert Files.exists(expected) : "System log file not created";
        String content = Files.readString(expected);
        assert content.contains("System startup OK") : "System log content missing expected text";
        System.out.println("Test 1 passed (logSystem)");
    }

    // ---------- TEST 2 ----------
    private static void testVehicleLogging(LogManager logManager, PathsConfig cfg) throws IOException {
        logManager.logVehicle("Van_Test", "Vehicle moved");
        Path expected = cfg.logsVehicles.resolve("Van_Test").resolve(LocalDate.now().toString() + ".log");
        assert Files.exists(expected) : "Vehicle log file not created";
        String content = Files.readString(expected);
        assert content.contains("Vehicle moved") : "Vehicle log content missing";
        assert content.contains("[VEHICLE:Van_Test]") : "Vehicle tag missing";
        System.out.println("Test 2 passed (logVehicle)");
    }

    // ---------- TEST 3 ----------
    private static void testChargingLogging(LogManager logManager, PathsConfig cfg) throws IOException {
        logManager.logCharging("Station_A", "Charging complete");
        Path expected = cfg.logsCharging.resolve("Station_A").resolve(LocalDate.now().toString() + ".log");
        assert Files.exists(expected) : "Charging log file not created";
        String content = Files.readString(expected);
        assert content.contains("Charging complete") : "Charging log content missing";
        assert content.contains("[CHARGING:Station_A]") : "Charging tag missing";
        System.out.println("Test 3 passed (logCharging)");
    }

    // ---------- TEST 4 ----------
    private static void testFindByEquipment(LogManager logManager, PathsConfig cfg) throws IOException {
        // first ensure a known file exists
        logManager.logVehicle("Van_TestFind", "Movement logged");
        List<Path> found = logManager.findByEquipmentOrDate("Van_TestFind");
        assert !found.isEmpty() : "findByEquipmentOrDate returned empty for existing vehicle";
        assert found.get(0).toString().contains("Van_TestFind") : "Returned path mismatch";
        System.out.println("Test 4 passed (findByEquipmentOrDate)");
    }

    // ---------- TEST 5 ----------
    private static void testReadLog(LogManager logManager, PathsConfig cfg) throws IOException {
        // create a temporary log
        Path logPath = cfg.logsSystem.resolve(LocalDate.now().toString() + "_readtest.log");
        Files.writeString(logPath, "Sample log line", StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String read = logManager.readLog(logPath);
        assert read.contains("Sample log line") : "readLog() did not read file content correctly";
        System.out.println("Test 5 passed (readLog)");
    }
}
