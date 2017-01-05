/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package agent.logging;

import func.PlanCostFunction;
import agent.Agent;
import java.io.FileNotFoundException;
import java.io.PrintStream;
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
 *
 * @author Peter
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
        double cost = costFunction.calcCost(agent.getSelectedPlan());
        log.log(epoch, new Token(run, agent.getIteration()), cost);
    }

    @Override
    public void print(MeasurementLog log) {
        if (filename == null) {
            internalPrint(log, System.out);
        } else {
            try (PrintStream out = new PrintStream("output-data/" + filename)) {
                internalPrint(log, out);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(LocalCostLogger.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void internalPrint(MeasurementLog log, PrintStream out) {
        int localCostEpoch = log.getMaxEpochNumber() + 1;

        List<Double> avg = new ArrayList<>();
        List<Double> std = new ArrayList<>();

        Aggregate test = log.getAggregate(LocalCostLogger.class.getName(), 0);
        if (test == null || test.getNumValues() < 1) {
            for (Object tokenObj : log.getTagsOfType(Token.class)) {
                Token token = (Token) tokenObj;

                Aggregate aggregate = log.getAggregate(token);
                log.log(localCostEpoch, LocalCostLogger.class.getName(), token.iter, aggregate.getAverage());
            }
        }

        for (int i = 0; true; i++) {
            Aggregate aggregate = log.getAggregateByEpochNumber(localCostEpoch, LocalCostLogger.class.getName(), i);
            if (aggregate == null || aggregate.getNumValues() < 1) {
                break;
            }
            avg.add(aggregate.getAverage());
            std.add(aggregate.getStdDev());
        }

        out.println("local cost:");
        out.println("avg = " + avg);
        out.println("std = " + std);
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
