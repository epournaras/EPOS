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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import protopeer.measurement.Aggregate;
import protopeer.measurement.MeasurementLog;
import data.DataType;

/**
 * An AgentLogger that logs the global cost after each iteration.
 * Used only for single runs!
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
    public void log(MeasurementLog log, int epoch, Agent<V> agent) {
        if (agent.isRepresentative()) {
            double cost = costFunction.calcCost(agent.getGlobalResponse());
            log.log(epoch, GlobalCostLogger.class.getName(), agent.getIteration(), cost);
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
    
    /**
     * Global cost value from each iteration is in separate line
     * @param avgs
     * @return
     */
    private String format(List<Double> avgs) {
    	StringBuilder sb = new StringBuilder();
    	sb.append("GLOBAL COST");
    	for(Double e : avgs) {
    		sb.append(System.lineSeparator() + e);
    	}
    	sb.append(System.lineSeparator());
    	return sb.toString();    	
    }
}
