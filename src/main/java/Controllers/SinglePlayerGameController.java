package Controllers;

import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.application.Platform;
import javafx.animation.*;
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

import java.io.*;
import java.util.*;

import static java.lang.System.out;

public class SinglePlayerGameController {
    public GridPane keyboardRow1;
    public GridPane keyboardRow2;
    public GridPane keyboardRow3;
    public GridPane keyboardRow4;
    public Label timeTitle;
    public Label wpmTitle;
    public Label accuracyTitle;
    public Label nameTitle;

    @FXML private VBox rootPane;
    @FXML private Label titleLabel;
    @FXML private TextField playerNameField;
    @FXML private TextFlow paragraphFlow;
    @FXML private ProgressBar progressBar;
    @FXML private Button startButton;
    @FXML private ListView<String> leaderboardList;
    @FXML private Label timeLabel;
    @FXML private TextField typingField;
    @FXML private TextField displayField;
    @FXML private Label wpmLabel;
    @FXML private Label accuracyLabel;
    @FXML private Label escText;

    private final List<String> inputStrings = new ArrayList<>();
    private final ObservableList<String> leaderboard = FXCollections.observableArrayList();
    private String playerName;
    private long startTime;
    private Timeline timer;
    private boolean typingDone = false;
    private String paragraphText; //User's input
    private final List<Text> textNodes = new ArrayList<>();
    private int currentIndex;
    private int correctCharCount;
    private int totalTyped;
    private char[] accuracyChecker;
    private char[] correctWordChecker;

    private String[] paragraphWords;
    private int currentWordIndex = 0;
    private int currentWordCharIndex = 0;

    private final Map<String, Rectangle> keyRectangles = new HashMap<>();
    private final Map<String, Text> keyTexts = new HashMap<>();

    private enum GameMode {
        TIME_15, TIME_30, TIME_60, WORDS_10, WORDS_25, WORDS_50, WORDS_100
    }
    private GameMode currentMode = GameMode.TIME_60; // Default mode
    private HBox modeContainer; // Moved to class level for visibility control
    private Label modeInstructionLabel;
    private int TIMED_MODE_DURATION;
    private int FIXED_PARAGRAPH_LENGTH;
    private String oldPara = "";
    private final List<Double> wpmDataPoints = new ArrayList<>();
    private long lastWpmUpdateTime = 0;

    private void setupModeSelection() {
        modeInstructionLabel = new Label();
        modeInstructionLabel.setStyle("-fx-text-fill: #e2b714; -fx-font-family: 'Roboto Mono'; -fx-font-size: 16px;");
        updateModeInstructions();

        modeContainer = new HBox(10);
        modeContainer.setAlignment(Pos.CENTER);
        modeContainer.setPadding(new Insets(10));
        modeContainer.setStyle("-fx-background-color: #2c2e31; -fx-border-radius: 5; -fx-background-radius: 5;");

        Button timeButton = new Button("Time");
        styleModeButton(timeButton, currentMode.toString().startsWith("TIME"));
        timeButton.setOnAction(e -> showTimeOptions());

        Button wordsButton = new Button("Words");
        styleModeButton(wordsButton, currentMode.toString().startsWith("WORDS"));
        wordsButton.setOnAction(e -> showWordsOptions());

        modeContainer.getChildren().addAll(timeButton, wordsButton, modeInstructionLabel);
        rootPane.getChildren().add(1, modeContainer); // Add below title
        showTimeOptions();
    }

