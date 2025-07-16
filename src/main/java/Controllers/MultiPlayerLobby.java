package Controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import network.Client;

import java.io.IOException;

public class MultiPlayerLobby {

    @FXML
    private ListView<String> playerListView;

    @FXML
    private Label lobbyStatusLabel;

    @FXML
    private Button startGameButton;

    private Client client;
    private ObservableList<String> players = FXCollections.observableArrayList();
    private boolean isHost = false;
    private String playerName;

    public void initialize() {
        // client will be set by MultiplayerChoiceController based on host/join logic
    }

    public void setClient(Client client, String name) {
        this.client = client;
        this.playerName = name;
        this.client.sendName(name);

        this.client.setOnMessageReceived(message -> {
            if (message.startsWith("LOBBY-PLAYERS:")) {
                String[] names = message.substring(14).split(",");
                Platform.runLater(() -> {
                    players.setAll(names);
                    playerListView.setItems(players);
                    lobbyStatusLabel.setText("Waiting for players: " + players.size() + "/5");
                });
            } else if (message.startsWith("HOST:")) {
                if (message.substring(5).equals(playerName)) {
                    isHost = true;
                    Platform.runLater(() -> startGameButton.setDisable(false));
                }
            } else if (message.equals("GAME-START")) {
                Platform.runLater(this::launchMultiplayerGame);
            } else if (message.startsWith("ERROR:")) {
                Platform.runLater(() -> showAlert("Error", message.substring(6)));
            }
        });
    }

    @FXML
    public void onStartGameClick() {
        if (isHost && players.size() >= 2) {
            client.sendResult("GAME-START");
        } else {
            lobbyStatusLabel.setText("Need at least 2 players to start");
        }
    }

    private void launchMultiplayerGame() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxmlFiles/MultiPlayerGame.fxml"));
            Parent root = loader.load();

            MultiPlayerGameController controller = loader.getController();
            controller.setClient(client, playerName);

            Stage stage = (Stage) startGameButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("TypeRacer - Multiplayer");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}