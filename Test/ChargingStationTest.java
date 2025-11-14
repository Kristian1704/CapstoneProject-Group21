import med.supply.system.model.ChargingStation;
import med.supply.system.exception.ExceptionHandler;

public class ChargingStationTest {
    public static void main(String[] args) {
        System.out.println("Running ChargingStation tests...");

        // Test 1: valid construction and getters
        try {
            ChargingStation c = new ChargingStation("ST-001", "Station_1");
            assert "ST-001".equals(c.getId()) : "id mismatch";
            assert "Station_1".equals(c.getName()) : "name mismatch";
            assert !c.isInUse() : "Station should be free by default";
            System.out.println(" Test 1 passed");
        } catch (Throwable t) {
            System.out.println(" Test 1 failed: " + t.getMessage());
        }

        // Test 2: occupy / release behavior
        try {
            ChargingStation c = new ChargingStation("ST-002", "Station_2");
            c.occupy();
            assert c.isInUse() : "Station should be in use after occupy()";
            c.release();
            assert !c.isInUse() : "Station should be free after release()";
            System.out.println(" Test 2 passed");
        } catch (Throwable t) {
            System.out.println(" Test 2 failed: " + t.getMessage());
        }

        // Test 3: invalid id should throw our custom exception
        try {
            new ChargingStation("", "Name");
            System.out.println(" Test 3 failed: Expected custom exception for blank id");
        } catch (ExceptionHandler.InvalidChargingStationException ok) {
            System.out.println(" Test 3 passed (caught: " + ok.getMessage() + ")");
        }

        // Test 4: invalid name should throw our custom exception
        try {
            new ChargingStation("ST-004", "");
            System.out.println(" Test 4 failed: Expected custom exception for blank name");
        } catch (ExceptionHandler.InvalidChargingStationException ok) {
            System.out.println(" Test 4 passed (caught: " + ok.getMessage() + ")");
        }

        // Test 5: toString includes id, name, and status
        try {
            ChargingStation c = new ChargingStation("ST-005", "Alpha");
            String s = c.toString();
            assert s.contains("ST-005") : "toString missing id";
            assert s.contains("Alpha") : "toString missing name";
            assert s.contains("FREE") || s.contains("IN_USE") : "toString missing status";
            System.out.println(" Test 5 passed");
        } catch (Throwable t) {
            System.out.println(" Test 5 failed: " + t.getMessage());
        }

        System.out.println("All ChargingStation tests finished.");
    }
}
