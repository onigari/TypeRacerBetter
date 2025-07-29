package Controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import network.Client;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import static java.lang.System.out;

public class MultiPlayerChoiceController {
    @FXML private VBox rootPane;
    @FXML private TextField nameField;
    @FXML private TextField ipField;
    @FXML private Label statusLabel;
    @FXML private Label escText;
    @FXML private Button joinButton;

    private String name;
    private Client client;

    public void initialize() {
        escText.setStyle("-fx-text-fill: #d1d0c5; -fx-font-family: 'Roboto Mono';");

        rootPane.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                try {
                    loadMainMenu();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    private void setupNetworkHandlers() {
        client.setOnMessageReceived(message -> {
            if(message.startsWith("PLAYERS:")){
                String[] players = message.substring(8).split(",");
                for(String n : players){
                    if(n.equals(name)){
                        Platform.runLater(() -> {
                            statusLabel.setText("Name is taken!");
                            statusLabel.setStyle("-fx-text-fill: #da0112;");
                            client.close();
                        });
                        return;
                    }
                }
                try {
                    Stage stage = (Stage) joinButton.getScene().getWindow();
                    loadLobby(stage, client, name, false);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            else if (message.startsWith("NUMBER:")){
                int number = Integer.parseInt(message.substring(7));
                if(number > 0){
                    Platform.runLater(() -> {
                        statusLabel.setText("Server is Taken!");
                        statusLabel.setStyle("-fx-text-fill: #da0112;");
                        client.close();
                    });
                    return;
                }
                try {
                    Stage stage = (Stage) joinButton.getScene().getWindow();
                    loadLobby(stage, client, name, true);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private void loadMainMenu() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxmlFiles/MainMenu.fxml"));
        Parent root = loader.load();

        Platform.runLater(() -> {
            Stage stage = (Stage) rootPane.getScene().getWindow();
            Scene scene = new Scene(root);

            stage.setTitle("TypeRacer");
            stage.setResizable(true);
            stage.setScene(scene);
            stage.show();
        });
    }

    @FXML
    private void handleHostGame() {
        name = nameField.getText().trim();
        if (name.isEmpty()) {
            statusLabel.setText("Please enter your name");
            return;
        }

        try {
//            new Thread(() -> network.Server.main(new String[]{})).start();
//            Thread fineThread = new Thread();
//            fineThread.sleep(1000);
            client = new Client("localhost", 5000);
            setupNetworkHandlers();
            client.sendMessage("IS_AVAILABLE");
        } catch (Exception e) {
            statusLabel.setText("Failed to host game: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleJoinGame() {
        name = nameField.getText().trim();
        String ip = ipField.getText().trim();

        if (name.isEmpty() || ip.isEmpty()) {
            statusLabel.setText("Please enter both name and IP address");
            return;
        }

        if(!isValidIPAddress(ip)){
            statusLabel.setText("Invalid IP address");
            statusLabel.setStyle("-fx-text-fill: #da0112; -fx-font-size: 14;");
            return;
        }

        if(!isServerReachable(ip)){
            statusLabel.setText("No Game available in this server");
            statusLabel.setStyle("-fx-text-fill: #da0112; -fx-font-size: 14;");
            return;
        }

        try {
            client = new Client(ip, 5000);
            setupNetworkHandlers();
            client.sendMessage("GET_PLAYERS");
        } catch (IOException e) {
            statusLabel.setText("Failed to connect to server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static boolean isServerReachable(String ip) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, 5000), 1000);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean isValidIPAddress(String ip) {
        try {
            InetAddress address = InetAddress.getByName(ip);
            out.println(address.toString());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void loadLobby(Stage stage, Client client, String name, boolean isHost) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxmlFiles/MultiPlayerLobby.fxml"));
        Parent root = loader.load();

        Platform.runLater(() -> {
            MultiPlayerLobbyController controller = loader.getController();
            controller.initialize(client, name, isHost);

            stage.setScene(new Scene(root));
            stage.setTitle("Multiplayer Lobby - " + name);
            stage.setOnCloseRequest(e -> client.close());
        });
    }
}