package med.supply.system.service;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;

public class DestinationService {

    private final String destinationFile = "resources/destination_log.csv";

    public DestinationService() {
        initializeFile();
    }

    private void initializeFile() {
        try (FileWriter writer = new FileWriter(destinationFile, false)) {
            writer.write("timestamp,vehicle,item,quantity\n");
        } catch (IOException ignored) {}
    }

    public synchronized void recordDelivery(String vehicleName, String itemName, int qty) {
        try (FileWriter writer = new FileWriter(destinationFile, true)) {

            writer.write(LocalDateTime.now() + "," +
                    vehicleName + "," +
                    itemName + "," +
                    qty + "\n");

        } catch (IOException e) {
            System.err.println("[DESTINATION] ERROR writing delivery: " + e.getMessage());
        }
    }
}
