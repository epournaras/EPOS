/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package agent.logging;

import func.CostFunction;
import agent.Agent;
import agent.TreeAgent;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import protopeer.measurement.Aggregate;
import protopeer.measurement.MeasurementLog;
import data.DataType;

/**
 *
 * @author Peter
 */
public class GlobalCostLogger<V extends DataType<V>> extends AgentLogger<Agent<V>> {

    private String filename;
    private CostFunction<V> costFunction;

    public GlobalCostLogger() {
        this((CostFunction<V>) null);
    }

    public GlobalCostLogger(String filename) {
        this(filename, null);
    }

    public GlobalCostLogger(CostFunction<V> costFunction) {
        this.costFunction = costFunction;
    }

    public GlobalCostLogger(String filename, CostFunction<V> costFunction) {
        this.filename = filename;
        this.costFunction = costFunction;
    }

    @Override
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
        if (filename == null) {
            internalPrint(log, System.out);
        } else {
            try (PrintStream out = new PrintStream("output-data/" + filename)) {
                internalPrint(log, out);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(GlobalCostLogger.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void internalPrint(MeasurementLog log, PrintStream out) {
        List<Double> avg = new ArrayList<>();
        List<Double> std = new ArrayList<>();

        for (int i = 0; true; i++) {
            Aggregate aggregate = log.getAggregate(GlobalCostLogger.class.getName(), i);
            if (aggregate == null || aggregate.getNumValues() < 1) {
                break;
            }
            avg.add(aggregate.getAverage());
            std.add(aggregate.getStdDev());
        }

        out.println("global cost:");
        out.println("avg = " + avg);
        out.println("std = " + std);
    }
}
