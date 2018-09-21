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
public class WeightsLogger<V extends DataType<V>> extends AgentLogger<Agent<V>> {
	
	private String 				filepath;
	
	public WeightsLogger(String filepath) {
		this.filepath = filepath;
	}

	@Override
	public void init(Agent<V> agent) {
		// TODO Auto-generated method stub
	}

	@Override
	public void log(MeasurementLog log, int epoch, Agent<V> agent) {
		if (agent.isRepresentative()) {
            if(agent instanceof MultiObjectiveIEPOSAgent) {
            	MultiObjectiveIEPOSAgent moagent = (MultiObjectiveIEPOSAgent) agent;            	
                log.log(epoch, GlobalCostLogger.class.getName()+"ALPHA", agent.getIteration(), moagent.getUnfairnessWeight());
                log.log(epoch, GlobalCostLogger.class.getName()+"BETA", agent.getIteration(), moagent.getLocalCostWeight());
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
        List<Double> avgAlpha = new ArrayList<>();
        List<Double> stdAlpha = new ArrayList<>();
        List<Double> avgBeta = new ArrayList<>();
        List<Double> stdBeta = new ArrayList<>();

        // not really sure what this thing is doing
        for (int i = 0; true; i++) {
            Aggregate aggregate = log.getAggregate(GlobalCostLogger.class.getName()+"ALPHA", i);
            if (aggregate == null || aggregate.getNumValues() < 1) {
                break;
            }
            avgAlpha.add(aggregate.getAverage());
            stdAlpha.add(aggregate.getStdDev());
            
            aggregate = log.getAggregate(GlobalCostLogger.class.getName()+"BETA", i);
            if (aggregate == null || aggregate.getNumValues() < 1) {
                break;
            }
            avgBeta.add(aggregate.getAverage());
            stdBeta.add(aggregate.getStdDev());
        }
        
        return this.format(avgAlpha, avgBeta);
    }
	
	private String format(List<Double> avgAlpha, List<Double> avgBeta) {
//		return this.formatOneList(avgAlpha) + this.formatOneList(avgBeta);
		StringBuilder sb = new StringBuilder();
    	sb.append("Unfairness weight").append(",").append("Local cost weight").append(",").append("Global cost weight").append(System.lineSeparator());
    	for(int i = 0; i < avgAlpha.size(); i++) {
    		sb.append(avgAlpha.get(i) + "," + avgBeta.get(i) + "," + (1-avgAlpha.get(i)-avgBeta.get(i))).append(System.lineSeparator());
    	}
    	sb.append(System.lineSeparator());
    	return sb.toString(); 
    }
	
//	private String formatOneList(List<Double> avgs) {
//		StringBuilder sb = new StringBuilder();
//    	sb.append(this.run);
//    	for(Double e : avgs) {
//    		sb.append("," + e);
//    	}
//    	sb.append(System.lineSeparator());
//    	return sb.toString(); 
//	}

}