    private void styleModeButton(Button button, boolean selected) {
        button.setStyle(selected ?
                "-fx-background-color: #e2b714; -fx-text-fill: #323437; -fx-font-family: 'Roboto Mono'; -fx-font-size: 14px; -fx-padding: 5 10; -fx-background-radius: 5;" :
                "-fx-background-color: #323437; -fx-text-fill: #d1d0c5; -fx-font-family: 'Roboto Mono'; -fx-font-size: 14px; -fx-padding: 5 10; -fx-background-radius: 5;");
        button.setOnMouseEntered(e -> {
            if (!button.getStyle().contains("-fx-background-color: #e2b714")) {
                button.setStyle("-fx-background-color: #3a3d42; -fx-text-fill: #d1d0c5; -fx-font-family: 'Roboto Mono'; -fx-font-size: 14px; -fx-padding: 5 10; -fx-background-radius: 5;");
            }
        });
        button.setOnMouseExited(e -> {
            if (!button.getStyle().contains("-fx-background-color: #e2b714")) {
                button.setStyle("-fx-background-color: #323437; -fx-text-fill: #d1d0c5; -fx-font-family: 'Roboto Mono'; -fx-font-size: 14px; -fx-padding: 5 10; -fx-background-radius: 5;");
            }
        });
    }

    private void showTimeOptions() {
        modeContainer.getChildren().clear();
        Button timeButton = new Button("Time");
        styleModeButton(timeButton, true);
        Button wordsButton = new Button("Words");
        styleModeButton(wordsButton, false);
        wordsButton.setOnAction(e -> showWordsOptions());

        Button time15 = new Button("15s");
        styleOptionButton(time15, currentMode == GameMode.TIME_15);
        time15.setOnAction(e -> setMode(GameMode.TIME_15));

        Button time30 = new Button("30s");
        styleOptionButton(time30, currentMode == GameMode.TIME_30);
        time30.setOnAction(e -> setMode(GameMode.TIME_30));

        Button time60 = new Button("60s");
        styleOptionButton(time60, currentMode == GameMode.TIME_60);
        time60.setOnAction(e -> setMode(GameMode.TIME_60));

        modeContainer.getChildren().addAll(timeButton, wordsButton, time15, time30, time60, modeInstructionLabel);
    }

    private void showWordsOptions() {
        modeContainer.getChildren().clear();
        Button timeButton = new Button("Time");
        styleModeButton(timeButton, false);
        timeButton.setOnAction(e -> showTimeOptions());
        Button wordsButton = new Button("Words");
        styleModeButton(wordsButton, true);

        Button words10 = new Button("10");
        styleOptionButton(words10, currentMode == GameMode.WORDS_10);
        words10.setOnAction(e -> setMode(GameMode.WORDS_10));

        Button words25 = new Button("25");
        styleOptionButton(words25, currentMode == GameMode.WORDS_25);
        words25.setOnAction(e -> setMode(GameMode.WORDS_25));

        Button words50 = new Button("50");
        styleOptionButton(words50, currentMode == GameMode.WORDS_50);
        words50.setOnAction(e -> setMode(GameMode.WORDS_50));

        Button words100 = new Button("100");
        styleOptionButton(words100, currentMode == GameMode.WORDS_100);
        words100.setOnAction(e -> setMode(GameMode.WORDS_100));

        modeContainer.getChildren().addAll(timeButton, wordsButton, words10, words25, words50, words100, modeInstructionLabel);
    }

    private void styleOptionButton(Button button, boolean selected) {
        button.setStyle(selected ?
                "-fx-background-color: #e2b714; -fx-text-fill: #323437; -fx-font-family: 'Roboto Mono'; -fx-font-size: 14px; -fx-padding: 5 10; -fx-background-radius: 5;" :
                "-fx-background-color: #323437; -fx-text-fill: #d1d0c5; -fx-font-family: 'Roboto Mono'; -fx-font-size: 14px; -fx-padding: 5 10; -fx-background-radius: 5;");
        button.setOnMouseEntered(e -> {
            if (!button.getStyle().contains("-fx-background-color: #e2b714")) {
                button.setStyle("-fx-background-color: #3a3d42; -fx-text-fill: #d1d0c5; -fx-font-family: 'Roboto Mono'; -fx-font-size: 14px; -fx-padding: 5 10; -fx-background-radius: 5;");
            }
        });
        button.setOnMouseExited(e -> {
            if (!button.getStyle().contains("-fx-background-color: #e2b714")) {
                button.setStyle("-fx-background-color: #323437; -fx-text-fill: #d1d0c5; -fx-font-family: 'Roboto Mono'; -fx-font-size: 14px; -fx-padding: 5 10; -fx-background-radius: 5;");
            }
        });
    }

