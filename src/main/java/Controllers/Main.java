package Controllers;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.util.Objects;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/fxmlFiles/MainMenu.fxml")));
            Scene scene = new Scene(root, 800, 600);
            primaryStage.setTitle("TypeRacer");
            primaryStage.getIcons().add(
                    new Image(Objects.requireNonNull(getClass().getResource("/img/icon.jpg")).toExternalForm())
            );
            Font.loadFont(getClass().getResourceAsStream("/fonts/ttf/JetBrainsMono-Medium.ttf"), 12);
            primaryStage.setResizable(true);
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
