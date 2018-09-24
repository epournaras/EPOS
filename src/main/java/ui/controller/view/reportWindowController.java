package ui.controller.view;

import ui.controller.MainApplication;
//TODO
import experiment.ExperimentGUI;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;

/**
 * @author melikaayoughi
 *
 */
public class reportWindowController {

	private MainApplication mainApp;

	private int imgSize = 250;
	private int iteration;
	private ExperimentGUI experiment;
	
	@FXML 
	private ImageView imageView1;	// 1 is top left --> global cost, average local cost
	@FXML 
	private ImageView imageView2;	// 2 is bottom left --> global response
	@FXML
	private ImageView imageView3;	// 3 is bottom right --> network
	@FXML
	private Button prevButton;
	@FXML
	private Button nextButton;
	@FXML
	private Label iterationLabel;
	
	@FXML
    private void handleNextBtnAction(ActionEvent e){
		if(iteration < experiment.getNumIterations()) {
			setIteration(iteration+1);
		}
	}
	
	@FXML
    private void handlePrevBtnAction(ActionEvent e){
		if(iteration > 1) {
			setIteration(iteration-1);
		}
	}
	
	private void setIteration(int iteration){
		this.iteration = iteration;
		iterationLabel.setText("Iteration " + iteration);
		
		imageView2.setImage(SwingFXUtils.toFXImage(experiment.getGlobalResponsePlot(imgSize, imgSize, iteration), new WritableImage(imgSize, imgSize)));
		imageView3.setImage(SwingFXUtils.toFXImage(experiment.getAgentChangesPlot(imgSize, imgSize, iteration), new WritableImage(imgSize, imgSize)));
	}
	
	private void initializeView(){
		iterationLabel.setAlignment(Pos.CENTER);
	}
	
	/**
	 * Sets the experiment to GUI experiment 
	 * specified in configuration window controller
	 * 
	 * @param experiment
	 */
	public void setExperiment(ExperimentGUI experiment){
		this.experiment = experiment;
		imageView1.setImage(SwingFXUtils.toFXImage(experiment.getGlobalCostPlot(2*imgSize, imgSize), new WritableImage(2*imgSize, imgSize)));
		setIteration(1);
	}
    
    /**
     * Initializes the controller class. This method is automatically called
     * after the fxml file has been loaded.
     */
    @FXML
    private void initialize() {
    	initializeView();
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
