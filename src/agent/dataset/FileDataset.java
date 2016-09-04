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
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Peter
 */
public class FileDataset implements Dataset<Vector> {

    private final String location;
    private final String config;
    private final String format = ".plans";
    private final File[] agentDataDirs;
    private int seed;
    private int planSize = -1;
    private final Comparator<Plan<Vector>> order;

    private Map<Integer, FileAgentDataset> cache = new HashMap<>();

    public FileDataset(String location, String config) {
        this(location, config, null);
    }

    public FileDataset(String location, String config, Comparator<Plan<Vector>> order) {
        this.location = location;
        this.config = config;
        this.order = order;

        File dir = new File(location + "/" + config);
        this.agentDataDirs = dir.listFiles((File pathname) -> pathname.isDirectory());
        if (agentDataDirs == null) {
            System.out.println("ERROR: directory " + dir.getPath() + " is empty");
            throw new IllegalArgumentException("inFolder is expected to contain a folder for each agent");
        }
    }

    @Override
    public List<FileAgentDataset> getAgentDatasets(int maxAgents) {
        TreeMap<Double, Integer> indices = new TreeMap<>();
        Random rand = new Random(seed);
        for (int i = 0; i < agentDataDirs.length; i++) {
            indices.put(rand.nextDouble(), i);
        }
        Set<Integer> selected = new TreeSet<>();
        for (Integer i : indices.values()) {
            selected.add(i);
            if (selected.size() == maxAgents) {
                break;
            }
        }

        List<FileAgentDataset> agents = new ArrayList<>();
        for (int i : selected) {
            if (cache.containsKey(i)) {
                agents.add(cache.get(i));
            } else {
                FileAgentDataset fad = new FileAgentDataset(location, config, agentDataDirs[i].getName(), format, getNumDimensions(), order);
                agents.add(fad);
                cache.put(i, fad);
            }
        }
        return agents;
    }

    @Override
    public int getNumDimensions() {
        if (planSize < 0) {
            File file = agentDataDirs[0];
            File[] planFiles = file.listFiles((File dir, String name) -> name.endsWith(format));
            file = planFiles[0];
            try (Scanner scanner = new Scanner(file)) {
                scanner.useLocale(Locale.US);
                String line = scanner.nextLine();
                planSize = line.split(",").length;
            } catch (FileNotFoundException ex) {
                Logger.getLogger(FileDataset.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return planSize;
    }

    @Override
    public void init(int num) {
        seed = num;
    }
}
