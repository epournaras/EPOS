package agent.dataset;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DatasetDescriptor {
	
	private String		datasetGroup 				=	null;
	private String 		datasetName 				= 	null;
	private int 		dimensionality 				= 	-1;
	private int 		totalNumAgentsAvailable		= 	-1;
	private int 		numPlansAvailable			=	-1;
	
	public DatasetDescriptor(String datasetGroup, String datasetName, int dimensionality, int totalNumAgentsAvailable, int numPlansAvailable) {
		this.datasetGroup = datasetGroup;
		this.datasetName = datasetName;
		this.dimensionality = dimensionality;
		this.totalNumAgentsAvailable = totalNumAgentsAvailable;
		this.numPlansAvailable = numPlansAvailable;
	}
	
	public DatasetDescriptor(String datasetName, int dimensionality, int totalNumAgentsAvailable, int numPlansAvailable) {
		this(null, datasetName, dimensionality, totalNumAgentsAvailable, numPlansAvailable);
	}
	
	public DatasetDescriptor(String datasetName, int dimensionality) {
		this(null, datasetName, dimensionality, -1, -1);
	}
	
	public String getDatasetName() {
		return this.datasetName;
	}
	
	public String getDatasetGroup() {
		return this.datasetGroup;
	}
	
	public int getDimensionality() {
		return this.dimensionality;
	}
	
	public int getTotalNumAgentsAvailable() {
		return this.totalNumAgentsAvailable;
	}
	
	public int getNumPlansAvailable() {
		return this.numPlansAvailable;
	}
	
	public String getPath() {
		String path = System.getProperty("user.dir");
		Path p = Paths.get(path, "datasets", this.getDatasetName());
		//path += File.separator + "datasets" +  File.separator + this.getDatasetName() + File.separator;
		return p.toString() + File.separator;
	}

}
