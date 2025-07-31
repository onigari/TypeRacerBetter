package Controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;


public class MainMenuController {
    @FXML
    private void onSinglePlayerClick(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxmlFiles/SinglePlayerGame.fxml"));
        Parent root = loader.load();

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

        Scene scene = new Scene(root, 1511, 850);

        stage.setScene(scene);
        stage.setTitle("TypeRacer - SinglePlayer");
        stage.centerOnScreen();
        stage.show();
    }

    @FXML
    private void onMultiPlayerClick(ActionEvent event) throws IOException {

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxmlFiles/MultiPlayerChoice.fxml"));
        Parent root = loader.load();

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

        Scene scene = new Scene(root);

        stage.setScene(scene);
        stage.setTitle("TypeRacer - MultiPlayer Choice");
        stage.centerOnScreen();
        stage.show();
    }

    @FXML
    private void onExitClick(ActionEvent event) {
        System.exit(0);
    }
}