    private void setMode(GameMode mode) {
        currentMode = mode;
        updateModeInstructions();
        updateModeButtonStyles();
    }

    private void updateModeButtonStyles() {
        if (modeContainer != null) {
            for (Node node : modeContainer.getChildren()) {
                if (node instanceof Button button) {
                    if (button.getText().equals("Time")) {
                        styleModeButton(button, currentMode.toString().startsWith("TIME"));
                    } else if (button.getText().equals("Words")) {
                        styleModeButton(button, currentMode.toString().startsWith("WORDS"));
                    } else if (button.getText().equals("15s")) {
                        styleOptionButton(button, currentMode == GameMode.TIME_15);
                    } else if (button.getText().equals("30s")) {
                        styleOptionButton(button, currentMode == GameMode.TIME_30);
                    } else if (button.getText().equals("60s")) {
                        styleOptionButton(button, currentMode == GameMode.TIME_60);
                    } else if (button.getText().equals("10")) {
                        styleOptionButton(button, currentMode == GameMode.WORDS_10);
                    } else if (button.getText().equals("25")) {
                        styleOptionButton(button, currentMode == GameMode.WORDS_25);
                    } else if (button.getText().equals("50")) {
                        styleOptionButton(button, currentMode == GameMode.WORDS_50);
                    } else if (button.getText().equals("100")) {
                        styleOptionButton(button, currentMode == GameMode.WORDS_100);
                    }
                }
            }
        }
    }

    private void prepareParagraph() {
        if (currentMode.toString().startsWith("WORDS")) {
            int wordCount = switch (currentMode) {
                case WORDS_10 -> 10;
                case WORDS_25 -> 25;
                case WORDS_50 -> 50;
                case WORDS_100 -> 100;
                default -> 50;
            };
            paragraphText = getFixedWordCountParagraph(wordCount);
        } else {
            paragraphText = inputStrings.get(new Random().nextInt(inputStrings.size()));
        }
        displayParagraph(paragraphText);
    }

    private String getFixedWordCountParagraph(int wordCount) {
        StringBuilder sb = new StringBuilder();
        Random rand = new Random();
        int wordsAdded = 0;

        while (wordsAdded < wordCount) {
            String randomLine = inputStrings.get(rand.nextInt(inputStrings.size()));
            String[] words = randomLine.split(" ");
            for (String word : words) {
                if (wordsAdded < wordCount) {
                    sb.append(word).append(" ");
                    wordsAdded++;
                } else {
                    break;
                }
            }
        }

        return sb.toString().trim();
    }

