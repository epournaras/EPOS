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
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import org.joda.time.DateTime;

/**
 *
 * @author Peter
 */
public class SparseAgentDataset extends OrderedAgentDataset<Vector> {

    private final int numPlans;
    private final int numDimensions;
    private final double std;
    private final long seed;
    private final int generationSteps;

    public SparseAgentDataset(int numPlans, int numDimensions, double std, int generationSteps, Random r, Comparator<Plan<Vector>> order) {
        super(order);
        this.numPlans = numPlans;
        this.numDimensions = numDimensions;
        this.std = std;
        this.seed = r.nextLong();
        this.generationSteps = Math.max(1, generationSteps);
    }

    @Override
    List<Plan<Vector>> getUnorderedPlans(DateTime phase) {
        Random r = new Random(seed);

        List<Plan<Vector>> plans = new ArrayList<>();
        for (int i = 0; i < numPlans; i++) {
            plans.add(generatePlan(r));
        }

        return plans;
    }

    @Override
    public List<DateTime> getPhases() {
        return Arrays.asList(new DateTime(0));
    }

    @Override
    public int getNumDimensions() {
        return numDimensions;
    }

    private Plan<Vector> generatePlan(Random r) {
        Vector vector = new Vector(numDimensions);

        for (int i = 0; i < generationSteps; i++) {
            int idx1 = r.nextInt(numDimensions);
            int idx2 = idx1;
            while (idx1 == idx2) {
                idx2 = r.nextInt(numDimensions);
            }
            double val = std * Math.sqrt((numDimensions - 1) / 2);
            vector.setValue(idx1, vector.getValue(idx1) + val);
            vector.setValue(idx2, vector.getValue(idx2) + -val);
        }

        vector.multiply(std / Math.sqrt(vector.variance()));

        return new Plan<>(vector);
    }
}
