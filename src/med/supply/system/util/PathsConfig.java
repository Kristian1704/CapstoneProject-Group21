
package med.supply.system.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public class PathsConfig {
    public final Path root = Paths.get("data");
    public final Path logsRoot = root.resolve("logs");
    public final Path logsVehicles = logsRoot.resolve("vehicles");
    public final Path logsCharging = logsRoot.resolve("charging");
    public final Path logsSystem = logsRoot.resolve("system");
    public final Path metaRoot = root.resolve("metadata");
    public final Path archiveRoot = root.resolve("archive");
    public final Path exchangeRoot = root.resolve("exchange");
    public final Path metaIndex = metaRoot.resolve("log_metadata.csv");

    public void ensure() throws IOException {
        Files.createDirectories(logsVehicles);
        Files.createDirectories(logsCharging);
        Files.createDirectories(logsSystem);
        Files.createDirectories(metaRoot);
        Files.createDirectories(archiveRoot);
        Files.createDirectories(exchangeRoot);
        if (!Files.exists(metaIndex)) {
            Files.writeString(metaIndex, "path,createdUtc,kind,notes\n", StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
    }
    public Path getLogBaseDir() {
        return logsSystem;
    }

}
