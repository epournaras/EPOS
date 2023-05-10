<<<<<<< HEAD
package agent.dataset;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.HashMap;


import agent.MultiObjectiveIEPOSAgent;
import config.Configuration;


/**
 *
 * Provides different behaviours for agents
 * @author AmalAldawsari
 *
 */

public class AgentsBehaviour {


	static String rootPath = System.getProperty("user.dir");
	static String datasetDir ;
	public HashMap<String, Double> alphaMap;
	public HashMap<String, Double> betaMap;
	//		 int idx;
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

	public static String getBehavioursFilename() {
		return rootPath+File.separator+ "datasets"+File.separator+ datasetDir+File.separator+"behaviours.csv";
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
			File f = new File(getBehavioursFilename() );

			if (f.exists())
			{
				String path = getBehavioursFilename() ;
				//System.out.println("The file exists");
				in = new BufferedReader(new FileReader(path));

				while ((line = in.readLine()) != null) {
					String[] columns = line.split(",");
					key = columns[0];
					//	 idx = Integer.parseInt(key) ;

					alphaMap.put(key, Double.parseDouble(columns[1]));
					betaMap.put(key, Double.parseDouble(columns[2]));
				}
			}
			else {
				for(int i=0;i<Configuration.numAgents;i++) {
					key = i+"";
					alphaMap.put(key,Double.parseDouble(Configuration.weights[0]));
					betaMap.put(key, Double.parseDouble(Configuration.weights[1]));
				}

			}


		}

		catch (NumberFormatException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}

	}


//		public static String noBehaviourFile() {
//		//	 String path = AgentsBehaviour.getBehavioursFilename();
//	  		 StringBuilder sb = new StringBuilder();
//	  	//	 FileWriter fw = null;
//	  	//	 PrintWriter pw = null;
//	  //		try {
//	  		//	fw = new FileWriter (path,true);
//	  		//	pw = new PrintWriter(fw);
//	  			for(int i=0;i<Configuration.numAgents;i++) {
//	  			sb.append(i)
//	  			.append(",")
//	  			.append(0)
//	  			.append(",")
//	  			.append(0);
//	  			 sb.append(System.lineSeparator());
//	  			}
//	  return sb.toString();
//
//	  		//	 pw.write(sb.toString());
//	  		//	 pw.close();
//	  		//	 fw.close();
//
//	  	//	} catch (IOException e) {
//	  	//		e.printStackTrace();
//	  	//	}
//
//
//		}
}
=======
package agent.dataset;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.HashMap;


import agent.MultiObjectiveIEPOSAgent;
import config.Configuration;


	/**
	 * 
	 * Provides different behaviours for agents
	 * @author AmalAldawsari
	 * 
	 */
	
	public class AgentsBehaviour {


		static String rootPath = System.getProperty("user.dir");
	     static String datasetDir ;
		 public HashMap<String, Double> alphaMap;
		 public HashMap<String, Double> betaMap;
//		 int idx;
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
		
		 public static String getBehavioursFilename() {
				return rootPath+File.separator+ "datasets"+File.separator+ datasetDir+File.separator+"behaviours.csv";
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
					File f = new File(getBehavioursFilename() );

					if (f.exists())
				{	
					String path = getBehavioursFilename() ;
					//System.out.println("The file exists");
					in = new BufferedReader(new FileReader(path));			
		    
					while ((line = in.readLine()) != null) {
						String[] columns = line.split(",");
						key = columns[0];
					//	 idx = Integer.parseInt(key) ;
					      
							alphaMap.put(key, Double.parseDouble(columns[1]));                
						    betaMap.put(key, Double.parseDouble(columns[2]));                        
					}
					}
				else {
					for(int i=0;i<Configuration.numAgents;i++) {
						key = i+"";
						alphaMap.put(key,Double.parseDouble(Configuration.weights[0]));
						betaMap.put(key, Double.parseDouble(Configuration.weights[1]));					
					}
				
				}
				
				
				} 
				
				catch (NumberFormatException e) {
					e.printStackTrace();
				}
				catch (IOException e) {
					e.printStackTrace();
				}	 
		
		      }
		
		
//		public static String noBehaviourFile() {
//		//	 String path = AgentsBehaviour.getBehavioursFilename();
//	  		 StringBuilder sb = new StringBuilder();
//	  	//	 FileWriter fw = null;
//	  	//	 PrintWriter pw = null;
//	  //		try {
//	  		//	fw = new FileWriter (path,true);
//	  		//	pw = new PrintWriter(fw);
//	  			for(int i=0;i<Configuration.numAgents;i++) {
//	  			sb.append(i)
//	  			.append(",")
//	  			.append(0)
//	  			.append(",")
//	  			.append(0);
//	  			 sb.append(System.lineSeparator());
//	  			}
//	  return sb.toString();
//	  			
//	  		//	 pw.write(sb.toString());
//	  		//	 pw.close();
//	  		//	 fw.close();
//	  			
//	  	//	} catch (IOException e) {
//	  	//		e.printStackTrace();
//	  	//	}
//			
//			
//		}
	}
>>>>>>> master
