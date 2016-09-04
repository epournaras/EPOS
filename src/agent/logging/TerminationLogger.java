/*
 * Copyright (C) 2016 Evangelos Pournaras
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package agent.logging;

import data.Value;
import func.CostFunction;
import agent.Agent;
import protopeer.measurement.Aggregate;
import protopeer.measurement.MeasurementLog;

/**
 * Determines when the algorithm terminates. The termination is defined at the
 * last iteration that changes the global cost of the result.
 *
 * @author Peter
 */
public class TerminationLogger<V extends Value<V>> extends AgentLogger<Agent<V>> {

    private CostFunction<V> globalCostFunc;
    private int index;
    private V prevGlobalResponse;

    /**
     * Creates a TerminationLogger that detects termination based on the global
     * cost function.
     */
    public TerminationLogger() {
    }

    /**
     * Creates a TerminationLogger that detects termination based on the
     * provided cost function.
     *
     * @param globalCostFunc
     */
    public TerminationLogger(CostFunction<V> globalCostFunc) {
        this.globalCostFunc = globalCostFunc;
    }

    @Override
    public void init(Agent<V> agent) {
        if (globalCostFunc == null) {
            globalCostFunc = agent.getGlobalCostFunction();
        }
    }

    @Override
    public void log(MeasurementLog log, int epoch, Agent<V> agent) {
        if (agent.isRepresentative()) {
            if (agent.getIteration() == 0) {
                index = agent.getIteration() + 1;
                prevGlobalResponse = agent.getGlobalResponse();
            } else {
                double globalCost = globalCostFunc.calcCost(agent.getGlobalResponse());
                double prevGlobalCost = globalCostFunc.calcCost(prevGlobalResponse);
                if (globalCost < prevGlobalCost) {
                    index = agent.getIteration() + 1;
                    prevGlobalResponse = agent.getGlobalResponse();
                }
            }

            if (agent.getIteration() == agent.getNumIterations() - 1) {
                log.log(epoch, TerminationLogger.class.getName(), index);
            }
        }
    }

    @Override
    public void print(MeasurementLog log) {
        for (Object t : log.getTagsOfType(String.class)) {
            if (t.equals(TerminationLogger.class.getName())) {
                Aggregate a = log.getAggregate(t);
                System.out.print("Termination after " + a.getAverage() + "+-" + a.getStdDev() + " iterations, " + a.getMin() + "/" + a.getMax());
            }
        }
        System.out.println();
    }
}
