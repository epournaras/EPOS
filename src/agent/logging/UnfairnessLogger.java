package agent.logging;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import agent.Agent;
import agent.MultiObjectiveIEPOSAgent;
import agent.planselection.PlanSelectionOptimizationFunctionCollection;
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
public class UnfairnessLogger<V extends DataType<V>> extends AgentLogger<Agent<V>> {
	
	private String filename;
	
	public UnfairnessLogger(String filename) {
		this.filename = filename;
	}

	@Override
	public void init(Agent<V> agent) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void log(MeasurementLog log, int epoch, Agent<V> agent) {
		MultiObjectiveIEPOSAgent moieposagent = (MultiObjectiveIEPOSAgent) agent;
		if(moieposagent.isRoot()) {
			double discomfortSum = moieposagent.getGlobalDiscomfortSum();
			double discomfortSumSqr = moieposagent.getGlobalDiscomfortSumSqr();
			int numAgents = moieposagent.getNumAgents();
			double unfairness = PlanSelectionOptimizationFunctionCollection.unfairness(discomfortSum, discomfortSumSqr, numAgents);
			log.log(epoch, UnfairnessLogger.class.getName(), moieposagent.getIteration(), unfairness);
		}		
	}

	@Override
	public void print(MeasurementLog log) {
		String outcome = this.internalFetching(log);
    	
        if (this.filename == null) {
            System.out.print(outcome);
        } else {
            try (PrintWriter out = new PrintWriter(new BufferedWriter(new java.io.FileWriter(this.filename, true)))) {   
                out.append(outcome);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(GlobalCostLogger.class.getName()).log(Level.SEVERE, null, ex);
            } catch(IOException e) {
            	Logger.getLogger(GlobalCostLogger.class.getName()).log(Level.SEVERE, null, e);
            }
        }
	}
	
	private String internalFetching(MeasurementLog log) {
		List<Double> avg = new ArrayList<>();
		List<Double> std = new ArrayList<>();

        for (int i = 0; true; i++) {
            Aggregate aggregate = log.getAggregate(UnfairnessLogger.class.getName(), i);
            if (aggregate == null || aggregate.getNumValues() < 1) {
                break;
            }
            avg.add(aggregate.getAverage());
            std.add(aggregate.getStdDev());
        }
        
        return this.format(avg);
	}
	
	private String format(List<Double> avgs) {
    	StringBuilder sb = new StringBuilder();
    	sb.append(this.run);
    	for(Double e : avgs) {
//    		sb.append("," + e);
    		sb.append(System.lineSeparator() + e);
    	}
    	sb.append(System.lineSeparator());
    	return sb.toString();    	
    }

}
