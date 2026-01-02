package il.cshaifasweng.OCSFMediatorExample.client;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class SecondaryController
{

    @FXML private Label resultLabel;

    public void setResultText(String text)
    {
        resultLabel.setText(text);
    }

    @FXML
    private void handleClose()
    {
        Stage stage = (Stage) resultLabel.getScene().getWindow();
        stage.close();
    }
}
