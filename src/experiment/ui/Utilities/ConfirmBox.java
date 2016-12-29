/**
 * 
 */
package Utilities;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * @author melikaayoughi
 *
 */
public class ConfirmBox {
	static boolean answer;
	
	public boolean display(String title, String message){
		Stage window = new Stage();
		
		window.initModality(Modality.APPLICATION_MODAL);
		window.setTitle(title);
		window.setMinWidth(800);
		window.setMinHeight(200);
		window.setResizable(false);
		
		Label label = new Label();
		label.setText(message);
		
		//create two buttons
		Button yesButton = new Button("Yes");
		Button noButton = new Button("No");
		
		yesButton.setOnAction(e -> {
			answer = true;
			window.close();
		});
		
		noButton.setOnAction(e -> {
			answer = false;
			window.close();
		});
		
		HBox buttons = new HBox(10);
		VBox layout = new VBox(10);
		
		buttons.getChildren().addAll(yesButton,noButton);
		layout.getChildren().addAll(label,buttons);
		
		layout.setAlignment(Pos.CENTER);
		buttons.setAlignment(Pos.CENTER);
		
		Scene scene = new Scene(layout);
		window.setScene(scene);
		window.showAndWait(); 
		
		return answer;
	}
}

