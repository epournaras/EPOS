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

import agent.Agent;
import java.util.logging.Level;
import java.util.logging.Logger;
import protopeer.measurement.MeasurementLog;

/**
 * An abstract class specifying the API for logging in the distributed system.
 * Each agent gets a clone of an AgentLogger object. The specified methods are called in the following order:
 * init is called first by the agent
 * log is called multiple times afterwards (once in each iteration)
 * print is called in the end to output the results for this logger (once for all runs and iterations together)
 * @author Peter
 * @param <A> the agent type this logger is used for
 */
public abstract class AgentLogger<A extends Agent> implements Cloneable {
    int run;
    
    /**
     * Sets the current run. Each run corresponds to one repetition of the experiment.
     * @param run 
     */
    public void setRun(int run) {
        this.run = run;
    }

    /**
     * Initializes the logger for an experiment
     * @param agent the initialized agent
     */
    public abstract void init(A agent);

    /**
     * Processes information for the current iteration. Any persisted output is written to the provided log.
     * @param log the log that stores data for this agent
     * @param epoch the current epoch that should be used for entries in the log
     * @param agent agent the agent that this log is computed for
     */
    public abstract void log(MeasurementLog log, int epoch, A agent);

    /**
     * Prints the results for this logger. The measured data is available in the given log.
     * @param log the combined measurements of all agents in the network
     */
    public abstract void print(MeasurementLog log);

    @Override
    public AgentLogger<A> clone() {
        try {
            return (AgentLogger<A>) super.clone();
        } catch (CloneNotSupportedException ex) {
            Logger.getLogger(AgentLogger.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
}
