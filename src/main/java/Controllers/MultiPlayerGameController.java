package Controllers;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
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
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import network.Client;

import java.io.IOException;
import java.util.*;
import java.util.stream.IntStream;

import static java.lang.System.out;


public class MultiPlayerGameController {
    public GridPane keyboardRow1;
    public GridPane keyboardRow2;
    public GridPane keyboardRow3;
    public GridPane keyboardRow4;

    @FXML public Label warningText;
    @FXML private Label titleLabel;
    @FXML private Label bigTimerLabel;
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
    @FXML private Button restartButton;

    private final int waitTime = 3;
    private int gameTime;

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
    private char[] correctWordChecker;

    private String[] paragraphWords;
    private int currentWordIndex = 0;
    private int currentWordCharIndex = 0;

    // Add these fields
    private final Map<String, ProgressBar> playerProgressBars = new HashMap<>();
    @FXML private VBox progressBarsContainer;

    private final Map<String, Rectangle> keyRectangles = new HashMap<>();
    private final Map<String, Text> keyTexts = new HashMap<>();

    private static boolean gameRunning;

    private void initializeKeyboard() {
        // Row 1: Tab Q W E R T Y U I O P { }
        String[] row1Keys = {"Tab", "Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P", "{", "}"};
        addKeysToRow(keyboardRow1, row1Keys);

        // Row 2: Caps Lock A S D F G H J K L ; " Enter
        String[] row2Keys = {"Caps Lock", "A", "S", "D", "F", "G", "H", "J", "K", "L", ";", "\"", "Enter"};
        addKeysToRow(keyboardRow2, row2Keys);

        // Row 3: Shift Z X C V B N M < > ? Shift
        String[] row3Keys = {"LShift", "Z", "X", "C", "V", "B", "N", "M", ",", ".", "?", "RShift"};
        addKeysToRow(keyboardRow3, row3Keys);

        // Row 4: Ctrl Alt (space) Alt Ctrl
        String[] row4Keys = {"LCtrl", "LAlt", " ", "RAlt", "RCtrl"};
        addKeysToRow(keyboardRow4, row4Keys);
    }

    private void addKeysToRow(GridPane row, String[] keys) {
        row.getChildren().clear();
        for (int i = 0; i < keys.length; i++) {
            String key = keys[i];


            // Create key rectangle
            Rectangle rect = new Rectangle(40, 40);
            if (key.equals(" ")) {
                rect.setWidth(200); // Make spacebar wider
            } else if (key.endsWith("Ctrl") || key.endsWith("Alt") || key.equals("Tab")) { // For modifier keys
                rect.setWidth(60);
            } else if (key.length() > 1) rect.setWidth(100);
            rect.setArcWidth(5);
            rect.setArcHeight(5);
            rect.setStyle("-fx-fill: #2c2e31; -fx-stroke: #646669; -fx-stroke-width: 1;");

            // Create key text
            Text text = new Text(key);
            text.setStyle("-fx-fill: #d1d0c5; -fx-font-family: 'Roboto Mono'; -fx-font-size: 14px;");

            // Create container and add both
            StackPane container = new StackPane();
            container.getChildren().addAll(rect, text);

            // Add to grid
            row.add(container, i, 0);

            // Store references for highlighting
            if (key.equals(" ")) {
                key = "Space";
            }
            keyRectangles.put(key, rect);
            keyTexts.put(key, text);
        }
    }

    // Add this method to highlight a key
    private void highlightKey(String c, boolean highlight) {
        Platform.runLater(() -> {
            Text text = keyTexts.get(c);
            Rectangle rect = keyRectangles.get(c);

            if (rect != null && text != null) {
                if (highlight) {
                    rect.setStyle("-fx-fill: #e2b714; -fx-stroke: #e2b714; -fx-stroke-width: 1;");
                    text.setStyle("-fx-fill: #323437; -fx-font-family: 'Roboto Mono'; -fx-font-size: 14px;");
                } else {
                    rect.setStyle("-fx-fill: #2c2e31; -fx-stroke: #646669; -fx-stroke-width: 1;");
                    text.setStyle("-fx-fill: #d1d0c5; -fx-font-family: 'Roboto Mono'; -fx-font-size: 14px;");
                }
            } else out.println("null in highlight");
        });
    }
    private void leaderBoardPopUp() {
        FadeTransition ft = new FadeTransition(Duration.millis(200), leaderboardList);
        ft.setFromValue(0);
        ft.setToValue(1);
        leaderboardList.setVisible(true);
        ft.play();
    }

