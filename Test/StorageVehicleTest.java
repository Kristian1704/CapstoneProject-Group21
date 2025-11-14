import med.supply.system.model.StorageVehicle;
import med.supply.system.model.StorageItem;
import med.supply.system.exception.ExceptionHandler;

public class StorageVehicleTest {
    public static void main(String[] args) {
        System.out.println("Running StorageVehicle tests...");

        // Test 1: Valid creation and getters
        try {
            StorageVehicle v = new StorageVehicle("VH-001", "Van_Alpha");
            assert v.getId().equals("VH-001");
            assert v.getName().equals("Van_Alpha");
            assert v.getBatteryLevelPct() == 20 : "Default battery should be 20";
            System.out.println(" Test 1 passed");
        } catch (Throwable e) {
            System.out.println(" Test 1 failed: " + e.getMessage());
        }

        // Test 2: Invalid battery level (<0)
        try {
            StorageVehicle v2 = new StorageVehicle("VH-002", "Van_Beta");
            v2.setBatteryLevelPct(-5);
            System.out.println(" Test 2 failed: Negative battery not handled");
        } catch (ExceptionHandler.InvalidStorageVehicleException e) {
            System.out.println(" Test 2 passed (caught: " + e.getMessage() + ")");
        }

        // Test 3: Invalid battery level (>100)
        try {
            StorageVehicle v3 = new StorageVehicle("VH-003", "Van_Gamma");
            v3.setBatteryLevelPct(150);
            System.out.println(" Test 3 failed: Battery >100 not handled");
        } catch (ExceptionHandler.InvalidStorageVehicleException e) {
            System.out.println(" Test 3 passed (caught: " + e.getMessage() + ")");
        }

        // Test 4: Adding items (merge same SKU)
        try {
            StorageVehicle v4 = new StorageVehicle("VH-004", "Van_Delta");
            v4.addItem(new StorageItem("SKU001", "Gloves", 5));
            v4.addItem(new StorageItem("SKU001", "Gloves", 3)); // merge

            int qty = v4.getInventory().get("SKU001").getQuantity();
            assert qty == 8 : "Quantity should merge to 8";
            System.out.println(" Test 4 passed (merge works)");
        } catch (Throwable e) {
            System.out.println(" Test 4 failed: " + e.getMessage());
        }

        // Test 5: Adding items (different SKUs)
        try {
            StorageVehicle v5 = new StorageVehicle("VH-005", "Van_Epsilon");
            v5.addItem(new StorageItem("SKU100", "Masks", 10));
            v5.addItem(new StorageItem("SKU200", "Syringes", 5));

            assert v5.getInventory().get("SKU100").getQuantity() == 10;
            assert v5.getInventory().get("SKU200").getQuantity() == 5;

            System.out.println(" Test 5 passed (separate SKUs handled)");
        } catch (Throwable e) {
            System.out.println(" Test 5 failed: " + e.getMessage());
        }

        // Test 6: Null item should throw exception
        try {
            StorageVehicle v6 = new StorageVehicle("VH-006", "Van_Zeta");
            v6.addItem(null);
            System.out.println(" Test 6 failed: Adding null item not handled");
        } catch (ExceptionHandler.InvalidStorageVehicleException e) {
            System.out.println(" Test 6 passed (caught: " + e.getMessage() + ")");
        }

        // Test 7: Remaining capacity calculation
        try {
            StorageVehicle v7 = new StorageVehicle("VH-007", "Van_Eta");
            v7.addItem(new StorageItem("A", "ItemA", 1));
            v7.addItem(new StorageItem("B", "ItemB", 1));

            int remaining = v7.remainingCapacity();
            assert remaining == StorageVehicle.getMaxCapacity() - 2;
            System.out.println(" Test 7 passed (remaining capacity correct)");
        } catch (Throwable e) {
            System.out.println(" Test 7 failed: " + e.getMessage());
        }

        System.out.println("All StorageVehicle tests finished.");
    }
}
