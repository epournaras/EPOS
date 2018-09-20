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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import config.Configuration;

/**
 * This class represents a dataset that is stored in a folder. The folder should
 * contain files named <code>agent_x.plans</code> where <code>x</code> is the id
 * of the user. These text files should contain one plan per line. Each plan is
 * encoded as follows: <code>"score:val0,val1,val2,..."</code>; where
 * <code>score</code> is a double value that makes the preference of the plan
 * comparable to the other plans. The following values are double values that
 * form the vector.
 *
 * @author Peter Pilgerstorfer
 */
public class FileVectorDataset implements Dataset<Vector> {

    private final String datasetDir;

    /**
     * Creates a dataset with the data in the given directory.
     *
     * @param datasetDir the directory of this dataset
     */
    public FileVectorDataset(String datasetDir) {
        this.datasetDir = datasetDir;
    }

    /**
     * Returns the plans for the specified agent.
     *
     * @param agentId the id of the specified agent; the first agent has id 0,
     * the second agent id 1 and so on
     * @return the plans for the specified agent
     */
    @Override
    public List<Plan<Vector>> getPlans(int agentId) {
        List<Plan<Vector>> plans = new ArrayList<>();

        // read plans from the data file
        File file = new File(datasetDir + File.separator + "agent_" + agentId + ".plans");
        try (Scanner scanner = new Scanner(file)) {
            scanner.useLocale(Locale.US);
            for (int i = 0; scanner.hasNextLine() && plans.size() < Configuration.numPlans; i++) {
                String line = scanner.nextLine();
                Plan<Vector> plan = parsePlan(line);                
                plan.setIndex(i);
                plans.add(plan);
            }
            if(plans.size() != Configuration.numPlans) {
            	System.out.println("Number of plans in file " + "agent_" + agentId + ".plans" + " is " + plans.size() + ", but expected number of plans is " + Configuration.numPlans );
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(FileVectorDataset.class.getName()).log(Level.SEVERE, null, ex);
        }

        return plans;
    }

    /**
     * Parse a plan of the form <code>"score:val0,val1,val2,..."</code> where
     * <code>score</code> is a double value that may be used to evaluate a plan.
     * The following values are double values that form the vector that
     * represents the plan.
     *
     * @param planStr the string representation of the plan
     * @return the plan represented by the given string
     */
    private Plan<Vector> parsePlan(String planStr) {
        List<Double> values = new ArrayList<>();

        Scanner scanner = new Scanner(planStr);
        scanner.useLocale(Locale.US);
        scanner.useDelimiter(":");
        double score = scanner.nextDouble();

        scanner.useDelimiter(",");
        scanner.skip(":");
        while (scanner.hasNextDouble()) {
            values.add(scanner.nextDouble());
        }

        Vector vector = new Vector(values.size());
        for (int i = 0; i < values.size(); i++) {
            vector.setValue(i, values.get(i));
        }

        Plan<Vector> plan = new Plan<>(vector);
        plan.setScore(score);
        return plan;
    }

    /**
     * Returns the number of agents in this dataset.
     *
     * @return the number of agents in this dataset.
     */
    public int getNumAgents() {
        return new File(datasetDir).listFiles((file, name) -> {
            return name.startsWith("agent_") && name.endsWith(".plans");
        }).length;
    }
}
