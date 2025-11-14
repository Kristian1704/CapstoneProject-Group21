package med.supply.system;

import med.supply.system.exception.ExceptionHandler;
import med.supply.system.model.*;
import med.supply.system.repository.Repository;
import med.supply.system.service.*;
import med.supply.system.util.*;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {

        PathsConfig cfg = new PathsConfig();
        cfg.ensure();
        LogManager log = new LogManager(cfg);
        Repository repo = new Repository();

        // Services
        StorageService storage = new StorageService(repo, log);
        TaskService tasks = new TaskService(repo, log);
        DestinationService dst = new DestinationService();

        // Wire services
        tasks.attachStorage(storage);
        tasks.attachDestination(dst);
        storage.attachTaskService(tasks);

        DataExchangeSimulator exchange = new DataExchangeSimulator(cfg, log);

        try (Scanner sc = new Scanner(System.in)) {
            while (true) {
                System.out.println("\n=== Medical Supplies System ===");
                System.out.println("1) List vehicles & inventory");
                System.out.println("2) List charging stations");
                System.out.println("3) Add vehicle");
                System.out.println("4) Add charging station");
                System.out.println("5) Add item");
                System.out.println("6) List all storage items");
                System.out.println("7) Update charging load");
                System.out.println("8) Create task");
                System.out.println("9) Update task status");
                System.out.println("10) Simulate data exchange");
                System.out.println("11) Archive logs to ZIP");
                System.out.println("12) Move log file");
                System.out.println("13) Delete log file");
                System.out.println("14) List all tasks");
                System.out.println("15) Assign vehicle to charging station");
                System.out.println("16) Test auto-charge");
                System.out.println("0) Exit");
                System.out.print("Choose: ");

                String c = sc.nextLine().trim();

                try {
                    switch (c) {
                        case "1" -> listVehicles(repo);
                        case "2" -> listStations(repo);
                        case "3" -> addVehicleUI(sc, storage);
                        case "4" -> addChargingUI(sc, storage);
                        case "5" -> addItemUI(sc, storage);
                        case "6" -> storage.listAllItems();
                        case "8" -> createTaskUI(sc, tasks);
                        case "9" -> updateTaskUI(sc, tasks);
                        case "10" -> simulateExchange(sc, repo, cfg, exchange);
                        case "11" -> archiveLogs(cfg);
                        case "12" -> moveFileUI(sc, cfg);
                        case "13" -> deleteFileUI(sc, cfg);
                        case "14" -> listTasks(repo);
                        case "15" -> assignVehicleToStation(sc, repo);
                        case "16" -> testCharging(sc, repo);
                        case "0" -> {
                            System.out.println("Bye.");
                            tasks.shutdown();
                            return;
                        }
                        default -> System.out.println("Invalid choice.");
                    }
                } catch (Exception ex) {
                    System.out.println("ERROR: " + ex.getMessage());
                }
            }
        }
    }

    // ----------------------- UI Functions -----------------------

    private static void createTaskUI(Scanner sc, TaskService tasks) throws IOException {
        System.out.println("Task Type:");
        System.out.println("  1) AUTO-DISTRIBUTE available items");
        System.out.println("  2) Manual task");
        System.out.print("Choose: ");
        String mode = sc.nextLine().trim();

        if (mode.equals("1")) {
            String id = "AUTO-" + System.currentTimeMillis();
            tasks.createTask(new Task(id, "Auto-distribute items", null));
            tasks.autoDistribute(id);
            System.out.println("Auto-distribute started (deliveries will appear in resources/destination_log.csv).");
            return;
        }

        if (mode.equals("2")) {
            System.out.print("Task ID: ");
            String id = sc.nextLine().trim();
            System.out.print("Description: ");
            String d = sc.nextLine().trim();
            System.out.print("Vehicle ID (required): ");
            String v = sc.nextLine().trim();


            if (v.isBlank()) {
                throw new IllegalArgumentException("Vehicle ID is required to create a task.");
            }

            tasks.createTask(new Task(id, d, v));
            System.out.println("Task created for vehicle " + v + ".");
            return;
        }

        System.out.println("Invalid option.");
    }

    private static void updateTaskUI(Scanner sc, TaskService tasks) throws Exception {
        System.out.print("Task ID: ");
        String id = sc.nextLine().trim();
        System.out.print("New status (PENDING, IN_PROGRESS, DONE): ");
        String s = sc.nextLine().toUpperCase(Locale.ROOT);

        tasks.updateStatus(id, TaskStatus.valueOf(s));
        System.out.println("Updated.");
    }

    private static void addVehicleUI(Scanner sc, StorageService storage) throws IOException {
        System.out.print("Vehicle ID: ");
        String id = sc.nextLine().trim();
        System.out.print("Vehicle name: ");
        String name = sc.nextLine().trim();
        storage.addVehicle(new StorageVehicle(id, name));
    }

    private static void addChargingUI(Scanner sc, StorageService storage) throws IOException {
        System.out.print("Station ID: ");
        String id = sc.nextLine().trim();
        System.out.print("Station name: ");
        String name = sc.nextLine().trim();
        storage.addChargingStation(new ChargingStation(id, name));
    }


    private static void addItemUI(Scanner sc, StorageService storage) throws IOException {


        System.out.print("SKU: ");
        String sku = sc.nextLine().trim();

        System.out.print("Name: ");
        String nm = sc.nextLine().trim();

        System.out.print("Quantity: ");
        int qty = Integer.parseInt(sc.nextLine().trim());

        storage.addItem(new StorageItem(sku, nm, qty));
        System.out.println("Item created successfully.");
    }


    private static void simulateExchange(Scanner sc, Repository repo, PathsConfig cfg, DataExchangeSimulator exchange) throws Exception {
        System.out.print("Vehicle ID: ");
        String vid = sc.nextLine().trim();

        StorageVehicle v = repo.vehicles.get(vid);
        if (v == null) {
            ExceptionHandler.handleVehicleNotFound(vid);
            return;
        }

        if (v.getInventory().isEmpty()) {
            System.out.println("Inventory empty.");
            return;
        }

        exchange.simulate(v);
        System.out.println("Data exchange simulated.");
    }

    private static void archiveLogs(PathsConfig cfg) throws IOException {
        Path p = cfg.archiveRoot.resolve("logs-" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".zip");
        MetadataManager.archiveZip(cfg.logsRoot, p, cfg.metaIndex);
        System.out.println("Archived: " + p);
    }

    private static void moveFileUI(Scanner sc, PathsConfig cfg) throws IOException {
        System.out.print("From: ");
        Path from = Path.of(sc.nextLine());
        System.out.print("To: ");
        Path to = Path.of(sc.nextLine());
        new LogManager(cfg).archiveLogWithHandler(from, to);
    }

    private static void deleteFileUI(Scanner sc, PathsConfig cfg) throws IOException {
        System.out.print("File to delete: ");
        Path f = Path.of(sc.nextLine());
        MetadataManager.delete(f, cfg.metaIndex);
    }

    private static void listVehicles(Repository repo) {
        if (repo.vehicles.isEmpty()) {
            System.out.println("(none)");
            return;
        }
        for (StorageVehicle v : repo.vehicles.values()) {
            System.out.println(" - " + v);
            for (StorageItem it : v.getInventory().values()) {
                System.out.println("    * " + it);
            }
        }
    }

    private static void listStations(Repository repo) {
        if (repo.stations.isEmpty()) {
            System.out.println("(none)");
            return;
        }
        repo.stations.values().forEach(System.out::println);
    }

    private static void listTasks(Repository repo) {
        if (repo.tasks.isEmpty()) {
            System.out.println("(none)");
            return;
        }
        for (Task t : repo.tasks.values()) {
            System.out.println(" - " + t);
        }
    }

    private static void assignVehicleToStation(Scanner sc, Repository repo) {
        System.out.print("Vehicle ID: ");
        String vid = sc.nextLine().trim();
        StorageVehicle v = repo.vehicles.get(vid);
        if (v == null) {
            System.out.println("Not found.");
            return;
        }

        System.out.print("Station ID: ");
        String sid = sc.nextLine().trim();
        ChargingStation s = repo.stations.get(sid);
        if (s == null) {
            System.out.println("Station not found.");
            return;
        }

        v.setAssignedStation(s);
        System.out.println("Assigned.");
    }

    private static void testCharging(Scanner sc, Repository repo) {
        System.out.print("Vehicle ID: ");
        String vid = sc.nextLine().trim();
        StorageVehicle v = repo.vehicles.get(vid);
        if (v == null) {
            System.out.println("Not found.");
            return;
        }

        for (int pct = 100; pct >= 10; pct -= 10) {
            v.setBatteryLevelPct(pct);
            System.out.println("Battery: " + pct + " | charging=" + v.isCharging());
        }
    }
}
