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
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.joda.time.DateTime;

/**
 *
 * @author Peter
 */
public class FileAgentDataset extends OrderedAgentDataset<Vector> {

    private String format;
    private String planDir;
    private int numDimensions;
    private List<Plan<Vector>> cache;

    public FileAgentDataset(String planLocation, String config, String id, String format, int numDimensions) {
        this(planLocation, config, id, format, numDimensions, null);
    }

    public FileAgentDataset(String planLocation, String config, String id, String format, int numDimensions, Comparator<Plan<Vector>> order) {
        super(order);
        this.format = format;
        this.planDir = planLocation + File.separator + config + File.separator + id;
        this.numDimensions = numDimensions;
    }

    @Override
    List<Plan<Vector>> getUnorderedPlans(DateTime phase) {
        if (cache != null) {
            return cache;
        }

        List<Plan<Vector>> plans = new ArrayList<>();

        File planFile = new File(planDir + File.separator + phase.toString("yyyy-MM-dd") + format);
        try (Scanner scanner = new Scanner(planFile)) {
            scanner.useLocale(Locale.US);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                plans.add(parsePlan(line, phase));
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(FileAgentDataset.class.getName()).log(Level.SEVERE, null, ex);
        }

        cache = plans;
        return plans;
    }

    @Override
    public List<DateTime> getPhases() {
        List<DateTime> phases = new ArrayList<>();

        File[] dates = new File(planDir).listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if (pathname.isHidden() || pathname.getName().charAt(0) == '.') {
                    return false;
                }
                return pathname.isFile();
            }
        });
        for (File date : dates) {
            StringTokenizer dateTokenizer = new StringTokenizer(date.getName(), ".");
            phases.add(DateTime.parse(dateTokenizer.nextToken()));
        }

        return phases;
    }

    private Plan<Vector> parsePlan(String planStr, DateTime phase) {
        Vector vector = new Vector(numDimensions);

        Scanner scanner = new Scanner(planStr);
        scanner.useLocale(Locale.US);
        scanner.useDelimiter(":");
        double score = scanner.nextDouble();

        scanner.useDelimiter(",");
        scanner.skip(":");
        for (int i = 0; scanner.hasNextDouble(); i++) {
            vector.setValue(i, scanner.nextDouble());
        }

        Plan<Vector> plan = new Plan<>(vector);
        plan.setDiscomfort(1.0 - score);
        return plan;
    }

    @Override
    public int getNumDimensions() {
        return numDimensions;
    }
}
