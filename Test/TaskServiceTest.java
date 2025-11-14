import med.supply.system.exception.ExceptionHandler;
import med.supply.system.model.*;
import med.supply.system.repository.Repository;
import med.supply.system.service.*;
import med.supply.system.util.*;

import java.io.IOException;

public class TaskServiceTest {

    public static void main(String[] args) throws Exception {
        System.out.println("Running TaskService tests...");

        PathsConfig cfg = new PathsConfig();
        cfg.ensure();

        Repository repo = new Repository();
        LogManager logs = new LogManager(cfg);
        StorageService storage = new StorageService(repo, logs);
        DestinationService dest = new DestinationService();
        TaskService tasks = new TaskService(repo, logs);

        tasks.attachStorage(storage);
        tasks.attachDestination(dest);

        testCreateTask_basic(tasks, repo, storage);
        testCreateManualTaskWithItem(tasks, repo, storage);
        testCreateTask_invalidVehicle(tasks);
        testTaskUpdateStatus(tasks, repo, storage);
        testAutoDistribute(tasks, repo, storage);

        System.out.println("All TaskService tests completed.");
    }

    // ===========================================================
    // TEST 1: BASIC TASK CREATION
    // ===========================================================
    private static void testCreateTask_basic(TaskService tasks,
                                             Repository repo,
                                             StorageService storage) throws Exception {

        StorageVehicle v = new StorageVehicle("V100", "AGV_Test");
        storage.addVehicle(v);

        Task t = new Task("T-001", "Deliver gloves", "V100");
        tasks.createTask(t);

        assert repo.tasks.containsKey("T-001") : "Task not added to repository";
        assert t.assigneeVehicleId.equals("V100") : "Task vehicle mismatch";

        System.out.println("Test 1 passed (createTask basic)");
    }

    // ===========================================================
    // TEST 2: MANUAL TASK CREATION WITH ITEM
    // ===========================================================
    private static void testCreateManualTaskWithItem(TaskService tasks,
                                                     Repository repo,
                                                     StorageService storage) throws Exception {

        StorageVehicle v = new StorageVehicle("V200", "AGV_Manual");
        storage.addVehicle(v);

        StorageItem it = new StorageItem("SKU500", "Medicines", 40);
        storage.addItem(it);

        Task t = new Task("T-MAN-1", "Manual delivery", "V200");
        tasks.createManualTaskWithItem(t, "SKU500");

        assert repo.tasks.containsKey("T-MAN-1") : "Manual task not created";
        assert !v.getInventory().isEmpty() : "Manual task did not load item into vehicle";
        assert v.getInventory().containsKey("SKU500") : "Vehicle missing loaded item";

        System.out.println("Test 2 passed (createManualTaskWithItem)");
    }

    // ===========================================================
    // TEST 3: INVALID VEHICLE FOR TASK CREATION
    // ===========================================================
    private static void testCreateTask_invalidVehicle(TaskService tasks) {
        try {
            Task t = new Task("T-ERR", "Bad task", "NO_VEHICLE");
            tasks.createTask(t);
            assert false : "Expected IllegalArgumentException not thrown";
        } catch (IllegalArgumentException | IOException e) {
            System.out.println("Test 3 passed (invalid vehicle check)");
        }
    }

    // ===========================================================
    // TEST 4: UPDATE TASK STATUS (NO WAITING)
    // ===========================================================
    private static void testTaskUpdateStatus(TaskService tasks,
                                             Repository repo,
                                             StorageService storage) throws Exception {

        StorageVehicle v = new StorageVehicle("V300", "AGV_Status");
        storage.addVehicle(v);

        StorageItem it = new StorageItem("SKU700", "Serum", 10);
        storage.addItemToVehicle("V300", it);

        Task t = new Task("T-STATUS", "Deliver Serum", "V300");
        repo.tasks.put("T-STATUS", t);

        tasks.updateStatus("T-STATUS", TaskStatus.IN_PROGRESS);

        assert t.status == TaskStatus.IN_PROGRESS : "Task status not updated";

        System.out.println("Test 4 passed (updateStatus basic)");
    }

    // ===========================================================
    // TEST 5: AUTO-DISTRIBUTE STARTUP CHECK
    // ===========================================================
    private static void testAutoDistribute(TaskService tasks,
                                           Repository repo,
                                           StorageService storage) {

        StorageVehicle v1 = new StorageVehicle("AGV1", "Auto1");
        StorageVehicle v2 = new StorageVehicle("AGV2", "Auto2");

        try {
            storage.addVehicle(v1);
            storage.addVehicle(v2);
        } catch (IOException ignored) {}

        v1.setBatteryLevelPct(80);
        v2.setBatteryLevelPct(90);

        storage.getUnassignedItemsRef().put("SKU900",
                new StorageItem("SKU900", "Masks", 30));

        tasks.autoDistribute("MASTER-01");

        assert !repo.tasks.isEmpty() : "Auto-distribute did not create tasks";

        System.out.println("Test 5 passed (autoDistribute initial)");
    }
}
