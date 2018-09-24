package agent.dataset;

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
		StringBuilder sb = new StringBuilder();
		sb.append("datasets/");
		if(this.datasetGroup != null && 
		  (this.datasetGroup.contains("energy2") || this.datasetGroup.contains("EPOS-ELECTRIC-VEHICLES"))
		  ) {
			sb.append(this.datasetGroup).append("/");
		}
		sb.append(this.datasetName + "/");
		return sb.toString();
	}

}
