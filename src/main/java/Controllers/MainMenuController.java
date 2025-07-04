package Controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainMenuController {

    @FXML
    private void onSinglePlayerClick(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxmlFiles/typeRacerScene1.fxml"));
        Parent root = loader.load();

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

        Scene scene = new Scene(root);

        stage.setScene(scene);
        stage.setTitle("TypeRacer - SinglePlayer");
        stage.show();
    }
}
/*@FXML
private void handleSceneSwitch(ActionEvent event) throws IOException {
    // Load the FXML layout for the new scene
    FXMLLoader loader = new FXMLLoader(getClass().getResource("/path/to/NextScene.fxml"));
    Parent root = loader.load();

    // Get the current stage (window)
    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

    // Create a new scene with the loaded layout
    Scene scene = new Scene(root);

    // Set the new scene on the stage and show it
    stage.setScene(scene);
    stage.setTitle("Next Scene");
    stage.show();
}
| Event Type   | Triggered By                  | Event Class   |
| ------------ | ----------------------------- | ------------- |
| Mouse event  | Clicking, hovering, dragging  | `MouseEvent`  |
| Action event | Button press, menu select     | `ActionEvent` |
| Key event    | Typing or pressing keys       | `KeyEvent`    |
| Window event | Minimizing, resizing, closing | `WindowEvent` |


| | When `ActionEvent` Fires        |
| ----------- | ------------------------------- |
| `Button`    | When the user clicks it         |
| `TextField` | When the user presses **Enter** |
| `CheckBox`  | When selected/deselected        |
| `ComboBox`  | When the user makes a selection |
| `MenuItem`  | When clicked                    |
event.getSource()    // The UI element that triggered the event
event.getTarget()    // The actual node receiving the event
event.getEventType() // Returns the type, like ACTION
*/