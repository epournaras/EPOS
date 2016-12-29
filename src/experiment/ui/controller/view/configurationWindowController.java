/**
 * 
 */
package experiment.ui.controller.view;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;


import Utilities.ExceptionDialog;
import Utilities.ProgressForm;
import Utilities.ConfirmBox;
import experiment.ui.controller.MainApplication;
//TODO:
import experiment.ExperimentGUI;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import jdk.nashorn.internal.runtime.regexp.joni.Warnings;
import javafx.stage.Stage;

/**
 * @author melikaayoughi
 *
 */
public class configurationWindowController {

	private MainApplication mainApp;
	//TODO:
	private ExperimentGUI experiment = new ExperimentGUI();
	
	@FXML
	private TextField dataSetLocationTextField;
	@FXML
	private Button dataSetBrowse;
	@FXML
	private TextField globalCostLocationTextField;
	@FXML
	private Button globalCostBrowse;
	@FXML
	private Slider localCostInfluenceSlider;
	@FXML
	private Label localCostLabel;
	@FXML
	private ComboBox<String> algorithmComboBox;
	@FXML
	private Slider numberOfChildrenSlider;
	@FXML
	private Label numberOfChildrenLabel;
	@FXML
	private Slider numberOfIterationsSlider;
	@FXML
	private Label numberOfIterationsLabel;
	@FXML
	private TextField seedTextField;
	@FXML
	private Button abortBtn;
	@FXML
	private Button runBtn;
	
	////////////////////////////////// public getters ////////////////////////////////////////
	
	public String getDataSetLocation(){
		if(!dataSetLocationTextField.getText().isEmpty())
			return dataSetLocationTextField.getText();
		else
			return dataSetLocationTextField.getPromptText();
	}
	
	public String getGlobalCostLocation(){
		if(!globalCostLocationTextField.getText().isEmpty())
			return globalCostLocationTextField.getText();
		else
			return globalCostLocationTextField.getPromptText();
	}
	
	public Double getLocalCostInfluence(){
		return Double.parseDouble(localCostLabel.getText().toString());
	}
	
	public String getAlgorithm(){
		return algorithmComboBox.getValue();
	}
	
	public Integer getNumberOfChildren(){
		return Integer.parseInt(numberOfChildrenLabel.getText());
	}

	public Integer getNumberOfIterations(){
		return Integer.parseInt(numberOfIterationsLabel.getText());
	}
	
	public Long getSeed(){
		if(!seedTextField.getText().isEmpty())
			return Long.parseLong(seedTextField.getText());
		else
			return Long.parseLong(seedTextField.getPromptText());
	}
	
	////////////////////////////////// Set Desired Defaults ///////////////////////////////////
	
	private void setDefaultsConfigurationWindow(){
		Locale.setDefault(Locale.US);
		
		// Data Set Location
		dataSetLocationTextField.setPromptText("datasets" + File.separatorChar + "gaussian");
		
		// Global Cost Location
		globalCostLocationTextField.setPromptText(getDataSetLocation() + File.separatorChar + "zero.target");
		
		// Local Cost Influence Slider
		localCostInfluenceSlider.setValue(0.1);
		localCostLabel.setText(String.format("%.1f", localCostInfluenceSlider.getValue()));
		localCostInfluenceSlider.valueProperty().addListener(new ChangeListener<Number>() {
			public void changed(ObservableValue<? extends Number> ov,
					Number old_val, Number new_val) {
				Double toBeTruncated = new Double(new_val.doubleValue());
				Double truncatedDouble = BigDecimal.valueOf(toBeTruncated)
					    .setScale(1, RoundingMode.HALF_UP)
					    .doubleValue();
				localCostInfluenceSlider.setValue(truncatedDouble);
				localCostLabel.setText(String.format("%.1f", new_val));
			}
		});

		// Algorithm combo box
		ObservableList<String> optAlgorithm = 
				FXCollections.observableArrayList(
						"I-EPOS",
						"Global Gradient",
						"Individual Gradient",
						"Adaptive Gradient");
		algorithmComboBox.setItems(optAlgorithm);
		algorithmComboBox.getSelectionModel().selectFirst();

		// Number of Children Slider
		numberOfChildrenLabel.setText(String.format("%.0f", numberOfChildrenSlider.getValue()));
		numberOfChildrenSlider.valueProperty().addListener(new ChangeListener<Number>() {
			public void changed(ObservableValue<? extends Number> ov,
					Number old_val, Number new_val) {
				numberOfChildrenSlider.setValue(new_val.intValue());
				numberOfChildrenLabel.setText(Integer.toString(new_val.intValue()));
				}
		});
				
		// Number of Iterations Slider
		numberOfIterationsSlider.setValue(15);
		numberOfIterationsLabel.setText(String.format("%.0f", numberOfIterationsSlider.getValue()));
		numberOfIterationsSlider.valueProperty().addListener(new ChangeListener<Number>() {
			public void changed(ObservableValue<? extends Number> ov,
					Number old_val, Number new_val) {
				numberOfIterationsSlider.setValue(new_val.intValue());
				numberOfIterationsLabel.setText(String.format("%.0f", new_val));
			}
		});

		// Seed Text Field
		seedTextField.setPromptText("" + new Random().nextLong());
	}
	
