package agent.logging;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import agent.Agent;
import config.Configuration;
import data.DataType;
import protopeer.measurement.Aggregate;
import protopeer.measurement.MeasurementLog;

/**
 * Tracks selected plan IDs in each iteration and writes them to:
 *  - console if <code>filepath == null</code>
 *  - specified file otherwise
 * 
 * @author jovan
 *
 * @param <V>
 */
public class SelectedPlanLogger<V extends DataType<V>> extends AgentLogger<Agent<V>> {
	
	private String 				filepath;
	private int					totalNumAgents;
	
    
    /**
     * Outputs the selected plans during runtime to the specified file.
     *
     * @param filename the output file
     */
    public SelectedPlanLogger(String filename, int totalNumAgents) {
        this.filepath = filename;
        this.totalNumAgents = totalNumAgents;
    }

	@Override
	public void init(Agent<V> agent) {
		
	}

	@Override
	public void log(MeasurementLog log, int epoch, Agent<V> agent) {
		log.log(epoch, 
				SelectedPlanLogger.class.getName(), 							// tag1
				"ID-run" + this.run + "-agent" + agent.getPeer().getIndexNumber(), 	// tag2
				agent.getIteration(), 											// tag3
				agent.getSelectedPlanID());										// value
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
                Logger.getLogger(SelectedPlanLogger.class.getName()).log(Level.SEVERE, null, ex);
            } catch(IOException e) {
            	Logger.getLogger(SelectedPlanLogger.class.getName()).log(Level.SEVERE, null, e);
            }
        }
	}
	
	private String internalFetching(MeasurementLog log) {
		StringBuilder sb = new StringBuilder();
		
		sb.append("Run")
		  .append(",")
		  .append("Iteration")
		  .append(",");
		
		IntStream.range(0, totalNumAgents).forEach(i -> {
			sb.append("agent-" + i);
			if(i < totalNumAgents-1) {
				sb.append(",");
			}
		});
		sb.append(System.lineSeparator());		
		
		for(int simID = 0; simID < Configuration.numSimulations; simID++) {
			sb.append(this.internalFetchingperRun(log, simID));
		}
		
		return sb.toString();		
	}
	
	private String internalFetchingperRun(MeasurementLog log, int run) {
		HashMap<Integer, List<Double>> selectedPlans = new HashMap<Integer, List<Double>>();
		
		for(int i = 0; i < this.totalNumAgents; i++) {
			selectedPlans.put(i, new ArrayList<Double>());
		}
		
		selectedPlans.keySet().forEach(agentIdx -> {
			int i = 0;
			for (; true; i++) {
	            Aggregate aggregate = log.getAggregate(SelectedPlanLogger.class.getName(), "ID-run" + run + "-agent" + agentIdx, i);
	            if (aggregate == null || aggregate.getNumValues() < 1) {
	                break;
	            }
	            
	            selectedPlans.get(agentIdx).add(aggregate.getAverage());
	        }
			Logger.getLogger(SelectedPlanLogger.class.getName()).log(Level.INFO, 
            		"NODE: " + agentIdx + " Number of samples: " + i);
		});
		
		return this.format(selectedPlans, run);
	}
	
	private String format(HashMap<Integer, List<Double>> selectedPlans, int run) {
		StringBuilder sb = new StringBuilder();
		int numIterations = selectedPlans.get(0).size();
		
		for(int iteration = 0; iteration < numIterations; iteration++) {
			sb.append(run)
			  .append(",")
			  .append(iteration);
			
			for(int agentIdx = 0; agentIdx < this.totalNumAgents; agentIdx++) {
				sb.append(",").append(selectedPlans.get(agentIdx).get(iteration).intValue());
			}
			sb.append(System.lineSeparator());
		}
		
		return sb.toString();
	}

}
