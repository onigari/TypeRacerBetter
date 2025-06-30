package com.example.real;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.application.Platform;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.io.*;
import java.util.*;

public class GameController {

    @FXML
    private Label titleLabel;

    @FXML
    private TextField playerNameField;

    @FXML
    private TextArea paragraphLabel;

    @FXML
    private TextArea typingArea;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private Button startButton;

    @FXML
    private ListView<String> leaderboardList;

    @FXML
    private Label timeLabel;

    private List<String> inputStrings = new ArrayList<>();
    private String paragraphText;
    private ObservableList<String> leaderboard = FXCollections.observableArrayList();
    private String name;
    private long startTime;
    private Timeline timer;

    private static void random() {

    }
    @FXML
    public void initialize() {
        try{
            File file = new File("src/main/resources/com/example/real/input.txt");
            Scanner takeIn = new Scanner(file);

            while(takeIn.hasNextLine()){
                inputStrings.add(takeIn.nextLine());
            }
        }
        catch(IOException e){
            e.printStackTrace();
        }
        /*paragraphText = inputStrings.get(new Random().nextInt(inputStrings.size()));
        paragraphLabel.setText(paragraphText);*/
        paragraphLabel.setWrapText(true);
        typingArea.setDisable(true);
        progressBar.setProgress(0.0);

        typingArea.textProperty().addListener((observable, oldValue, newValue) -> {
            int typedLength = Math.min(newValue.length(), paragraphText.length());
            double progress = (double) typedLength / paragraphText.length();
            progressBar.setProgress(progress);

            // Optional: live feedback
            if (!paragraphText.startsWith(newValue)) {
                typingArea.setStyle("-fx-text-fill: red;");
            } else {
                typingArea.setStyle("-fx-text-fill: black;");
            }

            if (newValue.equals(paragraphText)) {
                typingArea.setDisable(true);
                titleLabel.setText("Type Racer");
                startButton.setText("Restart");
                timer.stop();

                long finishTime = System.currentTimeMillis() - startTime;
                double timeInSeconds = finishTime / 1000.0;
                String entry = String.format("%s - %.2f s", name, timeInSeconds);
                leaderboard.add(entry);

                leaderboard.sort((a,b) -> {
                    double t1 = Double.parseDouble(a.split(" - ")[1].replace(" s", ""));
                    double t2 = Double.parseDouble(b.split(" - ")[1].replace(" s", ""));
                    return Double.compare(t1, t2);
                });
                leaderboardList.setItems(leaderboard);
            }
        });

        playerNameField.textProperty().addListener((observable, oldValue, newValue) -> {
            if(!newValue.equals(oldValue)) {
                startButton.setText("Start");
            }
        });
    }

    @FXML
    public void onStartButtonClick() {
        name = playerNameField.getText();
        if (name.isEmpty()) {
            showAlert();
            return;
        }
        paragraphText = inputStrings.get(new Random().nextInt(inputStrings.size()));
        paragraphLabel.setText(paragraphText);

        typingArea.clear();
        typingArea.setDisable(false);
        typingArea.requestFocus();
        progressBar.setProgress(0.0);
        titleLabel.setText("Get Typing!");

        startTime = System.currentTimeMillis();
        timeLabel.setText("Time: 0.00s");

        if(timer!= null) timer.stop();
        timer = new Timeline(
                new KeyFrame(Duration.millis(100), e -> {
                    long elapsed = System.currentTimeMillis() - startTime;
                    double seconds = (double) elapsed / 1000.0;
                    timeLabel.setText(String.format("Time: %.2f", seconds));
                })
        );
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
    }

    private void showAlert() {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("TypeRacer");
            alert.setHeaderText(null);
            alert.setContentText("Please enter your name before starting the race.");
            alert.showAndWait();
        });
    }
}
