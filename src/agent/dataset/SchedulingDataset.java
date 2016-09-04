/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
public class SchedulingDataset implements Dataset<Vector> {

    private Random random = new Random();
    private DateTime phase = new DateTime();

    private int numDimensions;

    private int avgDimension;
    private int stdDimension;
    private int avgDeviation;
    private int stdDeviation;

    private Comparator<Plan<Vector>> order;

    public SchedulingDataset(int numDimensions, int avgDimension, int stdDimension, int avgDeviation, int stdDeviation, Comparator<Plan<Vector>> order) {
        this.numDimensions = numDimensions;
        this.avgDimension = avgDimension;
        this.stdDimension = stdDimension;
        this.avgDeviation = avgDeviation;
        this.stdDeviation = stdDeviation;
        this.order = order;
    }

    @Override
    public void init(int num) {
        random.setSeed(num);
    }

    @Override
    public List<SchedulingAgentDataset> getAgentDatasets(int maxAgents) {
        List<SchedulingAgentDataset> agentDatasets = new ArrayList<>();
        for (int i = 0; i < maxAgents; i++) {
            agentDatasets.add(new SchedulingAgentDataset(order));
        }
        return agentDatasets;
    }

    @Override
    public int getNumDimensions() {
        return numDimensions;
    }

    private class SchedulingAgentDataset extends OrderedAgentDataset<Vector> {

        public SchedulingAgentDataset(Comparator<Plan<Vector>> order) {
            super(order);
        }

        @Override
        List<Plan<Vector>> getUnorderedPlans(DateTime phase) {
            List<Plan<Vector>> plans = new ArrayList<>();

            int dimension = (int) Math.round(random.nextGaussian() * stdDimension + avgDimension);
            int deviation = Math.max(0, (int) Math.round(random.nextGaussian() * stdDeviation + avgDeviation));
            int minDimension = Math.max(0, dimension - deviation);
            int maxDimension = Math.min(numDimensions - 1, dimension + deviation);

            for (int i = minDimension; i <= maxDimension; i++) {
                Vector vector = new Vector(numDimensions);
                vector.setValue(i, 1);

                double discomfort = (i - dimension) / (minDimension - dimension + 0.00001);
                discomfort = discomfort * discomfort;

                Plan<Vector> plan = new Plan<>(vector);
                plan.setDiscomfort(discomfort);

                plans.add(plan);
            }

            return plans;
        }

        @Override
        public List<DateTime> getPhases() {
            return Arrays.asList(phase);
        }

        @Override
        public int getNumDimensions() {
            return SchedulingDataset.this.getNumDimensions();
        }
    }
}