    public void initialize(Client client, String name, boolean isHost, int time) {
        this.client = client;
        this.playerName = name;
        playerNameLabel.setText(playerName);
        this.leaderboardList.setItems(leaderboard);
        this.isHost = isHost;
        restartButton.setDisable(true);
        gameRunning = true;
        gameTime = time;
        // Initialize with empty progress bar for current player
        addPlayerProgress(playerName);
        setupUI();
        setupNetworkHandlers();
        setupEventHandlers();
        setupGlobalHandlers();
        waitingQueue();
        initializeKeyboard();
        client.sendMessage("GET_PARAGRAPH");
    }

    private void waitingQueue() {
        final int[] timeLeft = {waitTime}; // 3, 2, 1

        Timeline countdown = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (timeLeft[0] > 0) {
                bigTimerLabel.setText("Starting in " + timeLeft[0]);
                timeLeft[0]--;
            } else {
                // Countdown done - run your game logic here
                bigTimerLabel.setText("GO!");
                startTimer();
                countDownTimer();
                typingField.setDisable(false);
                typingField.requestFocus();
            }
        }));

        countdown.setCycleCount(waitTime + 1); // Run 4 times: show 3, show 2, show 1, then start game
        countdown.play();

        // Show initial countdown
        bigTimerLabel.setText("Starting in 3");
    }

    private void countDownTimer() {
        final int[] timeLeft = {gameTime};

        Timeline countdown = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (timeLeft[0] > 0 && gameRunning) {
                int seconds = (int) timeLeft[0];
                bigTimerLabel.setText(String.format("Time left: %d seconds", seconds));
                if(timeLeft[0] < 5) {
                    bigTimerLabel.setStyle("-fx-text-fill: #800b0d;");
                }
                timeLeft[0]--;
            } else {
                bigTimerLabel.setText("TIMES UP!");
                typingFinished();
            }
        }));

        countdown.setCycleCount(gameTime + 1);
        countdown.play();

        bigTimerLabel.setText("Time left: " + timeLeft[0] + " seconds");
        timeLeft[0]--;
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

    private void loadLobby() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxmlFiles/MultiPlayerLobby.fxml"));
        Parent root = loader.load();

        MultiPlayerLobbyController controller = loader.getController();
        controller.initialize(client, playerName, isHost);

        Platform.runLater(() -> {
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setScene(new Scene(root, 1420, 800));

            stage.setTitle("Multiplayer Lobby - " + playerName);
            stage.setOnCloseRequest(e -> client.close());
        });
    }

    private void setupGlobalHandlers() {
        Platform.runLater(() -> {
            rootPane.requestFocus();
            rootPane.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
                if (e.getCode() == KeyCode.ESCAPE) {
                    try {
                        out.println("To Close");
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
                paragraphWords = paragraphText.split(" ");
                if (paragraphWords.length > 0) {
                    displayField.setText(paragraphWords[0]);
                }
                accuracyChecker = new char[paragraphText.length() + 1000];
                correctWordChecker = new char[paragraphText.length() + 1000];
                Arrays.fill(accuracyChecker, 'B');
                Arrays.fill(correctWordChecker, 'B');
                Platform.runLater(this::setupParagraph);
                client.sendMessage("GET_PLAYERS");
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
            } else if (message.equals("GAME_FINISHED")) {
                out.println("GAME FINISHED received");
                gameRunning = false;
                restartButton.setDisable(!isHost);
            } else if (message.equals("RESTART")) {
                try {
                    loadLobby();
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
        typingField.setDisable(true);
        displayField.setDisable(true);
        displayField.setEditable(false);

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

            if(currentIndex > 5 && IntStream.range(0, 5).allMatch(j -> correctWordChecker[currentIndex - j] == 'F')) {
                warningText.setText("All words have to be correct!!!");
            }
            handleNewCharacters(oldValue, newValue);

            updateDisplayField(newValue);
        });

        typingField.setOnKeyPressed(e -> {
            // highlight key
            String typedText = e.getCode().getName();
            out.println("typed: " + typedText);
            if(typedText.equals("Alt") || typedText.equals("Ctrl") || typedText.equals("Shift")) {
                highlightKey("L" + typedText, true);
                PauseTransition pause = new PauseTransition(Duration.millis(200));
                pause.setOnFinished(event -> highlightKey("L" + typedText, false));
                pause.play();
                highlightKey("R" + typedText, true);
                pause = new PauseTransition(Duration.millis(200));
                pause.setOnFinished(event -> highlightKey("R" + typedText, false));
                pause.play();
            } else {
                highlightKey(typedText, true);
                PauseTransition pause = new PauseTransition(Duration.millis(200));
                pause.setOnFinished(event -> highlightKey(typedText, false));
                pause.play();
            }
        });

        // Start typing immediately when typing field gets focus
        typingField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal && paragraphText != null && !typingDone) {
                typingField.requestFocus();
            }
        });

    }

    @FXML
    private void onRestartClicked(){
        client.sendMessage("RESTART");
    }

    private void startTimer() {
        startTime = System.currentTimeMillis();
        if (timer != null) timer.stop();
        timer = new Timeline(new KeyFrame(Duration.millis(100), e -> updateStats()));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
//        new Thread(() -> {
//            while (true){
//                boolean check = checkTimer();
//                if(check) break;
//            }
//        }).start();
    }

