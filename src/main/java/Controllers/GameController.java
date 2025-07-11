package Controllers;

import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.application.Platform;
import javafx.animation.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.*;
import javafx.util.Duration;

import java.io.*;
import java.util.*;

public class GameController {

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
    private Label wpmLabel;

    @FXML
    private Label accuracyLabel;

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
    private int correctWordCount;
    private int totalTyped;
    private boolean currentWordCorrect;

    @FXML
    public void initialize() {
        loadWords();

        setupUI();

        setupEventHandlers();

//        playerNameField.textProperty().addListener((observable, oldValue, newValue) -> {
//            if(!newValue.equals(oldValue)) {
//                startButton.setText("Start");
//            }
//        });
//        playerNameField.setOnAction(e -> {
//            if(paragraphText == null || paragraphText.isEmpty()) {
//                if(!playerNameField.getText().isEmpty()) {
//                    onStartButtonClick();
//                    playerNameField.setEditable(false);
//                    Platform.runLater(() -> rootPane.requestFocus());
//                }
//                else showAlert();
//            }
//        });
//        playerNameField.setOnKeyPressed(e -> {
//            if(e.getCode() == KeyCode.ENTER) {
//                onStartButtonClick();
//            }
//        });
//        rootPane.setOnKeyPressed(event -> {
//            if (event.isControlDown() && event.getCode().toString().equals("BACK_SPACE")) {
//                System.out.println("Ctrl + Backspace detected!");
//
//                // Optional: Clear last word logic here
//                handleCtrlBackspace();
//                event.consume(); // stop it from bubbling
//            }
//        });
    }

