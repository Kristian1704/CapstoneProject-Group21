package med.supply.system.repository;

import med.supply.system.model.*;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Map;

public class Repository {
    public final Map<String, StorageVehicle> vehicles = new HashMap<>();
    public final Map<String, ChargingStation> stations = new HashMap<>();
    public final Map<String, Task> tasks = new LinkedHashMap<>();

}
