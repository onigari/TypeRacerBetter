package Controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.text.*;
import javafx.scene.text.TextFlow;
import network.Client;

import java.util.ArrayList;
import java.util.List;

public class MultiPlayerGameController {

    @FXML
    private TextFlow paragraphFlow;

    @FXML
    private Label countdownLabel;

    @FXML
    private Label progressLabel;

    @FXML
    private ListView<String> leaderboardList;

    private List<Text> characterNodes = new ArrayList<>();
    private int currentIndex = 0;
    private int correctCount = 0;
    private int totalChars;
    private long startTime;
    private boolean raceStarted = false;

    private String paragraphText;
    private Client client;
    private String playerName;
    private List<String> leaderboard = new ArrayList<>();

    public void setClient(Client client, String playerName) {
        this.client = client;
        this.playerName = playerName;

        client.setOnMessageReceived(message -> {
            if (message.startsWith("PARAGRAPH:")) {
                paragraphText = message.substring(10);
                Platform.runLater(() -> setupParagraph());
            } else if (message.startsWith("RESULT:")) {
                Platform.runLater(() -> updateLeaderboard(message));
            }
        });
    }

    private void setupParagraph() {
        paragraphFlow.getChildren().clear();
        characterNodes.clear();

        for (char c : paragraphText.toCharArray()) {
            Text t = new Text(String.valueOf(c));
            t.setFill(javafx.scene.paint.Color.GRAY);
            t.setFont(Font.font("Consolas", FontWeight.NORMAL, 18));
            characterNodes.add(t);
        }

        paragraphFlow.getChildren().addAll(characterNodes);
        totalChars = characterNodes.size();

        runCountdown();
    }

    private void runCountdown() {
        countdownLabel.setText("3");
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                Platform.runLater(() -> countdownLabel.setText("2"));
                Thread.sleep(1000);
                Platform.runLater(() -> countdownLabel.setText("1"));
                Thread.sleep(1000);
                Platform.runLater(() -> {
                    countdownLabel.setText("Go!");
                    raceStarted = true;
                    startTime = System.currentTimeMillis();
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    public void onKeyPressed(KeyEvent event) {
        if (!raceStarted) return;

        String input = event.getText();
        if (input.isEmpty()) return;

        char typed = input.charAt(0);
        char expected = paragraphText.charAt(currentIndex);
        Text currentText = characterNodes.get(currentIndex);

        if (typed == expected) {
            currentText.setFill(javafx.scene.paint.Color.BLACK);
            correctCount++;
        } else {
            currentText.setFill(javafx.scene.paint.Color.RED);
        }

        currentIndex++;
        updateProgress();

        if (currentIndex >= totalChars) {
            finishRace();
        }
    }

    private void updateProgress() {
        double percentage = (double) currentIndex / totalChars * 100;
        progressLabel.setText(String.format("Progress: %.1f%%", percentage));
    }

    private void finishRace() {
        raceStarted = false;
        long endTime = System.currentTimeMillis();
        double timeTaken = (endTime - startTime) / 1000.0;
        double accuracy = ((double) correctCount / totalChars) * 100;
        double wpm = (paragraphText.split(" ").length / timeTaken) * 60;

        String result = String.format("RESULT:%s;%.2f;%.2f;%.2f",
                playerName, timeTaken, accuracy, wpm);

        client.sendResult(result);
    }

    private void updateLeaderboard(String message) {
        String[] parts = message.substring(7).split(";");
        String entry = String.format("%s - %.2fs - %.2f%% - %.2f WPM",
                parts[0], Double.parseDouble(parts[1]),
                Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));

        leaderboard.add(entry);
        leaderboardList.getItems().setAll(leaderboard);
    }
}
