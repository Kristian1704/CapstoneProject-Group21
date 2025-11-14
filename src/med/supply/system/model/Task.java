
package med.supply.system.model;

public class Task {
    public final String id;
    public final String description;
    public String assigneeVehicleId; // nullable
    public TaskStatus status = TaskStatus.PENDING;

    public Task(String id, String description, String assigneeVehicleId) {
        this.id = id;
        this.description = description;
        this.assigneeVehicleId = assigneeVehicleId;
    }

    @Override
    public String toString() {
        return "Task{" +
                "id='" + id + '\'' +
                ", description='" + description + '\'' +
                ", assigneeVehicleId='" + assigneeVehicleId + '\'' +
                ", status=" + status +
                '}';
    }
}
