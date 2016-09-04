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
package agent.dataset;

import data.Plan;
import data.Vector;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 *
 * @author Peter
 */
public class NoiseDataset implements Dataset<Vector> {

    private final int numPlansMin;
    private final int numPlansMax;
    private final int planSize;
    private final double mean;
    private final double std;
    private int seed;
    private final int nonZeroMin;
    private final int nonZeroMax;
    private final Comparator<Plan<Vector>> order;

    public NoiseDataset(int numPlansMin, int numPlansMax, int planSize, double mean, double std, int nonZeroMin, int nonZeroMax, Comparator<Plan<Vector>> order) {
        this.numPlansMin = numPlansMin;
        this.numPlansMax = numPlansMax;
        this.planSize = planSize;
        this.mean = mean;
        this.std = std;
        this.nonZeroMin = nonZeroMin;
        this.nonZeroMax = nonZeroMax;
        this.order = order;
    }

    @Override
    public List<NoiseAgentDataset> getAgentDatasets(int maxAgents) {
        Random rand = new Random(seed);
        List<NoiseAgentDataset> res = new ArrayList<>();
        for (int i = 0; i < maxAgents; i++) {
            res.add(new NoiseAgentDataset(numPlansMin, numPlansMax, planSize, mean, std, nonZeroMin, nonZeroMax, rand, order));
        }
        return res;
    }

    @Override
    public int getNumDimensions() {
        return planSize;
    }

    @Override
    public void init(int num) {
        this.seed = num;
    }
}
