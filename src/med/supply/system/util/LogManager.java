package med.supply.system.util;

import med.supply.system.exception.ExceptionHandler;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class LogManager {
    private final PathsConfig cfg;
    private final DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public LogManager(PathsConfig cfg) {
        this.cfg = cfg;
    }

    private Path dailySystemLog(LocalDate date) {
        return cfg.logsSystem.resolve(df.format(date) + ".log");
    }

    private Path dailyVehicleLog(String vehicleName, LocalDate date) {
        return cfg.logsVehicles.resolve(vehicleName).resolve(df.format(date) + ".log");
    }

    private Path dailyChargingLog(String stationName, LocalDate date) {
        return cfg.logsCharging.resolve(stationName).resolve(df.format(date) + ".log");
    }

    public void logSystem(String line) throws IOException {
        writeLine(dailySystemLog(LocalDate.now()), "[SYSTEM] " + timestamp() + " " + line);
    }

    public void logVehicle(String vehicleName, String line) throws IOException {
        Path p = dailyVehicleLog(vehicleName, LocalDate.now());
        writeLine(p, "[VEHICLE:" + vehicleName + "] " + timestamp() + " " + line);
    }

    public void logCharging(String stationName, String line) throws IOException {
        Path p = dailyChargingLog(stationName, LocalDate.now());
        writeLine(p, "[CHARGING:" + stationName + "] " + timestamp() + " " + line);
    }

    private String timestamp() {
        return ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    private void writeLine(Path file, String line) throws IOException {
        Files.createDirectories(file.getParent());
        boolean creating = !Files.exists(file);
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            w.write(line);
            w.newLine();
        }
        if (creating) {
            MetadataManager.append(cfg.metaIndex, file, "LOG", "created");
        }
    }

    public List<Path> findByEquipmentOrDate(String equipmentNameOrDate) throws IOException {
        List<Path> results = new ArrayList<>();
        if (RegexUtils.isIsoDate(equipmentNameOrDate)) {
            String fname = equipmentNameOrDate + ".log";
            try (var stream = Files.walk(cfg.logsRoot)) {
                stream.filter(p -> p.getFileName().toString().equals(fname)).forEach(results::add);
            }
        } else if (RegexUtils.isValidEquipment(equipmentNameOrDate)) {
            Path veh = cfg.logsVehicles.resolve(equipmentNameOrDate);
            Path chg = cfg.logsCharging.resolve(equipmentNameOrDate);
            if (Files.exists(veh)) {
                try (var stream = Files.list(veh)) {
                    stream.filter(Files::isRegularFile).forEach(results::add);
                }
            }
            if (Files.exists(chg)) {
                try (var stream = Files.list(chg)) {
                    stream.filter(Files::isRegularFile).forEach(results::add);
                }
            }
        } else if ("system".equalsIgnoreCase(equipmentNameOrDate)) {
            try (var stream = Files.list(cfg.logsSystem)) {
                stream.filter(Files::isRegularFile).forEach(results::add);
            }
        }
        results.sort(Comparator.naturalOrder());
        return results;
    }

    public String readLog(Path p) throws IOException {
        return Files.readString(p, StandardCharsets.UTF_8);
    }   //  this closing brace was missing!

    /**
     * Archive a log file using the centralized ExceptionHandler (multiple exceptions demo).
     * This DOES NOT replace existing behavior; it's an additional utility you can call.
     */
    public void archiveLogWithHandler(java.nio.file.Path source, java.nio.file.Path archive) {
        try {
            ExceptionHandler.moveLogFile(source, archive);
            logSystem("Archived log " + source + " -> " + archive);
            System.out.println("SUCCESS: Log file moved successfully.");
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage()); // <-- visible feedback
            try {
                logSystem("Archive failed: " + e.getMessage());
            } catch (IOException io) {
                System.err.println("Logging error: " + io.getMessage());
            }
        }
    }
}
