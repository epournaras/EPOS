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
import agent.planselection.PlanSelectionOptimizationFunctionCollection;
import data.DataType;
import protopeer.measurement.Aggregate;
import protopeer.measurement.MeasurementLog;

/**
 * 
 * 
 * @author Jovan N.
 *
 * @param <V>
 */
public class UnfairnessLogger<V extends DataType<V>> extends AgentLogger<Agent<V>> {
	
	private String filename;
	
	public UnfairnessLogger(String filename) {
		this.filename = filename;
	}

	@Override
	public void init(Agent<V> agent) { }

	@Override
	public void log(MeasurementLog log, int epoch, Agent<V> agent) {
		
		if(!(agent instanceof MultiObjectiveIEPOSAgent)) {
			System.err.println("UnfairnessLogger can only be used with MultiObjectiveIEPOSAgent.");
			throw new ClassCastException();
		}
		
		MultiObjectiveIEPOSAgent moieposagent = (MultiObjectiveIEPOSAgent) agent;
		if(moieposagent.isRoot()) {
			double discomfortSum = moieposagent.getGlobalDiscomfortSum();
			double discomfortSumSqr = moieposagent.getGlobalDiscomfortSumSqr();
			int numAgents = moieposagent.getNumAgents();
			double unfairness = PlanSelectionOptimizationFunctionCollection.unfairness(discomfortSum, discomfortSumSqr, numAgents);
			
			Token token = new Token(unfairness, agent.getIteration(), this.run);            
            log.log(epoch, UnfairnessLogger.class.getName(), token, 1.0);  
			log.log(epoch, UnfairnessLogger.class.getName()+"raw", moieposagent.getIteration(), unfairness);
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
            Aggregate aggregate = log.getAggregate(UnfairnessLogger.class.getName() + "raw", i);
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
				sb.append("," + perRun.get(sortedKeys.get(j)).get(i).unfairness);
			}
			sb.append(System.lineSeparator());
		}
		
		return sb.toString();
    }
	
	private class Token implements Comparable<Token> {
		
		public double unfairness;
		public int iteration;
		public int run;
		
		public Token(double unfairness, int iteration, int run) {
			this.unfairness = unfairness;
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
		public int compareTo(UnfairnessLogger<V>.Token other) {
			
			if		(this.run > other.run)					return 1;
			else if (this.run < other.run)					return -1;
			
			if		(this.iteration > other.iteration)		return 1;
			else if (this.iteration < other.iteration)		return -1;
			
			return  0;
		}		
	}

}