    private void loadWords() {
        try{
            File file = new File("src/main/resources/txtFiles/input.txt");
            Scanner takeIn = new Scanner(file);

            while(takeIn.hasNextLine()){
                inputStrings.add(takeIn.nextLine());
            }
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }

    private void setupUI() {
        progressBar.setProgress(0);
        typingField.setDisable(true);
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


        // On pressing space or end of game
//        typingField.setOnKeyPressed(e -> {
//            if(e.getCode() == KeyCode.SPACE) {
//                typingField.clear();
//            }
//        });

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

        // Allow pressing Enter in name field to start the game
        playerNameField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                onStartButtonClick();
            }
        });
    }

    private void handleBackspace(String oldValue, String newValue) {
        int diff = oldValue.length() - newValue.length();
        for (int i = 0; i < diff; i++) {
            if (currentIndex > 0) {
                currentIndex--;
                Text previous = textNodes.get(currentIndex);
                previous.setStyle("-fx-fill: #646669;"); // MonkeyType's untyped color
                totalTyped = Math.max(0, totalTyped - 1);

                if (paragraphText.charAt(currentIndex) == previous.getText().charAt(0)) {
                    correctCharCount--;
                }
            }
        }
        updateStats();
    }

    private void handleNewCharacters(String oldValue, String newValue) {
        for (int i = oldValue.length(); i < newValue.length(); i++) {
            if (currentIndex >= paragraphText.length()) {
                typingFinished();
                return;
            }

            char typedChar = newValue.charAt(i);
            char expectedChar = paragraphText.charAt(currentIndex);
            Text current = textNodes.get(currentIndex);

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

    private void displayParagraph(String text) {
        paragraphFlow.getChildren().clear();
        textNodes.clear();
        paragraphText = text;

        for (char c : text.toCharArray()) {
            Text t = new Text(String.valueOf(c));
            t.setStyle("-fx-fill: #646669;"); // MonkeyType's untyped color
            textNodes.add(t);
        }

        paragraphFlow.getChildren().addAll(textNodes);
    }

    private void handleCtrlBackspace() {
        if (currentIndex == 0) return;

        // Go backward from current index to find start of word
        int wordEnd = currentIndex;
        int wordStart = wordEnd - 1;

        while (wordStart >= 0 && paragraphText.charAt(wordStart) != ' ') {
            wordStart--;
        }
        wordStart++; // move to start of actual word

        for (int i = wordStart; i < wordEnd; i++) {
            Text t = textNodes.get(i);
            t.setStyle("-fx-fill: gray; -fx-font-size: 16px;");
            t.setUnderline(false);
            totalTyped = Math.max(0, totalTyped - 1);

            if (t.getText().charAt(0) == paragraphText.charAt(i)) {
                correctCharCount = Math.max(0, correctCharCount - 1);
            }
        }

        currentIndex = wordStart;
    }

    private void typingFinished() {
        if(typingDone) return;
        typingDone = true;
        if (timer != null) timer.stop();

        titleLabel.setText("type racer - finished!");
        startButton.setText("restart");
        typingField.setDisable(true);

        long finishTime = System.currentTimeMillis() - startTime;
        double timeInSeconds = finishTime / 1000.0;
        double wpm = calculateWPM();
        double accuracy = calculateAccuracy();
        String entry = String.format("%s - %.2fs - %d wpm - %.0f%%",
                name, timeInSeconds, (int)wpm, accuracy);
        leaderboard.add(entry);

        leaderboard.sort((a,b) -> {
            double t1 = Double.parseDouble(a.split(" - ")[1].replace("s", ""));
            double t2 = Double.parseDouble(b.split(" - ")[1].replace("s", ""));
            int result = Double.compare(t1, t2);
            if(result != 0) return result;
            else{
                double t3 = Double.parseDouble(a.split(" - ")[3].replace("%", ""));
                double t4 = Double.parseDouble(b.split(" - ")[4].replace("%", ""));
                return Double.compare(t3, t4);
            }
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

        // Reset game state
        resetGame();

        // Setup UI
        titleLabel.setText("type racer - go!");
        startButton.setText("restart");
        playerNameField.setEditable(false);

        // Select random paragraph
        paragraphText = inputStrings.get(new Random().nextInt(inputStrings.size()));
        displayParagraph(paragraphText);

        // Start timer
        startTimer();

        // Focus typing field
        typingField.requestFocus();
    }

    private void resetGame() {
        typingDone = false;
        currentIndex = 0;
        correctCharCount = 0;
        correctWordCount = 0;
        totalTyped = 0;
        currentWordCorrect = true;
        progressBar.setProgress(0);
        typingField.clear();
        typingField.setDisable(false);
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

//    @FXML
//    public void onKeyTyped(KeyEvent event) {
//        if (playerNameField.isFocused()) return;
//        if (paragraphText == null || paragraphText.isEmpty()) return;
//
//        String character = event.getCharacter();
//        if(character.isEmpty()) return;
//
//        char typedChar = character.charAt(0);
//        System.out.println(typedChar);
//
//        if((typedChar == '\r' || typedChar == '\n') && currentIndex > 0) {
//            typingFinished();
//            return;
//        }
//
//        if (typedChar == '\b') {
//            if (currentIndex > 0) {
//                currentIndex--;
//                Text previous = textNodes.get(currentIndex);
//                previous.setStyle("-fx-fill: gray; -fx-font-size: 16px;");
//                previous.setUnderline(false);
//                totalTyped = Math.max(0, totalTyped - 1);
//
//                if (paragraphText.charAt(currentIndex) == previous.getText().charAt(0)) {
//                    correctCharCount = Math.max(0, correctCharCount - 1);
//                }
//            }
//            return;
//        }
//
//        if (!Character.isLetter(typedChar) && typedChar != ' ') return;
//
//        if (currentIndex >= paragraphText.length()){
//            if (currentWordCorrect) {
//                correctWordCount++;
//                System.out.println("Correct word count: " + correctWordCount);
//            }
//            typingFinished();
//            return;
//        }
//
//        char expectedChar = paragraphText.charAt(currentIndex);
//        Text current = textNodes.get(currentIndex);
//        textNodes.get(currentIndex).setUnderline(true); // Cursor-esque shit
//
//        if (typedChar == expectedChar) {
//            current.setStyle("-fx-fill: black; -fx-font-size: 16px;");
//            correctCharCount++;
//        } else {
//            current.setStyle("-fx-fill: red; -fx-font-size: 16px;");
//            currentWordCorrect = false;
//        }
//
//        if (typedChar == ' ') {
//            if (currentWordCorrect) {
//                correctWordCount++;
//                System.out.println("Correct word count: " + correctWordCount);
//            }
//            currentWordCorrect = true; // reset for next word
//        }
//
//
//        currentIndex++;
//        totalTyped++;
//
//        progressBar.setProgress((double) currentIndex / paragraphText.length());
//
//        if (currentIndex >= paragraphText.length()){
//            typingFinished();
//        }
//        // Optionally move cursor effect here
//    }
}
