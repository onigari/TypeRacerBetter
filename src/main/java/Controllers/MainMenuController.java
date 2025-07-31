package Controllers;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class MainMenuController {
    @FXML private Pane backgroundPane;

    private final List<Circle> stars = new ArrayList<>();
    private final Random random = new Random();

    public void initialize() {
        createStars(100);
        animateStars();
    }

    private void createStars(int count) {
        for (int i = 0; i < count; i++) {
            Circle star = new Circle(1 + random.nextDouble() * 1.5, Color.rgb(226,183,20, 0.4));
            star.setLayoutX(random.nextDouble() * backgroundPane.getWidth());
            star.setLayoutY(random.nextDouble() * backgroundPane.getHeight());
            backgroundPane.getChildren().add(star);
            stars.add(star);
        }

        backgroundPane.widthProperty().addListener((obs, oldVal, newVal) -> repositionStars());
        backgroundPane.heightProperty().addListener((obs, oldVal, newVal) -> repositionStars());
    }

    private void repositionStars() {
        for (Circle star : stars) {
            star.setLayoutX(random.nextDouble() * backgroundPane.getWidth());
            star.setLayoutY(random.nextDouble() * backgroundPane.getHeight());
        }
    }

    private void animateStars() {
        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(50), e -> {
            for (Circle star : stars) {
                star.setLayoutY(star.getLayoutY() + 0.3);
                if (star.getLayoutY() > backgroundPane.getHeight()) {
                    star.setLayoutY(0);
                    star.setLayoutX(random.nextDouble() * backgroundPane.getWidth());
                }
            }
        }));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    @FXML
    private void onSinglePlayerClick(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxmlFiles/SinglePlayerGame.fxml"));
        Parent root = loader.load();

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

        Scene scene = new Scene(root, 1600, 900);

        stage.setScene(scene);
        stage.setTitle("TypeRacer - SinglePlayer");
        stage.centerOnScreen();
        stage.show();
    }

    @FXML
    private void onMultiPlayerClick(ActionEvent event) throws IOException {

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxmlFiles/MultiPlayerChoice.fxml"));
        Parent root = loader.load();

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

        Scene scene = new Scene(root, 800, 600);

        stage.setScene(scene);
        stage.setTitle("TypeRacer - MultiPlayer Choice");
        stage.centerOnScreen();
        stage.show();
    }

    @FXML
    private void onExitClick(ActionEvent event) {
        System.exit(0);
    }
}