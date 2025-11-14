import med.supply.system.model.StorageItem;
import med.supply.system.model.StorageVehicle;
import med.supply.system.service.DataExchangeSimulator;
import med.supply.system.util.LogManager;
import med.supply.system.util.PathsConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

public class DataExchangeSimulatorTest {

    private static void assertTrue(boolean condition, String message) {
        assert condition : " " + message;
    }

    private static void assertEquals(Object actual, Object expected) {
        assert actual.equals(expected)
                : "Expected '" + expected + "' but got '" + actual + "'";
    }

    public static void main(String[] args) {
        System.out.println("Running DataExchangeSimulator tests...\n");

        try {
            // Setup
            PathsConfig cfg = new PathsConfig();
            cfg.ensure();
            LogManager logs = new LogManager(cfg);

            // ============= TEST 1 =============
            System.out.println("Test 1: Simulation creates TXT & BIN files");
            StorageVehicle v1 = new StorageVehicle("VH-001", "VanTest1");
            v1.addItem(new StorageItem("SKU1", "Bandage", 5));

            DataExchangeSimulator sim1 = new DataExchangeSimulator(cfg, logs);
            sim1.simulate(v1);

            Path txt1 = cfg.exchangeRoot.resolve("exchange_VH-001.txt");
            Path bin1 = cfg.exchangeRoot.resolve("exchange_VH-001.bin");

            assertTrue(Files.exists(txt1), "TXT file not created");
            assertTrue(Files.exists(bin1), "BIN file not created");

            System.out.println("Test 1 passed\n");

            // ============= TEST 2 =============
            System.out.println("Test 2: TXT contains expected payload text");
            String text = Files.readString(txt1);
            assertTrue(text.contains("VEHICLE=VH-001"), "TXT missing vehicle ID");
            assertTrue(text.contains("SKU=SKU1"), "TXT missing SKU");
            assertTrue(text.contains("QTY=5"), "TXT missing quantity");

            System.out.println("Test 2 passed\n");

            // ============= TEST 3 =============
            System.out.println("Test 3: BIN file roundtrip text matches original payload");
            // Read BIN file manually
            byte[] data = Files.readAllBytes(bin1);
            // skip first 4 bytes (length header)
            int length = ((data[0] & 255) << 24) | ((data[1] & 255) << 16) | ((data[2] & 255) << 8) | ((data[3] & 255));
            String roundtrip = new String(data, 4, length, StandardCharsets.UTF_8);

            assertTrue(roundtrip.contains("SKU=SKU1"), "Binary roundtrip missing SKU");
            assertTrue(roundtrip.contains("VEHICLE=VH-001"), "Binary roundtrip missing vehicle ID");

            System.out.println("Test 3 passed\n");


            // ============= TEST 4 =============
            System.out.println("Test 4: Vehicle with no items does NOT throw & creates no files");
            StorageVehicle v2 = new StorageVehicle("VH-EMPTY", "VanEmpty");
            DataExchangeSimulator sim2 = new DataExchangeSimulator(cfg, logs);
            sim2.simulate(v2);

            Path txt2 = cfg.exchangeRoot.resolve("exchange_VH-EMPTY.txt");
            Path bin2 = cfg.exchangeRoot.resolve("exchange_VH-EMPTY.bin");

            assertTrue(!Files.exists(txt2), "TXT should NOT exist for empty vehicle");
            assertTrue(!Files.exists(bin2), "BIN should NOT exist for empty vehicle");

            System.out.println("Test 4 passed\n");


            // ============= TEST 5 =============
            System.out.println("Test 5: previewFirstLineWithHandler returns correct first line or error");
            String firstLine = sim1.previewFirstLineWithHandler(txt1);
            assertTrue(firstLine.contains("EVENT="), "preview did not return first line");

            String badPathResult = sim1.previewFirstLineWithHandler(Path.of("nonexistent_file.xyz"));
            assertTrue(badPathResult.startsWith("[ERROR]"), "preview did not return error prefix");

            System.out.println("Test 5 passed\n");


        } catch (AssertionError e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println("IO Error: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Unexpected error: " + e.getMessage());
        }

        System.out.println("\nAll DataExchangeSimulator tests completed");
    }
}
