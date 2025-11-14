package med.supply.system.model;

import med.supply.system.service.TaskService;
import med.supply.system.util.LogManager;

import java.util.HashMap;
import java.util.Map;
import med.supply.system.exception.ExceptionHandler;

/**
 * Represents an automated storage vehicle that can store and transfer items.
 * Includes automatic charging behavior with multiple default charging stations
 * and a shared FIFO queue system.
 */
public class StorageVehicle implements Runnable {

    private final String id;
    private final String name;
    private int batteryLevelPct = 20;

    private ChargingStation assignedStation;
    private final Map<String, StorageItem> inventory = new HashMap<>();

    private boolean isCharging = false;
    private transient LogManager logger;
    private Thread chargingThread;

    // Flags controlled by ChargingStation
    boolean waitingForCharge = false;
    boolean leftQueue = false;

    private static final int MAX_CAPACITY = 50;  // Maximum number of items a vehicle can hold

    // Callback to TaskService for auto-resume after charging
    private static TaskService taskServiceCallback;

    /**
     * Called once (from TaskService constructor) to register the callback.
     */
    public static void registerTaskService(TaskService ts) {
        taskServiceCallback = ts;
    }

    // ---------------------------------------------------

    public StorageVehicle(String id, String name) {
        if (id == null || id.isBlank())
            throw new ExceptionHandler.InvalidStorageVehicleException("Vehicle ID cannot be blank");

        if (name == null || name.isBlank())
            throw new ExceptionHandler.InvalidStorageVehicleException("Vehicle name cannot be blank");

        this.id = id.trim();
        this.name = name.trim();

        // Trigger automatic charging logic if battery <= 14
        setBatteryLevelPct(batteryLevelPct);
    }

    public void attachLogger(LogManager logManager) {
        this.logger = logManager;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getBatteryLevelPct() { return batteryLevelPct; }
    public boolean isCharging() { return isCharging; }
    public ChargingStation getAssignedStation() { return assignedStation; }
    public boolean isWaitingForCharge() { return waitingForCharge; }
    public boolean hasLeftQueue() { return leftQueue; }
    public Map<String, StorageItem> getInventory() { return inventory; }

    // ============================================================
    // Battery + Charging Logic
    // ============================================================

    public void setBatteryLevelPct(int pct) {
        if (pct < 0 || pct > 100)
            throw new ExceptionHandler.InvalidStorageVehicleException("Battery level must be 0–100");


        this.batteryLevelPct = pct;

        // Start charging automatically if too low
        if (pct <= 14 && !isCharging) {
            goCharge();
            return;
        }

        // Stop charging when fully charged
        if (isCharging && pct >= 100) {
            finishCharging();
        }
    }

    private void goCharge() {
        ChargingStation.requestCharge(this);
    }

    public void startChargingFromQueue(ChargingStation station) {
        this.waitingForCharge = false;
        this.leftQueue = false;
        startCharging(station);
    }

    private void startCharging(ChargingStation station) {
        this.assignedStation = station;
        station.occupy();
        isCharging = true;

        String msg = " Battery low (" + batteryLevelPct + "%). " + name +
                " going to " + station.getName() + " for charging...";
        System.out.println(msg);
        log(msg);

        chargingThread = new Thread(this);
        chargingThread.start();
    }

    private void finishCharging() {
        // guard in case it's called twice
        if (!isCharging) return;

        isCharging = false;

        if (assignedStation != null) {
            assignedStation.release();
            System.out.println(" " + assignedStation.getName() + " is now FREE.");
        }

        String msg = " " + name + " fully charged and ready to resume tasks.";
        System.out.println(msg);
        log(msg);

        assignedStation = null;

        // Notify queue to process next vehicle
        ChargingStation.processQueue();

        // After finishing charging → resume auto-distribution if active
        if (taskServiceCallback != null) {
            taskServiceCallback.tryResumeAutoDistributeAsync();
        }
    }

    @Override
    public void run() {
        try {
            // Loop only on isCharging; finishCharging() will turn this off
            while (isCharging) {
                Thread.sleep(20_000); // every 20 seconds +5%

                if (!isCharging) break;

                // Increase battery but never above 100
                batteryLevelPct = Math.min(100, batteryLevelPct + 5);

                String msg = "🔌 " + name + " charging at " + assignedStation.getName() +
                        "... Battery now " + batteryLevelPct + "%";
                System.out.println(msg);
                log(msg);

                // When we reach 100%, stop charging
                if (batteryLevelPct >= 100) {
                    finishCharging();
                    break;
                }
            }
        } catch (InterruptedException e) {
            System.err.println("Charging interrupted for " + name);
            Thread.currentThread().interrupt();
        }
    }

    private void log(String msg) {
        if (logger != null) {
            try {
                logger.logVehicle(name, msg);
            } catch (Exception e) {
                System.err.println("[LOG ERROR] " + e.getMessage());
            }
        }
    }

    // ============================================================
    // Inventory
    // ============================================================

    public void addItem(StorageItem item) {
        if (item == null)
            throw new ExceptionHandler.InvalidStorageVehicleException("Item cannot be null");

        if (hasCapacity()) {
            inventory.merge(item.getSku(), item, (a, b) -> {
                a.setQuantity(a.getQuantity() + b.getQuantity());
                return a;
            });
        } else {
            System.out.println(" " + name + " has no capacity to add more items.");
        }
    }

    public boolean hasCapacity() {
        return inventory.size() < MAX_CAPACITY;
    }

    public int remainingCapacity() {
        return MAX_CAPACITY - inventory.size();
    }
    public static int getMaxCapacity() {
        return MAX_CAPACITY;
    }

    @Override
    public String toString() {
        return "StorageVehicle{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", battery=" + batteryLevelPct + "%" +
                (assignedStation != null ? ", station='" + assignedStation.getName() + "'" : "") +
                (isCharging ? ", charging=true" : "") +
                '}';
    }

    public void setAssignedStation(ChargingStation station) {
        this.assignedStation = station;
        System.out.println("" + name + " assigned to " +
                (station != null ? station.getName() : "no station") +
                " (will use it when charging is needed).");
    }
}
