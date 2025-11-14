package med.supply.system.service;

import med.supply.system.exception.ExceptionHandler;
import med.supply.system.model.StorageItem;
import med.supply.system.model.StorageVehicle;
import med.supply.system.util.LogManager;
import med.supply.system.util.PathsConfig;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;

public class DataExchangeSimulator {
    private final PathsConfig cfg;
    private final LogManager logs;

    public DataExchangeSimulator(PathsConfig cfg, LogManager logs) {
        this.cfg = cfg;
        this.logs = logs;
    }

    // Now we take a Vehicle as input
    public void simulate(StorageVehicle vehicle) throws IOException {

        if (vehicle.getInventory().isEmpty()) {
            logs.logSystem("DataExchange: vehicle has NO items! Cannot simulate.");
            return;
        }

        // Take first item from vehicle inventory
        Map.Entry<String, StorageItem> entry = vehicle.getInventory().entrySet().iterator().next();
        StorageItem item = entry.getValue();

        // Build dynamic payload
        String payload =
                "EVENT=STOCK_TRANSFER" +
                        ";VEHICLE=" + vehicle.getId() +
                        ";SKU=" + item.getSku() +
                        ";NAME=" + item.getName() +
                        ";QTY=" + item.getQuantity() +
                        ";TS=" + Instant.now();

        // Write TXT file
        Path txt = cfg.exchangeRoot.resolve("exchange_" + vehicle.getId() + ".txt");
        Files.writeString(txt, payload + System.lineSeparator(), StandardCharsets.UTF_8);

        // Write BIN file
        Path bin = cfg.exchangeRoot.resolve("exchange_" + vehicle.getId() + ".bin");
        byte[] packet = payload.getBytes(StandardCharsets.UTF_8);
        try (OutputStream os = Files.newOutputStream(bin)) {
            os.write(intToBytes(packet.length));
            os.write(packet);
        }

        // Read binary back
        String roundtrip;
        try (InputStream is = Files.newInputStream(bin)) {
            byte[] lenB = is.readNBytes(4);
            int len = bytesToInt(lenB);
            byte[] data = is.readNBytes(len);
            roundtrip = new String(data, StandardCharsets.UTF_8);
        }

        logs.logSystem("DataExchange: exchange files created for vehicle=" + vehicle.getId());
        logs.logSystem("DataExchange: roundtrip payload: " + roundtrip);

        System.out.println("Data exchange completed for vehicle " + vehicle.getId());
    }

    private byte[] intToBytes(int v) {
        return new byte[]{
                (byte) (v >>> 24), (byte) (v >>> 16),
                (byte) (v >>> 8), (byte) v
        };
    }

    private int bytesToInt(byte[] b) {
        return ((b[0] & 255) << 24) |
                ((b[1] & 255) << 16) |
                ((b[2] & 255) << 8) |
                ((b[3] & 255));
    }

    /** Preview the first line of a file using try-with-resources via ExceptionHandler. */
    public String previewFirstLineWithHandler(Path path) {
        try {
            return ExceptionHandler.readFirstLine(path);
        } catch (Exception e) {
            return "[ERROR] " + e.getMessage();
        }
    }


}
