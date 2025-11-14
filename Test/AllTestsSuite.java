public class AllTestsSuite {
    public static void main(String[] args) {
        System.out.println("Starting Test Suite for MedicalStorageSystem...\n");

        long start = System.currentTimeMillis();

        // === MODEL TESTS ===
        try {
            StorageItemTest.main(args);
            StorageVehicleTest.main(args);
            TaskTest.main(args);
        } catch (Exception e) {
            System.err.println("Model tests failed: " + e.getMessage());
        }

        // === SERVICE TESTS ===
        try {
            StorageServiceTest.main(args);
            DataExchangeSimulatorTest.main(args);
        } catch (Exception e) {
            System.err.println("Service tests failed: " + e.getMessage());
        }

        // === UTIL TESTS ===
        try {
            LogManagerTest.main(args);
            MetadataManagerTest.main(args);
        } catch (Exception e) {
            System.err.println("Utility tests failed: " + e.getMessage());
        }

        // === EXCEPTION TESTS ===
        try {
            ExceptionHandlerTest.main(args);
        } catch (Exception e) {
            System.err.println("ExceptionHandlerTest failed: " + e.getMessage());
        }
        try {
            TaskServiceTest.main(args);
        } catch (Exception e) {
            System.err.println("TaskServiceTest failed: " + e.getMessage());
        }
        // === ADDITIONAL TESTS ===
        try {
            ChargingStationTest.main(args);
        } catch (Exception e) {
            System.err.println("ChargingStationTest failed: " + e.getMessage());
        }

        long end = System.currentTimeMillis();
        System.out.println("\nAllTestsSuite finished in " + (end - start) + " ms.");
    }
}
