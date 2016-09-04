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
public class SparseDataset implements Dataset<Vector> {
    private final int numPlans;
    private final int numDimensions;
    private final double std;
    private final int generationSteps;
    private int seed;
    private final Comparator<Plan<Vector>> order;

    public SparseDataset(int numPlans, int planSize, double std, int generationSteps, Comparator<Plan<Vector>> order) {
        this.numPlans = numPlans;
        this.numDimensions = planSize;
        this.std = std;
        this.generationSteps = generationSteps;
        this.order = order;
    }

    @Override
    public List<SparseAgentDataset> getAgentDatasets(int maxAgents) {
        Random rand = new Random(seed);
        List<SparseAgentDataset> res = new ArrayList<>();
        for(int i = 0; i < maxAgents; i++) {
            res.add(new SparseAgentDataset(numPlans, numDimensions, std, generationSteps, rand, order));
        }
        return res;
    }

    @Override
    public int getNumDimensions() {
        return numDimensions;
    }

    @Override
    public void init(int num) {
        this.seed = num;
    }
}
