package med.supply.system.model;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import med.supply.system.exception.ExceptionHandler;
import med.supply.system.util.LogManager;

public class ChargingStation {

    private final String id;
    private final String name;
    private boolean inUse;

    private static LogManager logger;

    public static void attachLogger(LogManager logManager) {
        logger = logManager;
    }

    // FIFO queue
    public static final Queue<StorageVehicle> WAITING_QUEUE = new LinkedList<>();
    public static final Map<StorageVehicle, Long> QUEUE_TIME = new HashMap<>();

    private static final long FIFTEEN_MIN = 15 * 60 * 1000;

    public static final List<ChargingStation> DEFAULT_STATIONS = new CopyOnWriteArrayList<>(List.of(
            new ChargingStation("CHG-DEFAULT-1", "Default_Station_1"),
            new ChargingStation("CHG-DEFAULT-2", "Default_Station_2"),
            new ChargingStation("CHG-DEFAULT-3", "Default_Station_3")
    ));

    public ChargingStation(String id, String name) throws ExceptionHandler.InvalidChargingStationException {

        if (id == null || id.isBlank())
            throw new ExceptionHandler.InvalidChargingStationException("Charging Station ID cannot be blank");

        if (name == null || name.isBlank())
            throw new ExceptionHandler.InvalidChargingStationException("Charging Station name cannot be blank");

        this.id = id.trim();
        this.name = name.trim();
        this.inUse = false;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public boolean isInUse() { return inUse; }

    public synchronized void occupy() { this.inUse = true; }
    public synchronized void release() { this.inUse = false; }

    // ============================================================
    // Queue Logic WITH CHARGING LOGGING
    // ============================================================

    private static void logCharge(String station, String msg) {
        try {
            if (logger != null)
                logger.logCharging(station, msg);
        } catch (Exception ignored) {}
    }

    public static synchronized void requestCharge(StorageVehicle v) {

        boolean anyFree = DEFAULT_STATIONS.stream().anyMatch(s -> !s.isInUse());

        // Immediate charging if free
        if (anyFree) {
            ChargingStation free = DEFAULT_STATIONS.stream()
                    .filter(s -> !s.isInUse())
                    .findFirst()
                    .orElse(null);

            if (free != null) {
                WAITING_QUEUE.remove(v);
                QUEUE_TIME.remove(v);

                v.waitingForCharge = false;
                v.leftQueue = false;

                logCharge(free.name,
                        v.getName() + " immediately assigned to " + free.name);

                v.startChargingFromQueue(free);
                return;
            }
        }

        // Queue
        if (!WAITING_QUEUE.contains(v)) {
            WAITING_QUEUE.add(v);
            QUEUE_TIME.put(v, System.currentTimeMillis());
            v.waitingForCharge = true;
            v.leftQueue = false;

            logCharge("QUEUE",
                    v.getName() + " added to charging queue.");
        }

        processQueue();
    }

    public static synchronized void processQueue() {
        long now = System.currentTimeMillis();
        Iterator<StorageVehicle> iterator = WAITING_QUEUE.iterator();

        while (iterator.hasNext()) {
            StorageVehicle v = iterator.next();
            long waited = now - QUEUE_TIME.getOrDefault(v, now);

            ChargingStation free = DEFAULT_STATIONS.stream()
                    .filter(s -> !s.isInUse())
                    .findFirst()
                    .orElse(null);

            // No station free → keep waiting or time out
            if (free == null) {
                if (waited < FIFTEEN_MIN) {
                    long left = (FIFTEEN_MIN - waited) / 1000;

                    logCharge("QUEUE",
                            v.getName() + " waiting (" + left + " sec left)");
                    continue;
                }

                iterator.remove();
                QUEUE_TIME.remove(v);
                v.leftQueue = true;
                v.waitingForCharge = false;

                logCharge("QUEUE",
                        v.getName() + " left queue after 15 minutes timeout.");

                return;
            }

            // Found free station → start charging
            iterator.remove();
            QUEUE_TIME.remove(v);

            v.waitingForCharge = false;
            v.leftQueue = false;

            logCharge(free.name,
                    v.getName() + " is now charging at " + free.name);

            v.startChargingFromQueue(free);
            return;
        }
    }

    @Override
    public String toString() {
        return "ChargingStation{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", status=" + (inUse ? "IN_USE" : "FREE") +
                '}';
    }
}
