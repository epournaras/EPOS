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
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import org.joda.time.DateTime;

/**
 *
 * @author Peter
 */
public class NoiseAgentDataset extends OrderedAgentDataset<Vector> {

    private final int numPlansMin;
    private final int numPlansMax;
    private final int numDimensions;
    private final double mean;
    private final double std;
    private final long seed;
    private final int nonZeroMin;
    private final int nonZeroMax;

    public NoiseAgentDataset(int numPlansMin, int numPlansMax, int numDimensions, double mean, double std, int nonZeroMin, int nonZeroMax, Random r, Comparator<Plan<Vector>> order) {
        super(order);
        this.numPlansMin = numPlansMin;
        this.numPlansMax = numPlansMax;
        this.numDimensions = numDimensions;
        this.mean = mean;
        this.std = std;
        this.seed = r.nextLong();
        this.nonZeroMin = nonZeroMin;
        this.nonZeroMax = nonZeroMax;
    }

    @Override
    List<Plan<Vector>> getUnorderedPlans(DateTime phase) {
        Random r = new Random(seed);

        int numPlans = numPlansMin + r.nextInt(numPlansMax - numPlansMin + 1);

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

        int nonZero = nonZeroMin + r.nextInt(nonZeroMax - nonZeroMin + 1);
        int setZero = numDimensions - nonZero;
        Queue<Double> indices = new PriorityQueue<>();

        for (int i = 0; i < numDimensions; i++) {
            indices.add(r.nextInt(numDimensions * 1000) + 0.5 * i / (double) numDimensions);
            vector.setValue(i, (r.nextGaussian() * std + mean));
        }

        if (mean == 0) {
            double std = Math.sqrt(vector.variance());

            for (int i = 0; i < setZero; i++) {
                double idx = indices.poll();
                idx = idx - Math.floor(idx);
                int j = (int) Math.round(2 * idx * numDimensions);
                vector.setValue(j, 0);
            }

            vector.multiply(std / Math.sqrt(vector.variance()));
        }

        return new Plan<>(vector);
    }
}
