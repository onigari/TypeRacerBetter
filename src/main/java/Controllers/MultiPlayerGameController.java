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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;

import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.*;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
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
    public Button backButton;
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
    private boolean isLeaderBoardOn;

    private void initializeKeyboard() {
        String[] row1Keys = {"Tab", "Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P", "{", "}", "Backspace"};
        addKeysToRow(keyboardRow1, row1Keys);

        String[] row2Keys = {"Caps Lock", "A", "S", "D", "F", "G", "H", "J", "K", "L", ";", "'", "Enter"};
        addKeysToRow(keyboardRow2, row2Keys);

        String[] row3Keys = {"LShift", "Z", "X", "C", "V", "B", "N", "M", ",", ".", "?", "RShift"};
        addKeysToRow(keyboardRow3, row3Keys);

        String[] row4Keys = {"LCtrl", "LAlt", " ", "RAlt", "RCtrl"};
        addKeysToRow(keyboardRow4, row4Keys);
    }

    private void addKeysToRow(GridPane row, String[] keys) {
        row.getChildren().clear();
        for (int i = 0; i < keys.length; i++) {
            String key = keys[i];

            Rectangle rect = new Rectangle(40, 40);
            if (key.equals(" ")) {
                rect.setWidth(200); // Make spacebar wider
            } else if (key.endsWith("Ctrl") || key.endsWith("Alt") || key.equals("Tab")) {
                rect.setWidth(60);
            } else if (key.equals("Backspace")) {
                rect.setWidth(120);
            }
            else if (key.length() > 1) rect.setWidth(100);
            rect.setArcWidth(5);
            rect.setArcHeight(5);
            rect.setStyle("-fx-fill: #2c2e31; -fx-stroke: #646669; -fx-stroke-width: 1;");

            Text text = new Text(key);
            text.setStyle("-fx-fill: #d1d0c5; -fx-font-family: 'Roboto Mono'; -fx-font-size: 14px;");

            StackPane container = new StackPane();
            container.getChildren().addAll(rect, text);

            row.add(container, i, 0);

            if (key.equals(" ")) {
                key = "Space";
            } else if (key.equals(",")) {
                key = "Comma";
            } else if (key.equals("'")) {
                key = "Quote";
            } else if (key.equals(".")) {
                key = "Period";
            } else if (key.equals("?")) {
                key = "Slash";
            } else if (key.equals(";")) {
                key = "Semicolon";
            }
            keyRectangles.put(key, rect);
            keyTexts.put(key, text);
        }
    }

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
        Stage leaderboardStage = new Stage();
        leaderboardStage.initModality(Modality.APPLICATION_MODAL);
        leaderboardStage.initOwner(rootPane.getScene().getWindow());
        leaderboardStage.initStyle(StageStyle.TRANSPARENT); // Borderless

        ListView<String> leaderboardList = new ListView<>();
        leaderboardList.setItems(leaderboard);
        leaderboardList.setStyle("""
            -fx-control-inner-background: #2c2e31;
            -fx-padding: 0;
            -fx-background-insets: 0;
            -fx-border-width: 0;
        """);

        leaderboardList.setCellFactory(lv -> new ListCell<>() {
            private final HBox hbox = new HBox(10);
            private final Text rank = new Text();
            private final Text entry = new Text();

            {
                hbox.setAlignment(Pos.CENTER_LEFT);
                hbox.setPadding(new Insets(5));
                rank.setStyle("""
            -fx-fill: #e2b714;
            -fx-font-weight: bold;
            -fx-font-family: 'Roboto Mono';
        """);
                entry.setStyle("""
            -fx-fill: #d1d0c5;
            -fx-font-family: 'Roboto Mono';
        """);
                hbox.getChildren().addAll(rank, entry);
                setPrefHeight(36);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    rank.setText((getIndex() + 1) + ".");
                    entry.setText(item);

                    if (item.contains(playerName)) {
                        hbox.setStyle("-fx-background-color: #3a3d42;");
                        rank.setStyle("""
                    -fx-fill: #e2b714;
                    -fx-font-weight: bold;
                    -fx-font-family: 'Roboto Mono';
                """);
                        entry.setStyle("""
                    -fx-fill: #e2b714;
                    -fx-font-weight: bold;
                    -fx-font-family: 'Roboto Mono';
                """);
                    } else {
                        hbox.setStyle("-fx-background-color: " + (getIndex() % 2 == 0 ? "#2c2e31;" : "#323437;"));
                        rank.setStyle("""
                    -fx-fill: #d1d0c5;
                    -fx-font-weight: bold;
                    -fx-font-family: 'Roboto Mono';
                """);
                        entry.setStyle("""
                    -fx-fill: #d1d0c5;
                    -fx-font-family: 'Roboto Mono';
                """);
                    }

                    setStyle("-fx-background-insets: 0; -fx-padding: 0;");
                    setGraphic(hbox);
                }
            }
        });
        Label escText = new Label();
        escText.setText("Press esc to close");
        escText.setStyle("-fx-font-size: 14px; -fx-text-fill: #d1d0c5;");

        Label header = new Label("LEADERBOARD");
        header.setStyle("-fx-text-fill: #e2b714; -fx-font-size: 24px; -fx-font-weight: bold;");

        Button closeBtn = new Button("✕");
        closeBtn.setStyle("""
        -fx-background-color: transparent;
        -fx-text-fill: #d1d0c5;
        -fx-font-size: 16px;
        -fx-font-weight: bold;
        -fx-padding: 0 8 0 8;
        -fx-cursor: hand;
        -fx-font-family: 'Roboto Mono';
        """);
        closeBtn.setOnAction(e -> leaderboardStage.close());
        closeBtn.hoverProperty().addListener((obs, oldVal, isHovering) -> {
            closeBtn.setStyle(isHovering ?
                    "-fx-background-color: transparent; -fx-text-fill: #ca4754; -fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 0 8 0 8; -fx-cursor: hand; -fx-font-family: 'Roboto Mono';" :
                    "-fx-background-color: transparent; -fx-text-fill: #d1d0c5; -fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 0 8 0 8; -fx-cursor: hand; -fx-font-family: 'Roboto Mono';");
        });

        HBox titleBar = new HBox(header, new Region(), closeBtn);
        titleBar.setAlignment(Pos.CENTER_RIGHT);
        titleBar.setPadding(new Insets(10, 10, 10, 20));
        titleBar.setStyle("-fx-background-color: #2c2e31;");
        HBox.setHgrow(titleBar.getChildren().get(1), Priority.ALWAYS);

        VBox root = new VBox(titleBar, leaderboardList);
        root.setStyle("""
        -fx-background-color: #323437;
        -fx-border-color: #e2b714;
        -fx-border-width: 2px;
        -fx-border-radius: 5;
        -fx-background-radius: 5;
        -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 10, 0, 0, 0);
        """);

        final double[] xOffset = new double[1];
        final double[] yOffset = new double[1];
        titleBar.setOnMousePressed(event -> {
            xOffset[0] = event.getSceneX();
            yOffset[0] = event.getSceneY();
        });
        titleBar.setOnMouseDragged(event -> {
            leaderboardStage.setX(event.getScreenX() - xOffset[0]);
            leaderboardStage.setY(event.getScreenY() - yOffset[0]);
        });

        root.getChildren().add(escText);
        escText.setTranslateX(140);

        Scene scene = new Scene(root, 400, 500);
        leaderboardList.setFocusTraversable(false);
        leaderboardList.setMouseTransparent(false);
        scene.setFill(Color.TRANSPARENT); // For rounded corners
        leaderboardStage.setScene(scene);
        root.setFocusTraversable(true);
        Platform.runLater(root::requestFocus);

        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        leaderboardStage.setX((screenBounds.getWidth() - scene.getWidth()) / 2);
        leaderboardStage.setY((screenBounds.getHeight() - scene.getHeight()) / 2);

        root.setOpacity(0);
        leaderboardStage.show();
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), root);
        fadeIn.setToValue(1);
        fadeIn.play();

        scene.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                isLeaderBoardOn = false;
                leaderboardStage.close();
            }
        });
    }

    public void initialize(Client client, String name, boolean isHost, int time) {
        this.client = client;
        this.playerName = name;
        playerNameLabel.setText(playerName);
        this.leaderboardList.setItems(leaderboard);
        this.isHost = isHost;
        restartButton.setDisable(true);
        gameRunning = true;
        client.setGameRunning(true);
        gameTime = time;
        isLeaderBoardOn = false;

        addPlayerProgress(playerName);
        setupUI();
        setupNetworkHandlers();
        setupEventHandlers();
        setupGlobalHandlers();
        waitingQueue();
        initializeKeyboard();
        client.sendMessage("GET_PLAYERS");
    }

    private void waitingQueue() {
        final int[] timeLeft = {waitTime}; // 3, 2, 1
        bigTimerLabel.setStyle("-fx-font-size: 14;");

        Timeline countdown = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (timeLeft[0] > 0) {
                bigTimerLabel.setText("Starting in " + timeLeft[0]);
                timeLeft[0]--;
            } else {
                bigTimerLabel.setText("GO!");
                startTimer();
                countDownTimer();
                typingField.setDisable(false);
                typingField.requestFocus();
            }
        }));

        countdown.setCycleCount(waitTime + 1);
        countdown.play();

        bigTimerLabel.setText("Starting in 3");
    }

    private void countDownTimer() {
        final int[] timeLeft = {gameTime*100};

        Timeline countdown = new Timeline(new KeyFrame(Duration.millis(10), e -> {
            if (timeLeft[0] > 0 && gameRunning) {
                int seconds = (int) (timeLeft[0]/(double)100);
                bigTimerLabel.setText(String.format("Time left: %d seconds", seconds));
                if(seconds <= 5) {
                    bigTimerLabel.setStyle("-fx-text-fill: #f20909; -fx-font-size: 14;");
                }
                timeLeft[0]--;
            } else {
                bigTimerLabel.setText("TIMES UP!");
                if(!typingDone) {
                    warningText.setStyle("-fx-opacity: 1;-fx-font-family: 'Roboto Mono'; -fx-font-size: 14; -fx-text-fill: #f20909;");
                    warningText.setText("TYPING UNFINISHED");
                    typingFinished();
                }
            }
            if(typingDone && timeLeft[0] > 0) {
                warningText.setStyle("-fx-opacity: 1;-fx-font-family: 'Roboto Mono'; -fx-font-size: 14; -fx-text-fill: #66993C;");
                warningText.setText("TYPING FINISHED");
            }
        }));

        countdown.setCycleCount(gameTime*100 + 1);
        countdown.play();

        bigTimerLabel.setText("Time left: " + timeLeft[0] + " seconds");
        timeLeft[0]--;
    }

    private void loadMultiPlayerChoice() throws IOException {
        new Thread(() -> {
            client.close();
        }).start();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxmlFiles/MultiPlayerChoice.fxml"));
        Parent root = loader.load();

        Platform.runLater(() -> {
            Stage stage = (Stage) rootPane.getScene().getWindow();
            Scene scene = new Scene(root, 800, 600);

            stage.setTitle("TypeRacer - MultiPlayer Choice");
            stage.setResizable(true);
            stage.setScene(scene);
            stage.centerOnScreen();
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
            stage.setScene(new Scene(root, 800, 600));

            stage.setTitle("Multiplayer Lobby - " + playerName);
            stage.centerOnScreen();
            stage.setOnCloseRequest(e -> client.close());
        });
    }

    private void setupGlobalHandlers() {
        Platform.runLater(() -> {
            rootPane.requestFocus();
            rootPane.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
                if (e.getCode() == KeyCode.ESCAPE) {
                    try {
                        if(isHost) client.closeAll();
                        else{
                            typingDone = true;
                            loadMultiPlayerChoice();
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
                if (e.getCode() == KeyCode.CONTROL) {
                   if(!isLeaderBoardOn && typingDone) {
                       isLeaderBoardOn = true;
                       leaderBoardPopUp();
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
            } else if (message.startsWith("LEADERBOARD:")) {
                Platform.runLater(() -> updateLeaderboard(message.substring(12)));
            } else if (message.startsWith("PROGRESS:")) {
                Platform.runLater(() -> updateAllProgress(message.substring(9)));
            } else if (message.startsWith("PLAYERS:")) {
                out.println(message);
                Platform.runLater(() -> {
                    String[] players = message.substring(8).split(",");
                    playerProgressBars.clear();
                    progressBarsContainer.getChildren().clear();
                    out.println(progressBarsContainer.getChildren().isEmpty());
                    addPlayerProgress(playerName);
                    for (String player : players) {
                        if (!player.equals(playerName)) {
                            addPlayerProgress(player);
                        }
                    }
                });
                if(paragraphText == null) client.sendMessage("GET_PARAGRAPH");
            } else if (message.equals("CLOSE_ALL")) {
                try {
                    typingDone = true;
                    loadMultiPlayerChoice();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else if (message.equals("GAME_FINISHED")) {
                out.println("GAME FINISHED received");
                gameRunning = false;
                client.setGameRunning(false);
                restartButton.setDisable(!isHost);
                restartButton.setVisible(true);
            } else if (message.equals("RESTART")) {
                try {
                    loadLobby();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
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

        leaderboardList.setCellFactory(lv -> new ListCell<String>() {
            private final HBox hbox = new HBox(10);
            private final Text rank = new Text();
            private final Text entry = new Text();

            {
                hbox.setAlignment(Pos.CENTER_LEFT);
                rank.setStyle("-fx-fill: #e2b714; -fx-font-weight: bold;");
                hbox.getChildren().addAll(rank, entry);

                setStyle("-fx-background-color: #2c2e31; -fx-text-fill: #d1d0c5;");
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    rank.setText((getIndex() + 1) + ".");
                    entry.setText(item);

                    if (item.contains(playerNameLabel.getText())) {
                        setStyle("-fx-background-color: #3a3d42; -fx-text-fill: #e2b714;"); // Highlight current client's entry
                        entry.setStyle("-fx-fill: #e2b714; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-background-color: " + (getIndex() % 2 == 0 ? "#2c2e31" : "#323437") + ";");
                        entry.setStyle("-fx-fill: #d1d0c5;");
                    }

                    setGraphic(hbox);
                }
            }
        });

        restartButton.setPrefSize(83, 31);
        restartButton.setStyle("-fx-background-color: #2c2e31; -fx-text-fill: #d1d0c5; -fx-font-family: 'Roboto Mono'; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 5 10; -fx-background-radius: 5;");
        restartButton.setOnMouseEntered(e -> {
            restartButton.setStyle("-fx-background-color: #e2b714; -fx-text-fill: #323437; -fx-font-family: 'Roboto Mono'; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 5 10; -fx-background-radius: 5;");
        });
        restartButton.setOnMouseExited(e -> {
            restartButton.setStyle("-fx-background-color: #2c2e31; -fx-text-fill: #d1d0c5; -fx-font-family: 'Roboto Mono'; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 5 10; -fx-background-radius: 5;");
        });

        restartButton.setVisible(false);

        backButton.setText("\uD83E\uDC20back");
        backButton.setStyle("""
        -fx-background-color: transparent;
        -fx-text-fill: #d1d0c5;
        -fx-font-size: 16px;
        -fx-font-weight: bold;
        -fx-padding: 0 8 0 8;
        -fx-cursor: hand;
        -fx-font-family: 'Roboto Mono';
        """);
        backButton.setOnAction(e -> {
            try {
                if(isHost) client.closeAll();
                else{
                    typingDone = true;
                    loadMultiPlayerChoice();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        backButton.hoverProperty().addListener((obs, oldVal, isHovering) -> {
            backButton.setStyle(isHovering ?
                    "-fx-background-color: transparent; -fx-text-fill: #ca4754; -fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 0 8 0 8; -fx-cursor: hand; -fx-font-family: 'Roboto Mono';" :
                    "-fx-background-color: transparent; -fx-text-fill: #d1d0c5; -fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 0 8 0 8; -fx-cursor: hand; -fx-font-family: 'Roboto Mono';");
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
        typingField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (paragraphText == null || paragraphText.isEmpty() || typingDone) return;

            if (newValue.length() < oldValue.length()) {
                handleBackspace(oldValue, newValue);
                return;
            }

            if(currentIndex > 5 && IntStream.range(0, 5).allMatch(j -> correctWordChecker[currentIndex - j] == 'F')) {
                warningText.setText("All words have to be correct!!!");
                warningText.setStyle("-fx-font-size: 14");
            }
            handleNewCharacters(oldValue, newValue);

            updateDisplayField(newValue);
        });

        typingField.setOnKeyPressed(e -> {
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

            if (e.getCode() == KeyCode.CONTROL) {
                leaderBoardPopUp();
            }
        });

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
    }

    @FXML
    private void handleNewCharacters(String oldValue, String newValue) {
        for (int i = oldValue.length(); i < newValue.length(); i++) {
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
                current.setStyle("-fx-fill: #d1d0c5;");
            } else {
                accuracyChecker[currentIndex] = 'F';
                correctWordChecker[currentIndex] = 'F';

                current.setStyle("-fx-fill: #ca4754;");
                currentWordCorrect = false;
            }

            currentIndex++;
            totalTyped++;

            updateStats();
            if (currentIndex > 0 && new String(correctWordChecker).contains("F")) {
                warningText.setStyle("-fx-opacity: 1;-fx-font-family: 'Roboto Mono';");
            } else warningText.setStyle("-fx-opacity: 0;-fx-font-family: 'Roboto Mono';");

            if (currentIndex >= paragraphText.length() && !new String(correctWordChecker).contains("F")) {
                typingFinished();
                return;
            }
        }
    }

    private void handleBackspace(String oldValue, String newValue) {
        int diff = oldValue.length() - newValue.length();
        for (int i = 0; i < diff; i++) {
            if (currentIndex > 0) {
                currentIndex--;
                Text previous = textNodes.get(currentIndex);
                previous.setStyle("-fx-fill: #646669;");
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

        int charCount = 0;
        int wordIndex = 0;
        int wordCharIndex = 0;

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

        String currentWord = paragraphWords[currentWordIndex];
        StringBuilder displayText = new StringBuilder();

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

            for (int i = 0; i < typedPortionOfWord.length(); i++) {
                if (i >= currentWord.length() || typedPortionOfWord.charAt(i) != currentWord.charAt(i)) {
                    wordIsCorrectSoFar = false;
                    break;
                }
            }
        }

        if (currentWordCharIndex == 0) {
            displayField.setStyle("""
            -fx-font-family: 'Roboto Mono';
            -fx-font-size: 24px;
            -fx-text-fill: #646669;
            -fx-background-color: transparent;
            -fx-border-color: transparent;
            -fx-alignment: CENTER_LEFT;
            -fx-border-width: 0 0 1 0;""");
        } else if (wordIsCorrectSoFar) {
            displayField.setStyle("""
            -fx-font-family: 'Roboto Mono';
            -fx-font-size: 24px;
            -fx-text-fill: #d1d0c5;
            -fx-background-color: transparent;
            -fx-border-color: transparent;
            -fx-alignment: CENTER_LEFT;
            -fx-border-width: 0 0 1 0;""");
        } else {
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
            client.sendProgress(progress);// Send progress update to server
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
        typingDone = true;
        leaderBoardPopUp();
        rootPane.requestFocus();
        if (timer != null) timer.stop();
        typingField.setDisable(true);
        displayField.clear();
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
        for(String entry : entries) {
            out.println(entry);
        }
        leaderboard.setAll(entries);
    }

    private void addPlayerProgress(String playerName) {
        if (!playerProgressBars.containsKey(playerName)) {
            ProgressBar pb = new ProgressBar(0);
            pb.setPrefWidth(1680);
            pb.setStyle("-fx-accent: " + getColorForPlayer(playerName) + ";");

            HBox playerBox = new HBox(5);
            playerBox.setUserData(playerName);

            Label nameLabel = new Label(playerName);
            nameLabel.setStyle("-fx-text-fill: #d1d0c5; -fx-font-family: 'JetBrains Mono Medium'; -fx-min-width: 75;");

            playerBox.getChildren().addAll(nameLabel, pb);
            progressBarsContainer.getChildren().add(playerBox);
            playerProgressBars.put(playerName, pb);
        }
    }

    private String getColorForPlayer(String playerName) {
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
}