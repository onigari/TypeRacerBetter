package Controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;

import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import network.Client;

import java.io.IOException;
import java.util.*;
import java.util.stream.IntStream;


public class MultiPlayerGameController {
    public Label titleLabel;
    @FXML private VBox rootPane;
    @FXML private TextFlow paragraphFlow;
    @FXML private Label timeLabel;
    @FXML private Label playerNameLabel;
    @FXML private Label wpmLabel;
    @FXML private Label accuracyLabel;
    @FXML private TextField typingField;
    @FXML private TextField displayField;
    @FXML private ListView<String> leaderboardList;
    @FXML private Label escText;

    private Client client;
    private String playerName;
    private String paragraphText;
    private long startTime;
    private Timeline timer;
    private ObservableList<String> leaderboard = FXCollections.observableArrayList();
    private int totalTyped;
    private int currentIndex;
    private int correctCharCount;
    private boolean currentWordCorrect;
    private final List<Text> textNodes = new ArrayList<>();
    private boolean typingDone = false;
    private boolean isHost;
    private char[] accuracyChecker;
    private char[] correctWordCheker;

    // Add these fields
    private final Map<String, ProgressBar> playerProgressBars = new HashMap<>();
    @FXML private VBox progressBarsContainer;



    public void initialize(Client client, String name, boolean isHost) {
        this.client = client;
        this.playerName = name;
        playerNameLabel.setText(playerName);
        this.leaderboardList.setItems(leaderboard);
        this.isHost = isHost;
        // Initialize with empty progress bar for current player
        addPlayerProgress(playerName);
        setupUI();
        setupNetworkHandlers();
        setupEventHandlers();
        setupGlobalHandlers();
        waitingQueue();
    }

