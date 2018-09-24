package ui.Utilities;

import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class ProgressForm {
	private final Stage dialogStage;
	private final ProgressIndicator pin = new ProgressIndicator();

	public ProgressForm(String string) {
		dialogStage = new Stage();
		dialogStage.initStyle(StageStyle.UTILITY);
		dialogStage.setResizable(false);
		dialogStage.initModality(Modality.APPLICATION_MODAL);

		pin.setProgress(-1F);

		final Label progressLabel = new Label();
		progressLabel.setText(string);
		
		final VBox vb = new VBox();
		vb.setSpacing(20);
		vb.setAlignment(Pos.CENTER);
		vb.getChildren().addAll(pin, progressLabel);

		Scene scene = new Scene(vb);
		dialogStage.setScene(scene);
		dialogStage.setTitle("Running");
		dialogStage.setWidth(200);
		dialogStage.setHeight(200);
	}

	public void activateProgressBar(final Task<?> task)  {
		pin.progressProperty().bind(task.progressProperty());
		dialogStage.show();
	}

	public Stage getDialogStage() {
		return dialogStage;
	}
}
