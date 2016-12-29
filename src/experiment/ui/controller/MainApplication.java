/**
 * 
 */
package experiment.ui.controller;

import java.io.IOException;

import experiment.ExperimentGUI;
import experiment.ui.controller.view.configurationWindowController;
import experiment.ui.controller.view.reportWindowController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

/**
 * @author melikaayoughi
 *
 */
public class MainApplication extends Application {
	
	private Stage primaryStage;
	private Stage secondaryStage = new Stage();
	private AnchorPane configurationWindow;
	private AnchorPane reportWindow;
	
	private ExperimentGUI experiment;
	
	public ExperimentGUI getExperiment() {
		return experiment;
	}
	
	@Override
	public void start(Stage primaryStage){
		
		try {
			this.primaryStage = primaryStage;
	        this.primaryStage.setTitle("Configuration");
	        showConfigurationWindow();
		} catch (Exception e) {
			e.printStackTrace();
		}	
	}
	
	/**
     * Shows the configuration window
     */
    public void showConfigurationWindow() {
        try {
            // Load configuration from fxml file.
            FXMLLoader configurationWindowloader = new FXMLLoader();
            configurationWindowloader.setLocation(MainApplication.class.getResource("view/configurationWindow.fxml"));
            configurationWindow = (AnchorPane) configurationWindowloader.load();
            
            // Give the "configuration window controller" access to the main.
            configurationWindowController cwc = configurationWindowloader.getController();
            cwc.setMainApp(this);
            
            // Show the scene containing the configuration window.
            Scene scene = new Scene(configurationWindow);
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Shows the report window
     */
    //TODO
    public void showReportWindow(ExperimentGUI experiment){
    	this.experiment = experiment;
    	
    	try {
    		// Load configuration from fxml file.
            FXMLLoader reportWindowloader = new FXMLLoader();
            reportWindowloader.setLocation(MainApplication.class.getResource("view/reportWindow.fxml"));
            reportWindow = (AnchorPane) reportWindowloader.load();
            
            // Show the scene containing the configuration window.
            Scene scene = new Scene(reportWindow);
            primaryStage.close();
            
            // Give the "configuration window controller" access to the main.
            reportWindowController rwc = reportWindowloader.getController();
            rwc.setExperiment(experiment);
            
            secondaryStage.setTitle("Report Window");
            secondaryStage.setScene(scene);
            secondaryStage.show();
            
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
	public static void main(String[] args) {
		try {
			launch(args);
		} catch (Exception e) {
			e.printStackTrace();
		}       
    }
}
