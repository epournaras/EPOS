/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package agent.logging;

import func.PlanCostFunction;
import agent.Agent;
import agent.logging.LocalCostLogger.Token;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
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
public class LocalCostLogger<V extends DataType<V>> extends AgentLogger<Agent<V>> {

    private String filename;
    private PlanCostFunction<V> costFunction;

    /**
     * Outputs the average local cost to std-out.
     */
    public LocalCostLogger() {
        this((PlanCostFunction<V>) null);
    }

    /**
     * Outputs the average local cost to the specified file.
     *
     * @param filename the output file
     */
    public LocalCostLogger(String filename) {
        this(filename, null);
    }

    /**
     * Outputs the average cost to std-out.
     *
     * @param costFunction the cost function to be used instead of the local
     * cost
     */
    public LocalCostLogger(PlanCostFunction<V> costFunction) {
        this.costFunction = costFunction;
    }

    /**
     * Outputs the average cost to the specified file.
     *
     * @param filename the output file
     * @param costFunction the cost function to be used instead of the local
     * cost
     */
    public LocalCostLogger(String filename, PlanCostFunction<V> costFunction) {
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
    	if(agent.getSelectedPlan() == null) {
    		Logger.getLogger(LocalCostLogger.class.toString()).log(Level.SEVERE, 
    				   "NODE: " + agent.getPeer().getIndexNumber() + 
			           " iteration: " + agent.getIteration() +
			           " SELECTED PLAN IS NULL");
    	}
    	
        double cost = costFunction.calcCost(agent.getSelectedPlan());
        log.log(epoch, new Token(this.run, agent.getIteration()), cost);
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
                Logger.getLogger(LocalCostLogger.class.getName()).log(Level.SEVERE, null, ex);
            } catch (Exception e) {
            	Logger.getLogger(LocalCostLogger.class.getName()).log(Level.SEVERE, null, e);
			}
        }
    }
    
    private String internalFetching(MeasurementLog log) {
        int localCostEpoch = log.getMaxEpochNumber() + 1;

        List<Double> avg = new ArrayList<>();
        List<Double> std = new ArrayList<>();

        Aggregate test = log.getAggregate(LocalCostLogger.class.getName(), 0);
        if (test == null || test.getNumValues() < 1) {
            for (Object tokenObj : log.getTagsOfType(Token.class)) {
                Token token = (Token) tokenObj;
                
                /*
                 * Relogs costs in such way that same iteration from different runs is logged under the same set of tags:
                 *  - max epoch + 1
                 *  - name of the logger class
                 *  - iteration number
                 */

                Aggregate aggregate = log.getAggregate(token);
                log.log(localCostEpoch, LocalCostLogger.class.getName(), token.iter, aggregate.getAverage());
            }
        }

        // iterates over iterations
        // info from everybody is logged with same keys, so we can calculate average and standard deviation
        for (int i = 0; true; i++) {
            Aggregate aggregate = log.getAggregateByEpochNumber(localCostEpoch, LocalCostLogger.class.getName(), i);
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
    	sb.append("LOCAL COST");
    	for(Double e : avgs) {
    		sb.append(System.lineSeparator() + e);
    	}
    	sb.append(System.lineSeparator());
    	return sb.toString();    	
    }

    protected static class Token implements Serializable {

        public int run;
        public int iter;

        public Token(int run, int iter) {
            this.run = run;
            this.iter = iter;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 53 * hash + this.run;
            hash = 53 * hash + this.iter;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Token other = (Token) obj;
            if (this.run != other.run) {
                return false;
            }
            if (this.iter != other.iter) {
                return false;
            }
            return true;
        }

    }
}
