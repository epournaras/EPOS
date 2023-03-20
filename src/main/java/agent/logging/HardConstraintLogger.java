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
                int hardConstraintCost = agent.getHardConstraintCost();
                Token token = new Token(hardConstraintCost, agent.getIteration(), this.run);
                log.log(epoch, HardConstraintLogger.class.getName(), token, 1.0);
                log.log(epoch, HardConstraintLogger.class.getName() + "raw", agent.getIteration(), hardConstraintCost);
            } else {
                int[] hardConstraint = agent.getHardConstraintPlan();
                Entry<V> e = new Entry<V>(hardConstraint.clone(), agent.getIteration(), this.run);
                log.log(epoch, HardConstraintLogger.class.getName(), e, 0.0);
            }
        }
    }

    @Override
    public void print(MeasurementLog log) {
        String outcome = (isCost) ? this.extractHardConstraintCost(log) : this.extractHardConstraint(log);

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

    private String extractHardConstraintCost(MeasurementLog log) {
        TreeSet<Object> allTokens = new TreeSet<Object>();
        allTokens.addAll(log.getTagsOfType(Token.class));
        Iterator<Object> iter = allTokens.iterator();

        HashMap<Integer, ArrayList<Token>> perRun = new HashMap<Integer, ArrayList<Token>>();

        while(iter.hasNext()) {
            Token token = (Token) iter.next();
            if(!perRun.containsKey(token.run)) {
                perRun.put(token.run, new ArrayList<Token>());
            }
            ArrayList<Token> thelist = perRun.get(token.run);
            thelist.add(token);
        }

        ArrayList<Integer> sortedKeys = new ArrayList<>(perRun.keySet());
        Collections.sort(sortedKeys);

        StringBuilder sb = new StringBuilder();
        sb.append("Iteration");
        for (Integer sortedKey : sortedKeys) {
            sb.append("," + "Run-").append(sortedKey);
        }
        sb.append(System.lineSeparator());

        for(int i = 0; i < perRun.get(sortedKeys.get(0)).size(); i++) {
            for (Integer sortedKey : sortedKeys) {
                sb.append(i).append(",").append(perRun.get(sortedKey).get(i).isviolation);
            }
            sb.append(System.lineSeparator());
        }

        return sb.toString();
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
        for(int i = 0; i < Configuration.planDim; i++) {
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

    private class Token implements Comparable<Token> {

        public int isviolation;
        public int iteration;
        public int run;

        public Token(int isviolation, int iteration, int run) {
            this.isviolation = isviolation;
            this.iteration = iteration;
            this.run = run;
        }

        @Override
        public int compareTo(Token other) {

            if		(this.run > other.run)					return 1;
            else if (this.run < other.run)					return -1;

            if		(this.iteration > other.iteration)		return 1;
            else if (this.iteration < other.iteration)		return -1;

            return  0;
        }
    }
}
