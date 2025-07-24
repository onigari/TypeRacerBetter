package Controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import network.Client;
import java.io.IOException;

public class MultiPlayerChoiceController {
    @FXML private VBox rootPane;
    @FXML private TextField nameField;
    @FXML private TextField ipField;
    @FXML private Label statusLabel;
    @FXML private Label escText;

    public void initialize() {
        escText.setStyle("-fx-text-fill: #d1d0c5; -fx-font-family: 'Roboto Mono';");

        rootPane.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                try {
                    loadMainMenu();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    private void loadMainMenu() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxmlFiles/MainMenu.fxml"));
        Parent root = loader.load();

        Stage stage = (Stage) rootPane.getScene().getWindow();
        Scene scene = new Scene(root, 1420, 800);

        stage.setTitle("TypeRacer");
        stage.setResizable(true);
        stage.setScene(scene);
        stage.show();
    }

    @FXML
    private void handleHostGame() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            statusLabel.setText("Please enter your name");
            return;
        }

        try {
            // Start server in a new thread
            new Thread(() -> network.Server.main(new String[]{})).start();
            // Give the server a moment to start
            Thread fineThread = new Thread();
            fineThread.sleep(1000);
            // Connect to localhost as host
            Client client = new Client("localhost", 5000);
            loadLobby(client, name, true);
        } catch (Exception e) {
            statusLabel.setText("Failed to host game: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleJoinGame() {
        String name = nameField.getText().trim();
        String ip = ipField.getText().trim();

        if (name.isEmpty() || ip.isEmpty()) {
            statusLabel.setText("Please enter both name and IP address");
            return;
        }

        try {
            Client client = new Client(ip, 5000);
            loadLobby(client, name, false);
        } catch (IOException e) {
            statusLabel.setText("Failed to connect to server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadLobby(Client client, String name, boolean isHost) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxmlFiles/MultiPlayerLobby.fxml"));
        Parent root = loader.load();

        MultiPlayerLobbyController controller = loader.getController();
        controller.initialize(client, name, isHost);

        Stage stage = (Stage) nameField.getScene().getWindow();
        stage.setScene(new Scene(root, 1420, 800));
        stage.setTitle("Multiplayer Lobby - " + name);
        stage.setOnCloseRequest(e -> client.close());
    }
}