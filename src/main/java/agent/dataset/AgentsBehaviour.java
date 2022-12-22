package agent.dataset;

	import java.io.BufferedReader;
	import java.io.File;
	import java.io.FileReader;
	import java.io.IOException;
	import java.util.HashMap;


	/**
	 * 
	 * Provides different behaviours for agents
	 * @author Amal
	 * 
	 */
	
	public class AgentsBehaviour {


		String rootPath = System.getProperty("user.dir");
	     String datasetDir ;
		 public HashMap<String, Double> alphaMap;
		 public HashMap<String, Double> betaMap;
		 int idx;
		 String key;
	     
		 public AgentsBehaviour(String datasetDir) {
		        this.datasetDir = datasetDir;
		        this.alphaMap = new HashMap<>();
				this.betaMap = new HashMap<>();
		    }
	
		 public void setBehaviourHashMap(HashMap<String, Double> alphaMap,HashMap<String, Double> betaMap ) {
			 this.alphaMap = new HashMap<>();
				this.betaMap = new HashMap<>();
		 }
		
		 /**
		  * 
		  * read behaviours from dataset file
		  * store the data into hashmap
		  * 
		  * */
		public void readBehaviours(){
			    String line;
			  BufferedReader in;
				try {
					
					String path = rootPath+File.separator+ "datasets"+File.separator+ datasetDir+File.separator+"behaviours.csv";
				
					in = new BufferedReader(new FileReader(path));			
		    
					while ((line = in.readLine()) != null) {
						String[] columns = line.split(",");
						key = columns[0];
						 idx = Integer.parseInt(key) ;
					      
							alphaMap.put(key, Double.parseDouble(columns[1]));                
						    betaMap.put(key, Double.parseDouble(columns[2]));                        
					}
					
				} 
				catch (NumberFormatException e) {
					e.printStackTrace();
				}
				catch (IOException e) {
					e.printStackTrace();
				}	 
		
		      }
	}
