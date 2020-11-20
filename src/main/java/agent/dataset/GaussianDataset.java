/*
 * Copyright (C) 2016 Peter Pilgerstorfer
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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Can be used to create a Gaussian distributed dataset
 *
 * @author Peter Pilgerstorfer
 */
public class GaussianDataset implements Dataset<Vector> {

    private final int numPlans;
    private final int numDimensions;
    private final double mean;
    private final double std;
    private final long seed;

    /**
     * Creates a generator for a Gaussian distributed dataset.
     *
     * @param numPlans the number of plans for each agent
     * @param numDimensions the number of dimensions of each plan (vector)
     * @param mean the mean of the Gaussian distribution
     * @param std the standard deviation of the Gaussian distribution
     * @param random the RNG that should be used to generate the data or NULL,
     * if the default RNG should be used
     */
    public GaussianDataset(int numPlans, int numDimensions, double mean, double std, Random random) {
        this.numPlans = numPlans;
        this.numDimensions = numDimensions;
        this.mean = mean;
        this.std = std;
        this.seed = (random == null ? new Random() : random).nextLong();
    }

    @Override
    public List<Plan<Vector>> getPlans(int agentId) {
        // get the correct seed of the agent
        // the seed for each agent is generated randomly based on the global seed
        Random random = new Random(seed);
        long agentSeed = random.nextLong();
        for (int i = 1; i <= agentId; i++) {
            agentSeed = random.nextLong();
        }
        random = new Random(agentSeed);

        // generate the plans
        List<Plan<Vector>> plans = new ArrayList<>();
        for (int p = 0; p < numPlans; p++) {
            Plan plan = generatePlan(numDimensions, random);
            plan.setIndex(p);
            plans.add(plan);
        }
        return plans;
    }

    /**
     * Writes the dataset to the given directory. The format is compatible with
     * the {@link FileVectorDataset}.
     *
     * @param datasetDir the target directory for the generated dataset
     * @param numAgents the number of agents that the generated dataset should
     * have
     * @throws FileNotFoundException if the dataset could not be written to disk
     */
    public void writeDataset(String datasetDir, int numAgents) throws FileNotFoundException {
        for (int a = 0; a < numAgents; a++) {
            File file = new File(datasetDir + File.separator + "agent_" + a + ".plans");
            try (PrintStream out = new PrintStream(file)) {
                for (Plan plan : getPlans(a)) {
                    out.println(planToString(plan));
                }
            }
        }
    }

    /**
     * Generates a new plan.
     *
     * @param numDimensions the number of dimensions of the plan (vector)
     * @param random the RNG used to generate the plan
     * @return the generated plan
     */
    private Plan<Vector> generatePlan(int numDimensions, Random random) {
        Vector vector = new Vector(numDimensions);
        for (int i = 0; i < numDimensions; i++) {
            vector.setValue(i, (random.nextGaussian() * std + mean));
        }
        return new Plan<>(vector);
    }

    /**
     * Returns the string representation of the given plan in a format
     * compatible with the {@link FileVectorDataset}.
     *
     * @param plan the plan to be converten
     * @return the string representation of the plan
     */
    private String planToString(Plan<Vector> plan) {
        StringBuilder sb = new StringBuilder();
        sb.append(plan.getScore());
        sb.append(':');

        Vector vector = plan.getValue();
        sb.append(vector.getValue(0));
        for (int d = 1; d < vector.getNumDimensions(); d++) {
            sb.append(',');
            sb.append(vector.getValue(d));
        }

        return sb.toString();
    }
}
