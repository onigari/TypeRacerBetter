package Controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.stage.Stage;
import network.Client;

public class MultiPlayerChoiceController {

    @FXML
    private Button hostGameButton;

    @FXML
    private Button joinGameButton;

    @FXML
    private TextField ipAddressField;

    @FXML
    private TextField nameField;

    @FXML
    private Label statusLabel;

    @FXML
    public void onHostClick(ActionEvent event) {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            statusLabel.setText("Enter a name first.");
            return;
        }

        try {
            // Starts server locally (host = server)
            network.Server.startNewRoom();

            // Connect to localhost as client
            Client client = new Client("127.0.0.1", 5000);
            loadLobbyScene(event, client, name);
        } catch (Exception e) {
            statusLabel.setText("Failed to host game.");
            e.printStackTrace();
        }
    }

    @FXML
    public void onJoinClick(ActionEvent event) {
        String ip = ipAddressField.getText().trim();
        String name = nameField.getText().trim();
        if (ip.isEmpty() || name.isEmpty()) {
            statusLabel.setText("Enter both name and IP.");
            return;
        }

        try {
            Client client = new Client(ip, 5000);
            loadLobbyScene(event, client, name);
        } catch (Exception e) {
            statusLabel.setText("Failed to connect to host.");
            e.printStackTrace();
        }
    }

    private void loadLobbyScene(ActionEvent event, Client client, String name) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxmlFiles/MultiPlayerLobby.fxml"));
        Parent root = loader.load();

        MultiPlayerLobby controller = loader.getController();
        controller.setClient(client, name);

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setTitle("Multiplayer Lobby");
        stage.show();
    }
}

