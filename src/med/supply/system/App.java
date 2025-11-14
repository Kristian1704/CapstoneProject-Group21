package med.supply.system;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.net.URL;

public class App extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        URL fxml = getClass().getResource("/gui/dashboard.fxml");
        if (fxml == null) throw new IllegalStateException("FXML NOT FOUND: /gui/dashboard.fxml");
        FXMLLoader loader = new FXMLLoader(fxml);
        Scene scene = new Scene(loader.load(), 1280, 860);
        stage.setTitle("Automated Storage System for Medical Supplies");
        stage.setScene(scene);
        stage.show();
    }
    public static void main(String[] args) { launch(args); }
}