	@FXML
    private void handleBrowseDataSetLocation(ActionEvent e){
    	DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Dataset Location");
        directoryChooser.setInitialDirectory(new File("datasets"));
        //Show open file dialog
        File file = directoryChooser.showDialog(null);

       if(file!=null) {
            dataSetLocationTextField.setText(file.getPath());
    		globalCostLocationTextField.setText(getDataSetLocation() + File.separatorChar + "zero.target");
       }
    }
	
	@FXML
    private void handleBrowseGlobalCostLocation(ActionEvent e){
    	FileChooser fileChooser = new FileChooser();
    	fileChooser.setTitle("Global Cost Location");
    	fileChooser.setInitialDirectory(new File(getDataSetLocation()));
    	fileChooser.getExtensionFilters().addAll(new ExtensionFilter("(.csv) (.target) files", "*.target", "*.csv"));
    	
        //Show open file dialog
        File file = fileChooser.showOpenDialog(null);

       if(file!=null)
            globalCostLocationTextField.setText(file.getPath());
    }
	
	@FXML
	public void handleAbortButtonAction(ActionEvent event) {
	    Stage stage = (Stage) abortBtn.getScene().getWindow();
	    stage.close();
	}
	
	@FXML
    private void handleRunBtn(ActionEvent e){
		try {
			ConfirmBox confirmBox = new ConfirmBox();
			boolean answer = confirmBox.display("Warning", "You have chosen the following settings."+
			"Please check them before running" + "\n" +
					"data set location: " + getDataSetLocation() + "\n" +
					"global cost location: " + getGlobalCostLocation() + "\n" +
					"local cost influence: " + getLocalCostInfluence().toString() + "\n" +
					"algorithm: " + getAlgorithm() + "\n" +
					"number of children: " + getNumberOfChildren() + "\n" +
					"number of iterations: " + getNumberOfIterations() + "\n" +
					"seed: " + getSeed());
			if (answer == true){
				try {
					
					ProgressForm pForm = new ProgressForm("Running...");

					Task<Void> task = new Task<Void>() {
						@Override
						public Void call() throws InterruptedException {
							updateProgress(0, 1);
							experiment.setAlgorithm(getAlgorithm());
							experiment.setDataset(getDataSetLocation());
							experiment.setGlobalCostFunc(getGlobalCostLocation());
							experiment.setLambda(getLocalCostInfluence());
							experiment.setNumChildren(getNumberOfChildren());
							experiment.setNumIterations(getNumberOfIterations());
							experiment.setSeed(getSeed());
							experiment.onProgressDo(percentComplete -> {
								updateProgress(percentComplete, 1);
							});
							experiment.run();
							updateProgress(1, 1);
							return null;
						}
					};

					// binds progress of progress bars to progress of task:
					pForm.activateProgressBar(task);

					// In real life this method would get the result of the task
					// and update the UI based on its value:
					task.setOnSucceeded(event -> {
						pForm.getDialogStage().close();
						
						// run is finished. It's time to open the report window.
						mainApp.showReportWindow(experiment);
					});
					task.setOnFailed(event -> {
						pForm.getDialogStage().close();
					});
					pForm.getDialogStage().show();

					Thread thread = new Thread(task);
					thread.start();
				} catch (Exception e2) {
					e2.printStackTrace();
					ExceptionDialog exDialog = new ExceptionDialog();
					exDialog.display(e2);
				}
			}
		} catch (Exception e2) {
			e2.printStackTrace();
		}
	}
	
	/**
     * Initializes the controller class. This method is automatically called
     * after the fxml file has been loaded.
     */
    @FXML
    private void initialize() {
    	// Set prompt values for text fields and combo boxes
    	setDefaultsConfigurationWindow();
    }
	
	/**
     * Is called by the main application to give a reference back to itself.
     * 
     * @param mainApp
     */
    public void setMainApp(MainApplication mainApp) {
        this.mainApp = mainApp;
    }
}
