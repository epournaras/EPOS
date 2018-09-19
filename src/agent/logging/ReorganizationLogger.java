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
import data.DataType;
import protopeer.measurement.Aggregate;
import protopeer.measurement.MeasurementLog;

/**
 * 
 * 
 * @author jovan
 *
 * @param <V>
 */
public class ReorganizationLogger<V extends DataType<V>> extends AgentLogger<Agent<V>> {
	
	private String 				filepath;
	
	public ReorganizationLogger(String filename) {
		this.filepath = filename;
	}

	@Override
	public void init(Agent<V> agent) {	
	}

	@Override
	public void log(MeasurementLog log, int epoch, Agent<V> agent) {
		if(agent.isRepresentative()) {
			log.log(epoch, 
					ReorganizationLogger.class.getName(), 			// tag1
					
					agent.getIteration(), 							// tag3
					agent.getNumReorganizations());					// value
		} else {
			
		}
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
                Logger.getLogger(ReorganizationLogger.class.getName()).log(Level.SEVERE, null, ex);
            } catch(IOException e) {
            	Logger.getLogger(ReorganizationLogger.class.getName()).log(Level.SEVERE, null, e);
            }
        }
	}
	
	private String internalFetching(MeasurementLog log) {
		List<Double>	reorganizationsVsiterations = new ArrayList<Double>();
		
		int i = 0;
		while(true) {
			Aggregate aggregate = log.getAggregate(ReorganizationLogger.class.getName(), i);
			if (aggregate == null || aggregate.getNumValues() < 1) {
                break;
            }
			reorganizationsVsiterations.add(aggregate.getAverage());
			i++;
		}
		Logger.getLogger(SelectedPlanLogger.class.getName()).log(Level.INFO, 
        		" Number of samples: " + i);
		
		return this.format(reorganizationsVsiterations);
	}
	
	
	private String format(List<Double> thelist) {
		StringBuilder sb = new StringBuilder();
		int numIterations = thelist.size();
		
		IntStream.range(0,  numIterations).forEach(i -> {
			sb.append(thelist.get(i).intValue());
			if(i < thelist.size()-1) {
				sb.append(",");
			}
		});
		sb.append(System.lineSeparator());
		
		return sb.toString();
	}

}
