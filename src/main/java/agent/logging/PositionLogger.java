package agent.logging;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import agent.Agent;
import config.Configuration;
import data.DataType;
import experiment.IEPOSExperiment;
import protopeer.measurement.MeasurementLog;
		
/**
		 * @author AmalAldawsari
		 * @param <V>
		 * prints the order of the agents in the tree to the output folder.
		 */
		public class PositionLogger<V extends DataType<V>> extends AgentLogger<Agent<V>>  {

	
			private String 				filepath;
			private int					totalNumAgents;
			private Integer[][] mappings;
			
		    
		    /**
		     * Outputs the selected plans during runtime to the specified file.
		     *
		     * @param filename the output file
		     */
		    public PositionLogger(String filename, int totalNumAgents ) {
		        this.filepath = filename;
		        this.totalNumAgents = totalNumAgents;
		     
		    }
		    public PositionLogger(String filename,Integer[][] mappings) {
		    	this.filepath = filename;
		    	this.mappings=mappings;
		    }
		    public PositionLogger(String filename) {
		        this(filename, null);
		    }
		    
		    public PositionLogger(Integer[][] mappings) {
		    	this.mappings=mappings;
		    }

			@Override
			public void init(Agent<V> agent) {
				
			}

			@Override
			public void log(MeasurementLog log, int epoch, Agent<V> agent) {
				log.log(epoch, 
						PositionLogger.class.getName(), 						// tag1
						"ID-run" + this.run + "-agent" , 							// tag2
						agent.getPositionIdx());									// value
			}

			@Override
			public void print(MeasurementLog log) {
				String outcome = this.internalFetching(log);
		    	
		        if (this.filepath == null) {
		            System.out.print(outcome);
		        } else {
		            try (PrintWriter out = new PrintWriter(new BufferedWriter(new java.io.FileWriter(this.filepath, true)))) {   
		                out.append(outcome);
		            } catch (FileNotFoundException ex) {
		                Logger.getLogger(PositionLogger.class.getName()).log(Level.SEVERE, null, ex);
		            } catch(IOException e) {
		            	Logger.getLogger(PositionLogger.class.getName()).log(Level.SEVERE, null, e);
		            }
		        }
			}
			
			private String internalFetching(MeasurementLog log) {
			    
				StringBuilder sb = new StringBuilder();
				
				sb.append("Run")
				  .append(",");
				
				IntStream.range(0, totalNumAgents).forEach(i -> {
					sb.append("agent-" + i);
					if(i < totalNumAgents-1) {
						sb.append(",");
					}
				});
				sb.append(System.lineSeparator());	
				
			for(int run = 0; run < Configuration.numSimulations; run++) {
						
				if (run==0) 
					{
					sb.append(run+1+",");
					for (Integer key: Configuration.mapping.keySet()){
						sb.append(key+",");	
					}
					 sb.append(System.lineSeparator());
					}
				else {
				sb.append(run+1+",");
				for (int i = 0; i <  totalNumAgents; i++) {
					
						mappings=IEPOSExperiment.mappings;
							
							sb.append(mappings[run-1][i]+",");
				}        	
					 sb.append(System.lineSeparator());		
				}			
					 }		
				
					return sb.toString();		
				}


}
