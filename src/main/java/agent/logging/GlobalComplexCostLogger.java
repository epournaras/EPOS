package agent.logging;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
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
		
		if(!(agent instanceof MultiObjectiveIEPOSAgent)) {
			System.err.println("GlobalComplexCostLogger can only be used with MultiObjectiveIEPOSAgent.");
			throw new ClassCastException();
		}
		
		MultiObjectiveIEPOSAgent moagent = (MultiObjectiveIEPOSAgent) agent;
		
		if (moagent.isRoot()) {            	
        	HashMap<OptimizationFactor, Object> parameters = new HashMap<OptimizationFactor, Object>();
            parameters.put(OptimizationFactor.GLOBAL_COST, agent.getGlobalCostFunction().calcCost(agent.getGlobalResponse()));
            parameters.put(OptimizationFactor.DISCOMFORT_SUM, moagent.getGlobalDiscomfortSum());
            parameters.put(OptimizationFactor.DISCOMFORT_SUM_SQR, moagent.getGlobalDiscomfortSumSqr());
            parameters.put(OptimizationFactor.ALPHA, moagent.getUnfairnessWeight());
            parameters.put(OptimizationFactor.BETA, moagent.getLocalCostWeight());
            parameters.put(OptimizationFactor.NUM_AGENTS, (double) Configuration.numAgents);
            double cost = Configuration.planOptimizationFunction.apply(parameters);
            
            Token token = new Token(cost, agent.getIteration(), this.run);            
            log.log(epoch, GlobalComplexCostLogger.class.getName(), token, 1.0);
            log.log(epoch, GlobalComplexCostLogger.class.getName()+"raw", agent.getIteration(), cost);          
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
    	
    	///////////////////////////////////////////////////////////////////////////////////////
    	// PER RUN, PER ITERATION
    	
    	TreeSet<Object> allTokens = new TreeSet<Object>();
		allTokens.addAll(log.getTagsOfType(Token.class));
		Iterator<Object> iter = allTokens.iterator();
		
		HashMap<Integer, ArrayList<Token>> perRun = new HashMap<Integer, ArrayList<Token>>(); 
		
		while(iter.hasNext()) {
			Token token = (Token) iter.next();			
			if(!perRun.containsKey(token.run)) {
				perRun.put(token.run, new ArrayList<Token>());
			}			
			ArrayList<Token> thelist = perRun.get(token.run);
			thelist.add(token);
		}
		
		ArrayList<Integer> sortedKeys = new ArrayList<>(perRun.keySet());
		Collections.sort(sortedKeys);
		
		///////////////////////////////////////////////////////////////////////////////////////
		// AVERAGE OVER RUNS
		
		List<Double> avg = new ArrayList<>();
        List<Double> std = new ArrayList<>();        

        // not really sure what this thing is doing
        for (int i = 0; true; i++) {
            Aggregate aggregate = log.getAggregate(GlobalComplexCostLogger.class.getName() + "raw", i);
            if (aggregate == null || aggregate.getNumValues() < 1) {
                break;
            }
            avg.add(aggregate.getAverage());
            std.add(aggregate.getStdDev());
        }
        
		///////////////////////////////////////////////////////////////////////////////////////
		// FORMTATTING        
		
		StringBuilder sb = new StringBuilder();
		sb.append("Iteration")
		  .append("," + "Mean")
		  .append("," + "Stdev");	
		
		for(int j = 0; j < sortedKeys.size(); j++) {
			sb.append("," + "Run-" + sortedKeys.get(j));
		}
		
		sb.append(System.lineSeparator());
		
		for(int i = 0; i < perRun.get(sortedKeys.get(0)).size(); i++) {
			sb.append(i)
			  .append("," + avg.get(i))
			  .append("," + std.get(i));
			
			for(int j = 0; j < sortedKeys.size(); j++) {
				sb.append("," + perRun.get(sortedKeys.get(j)).get(i).scalarizedCost);
			}
			sb.append(System.lineSeparator());
		}
		
		return sb.toString();
    }
	
	private class Token implements Comparable<Token> {
		
		public double scalarizedCost;
		public int iteration;
		public int run;
		
		public Token(double scalarizedCost, int iteration, int run) {
			this.scalarizedCost = scalarizedCost;
			this.iteration = iteration;
			this.run = run;
		}
		
		@Override
        public int hashCode() {
            int hash = 3;
            hash = 53 * hash + this.run;
            hash = 53 * hash + this.iteration;
            return hash;
        }

		@Override
		public int compareTo(GlobalComplexCostLogger<V>.Token other) {
			
			if		(this.run > other.run)					return 1;
			else if (this.run < other.run)					return -1;
			
			if		(this.iteration > other.iteration)		return 1;
			else if (this.iteration < other.iteration)		return -1;
			
			return  0;
		}		
	}

}
