package Controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.*;

public class TypeRacerTest extends Application {

    private TextFlow paragraphFlow = new TextFlow();
    private TextField nameField = new TextField();
    private Label timerLabel = new Label("Time: 0.00s");
    private Button startButton = new Button("Start");
    private ProgressBar progressBar = new ProgressBar(0);
    private ListView<String> leaderboardList = new ListView<>();
    private ObservableList<String> leaderboard = FXCollections.observableArrayList();

    private String paragraphText;
    private List<Text> textNodes = new ArrayList<>();
    private int currentIndex = 0;
    private int correctCount = 0;
    private int totalTyped = 0;

    private Timeline timer;
    private long startTime;
    private boolean typingDone = false;
    private String playerName = "";

    private final List<String> sampleParagraphs = List.of(
            "The quick brown fox jumps over the lazy dog.",
            "JavaFX makes building GUIs easier and cleaner.",
            "Typing games are fun and help improve speed."
    );

    @Override
    public void start(Stage primaryStage) {
        Label titleLabel = new Label("Type Racer");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 32));

        paragraphFlow.setPadding(new Insets(10));
        paragraphFlow.setPrefHeight(100);
        paragraphFlow.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #ccc; -fx-border-radius: 5;");

        nameField.setPromptText("Enter your name");

        VBox topSection = new VBox(10, titleLabel, nameField, startButton);
        topSection.setAlignment(Pos.CENTER);

        VBox gameSection = new VBox(10, paragraphFlow, progressBar, timerLabel);
        gameSection.setPadding(new Insets(10));
        gameSection.setAlignment(Pos.CENTER_LEFT);

        leaderboardList.setItems(leaderboard);
        leaderboardList.setPrefHeight(120);

        VBox leaderboardBox = new VBox(5, new Label("Leaderboard:"), leaderboardList);

        HBox root = new HBox(20, new VBox(20, topSection, gameSection), leaderboardBox);
        root.setPadding(new Insets(20));

        Scene scene = new Scene(root, 900, 500);

        // Key typing event
        scene.setOnKeyTyped(this::handleKeyTyped);

        startButton.setOnAction(e -> {
            if (nameField.getText().isEmpty()) {
                showAlert("Please enter your name first.");
                return;
            }
            playerName = nameField.getText();
            startGame();
        });

        primaryStage.setScene(scene);
        primaryStage.setTitle("JavaFX TypeRacer");
        primaryStage.setFullScreen(false); // set to true if you want full screen
        primaryStage.show();
    }

    private void startGame() {
        typingDone = false;
        paragraphText = sampleParagraphs.get(new Random().nextInt(sampleParagraphs.size()));
        displayParagraph(paragraphText);
        currentIndex = 0;
        correctCount = 0;
        totalTyped = 0;
        progressBar.setProgress(0);
        startTime = System.currentTimeMillis();
        nameField.setEditable(false);

        if (timer != null) timer.stop();
        timer = new Timeline(new KeyFrame(Duration.millis(100), e -> {
            long elapsed = System.currentTimeMillis() - startTime;
            timerLabel.setText(String.format("Time: %.2f", elapsed / 1000.0));
        }));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
    }

    private void displayParagraph(String text) {
        paragraphFlow.getChildren().clear();
        textNodes.clear();
        for (char c : text.toCharArray()) {
            Text t = new Text(String.valueOf(c));
            t.setFont(Font.font("Consolas", 16));
            t.setFill(javafx.scene.paint.Color.GRAY);
            textNodes.add(t);
        }
        paragraphFlow.getChildren().addAll(textNodes);
    }

    private void handleKeyTyped(KeyEvent event) {
        if (paragraphText == null || typingDone) return;
        String character = event.getCharacter();
        if (character.isEmpty()) return;
        char typedChar = character.charAt(0);

        if (currentIndex >= paragraphText.length()) {
            finishTyping();
            return;
        }

        char expectedChar = paragraphText.charAt(currentIndex);
        Text current = textNodes.get(currentIndex);

        if (typedChar == expectedChar) {
            current.setFill(javafx.scene.paint.Color.BLACK);
            correctCount++;
        } else {
            current.setFill(javafx.scene.paint.Color.RED);
        }

        totalTyped++;
        currentIndex++;
        progressBar.setProgress((double) currentIndex / paragraphText.length());

        if (currentIndex >= paragraphText.length()) {
            finishTyping();
        }
    }

    private void finishTyping() {
        if (typingDone) return;
        typingDone = true;
        timer.stop();
        nameField.setEditable(true);

        long elapsed = System.currentTimeMillis() - startTime;
        double timeInSeconds = elapsed / 1000.0;
        double accuracy = totalTyped == 0 ? 0 : (correctCount * 100.0 / totalTyped);

        String entry = String.format("%s - %.2f s - Accuracy: %.2f%%", playerName, timeInSeconds, accuracy);
        leaderboard.add(entry);

        leaderboard.sort((a, b) -> {
            double t1 = Double.parseDouble(a.split(" - ")[1].replace(" s", ""));
            double t2 = Double.parseDouble(b.split(" - ")[1].replace(" s", ""));
            return Double.compare(t1, t2);
        });
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Missing Info");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

