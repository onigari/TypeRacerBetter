package Controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.scene.text.*;
import javafx.util.Duration;
import network.Client;

public class MultiPlayerGameController {
    @FXML private TextFlow paragraphFlow;
    @FXML private Label timerLabel;
    @FXML private Label playerNameLabel;
    @FXML private ProgressBar progressBar;
    @FXML private ListView<String> leaderboard;

    private Client client;
    private String playerName;
    private String paragraph;
    private long startTime;
    private Timeline timer;
    private ObservableList<String> leaderboardData = FXCollections.observableArrayList();

    public void initialize(Client client, String name) {
        this.client = client;
        this.playerName = name;
        playerNameLabel.setText(playerName);
        this.leaderboard.setItems(leaderboardData);
        setupNetworkHandlers();
        client.sendDebugMessage("SEND_PARA");
    }

    private void setupNetworkHandlers() {
        client.setOnMessageReceived(message -> {
            if (message.startsWith("PARAGRAPH:")) {
                paragraph = message.substring(10);
                client.sendDebugMessage("WE GOT PARA::: " + paragraph);
                Platform.runLater(this::setupParagraph);
            } else if (message.startsWith("LEADERBOARD:")) {
                Platform.runLater(() -> updateLeaderboard(message.substring(12)));
            }
        });
    }

    private void setupParagraph() {
        paragraphFlow.getChildren().clear();
        for (char c : paragraph.toCharArray()) {
            Text t = new Text(String.valueOf(c));
            t.setStyle("-fx-fill: gray; -fx-font-size: 16;");
            paragraphFlow.getChildren().add(t);
        }
        startTimer();
    }

    private void startTimer() {
        startTime = System.currentTimeMillis();
        timer = new Timeline(new KeyFrame(Duration.millis(100), e -> {
            double elapsed = (System.currentTimeMillis() - startTime) / 1000.0;
            timerLabel.setText(String.format("%.1fs", elapsed));
        }));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
    }

    @FXML
    private void handleKeyTyped(KeyEvent event) {
        if (paragraph == null) return;

        String typed = event.getCharacter();
        int currentPos = paragraphFlow.getChildren().size();

        if (currentPos < paragraph.length()) {
            Text t = (Text) paragraphFlow.getChildren().get(currentPos);
            if (typed.charAt(0) == paragraph.charAt(currentPos)) {
                t.setFill(javafx.scene.paint.Color.GREEN);
            } else {
                t.setFill(javafx.scene.paint.Color.RED);
            }

            progressBar.setProgress((double)(currentPos+1)/paragraph.length());

            if (currentPos == paragraph.length()-1) {
                finishGame();
            }
        }
    }

    private void finishGame() {
        if (timer != null) timer.stop();
        double time = (System.currentTimeMillis() - startTime) / 1000.0;
        int wordCount = paragraph.split("\\s+").length;
        double wpm = (wordCount / time) * 60;
        client.sendResult(String.format("%s;%.2f;%.2f", playerName, time, wpm));
    }

    private void updateLeaderboard(String data) {
        leaderboardData.setAll(data.split("\\|"));
    }
}