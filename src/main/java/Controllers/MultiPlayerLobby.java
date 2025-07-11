package Controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import network.Client;

import java.io.IOException;

public class MultiPlayerLobby{

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
        try {
            client = new Client("localhost", 5000);
            client.setOnMessageReceived(message -> {
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
                    // Navigate to MultiplayerGameController scene
                    Platform.runLater(() -> {
                        try {
                            launchMultiplayerGame();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setPlayerName(String name) {
        this.playerName = name;
        client.sendName(name);
    }

    @FXML
    public void onStartGameClick() {
        if (isHost && players.size() >= 2) {
            client.sendResult("GAME-START");
        } else {
            lobbyStatusLabel.setText("Need at least 2 players to start");
        }
    }

    private void launchMultiplayerGame() throws IOException {
        // Switch to MultiplayerGameController scene (Scene switch logic goes here)
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxmlFiles/MultiPlayerGame.fxml"));
        Parent root = loader.load();

        Stage stage = (Stage) startGameButton.getScene().getWindow();

        Scene scene = new Scene(root);

        stage.setScene(scene);
        stage.setTitle("TypeRacer - MultiPlayer");
        stage.show();
        System.out.println("Game is starting...");
    }
}
