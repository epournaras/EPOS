package agent.logging;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import agent.Agent;
import config.Configuration;
import data.DataType;
import data.Vector;
import protopeer.measurement.MeasurementLog;

/**
 * Dumps whole satisfaction/violation for PLAN hard constraint per run per iteration.
 *
 * @author Jovan N.
 *
 * @param <V>
 */
public class HardConstraintLogger<V extends DataType<V>> extends AgentLogger<Agent<V>> {

    private String 					filepath;
    private boolean                 isCost;

    /**
     * Outputs the satisfaction/violation for PLAN hard constraint to the specified file.
     *
     * @param filename the output file
     */
    public HardConstraintLogger(String filename, boolean isCost) {
        this.filepath = filename;
        this.isCost = isCost;
    }

    @Override
    public void init(Agent<V> agent) { }

    @Override
    public void log(MeasurementLog log, int epoch, Agent<V> agent) {
        if (agent.isRepresentative()) {
            if (this.isCost) {
                int[] hardConstraint = isConstraintViolated(agent.getHardCostsArr(), false);
                if (agent.getIteration() == 0) Arrays.fill(hardConstraint, 1); // fill ones at initial iteration
                Entry<V> e = new Entry<V>(hardConstraint.clone(), agent.getIteration(), this.run);
                log.log(epoch, HardConstraintLogger.class.getName(), e, 0.0);

            } else {
                boolean isDoubleConst = (Objects.equals(Configuration.constraint, "HARD_PLANS") &&
                        Configuration.hardArray[2] != null && Configuration.hardArray[3] != null);
                DataType<Vector> response = (DataType<Vector>) agent.getGlobalResponse();
                int[] hardConstraint = isConstraintViolated(response.getValue().getArray(), isDoubleConst);
                if (agent.getIteration() == 0) Arrays.fill(hardConstraint, 1); // fill ones at initial iteration
                Entry<V> e = new Entry<V>(hardConstraint.clone(), agent.getIteration(), this.run);
                log.log(epoch, HardConstraintLogger.class.getName(), e, 0.0);
            }
        }
    }

    private int[] isConstraintViolated(double[] array, boolean isDoubleConst) {
        int[] v_array = new int[array.length];

        if (Objects.equals(Configuration.constraint, "SOFT")) {
            return v_array;
        }

        if (isDoubleConst) {
            for (int i = 0; i < array.length; i++) {
                int compare1 = (int) Configuration.hardArray[1][i];
                double hard1 = Configuration.hardArray[0][i];
                int compare2 = (int) Configuration.hardArray[3][i];
                double hard2 = Configuration.hardArray[2][i];
                if (compare1 == 0 && compare2 == 0) continue;
                if (compare1 == 1 && hard1 < array[i]) v_array[i] = 1;
                if (compare1 == 2 && hard1 > array[i]) v_array[i] = 1;
                if (compare2 == 1 && hard2 < array[i]) v_array[i] = 1;
                if (compare2 == 2 && hard2 > array[i]) v_array[i] = 1;
            }

            return v_array;
        }

        for (int i = 0; i < array.length; i++) {
            int compare = (int) Configuration.hardArray[1][i];
            double hard = Configuration.hardArray[0][i];
            if (compare == 0) continue;
            if (compare == 1 && hard < array[i]) v_array[i] = 1;
            if (compare == 2 && hard > array[i]) v_array[i] = 1;
            if (compare == 3 && hard != array[i]) v_array[i] = 1;
        }

        return v_array;
    }

    @Override
    public void print(MeasurementLog log) {
        String outcome = this.extractHardConstraint(log);

        if (this.filepath == null) {
            System.out.print(outcome);
        } else {
            try (PrintWriter out = new PrintWriter(new BufferedWriter(new java.io.FileWriter(this.filepath, false)))) {
                out.append(outcome);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(HardConstraintLogger.class.getName()).log(Level.SEVERE, null, ex);
            } catch(IOException e) {
                Logger.getLogger(HardConstraintLogger.class.getName()).log(Level.SEVERE, null, e);
            }
        }
    }

    private String extractHardConstraint(MeasurementLog log) {
        Set<Object> entries = log.getTagsOfType(Entry.class);

        Set<Object> sortedEntries = new TreeSet<Object>();
        sortedEntries.addAll(entries);

        StringBuilder sb = new StringBuilder();

        sb.append("Run")
                .append(",")
                .append("Iteration");

        //TODO confirm change with Jovan
        if (isCost) {

            sb.append("," + "Local cost")
                    .append("," + "Global cost")
                    .append("," + "Unfairness")
                    .append("," + "Global complex cost")
                    .append(System.lineSeparator());

        } else {

            for (int i = 0; i < Configuration.planDim; i++) {
                sb.append("," + "dim-" + i);
            }
            sb.append(System.lineSeparator());

            if(Configuration.goalSignalSupplier != null) {
                Vector globalSignal = Configuration.goalSignalSupplier.get(); //Moved after null check
                sb.append("-1")
                        .append(",")
                        .append("-1")
                        .append(",")
                        .append(globalSignal.toString()).append(System.lineSeparator());
            }
        }

        sortedEntries.forEach(obj -> {
            HardConstraintLogger.Entry entry = (HardConstraintLogger.Entry) obj;
            sb.append(entry.run)
                    .append(",")
                    .append(entry.iteration)
                    .append(",");
            for (int i = 0; i < entry.hardDecide.length; i++) {
                sb.append(entry.hardDecide[i]);
                if (i != entry.hardDecide.length - 1) sb.append(",");
            }
            sb.append(System.lineSeparator());
        });

        return sb.toString();
    }

    private class Entry<V> implements Comparable<Entry> {

        public int iteration;
        public int run;
        public int[] hardDecide;

        public Entry(int[] hardDecide, int iteration, int run) {
            this.hardDecide = hardDecide;
            this.iteration = iteration;
            this.run = run;
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
            final HardConstraintLogger.Entry other = (HardConstraintLogger.Entry) obj;
            if (this.iteration != other.iteration) {
                return false;
            }
            return true;
        }

        @Override
        public int compareTo(HardConstraintLogger.Entry other) {
            if		(this.run > other.run)					return 1;
            else if (this.run < other.run)					return -1;

            if		(this.iteration > other.iteration)		return 1;
            else if (this.iteration < other.iteration)		return -1;

            return  0;
        }

    }
}
