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

import agent.Agent;
import agent.MultiObjectiveIEPOSAgent;
import agent.planselection.OptimizationFactor;
import config.Configuration;
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
public class GlobalComplexCostLogger<V extends DataType<V>> extends AgentLogger<Agent<V>> {
	
	private String 				filepath;
	
	public GlobalComplexCostLogger(String filepath) {
		this.filepath = filepath;
	}

	@Override
	public void init(Agent<V> agent) {	}

	@Override
	public void log(MeasurementLog log, int epoch, Agent<V> agent) {
		if (agent.isRepresentative()) {
            if(agent instanceof MultiObjectiveIEPOSAgent) {
            	MultiObjectiveIEPOSAgent moagent = (MultiObjectiveIEPOSAgent) agent;
            	HashMap<OptimizationFactor, Object> parameters = new HashMap<OptimizationFactor, Object>();
                parameters.put(OptimizationFactor.GLOBAL_COST, agent.getGlobalCostFunction().calcCost(agent.getGlobalResponse()));
                parameters.put(OptimizationFactor.DISCOMFORT_SUM, moagent.getGlobalDiscomfortSum());
                parameters.put(OptimizationFactor.DISCOMFORT_SUM_SQR, moagent.getGlobalDiscomfortSumSqr());
                parameters.put(OptimizationFactor.ALPHA, moagent.getUnfairnessWeight());
                parameters.put(OptimizationFactor.BETA, moagent.getLocalCostWeight());
                parameters.put(OptimizationFactor.NUM_AGENTS, (double) Configuration.numAgents);
                double cost = Configuration.planOptimizationFunction.apply(parameters);
                
                log.log(epoch, GlobalCostLogger.class.getName(), agent.getIteration(), cost);
            }            
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
                Logger.getLogger(GlobalCostLogger.class.getName()).log(Level.SEVERE, null, ex);
            } catch(IOException e) {
            	Logger.getLogger(GlobalCostLogger.class.getName()).log(Level.SEVERE, null, e);
            }
        }
	}
	
	private String internalFetching(MeasurementLog log) {
        List<Double> avg = new ArrayList<>();
        List<Double> std = new ArrayList<>();

        // not really sure what this thing is doing
        for (int i = 0; true; i++) {
            Aggregate aggregate = log.getAggregate(GlobalCostLogger.class.getName(), i);
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
