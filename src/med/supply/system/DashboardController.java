package med.supply.system;


import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.Duration;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import med.supply.system.model.*;
import med.supply.system.repository.Repository;
import med.supply.system.service.*;
import med.supply.system.util.LogManager;
import med.supply.system.util.PathsConfig;
import med.supply.system.util.MetadataManager;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DashboardController {

    @FXML private TableView<StorageVehicle> vehicleTable;
    @FXML private TableColumn<StorageVehicle, String> vIdCol;
    @FXML private TableColumn<StorageVehicle, String> vNameCol;
    @FXML private TableColumn<StorageVehicle, Number> vBattCol;
    @FXML private TableColumn<StorageVehicle, String> vStationCol;
    @FXML private TableColumn<StorageVehicle, Boolean> vChargingCol;

    @FXML private TableView<ChargingStation> stationTable;
    @FXML private TableColumn<ChargingStation, String> sIdCol;
    @FXML private TableColumn<ChargingStation, String> sNameCol;
    @FXML private TableColumn<ChargingStation, Boolean> sBusyCol;

    @FXML private TableView<StorageItem> itemsTable;
    @FXML private TableColumn<StorageItem, String> iSkuCol;
    @FXML private TableColumn<StorageItem, String> iNameCol;
    @FXML private TableColumn<StorageItem, Number> iQtyCol;

    private final ObservableList<StorageItem> items = FXCollections.observableArrayList();


    @FXML private TableView<Task> taskTable;
    @FXML private TableColumn<Task, String> tIdCol;
    @FXML private TableColumn<Task, String> tDescCol;
    @FXML private TableColumn<Task, String> tAssigneeCol;
    @FXML private TableColumn<Task, String> tStatusCol;

    @FXML private TextArea logArea;
    @FXML private TextField newVehicleName;

    private final Repository repo = new Repository();
    private PathsConfig paths;
    private LogManager logs;
    private StorageService storageService;
    private TaskService taskService;
    private DestinationService destinationService;
    private DataExchangeSimulator exchange;

    private final ObservableList<StorageVehicle> vehicles = FXCollections.observableArrayList();
    private final ObservableList<ChargingStation> stations = FXCollections.observableArrayList();
    private final ObservableList<Task> tasks = FXCollections.observableArrayList();

    private final ExecutorService pool = Executors.newCachedThreadPool();
    private final Random rnd = new Random();

    @FXML
    public void initialize() {
        try { paths = new PathsConfig(); paths.ensure(); logs = new LogManager(paths); }
        catch (IOException e) { throw new RuntimeException(e); }

        storageService = new StorageService(repo, logs);
        taskService = new TaskService(repo, logs);
        destinationService = new DestinationService();
        taskService.attachStorage(storageService);
        taskService.attachDestination(destinationService);
        storageService.attachTaskService(taskService);
        exchange = new DataExchangeSimulator(paths, logs);
        ChargingStation.attachLogger(logs);

        vehicleTable.setItems(vehicles);
        vIdCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getId()));
        vNameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        vBattCol.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getBatteryLevelPct()));
        vStationCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAssignedStation()==null?"-":c.getValue().getAssignedStation().getName()));
        vChargingCol.setCellValueFactory(c -> new SimpleBooleanProperty(c.getValue().isCharging()));

        stationTable.setItems(stations);
        sIdCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getId()));
        sNameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        sBusyCol.setCellValueFactory(c -> new SimpleBooleanProperty(c.getValue().isInUse()));

        taskTable.setItems(tasks);
        tIdCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().id));
        tDescCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().description));
        tAssigneeCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().assigneeVehicleId==null?"-":c.getValue().assigneeVehicleId));
        tStatusCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().status.name()));

        // --- Items table setup ---
        itemsTable.setItems(items);
        iSkuCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSku()));
        iNameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        iQtyCol.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getQuantity()));

        // seed demo vehicles
        repo.vehicles.putAll(Map.of(
                "VH-1", new StorageVehicle("VH-1", "Van One"),
                "VH-2", new StorageVehicle("VH-2", "Van Two"),
                "VH-3", new StorageVehicle("VH-3", "Van Three")
        ));

        refreshTables();

        Timeline tl = new Timeline(new KeyFrame(Duration.millis(200), e -> refreshTables()));
        tl.setCycleCount(Timeline.INDEFINITE); tl.play();
    }

    private void refreshTables() {
        vehicles.setAll(repo.vehicles.values());
        stations.setAll(repo.stations.values());
        tasks.setAll(repo.tasks.values());
        items.setAll(storageService.getUnassignedItemsRef().values());
    }

    private void append(String s) { Platform.runLater(() -> logArea.appendText(s + "\n")); }

    // ==== Buttons replicating console options ====


    // 3) Add vehicle
    @FXML public void onAddVehicle(ActionEvent e) {
        String name = newVehicleName.getText()==null? "" : newVehicleName.getText().trim();
        if (name.isEmpty()) name = "Vehicle_" + (repo.vehicles.size()+1);
        String id = "VH-" + String.format("%03d", repo.vehicles.size()+1);
        StorageVehicle v = new StorageVehicle(id, name);
        try { storageService.addVehicle(v); } catch (Exception ex){ append("[ERR] " + ex.getMessage()); }
        append("Added vehicle " + v.getId()); refreshTables();
    }

    // 4) Add charging station
    @FXML public void onAddStation(ActionEvent e){
        TextInputDialog a = new TextInputDialog("CHG-"+(repo.stations.size()+1));
        a.setHeaderText("Station ID"); var rid = a.showAndWait(); if (rid.isEmpty()) return;
        TextInputDialog b = new TextInputDialog("Station_"+(repo.stations.size()+1));
        b.setHeaderText("Station Name"); var rname = b.showAndWait(); if (rname.isEmpty()) return;
        try { storageService.addChargingStation(new ChargingStation(rid.get(), rname.get())); append("Added station "+rname.get()); }
        catch(Exception ex){ append("[ERR] "+ex.getMessage()); }
        refreshTables();
    }

    // 5) Add item (unassigned)
    @FXML public void onAddItem(ActionEvent e){
        TextInputDialog skuD=new TextInputDialog("SKU-001"); skuD.setHeaderText("SKU"); var rSku=skuD.showAndWait(); if (rSku.isEmpty()) return;
        TextInputDialog nmD=new TextInputDialog("Item Name"); nmD.setHeaderText("Name"); var rNm=nmD.showAndWait(); if (rNm.isEmpty()) return;
        TextInputDialog qtD=new TextInputDialog("100"); qtD.setHeaderText("Quantity"); var rQt=qtD.showAndWait(); if (rQt.isEmpty()) return;
        try { int q=Integer.parseInt(rQt.get()); storageService.addItem(new StorageItem(rSku.get(), rNm.get(), q)); append("Added item "+rSku.get()); }
        catch(Exception ex){ append("[ERR] "+ex.getMessage()); }
    }


    // 8) Create task (auto or manual)
    @FXML public void onCreateTask(ActionEvent e){
        ChoiceDialog<String> mode=new ChoiceDialog<>("AUTO","AUTO","MANUAL"); mode.setHeaderText("Task Type"); var m=mode.showAndWait(); if (m.isEmpty()) return;
        if ("AUTO".equals(m.get())){
            String id="AUTO-"+System.currentTimeMillis();
            try{ taskService.createTask(new Task(id,"Auto-distribute items",null)); taskService.autoDistribute(id); append("[AUTO] Started "+id); } catch(Exception ex){ append("[ERR] "+ex.getMessage()); }
        } else {
            // Task ID
            TextInputDialog idD=new TextInputDialog("TASK-"+System.currentTimeMillis());
            idD.setHeaderText("Task ID");
            var id=idD.showAndWait();
            if (id.isEmpty()) return;

            // Description
            TextInputDialog dD=new TextInputDialog("Manual task");
            dD.setHeaderText("Description");
            var d=dD.showAndWait();
            if (d.isEmpty()) return;

            // Vehicle ID
            TextInputDialog vD=new TextInputDialog("");
            vD.setHeaderText("Vehicle ID");
            var v=vD.showAndWait();
            if (v.isEmpty()) return;

            // 🔥 New: Item SKU required only for MANUAL
            TextInputDialog skuD = new TextInputDialog("");
            skuD.setHeaderText("Item SKU to assign");
            var sku = skuD.showAndWait();
            if (sku.isEmpty()) return;

            try {
                taskService.createManualTaskWithItem(
                        new Task(id.get(), d.get(), v.get()),
                        sku.get()
                );
                append("Manual task created for " + v.get() + " with item " + sku.get());
            } catch (Exception ex) {
                append("[ERR] " + ex.getMessage());
            }
        }

        refreshTables();
    }

    // 9) Update task status
    @FXML public void onUpdateTaskStatus(ActionEvent e){
        TextInputDialog idD=new TextInputDialog(""); idD.setHeaderText("Task ID"); var id=idD.showAndWait(); if (id.isEmpty()) return;
        ChoiceDialog<String> st=new ChoiceDialog<>("PENDING","PENDING","IN_PROGRESS","DONE"); st.setHeaderText("New Status"); var s=st.showAndWait(); if (s.isEmpty()) return;
        try { taskService.updateStatus(id.get(), TaskStatus.valueOf(s.get().toUpperCase(Locale.ROOT))); append("Task "+id.get()+" -> "+s.get()); }
        catch(Exception ex){ append("[ERR] "+ex.getMessage()); }
        refreshTables();
    }

    // 10) Simulate data exchange
    @FXML public void onSimulateExchange(ActionEvent e){
        TextInputDialog vD=new TextInputDialog(""); vD.setHeaderText("Vehicle ID"); var v=vD.showAndWait(); if (v.isEmpty()) return;
        StorageVehicle veh=repo.vehicles.get(v.get()); if (veh==null){ append("[ERR] Vehicle not found"); return; }
        try { exchange.simulate(veh); append("Data exchange done for "+v.get()); } catch(Exception ex){ append("[ERR] "+ex.getMessage()); }
    }

    // 11) Archive logs to ZIP
    @FXML public void onArchiveLogs(ActionEvent e){
        var p = paths.archiveRoot.resolve("logs-"+ LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))+".zip");
        try{ MetadataManager.archiveZip(paths.logsRoot, p, paths.metaIndex); append("Archived: "+p); } catch(Exception ex){ append("[ERR] "+ex.getMessage()); }
    }

    // 12) Move log file
    @FXML public void onMoveLog(ActionEvent e){
        TextInputDialog fromD=new TextInputDialog(""); fromD.setHeaderText("From (path)"); var f=fromD.showAndWait(); if (f.isEmpty()) return;
        TextInputDialog toD=new TextInputDialog(""); toD.setHeaderText("To (path)"); var t=toD.showAndWait(); if (t.isEmpty()) return;
        try{ MetadataManager.move(Path.of(f.get()), Path.of(t.get()), paths.metaIndex); append("Moved log."); } catch(Exception ex){ append("[ERR] "+ex.getMessage()); }
    }

    // 13) Delete log file
    @FXML public void onDeleteLog(ActionEvent e){
        TextInputDialog fD=new TextInputDialog(""); fD.setHeaderText("File to delete"); var f=fD.showAndWait(); if (f.isEmpty()) return;
        try{ MetadataManager.delete(Path.of(f.get()), paths.metaIndex); append("Deleted."); } catch(Exception ex){ append("[ERR] "+ex.getMessage()); }
    }






    @FXML
    public void onOpenLog(ActionEvent e) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Open Log File");
        dialog.setHeaderText("Enter Equipment Name or Date (yyyy-mm-dd)");
        dialog.setContentText("Examples: VH-1, Default_Station_2, 2025-01-15, system");

        var result = dialog.showAndWait();
        if (result.isEmpty()) return;

        String query = result.get().trim();

        try {
            var matches = logs.findByEquipmentOrDate(query);

            if (matches.isEmpty()) {
                showAlert("No logs found for: " + query);
                return;
            }

            if (matches.size() == 1) {
                showLogContent(matches.get(0));
                return;
            }

            // Multiple results → user must choose
            ChoiceDialog<Path> chooser =
                    new ChoiceDialog<>(matches.get(0), matches);
            chooser.setTitle("Select Log");
            chooser.setHeaderText("Multiple logs found. Choose one to open:");
            var selected = chooser.showAndWait();
            selected.ifPresent(this::showLogContent);

        } catch (Exception ex) {
            showAlert("Error while searching logs: " + ex.getMessage());
        }
    }
    private void showLogContent(Path path) {
        try {
            String content = logs.readLog(path);

            TextArea area = new TextArea(content);
            area.setEditable(false);
            area.setWrapText(true);

            Stage stage = new Stage();
            stage.setTitle("Log Viewer - " + path.getFileName());
            stage.setScene(new Scene(new StackPane(area), 800, 600));
            stage.show();

        } catch (Exception ex) {
            showAlert("Error reading log: " + ex.getMessage());
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

}
