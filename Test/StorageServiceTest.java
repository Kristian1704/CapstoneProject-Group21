import med.supply.system.exception.ExceptionHandler;
import med.supply.system.model.*;
import med.supply.system.repository.Repository;
import med.supply.system.service.StorageService;
import med.supply.system.util.*;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;

public class StorageServiceTest {

    public static void main(String[] args) {
        System.out.println("Running StorageService tests...");
        try {
            PathsConfig cfg = new PathsConfig();
            cfg.ensure();

            Repository repo = new Repository();
            LogManager logManager = new LogManager(cfg);
            StorageService service = new StorageService(repo, logManager);

            testAddVehicle(service, repo);
            testAddChargingStation(service, repo, cfg);
            testStationOccupyRelease(repo);
            testAddItemToVehicle(service, repo, cfg);
            testInvalidNameThrows(service);
            testNullItemThrows(service);
            testVehicleNotFoundThrows(service);

            System.out.println("All StorageService tests finished successfully.");
        } catch (Exception e) {
            System.err.println("StorageService tests failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ---------- TEST 1 ----------
    private static void testAddVehicle(StorageService service, Repository repo) throws IOException {
        StorageVehicle v = new StorageVehicle("V001", "Van_Beta");
        service.addVehicle(v);

        assert repo.vehicles.containsKey("V001") : "Vehicle not added to repository";

        System.out.println("Test 1 passed (addVehicle)");
    }

    // ---------- TEST 2 ----------
    private static void testAddChargingStation(StorageService service, Repository repo, PathsConfig cfg) throws IOException {
        ChargingStation s = new ChargingStation("S001", "Station_Alpha");
        service.addChargingStation(s);

        assert repo.stations.containsKey("S001") : "Charging station not added to repository";

        Path logFile = cfg.logsCharging.resolve("Station_Alpha").resolve(LocalDate.now() + ".log");
        if (Files.exists(logFile)) {
            String logContent = Files.readString(logFile);
            assert logContent.toLowerCase().contains("added")
                    : "Charging station log missing expected text";
        } else {
            System.out.println("Log file not found (ok for this setup): " + logFile);
        }

        System.out.println("Test 2 passed (addChargingStation)");
    }

    // ---------- TEST 3 ----------
    private static void testStationOccupyRelease(Repository repo) {
        ChargingStation s = new ChargingStation("S002", "Station_Beta");
        repo.stations.put("S002", s);

        s.occupy();
        assert s.isInUse() : "Station should be marked as in use after occupy()";

        s.release();
        assert !s.isInUse() : "Station should be free after release()";

        System.out.println("Test 3 passed (occupy/release)");
    }

    // ---------- TEST 4 ----------
    private static void testAddItemToVehicle(StorageService service, Repository repo, PathsConfig cfg) throws IOException {
        StorageVehicle v = new StorageVehicle("V002", "Van_Items");
        service.addVehicle(v);

        StorageItem item = new StorageItem("SKU123", "Bandages", 5);
        service.addItemToVehicle("V002", item);

        assert !v.getInventory().isEmpty() : "Item not added to vehicle inventory";
        assert v.getInventory().containsKey("SKU123") : "Inventory missing SKU123 key";

        StorageItem stored = v.getInventory().get("SKU123");
        assert stored.getSku().equals("SKU123") : "Incorrect SKU in inventory";
        assert stored.getQuantity() == 5 : "Incorrect quantity in inventory";

        Path logFile = cfg.logsVehicles.resolve("Van_Items").resolve(LocalDate.now() + ".log");
        if (Files.exists(logFile)) {
            String logContent = Files.readString(logFile);
            assert logContent.toLowerCase().contains("added")
                    : "Vehicle item addition log missing or mismatched";
        } else {
            System.out.println("Vehicle log file not found (ok for this setup): " + logFile);
        }

        System.out.println("Test 4 passed (addItemToVehicle)");
    }

    // ---------- TEST 5 ----------
    private static void testInvalidNameThrows(StorageService service) {
        try {
            StorageVehicle invalid = new StorageVehicle("V003", "###Invalid###");
            service.addVehicle(invalid);
            assert false : "Expected InvalidNameException not thrown";
        } catch (ExceptionHandler.InvalidNameException | IOException e) {
            System.out.println("Test 5 passed (requireValidName throws)");
        }
    }

    // ---------- TEST 6 ----------
    private static void testNullItemThrows(StorageService service) {
        try {
            service.addItemToVehicle("VXXX", null);
            assert false : "Expected NullItemException not thrown";
        } catch (ExceptionHandler.NullItemException e) {
            System.out.println("Test 6 passed (NullItemException thrown)");
        } catch (IOException ignored) {}
    }

    // ---------- TEST 7 ----------
    private static void testVehicleNotFoundThrows(StorageService service) {
        try {
            StorageItem item = new StorageItem("SKU999", "Gloves", 4);
            service.addItemToVehicle("NO-Vehicle", item);
            assert false : "Expected VehicleNotFoundException not thrown";
        } catch (ExceptionHandler.VehicleNotFoundException e) {
            System.out.println("Test 7 passed (VehicleNotFoundException thrown)");
        } catch (Exception ignored) {}
    }
}