    private void waitingQueue() {
        final int[] timeLeft = {3}; // 3, 2, 1

        Timeline countdown = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (timeLeft[0] > 0) {
                titleLabel.setText("Starting in " + timeLeft[0]);
                timeLeft[0]--;
            } else {
                // Countdown done - run your game logic here
                titleLabel.setText("GO!");
                startTimer();
                typingField.setDisable(false);
                typingField.requestFocus();
            }
        }));

        countdown.setCycleCount(4); // Run 4 times: show 3, show 2, show 1, then start game
        countdown.play();

        // Show initial countdown
        titleLabel.setText("Starting in 3");
    }

    private void loadMainMenu() throws IOException {
        new Thread(() -> {
            client.close();
        }).start();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxmlFiles/MainMenu.fxml"));
        Parent root = loader.load();

        Platform.runLater(() -> {
            Stage stage = (Stage) rootPane.getScene().getWindow();
            Scene scene = new Scene(root, 800, 600);

            stage.setTitle("TypeRacer");
            stage.setResizable(true);
            stage.setScene(scene);
            stage.show();
        });
    }

    private void setupGlobalHandlers() {
        Platform.runLater(() -> {
            rootPane.requestFocus();
            rootPane.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
                if (e.getCode() == KeyCode.ESCAPE) {
                    try {
                        if(isHost) client.closeAll();
                        else loadMainMenu();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            });
        });
    }

    private void setupNetworkHandlers() {
        client.setOnMessageReceived(message -> {
            if (message.startsWith("PARAGRAPH:")) {
                paragraphText = message.substring(10);
                accuracyChecker = new char[paragraphText.length() + 100];
                correctWordCheker = new char[paragraphText.length() + 100];
                Arrays.fill(accuracyChecker, 'B');
                Arrays.fill(accuracyChecker, 'B');
                Platform.runLater(this::setupParagraph);
            } else if (message.startsWith("LEADERBOARD:")) {
                Platform.runLater(() -> updateLeaderboard(message.substring(12)));
            } else if (message.startsWith("PROGRESS:")) {
                Platform.runLater(() -> updateAllProgress(message.substring(9)));
            } else if (message.startsWith("PLAYERS:")) {
                Platform.runLater(() -> {
                    String[] players = message.substring(8).split(",");
                    for (String player : players) {
                        if (!player.isEmpty() && !player.equals(playerName)) {
                            addPlayerProgress(player);
                        }
                    }
                });
            } else if (message.equals("CLOSE_ALL")) {
                try {
                    loadMainMenu();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
//            else if (message.startsWith("CLOSE:")){
//                String removePlayer = message.substring(6);
//                if(!removePlayer.equals(playerName)) removePlayerProgress(removePlayer);
//            }
        });
    }

    private void setupUI() {
        //progressBar.setProgress(0);
        typingField.setDisable(true);
        typingField.setStyle("""
                -fx-opacity: 0;\s
                    -fx-background-color: transparent;
                    -fx-border-color: transparent;""");
        leaderboardList.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-text-fill: #d1d0c5; -fx-font-family: 'Roboto Mono';");
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #d1d0c5; -fx-font-family: 'Roboto Mono';");
                }
            }
        });

        escText.setStyle("-fx-text-fill: #d1d0c5; -fx-font-family: 'Roboto Mono';");
    }

    private void setupParagraph() {
        paragraphFlow.getChildren().clear();
        textNodes.clear();

        for (char c : paragraphText.toCharArray()) {
            Text t = new Text(String.valueOf(c));
            t.setStyle("-fx-fill: #646669;");
            textNodes.add(t);
        }
        paragraphFlow.getChildren().addAll(textNodes);

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
//            if (new String(accuracyChecker).indexOf('F') != -1) {
            if(currentIndex > 0 && correctWordCheker[currentIndex-1] == 'F') {
                showAlert("All words have to be correct!!!");
                return;
            } else handleNewCharacters(oldValue, newValue);
        });

        // Start typing immediately when typing field gets focus
        typingField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal && paragraphText != null && !typingDone) {
                typingField.requestFocus();
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
                if(accuracyChecker[currentIndex] == 'B') {
                    accuracyChecker[currentIndex] = 'T';
                    correctCharCount++;
                }
                correctWordCheker[currentIndex] = 'T';
                current.setStyle("-fx-fill: #d1d0c5;"); // MonkeyType's correct color
            } else {
                accuracyChecker[currentIndex] = 'F';
                correctWordCheker[currentIndex] = 'F';

                current.setStyle("-fx-fill: #ca4754;"); // MonkeyType's incorrect color
                currentWordCorrect = false;
            }

            currentIndex++;
            totalTyped++;

            updateStats();
            if (currentIndex >= paragraphText.length()) {
                typingFinished();
            }
            if (IntStream.range(0, 5).noneMatch(j -> accuracyChecker[currentIndex - j] == 'T')) {
                showAlert("You have to type the correct word!!!!");
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
                    if(accuracyChecker[currentIndex] != 'F') {
                        correctCharCount--;
                        accuracyChecker[currentIndex] = 'F';
                    }
                    correctWordCheker[currentIndex] = 'B';
                }
            }
        }
        updateStats();
    }

    private void updateDisplayField(String input) {
        if (input.contains(" ")) {
            // Reset displayField to show only the next word
            String[] words = paragraphText.split("\\s+");
            int wordIndex = currentIndex / (paragraphText.length() / words.length); // Approximate word index
            if (wordIndex < words.length) {
                displayField.setText(words[wordIndex].trim());
            } else {
                displayField.setText("");
            }
            typingField.setText(""); // Clear typingField after space
        } else {
            // Show the current input as the word being typed
            displayField.setText(input);
        }
    }

    private void updateStats() {
        Platform.runLater(() -> {
            long elapsed = System.currentTimeMillis() - startTime;
            double seconds = elapsed / 1000.0;
            timeLabel.setText(String.format("%.2fs", seconds));
            wpmLabel.setText(String.format("%d", (int) calculateWPM()));
            accuracyLabel.setText(String.format("%.0f%%", calculateAccuracy()));
            double progress = (double) currentIndex / paragraphText.length();
            client.sendProgress(progress); // Send progress update to server

            double time = (System.currentTimeMillis() - startTime) / 1000.0;
            double wpm = calculateWPM();
            double accuracy = calculateAccuracy();
            client.sendResult(String.format("%s;%.2f;%d;%.2f", playerName, time, (int) wpm, accuracy));
        });
    }

    private double calculateAccuracy() {
        return totalTyped == 0 ? 0.0 : Math.max(0, (correctCharCount * 100.0 / totalTyped));
    }

    private double calculateWPM() {
        long elapsedMillis = System.currentTimeMillis() - startTime;
        double elapsedMinutes = elapsedMillis / 60000.0;
        return elapsedMinutes == 0 ? 0 : (correctCharCount / 5.0 / elapsedMinutes);
    }

    private void typingFinished() {
        if (timer != null) timer.stop();
        typingField.setDisable(true);
        double time = (System.currentTimeMillis() - startTime) / 1000.0;
        double wpm = calculateWPM();
        client.sendResult(String.format("%s;%.2f;%.2f", playerName, time, wpm));
    }

    private void updateLeaderboard(String data) {
        String[] entries = data.split("\\|");
        leaderboard.setAll(entries);
//        leaderboard.sort((a,b) -> {
//            double t1 = Double.parseDouble(a.split(" - ")[1].replace("s", ""));
//            double t2 = Double.parseDouble(b.split(" - ")[1].replace("s", ""));
//            int toReturn = Double.compare(t1, t2);
//            if (toReturn == 0) {
//                double t3 = Double.parseDouble(a.split(" - ")[2].replace(" WPM", ""));
//                double t4 = Double.parseDouble(b.split(" - ")[2].replace(" WPM", ""));
//                int toReturn2 = Double.compare(t3, t4);
//                if (toReturn2 == 0) {
//                    return a.split(" - ")[0].compareTo(b.split(" - ")[0]);
//                }
//                return toReturn2;
//            }
//            return toReturn;
//        });
        //leaderboardList.getItems().clear();
        //leaderboardList.setItems(leaderboard);
    }

