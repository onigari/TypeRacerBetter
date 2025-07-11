package Controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.application.Platform;
import javafx.animation.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.*;
import javafx.util.Duration;

import network.Client;


public class MultiPlayerGameController {

    @FXML
    private TextField playerNameField;

    @FXML
    private TextFlow paragraphFlow;

    @FXML
    private ListView<String> leaderboardList;

    @FXML
    private Button startButton;

    private Client client;
    private ObservableList<String> leaderboard = FXCollections.observableArrayList();
    private String paragraphText;

    public void initialize() {
        try {
            client = new Client("localhost", 5000);
            client.setOnMessageReceived(message -> {
                if (message.startsWith("PARAGRAPH:")) {
                    paragraphText = message.substring(9);
                    Platform.runLater(() -> displayParagraph(paragraphText));
                } else if (message.startsWith("RESULT:")) {
                    String[] parts = message.substring(7).split(";");
                    if (parts.length == 4) {
                        String entry = String.format("%s - %.2f s - Accuracy: %.2f%% - WPM: %.2f",
                                parts[0], Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
                        Platform.runLater(() -> {
                            leaderboard.add(entry);
                            leaderboardList.setItems(FXCollections.observableArrayList(leaderboard));
                        });
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void onStartButtonClick() {
        String name = playerNameField.getText();
        if (name == null || name.isEmpty()) return;
        client.sendName(name);
        leaderboard.clear();
        leaderboardList.setItems(leaderboard);
    }

    private void displayParagraph(String paragraph) {
        paragraphFlow.getChildren().clear();
        for (char c : paragraph.toCharArray()) {
            javafx.scene.text.Text letter = new javafx.scene.text.Text(String.valueOf(c));
            letter.setStyle("-fx-fill: gray;");
            paragraphFlow.getChildren().add(letter);
        }
        // Typing logic needs to be implemented to track user input over time
    }

    public void sendFinalResult(String name, double time, double accuracy, double wpm) {
        String result = name + ";" + time + ";" + accuracy + ";" + wpm;
        client.sendResult(result);
    }
}
