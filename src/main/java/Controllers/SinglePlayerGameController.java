package Controllers;

import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.application.Platform;
import javafx.animation.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.*;
import java.util.*;

public class SinglePlayerGameController {

    public GridPane keyboardRow1;
    public GridPane keyboardRow2;
    public GridPane keyboardRow3;
    public GridPane keyboardRow4;
    @FXML
    private VBox rootPane;

    @FXML
    private Label titleLabel;

    @FXML
    private TextField playerNameField;

    @FXML
    private TextFlow paragraphFlow;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private Button startButton;

    @FXML
    private ListView<String> leaderboardList;

    @FXML
    private Label timeLabel;

    @FXML
    private TextField typingField;

    @FXML
    private TextField displayField;  // New field to show one word at a time

    @FXML
    private Label wpmLabel;

    @FXML
    private Label accuracyLabel;

    @FXML
    private Label escText;

    // Game state fields
    private final List<String> inputStrings = new ArrayList<>();
    private final ObservableList<String> leaderboard = FXCollections.observableArrayList();
    private String name;
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

    // New fields for word-by-word display
    private String[] paragraphWords;
    private int currentWordIndex = 0;
    private int currentWordCharIndex = 0;

    private final Map<Character, Rectangle> keyRectangles = new HashMap<>();
    private final Map<Character, Text> keyTexts = new HashMap<>();

    // Add this method to initialize the keyboard
    private void initializeKeyboard() {
        // Row 1: Tab Q W E R T Y U I O P { }
        String[] row1Keys = {"Tab", "Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P", "{", "}"};
        addKeysToRow(keyboardRow1, row1Keys);

        // Row 2: Caps Lock A S D F G H J K L ; " Enter
        String[] row2Keys = {"Caps Lock", "A", "S", "D", "F", "G", "H", "J", "K", "L", ";", "\"", "Enter"};
        addKeysToRow(keyboardRow2, row2Keys);

        // Row 3: Shift Z X C V B N M < > ? Shift
        String[] row3Keys = {"Shift", "Z", "X", "C", "V", "B", "N", "M", "<", ">", "?", "Shift"};
        addKeysToRow(keyboardRow3, row3Keys);

        // Row 4: Ctrl Alt (space) Alt Ctrl
        String[] row4Keys = {"Ctrl", "Alt", " ", "Alt", "Ctrl"};
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
            } else if (key.equals("Ctrl") || key.equals("Alt") || key.equals("Tab")) { // For modifier keys
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
            if (key.length() == 1) {
                char c = key.charAt(0);
                keyRectangles.put(c, rect);
                keyTexts.put(c, text);
            } else if (key.equals(" ")) {
                keyRectangles.put(' ', rect);
                keyTexts.put(' ', text);
            }
        }
    }

    // Add this method to highlight a key
    private void highlightKey(char c, boolean highlight) {
        Platform.runLater(() -> {
            Rectangle rect = keyRectangles.get(Character.toUpperCase(c));
            Text text = keyTexts.get(Character.toUpperCase(c));

            if (rect != null && text != null) {
                if (highlight) {
                    rect.setStyle("-fx-fill: #e2b714; -fx-stroke: #e2b714; -fx-stroke-width: 1;");
                    text.setStyle("-fx-fill: #323437; -fx-font-family: 'Roboto Mono'; -fx-font-size: 14px;");
                } else {
                    rect.setStyle("-fx-fill: #2c2e31; -fx-stroke: #646669; -fx-stroke-width: 1;");
                    text.setStyle("-fx-fill: #d1d0c5; -fx-font-family: 'Roboto Mono'; -fx-font-size: 14px;");
                }
            }
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
        try {
            File file = new File("src/main/resources/txtFiles/input.txt");
            Scanner takeIn = new Scanner(file);

            while (takeIn.hasNextLine()) {
                inputStrings.add(takeIn.nextLine());
            }
            accuracyChecker = new char[inputStrings.size() + 1000];
            Arrays.fill(accuracyChecker, 'B');
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

        // Enter key to finish
        typingField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                typingFinished();
            }
        });

        // tab key to restart
        typingField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.TAB) {
                resetGame();
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
                        accuracyChecker[currentIndex] = 'F';
                    }
                }
            }
        }
        updateStats();

        // Update display field after backspace
        updateDisplayField(newValue);
    }

    private void handleNewCharacters(String oldValue, String newValue) {
        for (int i = oldValue.length(); i < newValue.length(); i++) {
            if (currentIndex >= paragraphText.length()) {
                typingFinished();
                return;
            }

            char typedChar = newValue.charAt(i);
            highlightKey(typedChar, true);

            PauseTransition pause = new PauseTransition(Duration.millis(200));
            pause.setOnFinished(e -> highlightKey(typedChar, false));
            pause.play();

            char expectedChar = paragraphText.charAt(currentIndex);
            Text current = textNodes.get(currentIndex);
            textNodes.get(currentIndex).setUnderline(true);

            if (typedChar == expectedChar) {
                if(accuracyChecker[currentIndex] == 'B') {
                    accuracyChecker[currentIndex] = 'T';
                    correctCharCount++;
                }

                current.setStyle("-fx-fill: #d1d0c5;"); // MonkeyType's correct color
            } else {
                accuracyChecker[currentIndex] = 'F';

                current.setStyle("-fx-fill: #ca4754;"); // MonkeyType's incorrect color
                currentWordCorrect = false;
            }

            currentIndex++;
            totalTyped++;
            updateStats();

            if (currentIndex >= paragraphText.length()) {
                typingFinished();
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

        titleLabel.setText("type racer - finished!");
        startButton.setText("restart");
        typingField.setDisable(true);
        displayField.clear(); // Clear display field when finished

        long finishTime = System.currentTimeMillis() - startTime;
        double timeInSeconds = finishTime / 1000.0;
        double wpm = calculateWPM();
        double accuracy = calculateAccuracy();
        String entry = String.format("%s - %.2fs - %d wpm - %.0f%%", name, timeInSeconds, (int) wpm, accuracy);
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

        name = playerNameField.getText().trim();
        if (name.isEmpty()) {
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