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
import protopeer.measurement.MeasurementLog;

/**
 * Prints the progress of the computation to std-out
 * @author Peter
 */
public class ProgressIndicator extends AgentLogger<Agent> {

    @Override
    public void init(Agent agent) {
    }

    @Override
    public void log(MeasurementLog log, int epoch, Agent agent) {
        if(agent.isRepresentative()) {
            if (agent.getIteration() % 10 == 9) {
                System.out.print("%");
            }
            if (agent.getIteration() % 100 == 99) {
                System.out.print(" ");
            }
            if (agent.getIteration() + 1 == agent.getNumIterations()) {
                System.out.print(";");
                if(run % 10 == 9) {
                    System.out.println();
                }
            }
        }
    }

    @Override
    public void print(MeasurementLog log) {
        System.out.println();
    }
    
}
