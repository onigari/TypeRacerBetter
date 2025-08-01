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
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static java.lang.System.out;

public class MainMenuController {
    public Text titleText;
    @FXML private Pane backgroundPane;

    private final List<Pane> keys = new ArrayList<>();
    private final Random random = new Random();
    private final String[] characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".split("");

    public void initialize() {
        createKeys(15);
        animateKeys();
        titleText.setStyle("-fx-text-fill: white; -fx-font-size: 40.0px; -fx-font-weight: bold; -fx-font-family: 'JetBrains Mono Medium';");

    }

    private void createKeys(int count) {
        for (int i = 0; i < count; i++) {
            Pane keyPane = new Pane();
            int dim = random.nextInt(25, 50);

            Rectangle key = new Rectangle(dim, dim);
            key.setArcWidth(5);
            key.setArcHeight(5);
            key.setStyle("-fx-fill: #2E2E2E; -fx-stroke: #3F3E3E; -fx-stroke-width: 1; -fx-opacity: 0.5;");

            String text = characters[random.nextInt(characters.length)];
            Label textLabel = new Label(text);
            int fontSize = (int) ((key.getHeight() / 60) * 8 + 8);
            textLabel.setStyle("-fx-text-fill: white; -fx-font-size: " + fontSize + " px; -fx-font-weight: bold; -fx-font-family: 'JetBrains Mono Medium';-fx-opacity: 0.5;");
            textLabel.getEffect();
            textLabel.setLayoutX((dim - textLabel.getWidth()) / 2 - 6);
            textLabel.setLayoutY((dim - textLabel.getHeight()) / 2 - 8);

            keyPane.getChildren().addAll(key, textLabel);

            keyPane.setLayoutX(random.nextDouble() * backgroundPane.getWidth());
            keyPane.setLayoutY(random.nextDouble() * backgroundPane.getHeight());

            backgroundPane.getChildren().add(keyPane);
            keys.add(keyPane);
        }

        backgroundPane.widthProperty().addListener((obs, oldVal, newVal) -> repositionKeys());
        backgroundPane.heightProperty().addListener((obs, oldVal, newVal) -> repositionKeys());
    }

    private void repositionKeys() {
        for (Pane keyPane : keys) {
            keyPane.setLayoutX(random.nextDouble() * backgroundPane.getWidth());
            keyPane.setLayoutY(random.nextDouble() * backgroundPane.getHeight());
        }
    }

    private void animateKeys() {
        Timeline timeline1 = new Timeline(new KeyFrame(Duration.millis(50), e -> {
            for (Pane keyPane : keys) {
                keyPane.setLayoutY(keyPane.getLayoutY() + 0.7);
                if (keyPane.getLayoutY() > backgroundPane.getHeight()) {
                    keyPane.setLayoutY(0);
                    keyPane.setLayoutX(random.nextDouble() * backgroundPane.getWidth());
                }
            }
        }));
        Timeline timeline2 = new Timeline(new KeyFrame(Duration.seconds(0.7), e -> {
            for (Pane keyPane : keys) {
                if (keyPane.getChildren().getFirst() instanceof Rectangle rectangle) {
                    int fontSize = (int) ((rectangle.getHeight() / 60) * 8 + 8);
                    highlightKey(keyPane, random.nextInt() % 47 == 0, fontSize);
                }
            }
        }));
        Timeline timeline3 = new Timeline(new KeyFrame(Duration.seconds(0.3), e -> {
            for (Pane keyPane : keys) {
                if (keyPane.getChildren().getFirst() instanceof Rectangle rectangle) {
                    int fontSize = (int) ((rectangle.getHeight() / 60) * 8 + 8);
                    highlightKey(keyPane, random.nextInt() % 77 == 0, fontSize);
                }
            }
        }));
        timeline1.setCycleCount(Animation.INDEFINITE);
        timeline2.setCycleCount(Animation.INDEFINITE);
        timeline3.setCycleCount(Animation.INDEFINITE);
        timeline1.play();
        timeline2.play();
        timeline3.play();
    }

    private void highlightKey(Node node, boolean highlight, int fontSize) {
        if (node instanceof Pane pane) {
            if (highlight) {
                pane.getChildren().getFirst().setStyle("-fx-fill: #e2b714; -fx-stroke: #e2b714; -fx-opacity: 0.3; -fx-stroke-width: 1;");
                pane.getChildren().getLast().setStyle("-fx-text-fill: black; -fx-font-size: " + fontSize + " px; -fx-opacity: 0.5; -fx-font-weight: bold; -fx-font-family: 'JetBrains Mono Medium';");
            } else {
                pane.getChildren().getFirst().setStyle("-fx-fill: #2E2E2E; -fx-stroke: #3F3E3E; -fx-opacity: 0.5; -fx-stroke-width: 1;");
                pane.getChildren().getLast().setStyle("-fx-text-fill: white; -fx-font-size: " + fontSize + " px; -fx-opacity: 0.5; -fx-font-weight: bold; -fx-font-family: 'JetBrains Mono Medium';");
            }
        } else out.println("null in highlight");
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