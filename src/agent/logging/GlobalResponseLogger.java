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
package agent.logging;

import data.Plan;
import data.Vector;
import func.DifferentiableCostFunction;
import func.VarCostFunction;
import agent.Agent;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import protopeer.measurement.MeasurementLog;
import util.JFreeChartCustomLegend;

/**
 * Outputs the global response for each iteration in MATALB format
 *
 * @author Peter
 */
public class GlobalResponseLogger extends AgentLogger<Agent<Vector>> {

    private String outputDir;
    private Vector cumulatedResponse;
    private DifferentiableCostFunction<Vector> globalCostFunc = new VarCostFunction();

    private Map<Integer, Vector> measurements = new HashMap<>();

    private static File defaultDstDir = new File(".");
    private Font font = new Font("Computer Modern", Font.PLAIN, 12);

    public GlobalResponseLogger() {
    }

    public GlobalResponseLogger(String dir) {
        this.outputDir = "output-data/" + dir;
    }

    @Override
    public void init(Agent<Vector> agent) {
    }

    @Override
    public void log(MeasurementLog log, int epoch, Agent<Vector> agent) {
        if (agent.isRepresentative()) {
            if (cumulatedResponse == null) {
                cumulatedResponse = globalCostFunc.calcGradient(agent.getGlobalResponse());
            } else {
                cumulatedResponse = cumulatedResponse.cloneThis();
                cumulatedResponse.add(globalCostFunc.calcGradient(agent.getGlobalResponse()));
            }

            Entry entry = new Entry();
            entry.iteration = agent.getIteration();
            entry.globalResponse = agent.getGlobalResponse();
            entry.cumulatedResponse = cumulatedResponse;

            log.log(epoch, entry, 0.0);
        }
    }

    private String str2filename(String str) {
        return str.replace(' ', '_').replace('-', 'm').replace('.', '_');
    }

    @Override
    public void print(MeasurementLog log) {
        if (outputDir == null) {
            internalPrint(log, null);
        } else {
            // get title and label (of the plots) for the current experiment
            // label of the current execution is stored in the log as String tag: "label=..."
            String filename = this.outputDir;
            Set<Object> info = (Set<Object>) log.getTagsOfType(String.class);
            String title = "";
            String label = "";
            for (Object o : info) {
                String str = (String) o;
                if (str.startsWith("label=")) {
                    label = str2filename(str.substring(6, Math.min(str.length(), 36)));
                }
                if (str.startsWith("title=")) {
                    title = str2filename(str.substring(6, Math.min(str.length(), 33)));
                }
            }

            // compute output file name
            filename += "/movie_" + title + "_" + label + ".m";
            new File(filename).getParentFile().mkdir();

            try (PrintStream out = new PrintStream(filename)) {
                internalPrint(log, out);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(GlobalResponseLogger.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public BufferedImage getPlotImage(int width, int height, int iteration) {
        BufferedImage outputImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Vector v = measurements.get(iteration);
        
        double min = v.min();
        double max = v.max();
        for(Vector vv : measurements.values()) {
            min = Math.min(min, vv.min());
            max = Math.max(max, vv.max());
        }
        
        JFrame invisibleFrame = new JFrame();
        invisibleFrame.setContentPane(createPanel(measurements.get(iteration), min, max));
        invisibleFrame.setSize(outputImg.getWidth(), outputImg.getHeight());
        invisibleFrame.setVisible(true);
        invisibleFrame.paint(outputImg.getGraphics());
        invisibleFrame.setVisible(false);
        return outputImg;
        /*
        
        int[] x = new int[v.getNumDimensions()];
        int[] y = new int[v.getNumDimensions()];
        for (int i = 0; i < v.getNumDimensions(); i++) {
            x[i] = (int) Math.round(width * i / (double) v.getNumDimensions());
            y[i] = height - (int) Math.round(height * (v.getValue(i) - min) / (max - min));
        }

        Graphics g = outputImg.getGraphics();
        g.setColor(Color.black);
        g.drawPolyline(x, y, x.length);
        
        return outputImg;*/
    }

    private void internalPrint(MeasurementLog log, PrintStream out) {
        boolean first = true;

        Set<Object> sortedEntries = new TreeSet<>((x, y) -> Integer.compare(((Entry) x).iteration, ((Entry) y).iteration));
        sortedEntries.addAll(log.getTagsOfType(Entry.class));

        for (Object entryObj : sortedEntries) {
            Entry entry = (Entry) entryObj;

            if(out != null) {
                if (first) {
                    out.println("D=zeros(" + entry.globalResponse.getNumDimensions() + ",0);");
                    out.println("T=zeros(" + entry.cumulatedResponse.getNumDimensions() + ",0);");
                    first = false;
                }

                out.println("D(:," + (entry.iteration + 1) + ")=" + entry.globalResponse + "';");
                out.println("T(:," + (entry.iteration + 1) + ")=" + entry.cumulatedResponse + "';");
            }
            
            measurements.put(entry.iteration+1, entry.globalResponse);
        }
    }
    
    private ChartPanel createPanel(Vector vector, double min, double max) {
        XYSeries series = new XYSeries("global response");
        for(int i = 0; i < vector.getNumDimensions(); i++) {
            series.add(i+1, vector.getValue(i));
        }
        XYDataset dataset = new XYSeriesCollection(series);
        
        ValueAxis domainAxis = new NumberAxis("dimension");
        domainAxis.setRange(1, vector.getNumDimensions());
        ValueAxis rangeAxis = new NumberAxis("value");
        rangeAxis.setRange(min, max);
        
        XYItemRenderer renderer = new XYLineAndShapeRenderer(true, false);
        
        XYPlot plot = new XYPlot(dataset, domainAxis, rangeAxis, renderer);
        
        JFreeChartCustomLegend chart = new JFreeChartCustomLegend(null, font, plot, null);
        chart.setBackgroundPaint(Color.WHITE);
        chart.getXYPlot().getDomainAxis().setLabelFont(font);
        chart.getXYPlot().getDomainAxis().setTickLabelFont(font);
        chart.getXYPlot().getRangeAxis().setLabelFont(font);
        chart.getXYPlot().getRangeAxis().setTickLabelFont(font);

        ChartPanel panel = chart.getPanel();
        panel.setDefaultDirectoryForSaveAs(defaultDstDir);
        panel.setMinimumDrawHeight(1);
        panel.setMinimumDrawWidth(1);
        
        return panel;
    }

    private class Entry implements Serializable {

        public int iteration;
        public Vector globalResponse;
        public Vector cumulatedResponse;

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 97 * hash + this.iteration;
            return hash;
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
            final Entry other = (Entry) obj;
            if (this.iteration != other.iteration) {
                return false;
            }
            return true;
        }
    }
}
