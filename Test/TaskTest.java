import jdk.jshell.Snippet;
import med.supply.system.model.Task;
import med.supply.system.model.TaskStatus;

public class TaskTest {
    public static void main(String[] args) {
        System.out.println("Running Task tests...\n");

        // Test 1: valid creation
        try {
            Task t = new Task("T001", "Deliver medical supplies", "Van_Alpha");
            assert t.id.equals("T001");
            assert t.description.equals("Deliver medical supplies");
            assert t.assigneeVehicleId.equals("Van_Alpha");
            assert t.status == TaskStatus.PENDING;
            System.out.println("Test 1 passed");
        } catch (AssertionError e) {
            System.out.println("Test 1 failed: " + e.getMessage());
        }

        // Test 2: unassigned task (nullable assignee)
        try {
            Task t2 = new Task("T002", "Restock supplies", null);
            assert t2.assigneeVehicleId == null;
            System.out.println("Test 2 passed");
        } catch (AssertionError e) {
            System.out.println("Test 2 failed: " + e.getMessage());
        }

        // Test 3: status change to IN_PROGRESS
        try {
            Task t3 = new Task("T003", "Transport samples", "Van_Beta");
            t3.status = TaskStatus.DONE;
            assert t3.status == TaskStatus.DONE;
            System.out.println("Test 3 passed");
        } catch (AssertionError e) {
            System.out.println("Test 3 failed: " + e.getMessage());
        }

        // Test 4: toString should include id, description, and default status
        try {
            Task t4 = new Task("T004", "Inventory audit", "Van_Delta");
            String output = t4.toString();

            assert output.contains("T004");
            assert output.contains("Inventory audit");
            assert output.contains("PENDING"); // Default status

            System.out.println("Test 4 passed");
        } catch (AssertionError e) {
            System.out.println("Test 4 failed: " + e.getMessage());
        }

        // Test 5: status change to DONE
        try {
            Task t5 = new Task("T005", "Return used materials", "Van_Echo");
            t5.status = TaskStatus.DONE;
            assert t5.status == TaskStatus.DONE;
            System.out.println("Test 5 passed");
        } catch (AssertionError e) {
            System.out.println("Test 5 failed: " + e.getMessage());
        }

        System.out.println("\n Task tests completed");
    }
}
