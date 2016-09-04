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
import agent.Agent;
import java.io.Serializable;
import java.util.TreeMap;
import protopeer.measurement.MeasurementLog;

/**
 * Writes the histogram of selected plan indices in the last iteration to
 * std-out
 *
 * @author Peter
 */
public class DistributionLogger<V extends Value<V>> extends AgentLogger<Agent<V>> {

    @Override
    public void init(Agent<V> agent) {
    }

    @Override
    public void log(MeasurementLog log, int epoch, Agent<V> agent) {
        if (agent.getIteration() == agent.getNumIterations() - 1) {
            int idx = agent.getPossiblePlans().indexOf(agent.getSelectedPlan());
            log.log(epoch, new Token(idx), 1);
        }
    }

    @Override
    public void print(MeasurementLog log) {
        TreeMap<Integer, Integer> hist = new TreeMap<>();
        for (Object token : log.getTagsOfType(Token.class)) {
            hist.put(((Token) token).idx, log.getAggregate(token).getNumValues());
        }

        System.out.print(hist.getOrDefault(0, 0));
        for (int i = 1; i <= hist.lastKey(); i++) {
            System.out.print("," + hist.getOrDefault(i, 0));
        }
        System.out.println();
    }

    private class Token implements Serializable {

        public int idx;

        public Token(int idx) {
            this.idx = idx;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 61 * hash + this.idx;
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
            if (this.idx != other.idx) {
                return false;
            }
            return true;
        }
    }
}