//    private void removePlayerProgress(String playerName) {
//        ProgressBar pb = playerProgressBars.remove(playerName);
//        if (pb != null) {
//            // Find the HBox in the VBox and remove it
//            progressBarsContainer.getChildren().removeIf(node -> {
//                if (node instanceof HBox hbox) {
//                    return hbox.getChildren().contains(pb);
//                }
//                return false;
//            });
//        }
//    }

    // variable progress bar
    private void addPlayerProgress(String playerName) {
        if (!playerProgressBars.containsKey(playerName)) {
            ProgressBar pb = new ProgressBar(0);
            pb.setPrefWidth(1680);
            pb.setStyle("-fx-accent: " + getColorForPlayer(playerName) + ";");

            HBox playerBox = new HBox(5);
            Label nameLabel = new Label(playerName);
            nameLabel.setStyle("-fx-text-fill: #d1d0c5; -fx-font-family: 'JetBrains Mono Medium'; -fx-min-width: 75;");

            playerBox.getChildren().addAll(nameLabel, pb);
            progressBarsContainer.getChildren().add(playerBox);
            playerProgressBars.put(playerName, pb);
        }
    }

    private String getColorForPlayer(String playerName) {
        // Simple hash-based color assignment
        int hash = playerName.hashCode();
        String[] colors = {"#e2b714", "#d1d0c5", "#ca4754", "#7e57c2", "#26a69a"};
        return colors[Math.abs(hash) % colors.length];
    }

    private void updateAllProgress(String progressData) {
        String[] entries = progressData.split("\\|");
        for (String entry : entries) {
            if (!entry.isEmpty()) {
                String[] parts = entry.split(";");
                if (parts.length >= 2) {
                    String player = parts[0];
                    double progress = Double.parseDouble(parts[1]);

                    Platform.runLater(() -> {
                        if (!playerProgressBars.containsKey(player)) {
                            addPlayerProgress(player);
                        }
                        ProgressBar pb = playerProgressBars.get(player);
                        if (pb != null) {
                            pb.setProgress(progress);
                            // Highlight current player's bar
                            if (player.equals(playerName)) {
                                pb.setStyle("-fx-accent: " + getColorForPlayer(player) + "; -fx-border-color: " + getColorForPlayer(player) + ";");
                            } else {
                                pb.setStyle("-fx-accent: " + getColorForPlayer(player) + ";");
                            }
                        }
                    });
                }
            }
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("TypeRacer");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}