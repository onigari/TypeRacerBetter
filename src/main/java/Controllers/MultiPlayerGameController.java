package Controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.*;
import javafx.util.Duration;
import network.Client;

import java.util.ArrayList;
import java.util.List;

public class MultiPlayerGameController {
    @FXML private TextFlow paragraphFlow;
    @FXML private Label timeLabel;
    @FXML private Label playerNameLabel;
    @FXML private ProgressBar progressBar;
    @FXML private ListView<String> leaderboard;
    @FXML private Label wpmLabel;
    @FXML private Label accuracyLabel;
    @FXML private TextField typingField;
    @FXML private ListView<String> leaderboardList;

    private Client client;
    private String playerName;
    private String paragraphText;
    private long startTime;
    private Timeline timer;
    private ObservableList<String> leaderboardData = FXCollections.observableArrayList();
    private int totalTyped;
    private int currentIndex;
    private int correctCharCount;
    private int correctWordCount;
    private boolean currentWordCorrect;
    private final List<Text> textNodes = new ArrayList<>();
    private boolean typingDone = false;


    public void initialize(Client client, String name) {
        this.client = client;
        this.playerName = name;
        playerNameLabel.setText(playerName);
        this.leaderboard.setItems(leaderboardData);
        setupNetworkHandlers();
        requestParagraph();
        setupUI();
        setupEventHandlers();
    }

    private void setupNetworkHandlers() {
        client.setOnMessageReceived(message -> {
            if (message.startsWith("LEADERBOARD:")) {
                Platform.runLater(() -> updateLeaderboard(message.substring(12)));
            }
        });
    }

    private void requestParagraph() {
        client.sendMessage("SEND_PARA");
        client.setOnMessageReceived(message -> {
            if (message.startsWith("PARAGRAPH:")) {
                paragraphText = message.substring(10);
//                client.sendDebugMessage("WE GOT PARA::: " + paragraph);
                Platform.runLater(this::setupParagraph);
            }
        });
    }

    private void setupUI() {
        progressBar.setProgress(0);
        typingField.setDisable(true);
//        leaderboardList.setCellFactory(lv -> new ListCell<String>() {
//            @Override
//            protected void updateItem(String item, boolean empty) {
//                super.updateItem(item, empty);
//                if (empty || item == null) {
//                    setText(null);
//                    setStyle("-fx-text-fill: #d1d0c5; -fx-font-family: 'Roboto Mono';");
//                } else {
//                    setText(item);
//                    setStyle("-fx-text-fill: #d1d0c5; -fx-font-family: 'Roboto Mono';");
//                }
//            }
//        });
    }

    private void setupParagraph() {
        paragraphFlow.getChildren().clear();
        for (char c : paragraphText.toCharArray()) {
            Text t = new Text(String.valueOf(c));
            t.setStyle("-fx-fill: gray; -fx-font-size: 16;");
            paragraphFlow.getChildren().add(t);
        }
        startTimer();
    }

    private void setupEventHandlers() {
        // Typing field listener for character-by-character comparison
        typingField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (paragraphText == null || paragraphText.isEmpty() || typingDone) return;

            // Handle backspace
            if (newValue.length() < oldValue.length()) {
                handleBackspace(oldValue, newValue);
                return;
            }
            // Handle new characters
            handleNewCharacters(oldValue, newValue);
        });

        TODO:
        // On pressing space or end of game

        // Enter key to finish
        typingField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                typingFinished();
            }
        });

        // Start typing immediately when typing field gets focus
        typingField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal && paragraphText != null && !typingDone) {
                typingField.clear();
            }
        });

    }

    private void startTimer() {
        startTime = System.currentTimeMillis();
        if (timer != null) timer.stop();
        timer = new Timeline(new KeyFrame(Duration.millis(100), e -> updateStats()));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
    }

    @FXML
    private void handleNewCharacters(String oldValue, String newValue) {
        for (int i = oldValue.length(); i < newValue.length(); i++) {
            if (currentIndex >= paragraphText.length()) {
                typingFinished();
                return;
            }

            char typedChar = newValue.charAt(i);
            char expectedChar = paragraphText.charAt(currentIndex);
            Text current = textNodes.get(currentIndex);
            textNodes.get(currentIndex).setUnderline(true);

            if (typedChar == expectedChar) {
                current.setStyle("-fx-fill: #d1d0c5;"); // MonkeyType's correct color
                correctCharCount++;
            } else {
                current.setStyle("-fx-fill: #ca4754;"); // MonkeyType's incorrect color
                currentWordCorrect = false;
            }

            if (typedChar == ' ') {
                if (currentWordCorrect) correctWordCount++;
                currentWordCorrect = true;
//                typingField.replaceSelection("");
            }
            currentIndex++;
            totalTyped++;
            progressBar.setProgress((double) currentIndex / paragraphText.length());
            updateStats();

            if (currentIndex >= paragraphText.length()) {
                typingFinished();
            }
        }
    }

    private void handleBackspace(String oldValue, String newValue) {
        int diff = oldValue.length() - newValue.length();
        for (int i = 0; i < diff; i++) {
            if (currentIndex > 0) {
                currentIndex--;
                Text previous = textNodes.get(currentIndex);
                previous.setStyle("-fx-fill: #646669;"); // MonkeyType's untyped color
                previous.setUnderline(false); // underline
                totalTyped = Math.max(0, totalTyped - 1);

                if (paragraphText.charAt(currentIndex) == previous.getText().charAt(0)) {
                    correctCharCount--;
                }
            }
        }
        updateStats();
    }

    private void updateStats() {
        Platform.runLater(() -> {
            long elapsed = System.currentTimeMillis() - startTime;
            double seconds = elapsed / 1000.0;
            timeLabel.setText(String.format("%.2fs", seconds));
            wpmLabel.setText(String.format("%.0f", calculateWPM()));
            accuracyLabel.setText(String.format("%.0f%%", calculateAccuracy()));
        });
    }

    private double calculateAccuracy() {
        return totalTyped == 0 ? 0.0 : Math.max(0, (correctCharCount * 100.0 / totalTyped));
    }

    private double calculateWPM() {
        long elapsedMillis = System.currentTimeMillis() - startTime;
        double elapsedMinutes = elapsedMillis / 60000.0;
        return elapsedMinutes == 0 ? 0 : (correctWordCount / elapsedMinutes);
    }

    private void typingFinished() {
        if (timer != null) timer.stop();
        double time = (System.currentTimeMillis() - startTime) / 1000.0;
        int wordCount = paragraphText.split("\\s+").length;
        double wpm = (wordCount / time) * 60;
        client.sendResult(String.format("%s;%.2f;%.2f", playerName, time, wpm));
    }

    private void updateLeaderboard(String data) {
        leaderboardData.setAll(data.split("\\|"));
    }
}