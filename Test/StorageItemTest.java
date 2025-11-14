import med.supply.system.model.StorageItem;
import med.supply.system.exception.ExceptionHandler.InvalidStorageItemException;

public class StorageItemTest {
    public static void main(String[] args) {
        System.out.println("Running StorageItem tests...");

        try {
            testConstructorSetsValues();
            testTrimmedInputs();
            testSetQuantityUpdates();
            testSetQuantityThrowsForNegative();
            testConstructorThrowsForInvalidInputs();

            System.out.println("All StorageItem tests finished.");
        } catch (AssertionError e) {
            System.err.println("StorageItem test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ---------- TEST 1 ----------
    private static void testConstructorSetsValues() {
        StorageItem item = new StorageItem("SKU001", "Gloves", 10);

        assert item.getSku().equals("SKU001") : "SKU not set correctly";
        assert item.getName().equals("Gloves") : "Name not set correctly";
        assert item.getQuantity() == 10 : "Quantity not set correctly";

        System.out.println("Test 1 passed (constructor sets values)");
    }

    // ---------- TEST 2 ----------
    private static void testTrimmedInputs() {
        StorageItem item = new StorageItem("  SKU002  ", "  Masks  ", 7);

        assert item.getSku().equals("SKU002") : "SKU not trimmed";
        assert item.getName().equals("Masks") : "Name not trimmed";

        System.out.println("Test 2 passed (inputs trimmed correctly)");
    }

    // ---------- TEST 3 ----------
    private static void testSetQuantityUpdates() {
        StorageItem item = new StorageItem("SKU003", "Disinfectant", 3);
        item.setQuantity(8);

        assert item.getQuantity() == 8 : "setQuantity did not update correctly";

        System.out.println("Test 3 passed (setQuantity updates quantity)");
    }

    // ---------- TEST 4 ----------
    private static void testSetQuantityThrowsForNegative() {
        try {
            StorageItem item = new StorageItem("SKU004", "Syringes", 2);
            item.setQuantity(-1);
            assert false : "Expected InvalidStorageItemException for negative quantity not thrown";
        } catch (InvalidStorageItemException e) {
            System.out.println("Test 4 passed (setQuantity throws for negative value)");
        }
    }

    // ---------- TEST 5 ----------
    private static void testConstructorThrowsForInvalidInputs() {
        // Blank SKU
        try {
            new StorageItem("   ", "Item", 5);
            assert false : "Expected InvalidStorageItemException for blank SKU not thrown";
        } catch (InvalidStorageItemException e) {
            // expected
        }

        // Blank name
        try {
            new StorageItem("SKU005", "   ", 5);
            assert false : "Expected InvalidStorageItemException for blank name not thrown";
        } catch (InvalidStorageItemException e) {
            // expected
        }

        // Negative quantity
        try {
            new StorageItem("SKU006", "Bandage", -5);
            assert false : "Expected InvalidStorageItemException for negative quantity not thrown";
        } catch (InvalidStorageItemException e) {
            System.out.println("Test 5 passed (constructor throws for invalid inputs)");
        }
    }
}
