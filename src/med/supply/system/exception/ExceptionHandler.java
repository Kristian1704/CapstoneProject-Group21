package med.supply.system.exception;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

public class ExceptionHandler {

    // a) Handling Multiple Exceptions
    public static void moveLogFile(Path source, Path destination) throws Exception {
        try {
            if (destination.getParent() != null) {
                Files.createDirectories(destination.getParent());
            }
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | SecurityException | UnsupportedOperationException e) {
            throw new Exception("Failed to move log file: " + source + " -> " + destination, e);
        }
    }


    // c) Resource Management (try-with-resources)
    public static String readFirstLine(Path filePath) throws Exception {
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            return reader.readLine();
        } catch (IOException e) {
            throw new Exception("Failed to read file: " + filePath, e);
        }
    }

    // d) Chaining Exceptions
    public static void scheduleTask(String taskId, String vehicleId) throws Exception {
        try {
            Objects.requireNonNull(taskId, "Task ID cannot be null");
            Objects.requireNonNull(vehicleId, "Vehicle ID cannot be null");
            simulateDatabaseInsert(taskId, vehicleId);
        } catch (RuntimeException e) {
            throw new Exception("Failed to schedule task: " + taskId + " for " + vehicleId, e);
        }
    }

    private static void simulateDatabaseInsert(String taskId, String vehicleId) {
        throw new IllegalStateException("This is for testing chaining exception ");
    }
    public static void handleVehicleNotFound(String vehicleId) throws Exception {
        throw new Exception("Vehicle not found: " + vehicleId);
    }
    public static void handleInvalidTaskStatus(String status, Exception cause) throws Exception {
        throw new Exception("Invalid task status entered: " + status, cause);
    }
    public static void handleTaskNotFound(String taskId, Exception cause) throws Exception {
        throw new Exception("Task not found during update: " + taskId, cause);
    }

    public static class InvalidChargingStationException extends RuntimeException {
        public InvalidChargingStationException(String message) {
            super(message);
        }
    }
    public static class InvalidStorageItemException extends RuntimeException {
        public InvalidStorageItemException(String message) {
            super(message);
        }
    }

    // Custom exception for invalid StorageVehicle properties
    public static class InvalidStorageVehicleException extends RuntimeException {
        public InvalidStorageVehicleException(String message) {
            super(message);
        }
    }
    public static class NullItemException extends RuntimeException {
        public NullItemException(String message) { super(message); }
    }

    public static class VehicleNotFoundException extends RuntimeException {
        public VehicleNotFoundException(String message) { super(message); }
    }
    public static class InvalidNameException extends RuntimeException {
        public InvalidNameException(String message) { super(message); }
    }
}
