package Controllers;

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
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import network.Client;

import java.io.IOException;

public class MultiPlayerLobbyController {
    public Button backButton;
    @FXML private Label warningLabel;
    @FXML private VBox rootPane;
    @FXML private ListView<String> playerListView;
    @FXML private Label statusLabel;
    @FXML private Button startButton;
    @FXML private Label hostIndicator;
    @FXML private Label escText;
    @FXML private Button fortySecondButton;
    @FXML private Button sixtySecondButton;
    @FXML private Button ninetySecondButton;

    private int time;
    private Client client;
    private String playerName;
    private ObservableList<String> players = FXCollections.observableArrayList();
    private boolean isHost = false;

    public void initialize(Client client, String name, boolean isHost) {
        this.client = client;
        this.playerName = name;
        this.isHost = isHost;

        setupUI();
        setupNetworkHandlers();
        client.sendName(name);

        Platform.runLater(() -> {
            rootPane.requestFocus();
            setupEventHandlers();
        });

        fortySecondButton.setDisable(!isHost);
        sixtySecondButton.setDisable(!isHost);
        ninetySecondButton.setDisable(!isHost);
    }

    private void setupUI() {
        playerListView.setItems(players);
        startButton.setDisable(!isHost);
        fortySecondButton.setPrefSize(80,36);
        sixtySecondButton.setPrefSize(80,36);
        ninetySecondButton.setPrefSize(80,36);
        startButton.setPrefSize(150,36);

        styleModeButton(fortySecondButton, false);
        styleModeButton(sixtySecondButton, false);
        styleModeButton(ninetySecondButton, false);
        styleModeButton(startButton, false);

        if (hostIndicator != null) {
            hostIndicator.setText(isHost ? "(Host)" : "(Player)");
        }

        escText.setStyle("-fx-text-fill: #d1d0c5; -fx-font-family: 'Roboto Mono';");
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
                loadChoice((Stage) rootPane.getScene().getWindow() );
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        backButton.hoverProperty().addListener((obs, oldVal, isHovering) -> {
            backButton.setStyle(isHovering ?
                    "-fx-background-color: transparent; -fx-text-fill: #ca4754; -fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 0 8 0 8; -fx-cursor: hand; -fx-font-family: 'Roboto Mono';" :
                    "-fx-background-color: transparent; -fx-text-fill: #d1d0c5; -fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 0 8 0 8; -fx-cursor: hand; -fx-font-family: 'Roboto Mono';");
        });
    }

    private void styleModeButton(Button button, boolean selected) {
        button.setStyle(selected ?
                "-fx-background-color: #e2b714; -fx-text-fill: #323437; -fx-font-family: 'Roboto Mono'; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 5 10; -fx-background-radius: 5;" :
                "-fx-background-color: #323437; -fx-text-fill: #d1d0c5; -fx-font-family: 'Roboto Mono'; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 5 10; -fx-background-radius: 5;");
        if(!selected && button == startButton) {button.setStyle("-fx-background-color: #2c2e31; -fx-text-fill: #d1d0c5; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 5 10; -fx-background-radius: 5;");}
        button.setOnMouseEntered(e -> {
            if(!selected) {
                button.setStyle("-fx-background-color: #e2b714; -fx-text-fill: #323437; -fx-font-family: 'Roboto Mono'; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 5 10; -fx-background-radius: 5;");
            }
        });
        button.setOnMouseExited(e -> {
            if(!selected) {
                button.setStyle("-fx-background-color: #323437; -fx-text-fill: #d1d0c5; -fx-font-family: 'Roboto Mono'; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 5 10; -fx-background-radius: 5;");
            }
            if(button == startButton) {button.setStyle("-fx-background-color: #2c2e31; -fx-text-fill: #d1d0c5; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 5 10; -fx-background-radius: 5;");}
        });
    }

    private void setupNetworkHandlers() {
        client.setOnMessageReceived(message -> {
            if (message.startsWith("PLAYERS:")) {
                Platform.runLater(() -> {
                    players.setAll(message.substring(8).split(","));
                    statusLabel.setText(players.size() + " players connected");
                });
            } else if (message.equals("START_GAME")) {
                if(players.size() < 2){
                    if(isHost) {
                        Platform.runLater(() -> {
                            warningLabel.setText("You need to have at least two players");
                            warningLabel.setStyle("-fx-text-fill: #D00515; -fx-font-size: 14;");
                        });
                    }
                } else if(time == 0) {
                    if(isHost) {
                        Platform.runLater(() -> {
                            warningLabel.setText("You have to select a time");
                            warningLabel.setStyle("-fx-text-fill: #D00515; -fx-font-size: 14;");
                        });
                    }
                }
                else {
                    Platform.runLater(this::startGame);
                }
            } else if (message.equals("CLOSE_ALL")){
                try{
                    loadChoice((Stage) rootPane.getScene().getWindow());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (message.startsWith("TIME:")) {
                time = Integer.parseInt(message.substring(5));
                switch (time) {
                    case 40:
                        styleModeButton(fortySecondButton, true);
                        styleModeButton(sixtySecondButton, false);
                        styleModeButton(ninetySecondButton, false);
                        break;
                    case 60:
                        styleModeButton(fortySecondButton, false);
                        styleModeButton(sixtySecondButton, true);
                        styleModeButton(ninetySecondButton, false);
                        break;
                    case 90:
                        styleModeButton(fortySecondButton, false);
                        styleModeButton(sixtySecondButton, false);
                        styleModeButton(ninetySecondButton, true);
                        break;
                    default:
                        styleModeButton(fortySecondButton, false);
                        styleModeButton(sixtySecondButton, false);
                        styleModeButton(ninetySecondButton, false);
                }
            }
        });
    }

    private void setupEventHandlers() {
        Platform.runLater(() -> {
            rootPane.requestFocus();
            rootPane.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
                if (e.getCode() == KeyCode.ESCAPE) {
                    try {
                        if(isHost) client.closeAll();
                        loadChoice((Stage) rootPane.getScene().getWindow() );
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            });
        });
    }

    @FXML
    private void handleStartGame() {
        if (isHost) {
            client.sendStartGame();
        }
    }

    @FXML
    private void onFortyClick(){
        client.sendMessage("TIME:" + 40);
    }

    @FXML
    private void onSixtyClick(){
        client.sendMessage("TIME:" + 60);
    }

    @FXML
    private void onNinetyClick(){
        client.sendMessage("TIME:" + 90);
    }

    private void loadChoice(Stage stage) throws IOException {
        new Thread(() -> {
            client.sendMessage("TIME:" + 0);
            client.close();
        }).start();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxmlFiles/MultiPlayerChoice.fxml"));
        Parent root = loader.load();

        Platform.runLater(() -> {
            Scene scene = new Scene(root, 800, 600);

            stage.setTitle("TypeRacer - MultiPlayer Choice");
            stage.setResizable(true);
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();
        });
    }

    private void startGame() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxmlFiles/MultiPlayerGame.fxml"));
            Parent root = loader.load();

            MultiPlayerGameController controller = loader.getController();
            controller.initialize(client, playerName, isHost, time);

            Stage stage = (Stage) playerListView.getScene().getWindow();

            stage.setScene(new Scene(root, 1600, 900));
            stage.centerOnScreen();
            stage.setTitle("TypeRacer - " + playerName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}