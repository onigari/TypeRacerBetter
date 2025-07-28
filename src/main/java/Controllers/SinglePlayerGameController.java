package Controllers;

import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
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

    @FXML private VBox rootPane;
    @FXML private Label titleLabel;
    @FXML private TextField playerNameField;
    @FXML private TextFlow paragraphFlow;
    @FXML private ProgressBar progressBar;
    @FXML private Button startButton;
    @FXML private ListView<String> leaderboardList;
    @FXML private Label timeLabel;
    @FXML private TextField typingField;
    @FXML private TextField displayField;  // New field to show one word at a time
    @FXML private Label wpmLabel;
    @FXML private Label accuracyLabel;
    @FXML private Label escText;

    // Game state fields
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
    private boolean currentWordCorrect;
    private char[] accuracyChecker;
    private char[] correctWordChecker;

    // New fields for word-by-word display
    private String[] paragraphWords;
    private int currentWordIndex = 0;
    private int currentWordCharIndex = 0;

    private final Map<String, Rectangle> keyRectangles = new HashMap<>();
    private final Map<String, Text> keyTexts = new HashMap<>();

    // Add this method to initialize the keyboard
    private void initializeKeyboard() {
        // Row 1: Tab Q W E R T Y U I O P { }
        String[] row1Keys = {"Tab", "Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P", "{", "}"};
        addKeysToRow(keyboardRow1, row1Keys);

        // Row 2: Caps Lock A S D F G H J K L ; ' Enter
        String[] row2Keys = {"Caps Lock", "A", "S", "D", "F", "G", "H", "J", "K", "L", ";", "'", "Enter"};
        addKeysToRow(keyboardRow2, row2Keys);

        // Row 3: Shift Z X C V B N M , . ? Shift
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

            Rectangle rect = new Rectangle(40, 40);
            if (key.equals(" ")) {
                rect.setWidth(200);
            } else if (key.endsWith("Ctrl") || key.endsWith("Alt") || key.equals("Tab")) { // modifier keys
                rect.setWidth(60);
            } else if (key.length() > 1) rect.setWidth(100); // modifier keys
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
        // Create the stage
        Stage leaderboardStage = new Stage();
        leaderboardStage.initModality(Modality.APPLICATION_MODAL);
        leaderboardStage.initOwner(rootPane.getScene().getWindow());
        leaderboardStage.initStyle(StageStyle.TRANSPARENT); // Borderless

        // Create ListView with custom cells
        ListView<String> leaderboardList = new ListView<>();
        leaderboardList.setItems(leaderboard);

        // Apply your custom cell factory
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
                    if (item.contains(playerName)) {
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
        Label escText = new Label();
        escText.setText("Press esc to close");
        // Header
        Label header = new Label("LEADERBOARD");
        header.setStyle("-fx-text-fill: #e2b714; -fx-font-size: 24px; -fx-font-weight: bold;");

        // Close button
        Button closeBtn = new Button("✕");
        closeBtn.setStyle("""
        -fx-background-color: transparent;
        -fx-text-fill: #d1d0c5;
        -fx-font-size: 16px;
        -fx-font-weight: bold;
        -fx-padding: 0 8 0 8;
        -fx-cursor: hand;
        """);
        closeBtn.setOnAction(e -> leaderboardStage.close());
        closeBtn.hoverProperty().addListener((obs, oldVal, isHovering) -> {
            closeBtn.setStyle(isHovering ?
                    "-fx-text-fill: #ca4754;" :
                    "-fx-text-fill: #d1d0c5;");
        });

        // Title bar
        HBox titleBar = new HBox(header, new Region(), closeBtn);
        titleBar.setAlignment(Pos.CENTER_RIGHT);
        titleBar.setPadding(new Insets(10, 10, 10, 20));
        titleBar.setStyle("-fx-background-color: #2c2e31;");
        HBox.setHgrow(titleBar.getChildren().get(1), Priority.ALWAYS);

        // Main container
        VBox root = new VBox(titleBar, leaderboardList);
        root.setStyle("""
        -fx-background-color: #323437;
        -fx-border-color: #e2b714;
        -fx-border-width: 2px;
        -fx-border-radius: 5;
        -fx-background-radius: 5;
        -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 10, 0, 0, 0);
        """);

        // Make draggable
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
        scene.setFill(Color.TRANSPARENT); // For rounded corners
        leaderboardStage.setScene(scene);

        // Center on screen
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        leaderboardStage.setX((screenBounds.getWidth() - scene.getWidth()) / 2);
        leaderboardStage.setY((screenBounds.getHeight() - scene.getHeight()) / 2);

        // Show with animation
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

        // Setup displayField
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

                // Default style
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

                    // Highlight current player's entry
                    if (item.contains(playerNameField.getText())) {
                        setStyle("-fx-background-color: #3a3d42; -fx-text-fill: #e2b714;");
                        entry.setStyle("-fx-fill: #e2b714; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-background-color: " + (getIndex() % 2 == 0 ? "#2c2e31" : "#323437") + ";");
                        entry.setStyle("-fx-fill: #d1d0c5;");
                    }

                    setGraphic(hbox);

                    // Tooltip with full details
                    //setTooltip(new Tooltip(item));
                }
            }
        });
        escText.setStyle("-fx-text-fill: #d1d0c5; -fx-font-family: 'Roboto Mono';");
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

            // Update display field
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
            // tab key to restart
            if (e.getCode() == KeyCode.TAB) {
                resetGame();
            }
            // enter key to
            if (e.getCode() == KeyCode.ENTER) {
                typingFinished();
            }
        });

        // esc to go to main menu
        rootPane.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                try {
                    loadMainMenu();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        // Start typing immediately when typing field gets focus
        typingField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal && paragraphText != null && !typingDone) {
                typingField.requestFocus();
            }
        });

        // Allow pressing Enter in name field to start the game
        playerNameField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                onStartButtonClick();
            }
        });
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
        if (currentWordIndex < paragraphWords.length) {
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
                -fx-alignment: CENTER_LEFT;""");
            } else if (wordIsCorrectSoFar) {
                // Correct so far - green text
                displayField.setStyle("""
                -fx-font-family: 'Roboto Mono';
                -fx-font-size: 24px;
                -fx-text-fill: #d1d0c5;
                -fx-background-color: transparent;
                -fx-border-color: transparent;
                -fx-alignment: CENTER_LEFT;""");
            } else {
                // Incorrect - red text
                displayField.setStyle("""
                -fx-font-family: 'Roboto Mono';
                -fx-font-size: 24px;
                -fx-text-fill: #ca4754;
                -fx-background-color: transparent;
                -fx-border-color: transparent;
                -fx-alignment: CENTER_LEFT;""");
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
                        out.println(correctCharCount);
                        accuracyChecker[currentIndex] = 'F';
                    }
                    correctWordChecker[currentIndex] = 'B';
                }
            }
        }
        updateStats();

        // Update display field after backspace
        updateDisplayField(newValue);
    }

    private void handleNewCharacters(String oldValue, String newValue) {
        for (int i = oldValue.length(); i < newValue.length(); i++) {
            char typedChar = newValue.charAt(i);
//            out.println(typedChar);
//            highlightKey(String.valueOf(typedChar), true);
//
//            PauseTransition pause = new PauseTransition(Duration.millis(200));
//            pause.setOnFinished(e -> highlightKey(String.valueOf(typedChar), false));
//            pause.play();

            char expectedChar = paragraphText.charAt(currentIndex);
            Text current = textNodes.get(currentIndex);
            textNodes.get(currentIndex).setUnderline(true);

            if (typedChar == expectedChar) {
                if(accuracyChecker[currentIndex] == 'B') {
                    accuracyChecker[currentIndex] = 'T';
                    correctCharCount++;
//                    out.println(correctCharCount);
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

            if (currentIndex >= paragraphText.length()) {
                typingFinished();
                return;
            }
        }
    }

    private void updateStats() {
        Platform.runLater(() -> {
            long elapsed = System.currentTimeMillis() - startTime;
            double seconds = elapsed / 1000.0;
            timeLabel.setText(String.format("%ds", (int) seconds));
            wpmLabel.setText(String.format("%.0f", calculateWPM()));
            accuracyLabel.setText(String.format("%.0f%%", calculateAccuracy()));
            progressBar.setProgress((double) currentIndex / paragraphText.length());
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

        // Split paragraph into words for the display field
        paragraphWords = text.split(" ");

        for (char c : text.toCharArray()) {
            Text t = new Text(String.valueOf(c));
            t.setStyle("-fx-fill: #646669;"); // MonkeyType's untyped color
            textNodes.add(t);
        }

        paragraphFlow.getChildren().addAll(textNodes);

        // Initialize display field with first word
        if (paragraphWords.length > 0) {
            displayField.setText(paragraphWords[0]);
        }
    }

    private void typingFinished() {
        if (typingDone) return;
        typingDone = true;
        if (timer != null) timer.stop();
        leaderBoardPopUp();
        titleLabel.setText("type racer - finished!");
        startButton.setText("restart");
        typingField.setDisable(true);
        displayField.clear(); // Clear display field when finished
        updateStats();

        long finishTime = System.currentTimeMillis() - startTime;
        double timeInSeconds = finishTime / 1000.0;
        double wpm = calculateWPM();
        double accuracy = calculateAccuracy();
        String entry = String.format("%s - %.2fs - %d wpm - %.0f%%", playerName, timeInSeconds, (int) wpm, accuracy);
        leaderboard.add(entry);

        leaderboard.sort((a, b) -> {
            double t1 = Double.parseDouble(a.split(" - ")[1].replace("s", "").trim());
            double t2 = Double.parseDouble(b.split(" - ")[1].replace("s", "").trim());
            int toReturn = Double.compare(t1, t2);
            if (toReturn == 0) {
                double t3 = Double.parseDouble(a.split(" - ")[2].replace(" WPM", "").trim());
                double t4 = Double.parseDouble(b.split(" - ")[2].replace(" WPM", "").trim());
                int toReturn2 = Double.compare(t3, t4);
                if (toReturn2 == 0) {
                    return a.split(" - ")[0].compareTo(b.split(" - ")[0]);
                }
                return toReturn2;
            }
            return toReturn;
        });
        leaderboardList.setItems(leaderboard);

        playerNameField.setEditable(true);
        playerNameField.requestFocus();
    }

    @FXML
    private void onStartButtonClick() {
        if (inputStrings.isEmpty()) {
            showAlert("No paragraphs available to type!");
            return;
        }

        playerName = playerNameField.getText().trim();
        if (playerName.isEmpty()) {
            showAlert("Please enter your name before starting!");
            return;
        }

        // Setup UI
        titleLabel.setText("type racer - go!");
        startButton.setText("restart");
        playerNameField.setEditable(false);

        // Select random paragraph
        paragraphText = inputStrings.get(new Random().nextInt(inputStrings.size()));

        // Reset game state
        resetGame();
    }

    private void resetGame() {
        typingDone = false;
        currentIndex = 0;
        correctCharCount = 0;
        totalTyped = 0;
        currentWordCorrect = true;
        currentWordIndex = 0;
        currentWordCharIndex = 0;
        progressBar.setProgress(0);
        typingField.clear();
        typingField.setDisable(false);
        displayField.clear();
        accuracyChecker = new char[paragraphText.length() + 1000];
        correctWordChecker = new char[paragraphText.length() + 1000];
        Arrays.fill(accuracyChecker, 'B');
        Arrays.fill(correctWordChecker, 'B');
        displayParagraph(paragraphText);

        startTimer();
        typingField.requestFocus();
    }

    private void startTimer() {
        startTime = System.currentTimeMillis();
        if (timer != null) timer.stop();
        timer = new Timeline(new KeyFrame(Duration.millis(100), e -> updateStats()));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
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