//    private boolean checkTimer() {
//        long elapsed = System.currentTimeMillis() - startTime;
//        if(elapsed > 10000) {
//            typingFinished();
//            return true;
//        }
//        return false;
//    }

    @FXML
    private void handleNewCharacters(String oldValue, String newValue) {
        for (int i = oldValue.length(); i < newValue.length(); i++) {
            if (currentIndex >= paragraphText.length() && !new String(correctWordChecker).contains("F") && !typingDone) {
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
                correctWordChecker[currentIndex] = 'T';
                current.setStyle("-fx-fill: #d1d0c5;"); // MonkeyType's correct color
            } else {
                accuracyChecker[currentIndex] = 'F';
                correctWordChecker[currentIndex] = 'F';

                current.setStyle("-fx-fill: #ca4754;"); // MonkeyType's incorrect color
                currentWordCorrect = false;
            }

            currentIndex++;
            totalTyped++;

            updateStats();
            if (currentIndex >= paragraphText.length() && !new String(correctWordChecker).contains("F") && !typingDone) {
                typingFinished();
            }
//            } else showAlert("You have to type the correct word!!!!");
            if (currentIndex > 0 && new String(correctWordChecker).contains("F")) {
                warningText.setStyle("-fx-opacity: 1;-fx-font-family: 'Roboto Mono';");
            } else warningText.setStyle("-fx-opacity: 0;-fx-font-family: 'Roboto Mono';");
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
                    correctWordChecker[currentIndex] = 'B';
                }
            }
        }
        if (currentIndex > 0 && new String(correctWordChecker).contains("F")) {
            warningText.setStyle("-fx-opacity: 1;-fx-font-family: 'Roboto Mono';");
        } else warningText.setStyle("-fx-opacity: 0;-fx-font-family: 'Roboto Mono';");
        updateStats();

        updateDisplayField(newValue);
    }

    private void updateDisplayField(String typedText) {
        if (paragraphWords == null || paragraphWords.length == 0) {
            return;
        }

        // Find current word and position based on typed characters
        int charCount = 0;
        int wordIndex = 0;
        int wordCharIndex = 0;

        // Calculate which word we're currently typing
        for (int i = 0; i < paragraphWords.length; i++) {
            if (charCount + paragraphWords[i].length() >= typedText.length()) {
                wordIndex = i;
                wordCharIndex = typedText.length() - charCount;
                break;
            }
            charCount += paragraphWords[i].length() + 1; // +1 for space
            if (charCount > typedText.length()) {
                wordIndex = i;
                wordCharIndex = 0;
                break;
            }
        }

        currentWordIndex = wordIndex;
        currentWordCharIndex = Math.max(0, Math.min(wordCharIndex, paragraphWords[wordIndex].length()));

        // Build display text for current word
        String currentWord = paragraphWords[currentWordIndex];
        StringBuilder displayText = new StringBuilder();

        // Add typed characters with appropriate styling
        for (int i = 0; i < currentWord.length(); i++) {
            displayText.append(currentWord.charAt(i));
        }

        displayField.setText(displayText.toString());

        int wordStartPosition = 0;
        for (int i = 0; i < currentWordIndex; i++) {
            wordStartPosition += paragraphWords[i].length() + 1; // +1 for space
        }

        boolean wordIsCorrectSoFar = true;
        if (typedText.length() > wordStartPosition) {
            String typedPortionOfWord = typedText.substring(wordStartPosition,
                    Math.min(typedText.length(), wordStartPosition + currentWord.length()));

            // Check if typed portion matches the expected word portion
            for (int i = 0; i < typedPortionOfWord.length(); i++) {
                if (i >= currentWord.length() || typedPortionOfWord.charAt(i) != currentWord.charAt(i)) {
                    wordIsCorrectSoFar = false;
                    break;
                }
            }
        }

        // Style the display field based on typing correctness
        if (currentWordCharIndex == 0) {
            // No characters typed yet - default style
            displayField.setStyle("""
            -fx-font-family: 'Roboto Mono';
            -fx-font-size: 24px;
            -fx-text-fill: #646669;
            -fx-background-color: transparent;
            -fx-border-color: transparent;
            -fx-alignment: CENTER_LEFT;
            -fx-border-width: 0 0 1 0;""");
        } else if (wordIsCorrectSoFar) {
            // Correct so far - green text
            displayField.setStyle("""
            -fx-font-family: 'Roboto Mono';
            -fx-font-size: 24px;
            -fx-text-fill: #d1d0c5;
            -fx-background-color: transparent;
            -fx-border-color: transparent;
            -fx-alignment: CENTER_LEFT;
            -fx-border-width: 0 0 1 0;""");
        } else {
            // Incorrect - red text
            displayField.setStyle("""
            -fx-font-family: 'Roboto Mono';
            -fx-font-size: 24px;
            -fx-text-fill: #ca4754;
            -fx-background-color: transparent;
            -fx-border-color: transparent;
            -fx-alignment: CENTER_LEFT;
            -fx-border-width: 0 0 1 0;""");
        }
    }

    private void updateStats() {
        Platform.runLater(() -> {
            long elapsed = System.currentTimeMillis() - startTime;
            double seconds = elapsed / 1000.0;
            timeLabel.setText(String.format("%.1fs", seconds));
            timeLabel.setText(String.format("%d", (int) seconds));
            wpmLabel.setText(String.format("%d", (int) calculateWPM()));
            accuracyLabel.setText(String.format("%.0f%%", calculateAccuracy()));
            double progress = (double) currentIndex / paragraphText.length();
            client.sendProgress(progress); // Send progress update to server

//            double time = (System.currentTimeMillis() - startTime) / 1000.0;
//            double wpm = calculateWPM();
//            double accuracy = calculateAccuracy();
//            client.sendResult(String.format("%s;%.2f;%d;%.2f", playerName, time, (int) wpm, accuracy));
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
        if(typingDone) {
            return;
        }
        leaderBoardPopUp();
        typingDone = true;
        rootPane.requestFocus();
        if (timer != null) timer.stop();
        typingField.setDisable(true);
        displayField.clear(); // Clear display field when finished
        updateStats();
        double time = (System.currentTimeMillis() - startTime) / 1000.0;
        double wpm = calculateWPM();
        double accuracy = calculateAccuracy();
        client.sendResult(String.format("%s;%.0f;%d;%.2f", playerName, time, (int) wpm, accuracy));
        rootPane.requestFocus();
        accuracyChecker = new char[paragraphText.length() + 1000];
        correctWordChecker = new char[paragraphText.length() + 1000];
        Arrays.fill(accuracyChecker, 'B');
        Arrays.fill(correctWordChecker, 'B');
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