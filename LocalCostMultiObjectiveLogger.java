/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package agent.logging;

import func.PlanCostFunction;
import agent.Agent;
import agent.MultiObjectiveIEPOSAgent;
import agent.planselection.PlanSelectionOptimizationFunctionCollection;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import protopeer.measurement.Aggregate;
import protopeer.measurement.MeasurementLog;
import data.DataType;

/**
 * Logs the local cost for each agent after each iteration.
 * Used only for single runs!
 *
 * @author Peter P. & Jovan N.
 */
public class LocalCostMultiObjectiveLogger<V extends DataType<V>> extends AgentLogger<Agent<V>> {

    private String filename;
    private PlanCostFunction<V> costFunction;

    /**
     * Outputs the average local cost to std-out.
     */
    public LocalCostMultiObjectiveLogger() {
        this((PlanCostFunction<V>) null);
    }

    /**
     * Outputs the average local cost to the specified file.
     *
     * @param filename the output file
     */
    public LocalCostMultiObjectiveLogger(String filename) {
        this(filename, null);
    }

    /**
     * Outputs the average cost to std-out.
     *
     * @param costFunction the cost function to be used instead of the local
     * cost
     */
    public LocalCostMultiObjectiveLogger(PlanCostFunction<V> costFunction) {
        this.costFunction = costFunction;
    }

    /**
     * Outputs the average cost to the specified file.
     *
     * @param filename the output file
     * @param costFunction the cost function to be used instead of the local
     * cost
     */
    public LocalCostMultiObjectiveLogger(String filename, PlanCostFunction<V> costFunction) {
        this.filename = filename;
        this.costFunction = costFunction;
    }

    @Override
    public void init(Agent<V> agent) {
        if (costFunction == null) {
            costFunction = agent.getLocalCostFunction();
        }
    }

    @Override
    public void log(MeasurementLog log, int epoch, Agent<V> agent) {
    	MultiObjectiveIEPOSAgent moieposagent = (MultiObjectiveIEPOSAgent) agent;
    	if (moieposagent.isRoot()) {        	
    		double discomfortSum = moieposagent.getGlobalDiscomfortSum();
    		int numAgents = moieposagent.getNumAgents();
            double cost = PlanSelectionOptimizationFunctionCollection.localCost(discomfortSum, numAgents);
            Token token = new Token(cost, agent.getIteration(), this.run);            
            log.log(epoch, LocalCostMultiObjectiveLogger.class.getName(), token, 1.0);            
            log.log(epoch, LocalCostMultiObjectiveLogger.class.getName() + "raw", agent.getIteration(), cost);
        }
    }

    @Override
    public void print(MeasurementLog log) {
    	String outcome = this.internalFetching(log);    	
        if (filename == null) {            
            System.out.print(outcome);
        } else {																					
            try (PrintWriter out = new PrintWriter(new BufferedWriter(new java.io.FileWriter(this.filename, true)))) {             	
            	out.append(outcome);										//								^
            } catch (FileNotFoundException ex) {							//								appends!
                Logger.getLogger(LocalCostMultiObjectiveLogger.class.getName()).log(Level.SEVERE, null, ex);
            } catch (Exception e) {
            	Logger.getLogger(LocalCostMultiObjectiveLogger.class.getName()).log(Level.SEVERE, null, e);
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
            Aggregate aggregate = log.getAggregate(LocalCostMultiObjectiveLogger.class.getName() + "raw", i);
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
				sb.append("," + perRun.get(sortedKeys.get(j)).get(i).localCost);
			}
			sb.append(System.lineSeparator());
		}
		
		return sb.toString();
    }
    
    
    //TODO Confirm that change from private to public makes sense
    public class Token implements Comparable<Token> {
		
		public double localCost;
		public int iteration;
		public int run;
		
		public Token(double localCost, int iteration, int run) {
			this.localCost = localCost;
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
		public int compareTo(LocalCostMultiObjectiveLogger<V>.Token other) {
			
			if		(this.run > other.run)					return 1;
			else if (this.run < other.run)					return -1;
			
			if		(this.iteration > other.iteration)		return 1;
			else if (this.iteration < other.iteration)		return -1;
			
			return  0;
		}		
	}

}