    private void updateModeInstructions() {
        String instruction = switch (currentMode) {
            case TIME_15 -> "Type as much as you can in 15 seconds!";
            case TIME_30 -> "Type as much as you can in 30 seconds!";
            case TIME_60 -> "Type as much as you can in 60 seconds!";
            case WORDS_10 -> "Type 10 words as fast as you can!";
            case WORDS_25 -> "Type 25 words as fast as you can!";
            case WORDS_50 -> "Type 50 words as fast as you can!";
            case WORDS_100 -> "Type 100 words as fast as you can!";
            default -> "Select a mode to start typing!";
        };
        modeInstructionLabel.setText(instruction);
    }

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
                rect.setWidth(200);
            } else if (key.endsWith("Ctrl") || key.endsWith("Alt") || key.equals("Tab")) { // modifier keys
                rect.setWidth(60);
            } else if (key.equals("Backspace")) {
                rect.setWidth(120);
            }
            else if (key.length() > 1) rect.setWidth(100); // modifier keys
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

    private void leaderBoardPopUp() {
        Stage leaderboardStage = new Stage();
        leaderboardStage.initModality(Modality.APPLICATION_MODAL);
        leaderboardStage.initOwner(rootPane.getScene().getWindow());
        leaderboardStage.initStyle(StageStyle.TRANSPARENT);

        // Create LineChart for WPM vs Time
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Time (seconds)");
        xAxis.setStyle("-fx-font-family: 'Roboto Mono'; -fx-text-fill: #d1d0c5; -fx-tick-label-fill: #d1d0c5;");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("WPM");
        yAxis.setStyle("-fx-font-family: 'Roboto Mono'; -fx-text-fill: #d1d0c5; -fx-tick-label-fill: #d1d0c5;");

        LineChart<Number, Number> wpmChart = new LineChart<>(xAxis, yAxis);
        wpmChart.setTitle("WPM vs Time");
        wpmChart.setStyle("-fx-background-color: #2c2e31; -fx-title-fill: #e2b714; -fx-font-family: 'Roboto Mono'; -fx-font-size: 16px; -fx-background-radius: 5;");
        wpmChart.lookupAll(".chart-plot-background").forEach(node ->
                node.setStyle("-fx-background-color: #323437;"));

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("WPM Over Time");
        for (int i = 0; i < wpmDataPoints.size(); i++) {
            series.getData().add(new XYChart.Data<>(i, wpmDataPoints.get(i)));
        }
        wpmChart.getData().add(series);

        // Style the chart
        wpmChart.setCreateSymbols(true); // Show data points
        wpmChart.setLegendVisible(false); // Hide legend
        wpmChart.lookup(".chart-series-line").setStyle("-fx-stroke: #e2b714;");
        wpmChart.lookupAll(".chart-line-symbol").forEach(node ->
                node.setStyle("-fx-background-color: #e2b714, #323437;"));

        // WPM and Accuracy labels
        Label wpmLabelDisplay = new Label(String.format("WPM: %.0f", calculateWPM()));
        wpmLabelDisplay.setStyle("-fx-font-family: 'Roboto Mono'; -fx-font-size: 28px; -fx-text-fill: #e2b714; -fx-font-weight: bold;");
        Label accuracyLabelDisplay = new Label(String.format("Accuracy: %.0f%%", calculateAccuracy()));
        accuracyLabelDisplay.setStyle("-fx-font-family: 'Roboto Mono'; -fx-font-size: 28px; -fx-text-fill: #e2b714; -fx-font-weight: bold;");

        HBox statsBox = new HBox(20, wpmLabelDisplay, accuracyLabelDisplay);
        statsBox.setAlignment(Pos.CENTER);
        statsBox.setPadding(new Insets(10));

        // Header
        Label header = new Label("WPM PERFORMANCE");
        header.setStyle("-fx-text-fill: #e2b714; -fx-font-size: 24px; -fx-font-weight: bold; -fx-font-family: 'Roboto Mono';");

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

        // Esc instruction
        Label escText = new Label("Press esc to close");
        escText.setStyle("-fx-font-size: 14px; -fx-text-fill: #d1d0c5; -fx-font-family: 'Roboto Mono';");

        VBox root = new VBox(titleBar, statsBox, wpmChart, escText);
        root.setStyle("""
        -fx-background-color: #323437;
        -fx-border-color: #e2b714;
        -fx-border-width: 2px;
        -fx-border-radius: 5;
        -fx-background-radius: 5;
        -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 10, 0, 0, 0);
        """);
        escText.setTranslateX(140);
        VBox.setVgrow(wpmChart, Priority.ALWAYS);

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

        // Configure stage
        Scene scene = new Scene(root, 400, 500);
        wpmChart.setFocusTraversable(false); // Avoid focus ring glitches
        wpmChart.setMouseTransparent(false); // Ensure hover works correctly
        scene.setFill(Color.TRANSPARENT); // For rounded corners
        leaderboardStage.setScene(scene);

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
                leaderboardStage.close();
            }
        });
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

    @FXML
    public void initialize() {
        loadWords();
        setupUI();
        setupEventHandlers();
        initializeKeyboard();
        setupModeSelection();
    }

    private void loadWords() {
        String resourcePath = "/data/input.txt";
        try (InputStream input = getClass().getResourceAsStream(resourcePath);
             Scanner takeIn = new Scanner(input)){

            while (takeIn.hasNextLine()) {
                inputStrings.add(takeIn.nextLine());
            }
            accuracyChecker = new char[inputStrings.size() + 1000];
            correctWordChecker = new char[inputStrings.size() + 1000];
            Arrays.fill(accuracyChecker, 'B');
            Arrays.fill(correctWordChecker, 'B');
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setupUI() {
        progressBar.setProgress(0);
        typingField.setDisable(true);

        displayField.setDisable(true);
        displayField.setEditable(false);

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

                    if (item.contains(playerNameField.getText())) {
                        setStyle("-fx-background-color: #3a3d42; -fx-text-fill: #e2b714;");
                        entry.setStyle("-fx-fill: #e2b714; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-background-color: " + (getIndex() % 2 == 0 ? "#2c2e31" : "#323437") + ";");
                        entry.setStyle("-fx-fill: #d1d0c5;");
                    }

                    setGraphic(hbox);
                }
            }
        });
        escText.setStyle("-fx-text-fill: #d1d0c5; -fx-font-family: 'Roboto Mono';");
    }

    private void setupEventHandlers() {
        // Typing field listener for character-by-character comparison
        typingField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (paragraphText == null || paragraphText.isEmpty() || typingDone) return;

            if (newValue.length() < oldValue.length()) {
                handleBackspace(oldValue, newValue);
                return;
            }
            handleNewCharacters(oldValue, newValue);

            out.println(newValue);
            if (oldPara.isEmpty()) {
                updateDisplayField(newValue);
            } else {
                String typedText = newValue.substring(oldPara.length());
                updateDisplayField(typedText);
            }
        });

        typingField.setOnKeyPressed(e -> {
            String typedText = e.getCode().getName();
            out.println("typed: " + typedText);
            if (typedText.equals("Alt") || typedText.equals("Ctrl") || typedText.equals("Shift")) {
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

        rootPane.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                try {
                    timer.stop();
                    loadMainMenu();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        typingField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal && paragraphText != null && !typingDone) {
                typingField.requestFocus();
            }
        });

        playerNameField.setOnKeyPressed(e -> {
            modeContainer.setStyle("-fx-opacity: 0;");
            if (e.getCode() == KeyCode.ENTER) {
                onStartButtonClick();
            }
        });
    }

    private void updateDisplayField(String typedText) {
        out.println("in updateDisplayField with argument : " + typedText);
        if (paragraphWords == null || paragraphWords.length == 0 || typedText == null) {
            displayField.setText("");
            return;
        }

        try {
            int charCount = 0;
            int wordIndex = 0;
            int wordCharIndex = 0;

            for (int i = 0; i < paragraphWords.length; i++) {
                if (charCount + paragraphWords[i].length() >= typedText.length()) {
                    wordIndex = i;
                    wordCharIndex = typedText.length() - charCount;
                    break;
                }
                charCount += paragraphWords[i].length() + 1;
                if (charCount > typedText.length()) {
                    wordIndex = i;
                    wordCharIndex = 0;
                    break;
                }
            }

            wordIndex = Math.min(wordIndex, paragraphWords.length - 1);
            currentWordIndex = wordIndex;
            currentWordCharIndex = Math.max(0, Math.min(wordCharIndex, paragraphWords[wordIndex].length()));

            String currentWord = paragraphWords[currentWordIndex];
            out.println("Current word index is " + currentWordIndex);
            out.println("Current word is " + currentWord);
            displayField.setText(currentWord);

            int wordStartPosition = 0;
            for (int i = 0; i < currentWordIndex; i++) {
                wordStartPosition += paragraphWords[i].length() + 1;
            }

            boolean wordIsCorrectSoFar = true;
            if (typedText.length() > wordStartPosition) {
                int endIndex = Math.min(typedText.length(), wordStartPosition + currentWord.length());
                if (wordStartPosition <= typedText.length() && wordStartPosition <= endIndex) {
                    String typedPortionOfWord = typedText.substring(wordStartPosition, endIndex);

                    for (int i = 0; i < typedPortionOfWord.length(); i++) {
                        if (i >= currentWord.length() || typedPortionOfWord.charAt(i) != currentWord.charAt(i)) {
                            wordIsCorrectSoFar = false;
                            break;
                        }
                    }
                } else {
                    wordIsCorrectSoFar = false;
                }
            }

            String style = """
        -fx-font-family: 'Roboto Mono';
        -fx-font-size: 24px;
        -fx-background-color: transparent;
        -fx-border-color: transparent;
        -fx-alignment: CENTER_LEFT;
        """;

            if (currentWordCharIndex == 0) {
                style += "-fx-text-fill: #646669;";
            } else if (wordIsCorrectSoFar) {
                style += "-fx-text-fill: #d1d0c5;";
            } else {
                style += "-fx-text-fill: #ca4754;";
            }

            displayField.setStyle(style);
        } catch (Exception e) {
            System.err.println("Error updating display field: " + e.getMessage());
            e.printStackTrace();
            displayField.setText("");
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
                        out.println(correctCharCount);
                        accuracyChecker[currentIndex] = 'F';
                    }
                    correctWordChecker[currentIndex] = 'B';
                }
            }
        }
        updateStats();

        out.println(newValue);
        if (oldPara.isEmpty()) {
            updateDisplayField(newValue);
        } else {
            if (oldPara.length() <= newValue.length()) {
                String typedText = newValue.substring(oldPara.length());
                updateDisplayField(typedText);
            } else oldPara = newValue;
        }
    }

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
            }

            currentIndex++;
            totalTyped++;
            updateStats();

            if (currentIndex >= paragraphText.length()) {
                if (currentMode.equals(GameMode.WORDS_10) || currentMode.equals(GameMode.WORDS_25) ||
                        currentMode.equals(GameMode.WORDS_50) || currentMode.equals(GameMode.WORDS_100)) {
                    typingFinished();
                } else {
                    oldPara = newValue;
                    out.println(oldPara);
                    prepareParagraph();
                    currentIndex = 0;
                    currentWordIndex = 0;
                    currentWordCharIndex = 0;
                    displayField.clear();

                    accuracyChecker = new char[paragraphText.length() + 1000];
                    correctWordChecker = new char[paragraphText.length() + 1000];
                    Arrays.fill(accuracyChecker, 'B');
                    Arrays.fill(correctWordChecker, 'B');
                    displayParagraph(paragraphText);
                    typingField.requestFocus();
                }
                return;
            }
        }
    }

    private void updateStats() {
        Platform.runLater(() -> {
            long elapsed = (System.currentTimeMillis() - startTime) / 1000;
            if (currentMode.equals(GameMode.WORDS_10) || currentMode.equals(GameMode.WORDS_25) ||
                    currentMode.equals(GameMode.WORDS_50) || currentMode.equals(GameMode.WORDS_100)) {
                timeLabel.setText(String.format("%ds", (int) elapsed));
            }
            double wpm = calculateWPM();
            wpmLabel.setText(String.format("%.0f", wpm));
            accuracyLabel.setText(String.format("%.0f%%", calculateAccuracy()));
            if (currentMode.toString().startsWith("TIME")) {
                progressBar.setProgress((double) elapsed / TIMED_MODE_DURATION);
            } else {
                progressBar.setProgress((double) currentIndex / paragraphText.length());
            }

            // Collect WPM data every second
            if (System.currentTimeMillis() - lastWpmUpdateTime >= 1000) {
                wpmDataPoints.add(wpm);
                lastWpmUpdateTime = System.currentTimeMillis();
            }
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

    private void displayParagraph(String text) {
        paragraphFlow.getChildren().clear();
        textNodes.clear();
        paragraphText = text;

        paragraphWords = text.split(" ");

        for (char c : text.toCharArray()) {
            Text t = new Text(String.valueOf(c));
            t.setStyle("-fx-fill: #646669;");
            textNodes.add(t);
        }

        paragraphFlow.getChildren().addAll(textNodes);

        if (paragraphWords.length > 0) {
            displayField.setText(paragraphWords[0]);
        }
    }

    private void startTimer() {
        startTime = System.currentTimeMillis();
        if (timer != null) timer.stop();
        if (currentMode.toString().startsWith("TIME")) {
            TIMED_MODE_DURATION = switch (currentMode) {
                case TIME_15 -> 15;
                case TIME_30 -> 30;
                case TIME_60 -> 60;
                default -> 60;
            };
            int duration = TIMED_MODE_DURATION;
            timeLabel.setText(duration + "s");
            timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
                updateStats();
                long elapsed = (System.currentTimeMillis() - startTime) / 1000;
                long remaining = duration - elapsed;
                timeTitle.setText("time left:");
                timeLabel.setText(remaining + "s");
                if (remaining <= 0) {
                    typingFinished();
                }
            }));
            timer.setCycleCount(Timeline.INDEFINITE);
            timer.play();
        } else {
            timeTitle.setText("time :");
            timeLabel.setText("0s");
            timer = new Timeline(new KeyFrame(Duration.millis(100), e -> {
                updateStats();
            }));
            timer.setCycleCount(Timeline.INDEFINITE);
            timer.play();
        }
    }

    private void typingFinished() {
        if (typingDone) return;
        updateStats();
        leaderBoardPopUp();
        typingDone = true;
        if (timer != null) timer.stop();
        playerNameField.setVisible(true);
        playerNameField.setEditable(true);
        nameTitle.setVisible(true);
        titleLabel.setText("type racer - finished!");
        modeContainer.setStyle("-fx-opacity: 1;");
        startButton.setText("restart");
        typingField.setDisable(true);
        displayField.clear();
        modeContainer.setVisible(true);

        long finishTime = System.currentTimeMillis() - startTime;
        double timeInSeconds = finishTime / 1000.0;
        double wpm = calculateWPM();
        double accuracy = calculateAccuracy();
        String entry;
        if (currentMode.toString().startsWith("TIME")) {
            entry = String.format("%s - Timed: %d WPM - %.0f%%",
                    playerName, (int) wpm, accuracy);
        } else {
            entry = String.format("%s - Words: %.2fs - %d WPM - %.0f%%",
                    playerName, timeInSeconds, (int) wpm, accuracy);
        }
        leaderboard.add(entry);

        if(currentMode.toString().startsWith("TIME")) {
            leaderboard.sort((a, b) -> {
                String[] partsA = a.split(" - ");
                String[] partsB = b.split(" - ");
                int t1 = Integer.parseInt(partsA[1].replace(" WPM", "").replace("Timed: ", "").trim());
                int t2 = Integer.parseInt(partsB[1].replace(" WPM", "").replace("Timed: ", "").trim());
                int toReturn = Integer.compare(t1, t2);
                if (toReturn == 0) {
                    double t3 = Double.parseDouble(partsA[2].replace("%", "").trim());
                    double t4 = Double.parseDouble(partsB[2].replace("%", "").trim());
                    int toReturn2 = Double.compare(t3, t4);
                    if (toReturn2 == 0) {
                        return partsA[0].compareTo(partsB[0]);
                    }
                    return toReturn2;
                }
                return toReturn;
            });
        } else{
            leaderboard.sort((a, b) -> {
                String[] partsA = a.split(" - ");
                String[] partsB = b.split(" - ");
                double t1 = Double.parseDouble(partsA[1].replace("s", "").replace("Words: ", "").trim());
                double t2 = Double.parseDouble(partsB[1].replace("s", "").replace("Words: ", "").trim());
                int toReturn = Double.compare(t1, t2);
                if (toReturn == 0) {
                    int t3 = Integer.parseInt(partsA[1].replace(" WPM", "").trim());
                    int t4 = Integer.parseInt(partsB[1].replace(" WPM", "").trim());
                    int toReturn2 = Integer.compare(t3, t4);
                    if (toReturn2 == 0) {
                        double t5 = Double.parseDouble(partsA[2].replace("%", "").trim());
                        double t6 = Double.parseDouble(partsB[2].replace("%", "").trim());
                        int toReturn3 = Double.compare(t5, t6);
                        if(toReturn3 == 0) {
                            return partsA[0].compareTo(partsB[0]);
                        }
                        return toReturn3;
                    }
                    return toReturn2;
                }
                return toReturn;
            });
        }
        leaderboardList.setItems(leaderboard);

        playerNameField.setEditable(true);
        playerNameField.requestFocus();
    }

    @FXML
    private void onStartButtonClick() {
        typingField.setFocusTraversable(false);
        if (inputStrings.isEmpty()) {
            showAlert("No paragraphs available to type!");
            return;
        }

        playerName = playerNameField.getText().trim();
        if (playerName.isEmpty()) {
            showAlert("Please enter your name before starting!");
            return;
        }

        if (currentMode.toString().startsWith("TIME")) {
            int duration = switch (currentMode) {
                case TIME_15 -> 15;
                case TIME_30 -> 30;
                case TIME_60 -> 60;
                default -> 60;
            };
            titleLabel.setText("Timed Mode - " + duration + "s Challenge!");
            timeLabel.setText(duration + "s");
        } else {
            int wordCount = switch (currentMode) {
                case WORDS_10 -> 10;
                case WORDS_25 -> 25;
                case WORDS_50 -> 50;
                case WORDS_100 -> 100;
                default -> 50;
            };
            titleLabel.setText("Words Mode - " + wordCount + " Words Challenge!");
            timeLabel.setText("0s");
        }

        startButton.setText("restart");
        playerNameField.setEditable(false);
        playerNameField.setVisible(false);
        nameTitle.setVisible(false);
        modeContainer.setVisible(false);
        modeContainer.setStyle("-fx-opacity: 0;");

        prepareParagraph();
        if (paragraphText == null || paragraphText.isEmpty()) {
            showAlert("Failed to load paragraph text!");
            return;
        }

        resetGame();
    }

    private void resetGame() {
        typingDone = false;
        currentIndex = 0;
        correctCharCount = 0;
        totalTyped = 0;
        oldPara = "";
        currentWordIndex = 0;
        currentWordCharIndex = 0;
        wpmDataPoints.clear();
        lastWpmUpdateTime = 0;
        progressBar.setProgress(0);
        typingField.clear();
        typingField.setDisable(false);
        displayField.clear();
        accuracyChecker = new char[paragraphText.length() + 1000];
        correctWordChecker = new char[paragraphText.length() + 1000];
        Arrays.fill(accuracyChecker, 'B');
        Arrays.fill(correctWordChecker, 'B');
        displayParagraph(paragraphText);
        modeContainer.setVisible(true);

        startTimer();
        typingField.requestFocus();
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("TypeRacer");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void loadMainMenu() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxmlFiles/MainMenu.fxml"));
        Parent root = loader.load();

        Stage stage = (Stage) startButton.getScene().getWindow();
        Scene scene = new Scene(root, 1420, 800);

        stage.setTitle("TypeRacer");
        stage.setResizable(true);
        stage.setScene(scene);
        stage.show();
    }
}