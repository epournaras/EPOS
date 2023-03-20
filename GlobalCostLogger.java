/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package agent.logging;

import func.CostFunction;
import agent.Agent;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
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
 * An AgentLogger that logs the global cost after each iteration. Note that global cost
 * is always effectively computed at root node by the end of bottom-up phase.
 *
 * @author Peter P. & Jovan N.
 */
public class GlobalCostLogger<V extends DataType<V>> extends AgentLogger<Agent<V>> {

    private String 				filepath;
    private CostFunction<V> 	costFunction;

    /**
     * Outputs the global cost to std-out.
     */
    public GlobalCostLogger() {
        this((CostFunction<V>) null);
    }

    /**
     * Outputs the global cost to the specified file.
     *
     * @param filename the output file
     */
    public GlobalCostLogger(String filename) {
        this(filename, null);
    }

    /**
     * Outputs the cost to std-out.
     *
     * @param costFunction the cost function to be used instead of the global
     * cost
     */
    public GlobalCostLogger(CostFunction<V> costFunction) {
        this.costFunction = costFunction;
    }

    /**
     * Outputs the cost to the specified file.
     *
     * @param filename 		the path to output file
     * @param costFunction 	the cost function to be used instead of the global
     * cost
     */
    public GlobalCostLogger(String filepath, CostFunction<V> costFunction) {
        this.filepath = filepath;
        this.costFunction = costFunction;
    }

    @Override
    /**
     * If cost function hasn't already been set, initializes cost function
     * with global cost function of the agent. Otherwise, does nothing.
     */
    public void init(Agent agent) {
        if (costFunction == null) {
            costFunction = agent.getGlobalCostFunction();
        }
    }

    @Override
    /**
     * Logs global response from node with index 0, per epoch per agent.
     */
    public void log(MeasurementLog log, int epoch, Agent<V> agent) {
        if (agent.isRepresentative()) {        	
            double cost = costFunction.calcCost(agent.getGlobalResponse());  
            Token token = new Token(cost, agent.getIteration(), this.run);            
            log.log(epoch, GlobalCostLogger.class.getName(), token, 1.0);            
            log.log(epoch, GlobalCostLogger.class.getName() + "raw", agent.getIteration(), cost);
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
            Aggregate aggregate = log.getAggregate(GlobalCostLogger.class.getName() + "raw", i);
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
				sb.append("," + perRun.get(sortedKeys.get(j)).get(i).globalCost);
			}
			sb.append(System.lineSeparator());
		}
		
		return sb.toString();
    }
   
    private class Token implements Comparable<Token> {
		
		public double globalCost;
		public int iteration;
		public int run;
		
		public Token(double globalCost, int iteration, int run) {
			this.globalCost = globalCost;
			this.iteration = iteration;
			this.run = run;
		}

		@Override
		public int compareTo(GlobalCostLogger<V>.Token other) {
			
			if		(this.run > other.run)					return 1;
			else if (this.run < other.run)					return -1;
			
			if		(this.iteration > other.iteration)		return 1;
			else if (this.iteration < other.iteration)		return -1;
			
			return  0;
		}		
	}
    
}
