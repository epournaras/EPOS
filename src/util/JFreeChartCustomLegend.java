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
package util;

import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.List;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.block.Block;
import org.jfree.chart.block.RectangleConstraint;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.PlotEntity;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.LegendTitle;
import org.jfree.ui.HorizontalAlignment;
import org.jfree.ui.Size2D;
import org.jfree.ui.VerticalAlignment;

/**
 *
 * @author Peter
 */
public class JFreeChartCustomLegend extends JFreeChart {

    private XYPlot plot;
    private LegendTitle legend;
    private ChartPanel panel;

    public JFreeChartCustomLegend(String title, Font titleFont, XYPlot plot, LegendTitle legend) {
        super(title, titleFont, plot, false);
        this.plot = plot;
        this.legend = legend;
        this.panel = new ChartPanel(this);
    }
    
    public ChartPanel getPanel() {
        return panel;
    }

    @Override
    public void draw(Graphics2D g2, Rectangle2D chartArea, Point2D anchor, ChartRenderingInfo info) {
        panel.getChartRenderingInfo().setChartArea(panel.getScreenDataArea());
        super.draw(g2, chartArea, anchor, info);
        
        if(plot.getSeriesCount() <= 1) {
            return;
        }

        // arrange legend and compute width and height
        double w = 0;
        double h = 0;

        Size2D size = legend.arrange(g2, new RectangleConstraint(0, Double.POSITIVE_INFINITY));
        h = size.height;
        for (Block item : (List<Block>) legend.getItemContainer().getBlocks()) {
            Size2D itemSize = item.arrange(g2, RectangleConstraint.NONE);
            w = Math.max(w, itemSize.width);
        }

        //compute content rectangle
        double scaleX = 1;
        double scaleY = 1;
        double offsetX = 0;
        double offsetY = 0;

        Rectangle2D plotBounds;
        if (info != null) {
            plotBounds = info.getPlotInfo().getPlotArea();
            //System.out.println(chartArea + "," + panel.getScreenDataArea() + "," + plotBounds);
        } else {
            plotBounds = panel.getChartRenderingInfo().getPlotInfo().getPlotArea();
            //System.out.println(chartArea + "," + panel.getScreenDataArea());

            scaleX = panel.getScaleX();
            scaleY = panel.getScaleY();
            offsetX = 2;
            offsetY = 0;
        }

        // get unscaled content rectangle
        Rectangle2D contentRectRaw = null;
        for (ChartEntity entity : (Collection<ChartEntity>) panel.getChartRenderingInfo().getEntityCollection().getEntities()) {
            if (entity instanceof PlotEntity) {
                contentRectRaw = entity.getArea().getBounds2D();
            }
        }

        //panel.getScaleX() returns the effective scale (input size -> output size), however the border (8px) has constant width (is not scaled!)
        double plotXRaw = plotBounds.getX();
        double plotYRaw = plotBounds.getY();
        double plotWRaw = plotBounds.getWidth();
        double plotHRaw = plotBounds.getHeight();
        double plotW = (2 * plotXRaw + plotWRaw) * scaleX - 2 * plotXRaw;
        double plotH = (2 * plotYRaw + plotHRaw) * scaleY - 2 * plotYRaw;
        double newScaleX = plotW / plotWRaw;
        double newScaleY = plotH / plotHRaw;

        double border = ((BasicStroke) plot.getRangeGridlineStroke()).getLineWidth() / 2;

        System.out.println(plotBounds + "," + contentRectRaw);
        /*double contentX = plotBounds.getX()-border;
                double contentY = plotBounds.getY()-border;
         */

        double ox = 0;
        double oy = 0;
        if (plot.getRangeAxisCount() > 1 || plot.getRangeAxisLocation() == AxisLocation.TOP_OR_LEFT) {
            ox = offsetX;
            oy = offsetY;
        }
        boolean leftAxis = plot.getRangeAxisCount() > 1 || plot.getRangeAxisLocation() == AxisLocation.TOP_OR_LEFT || plot.getRangeAxisLocation() == AxisLocation.BOTTOM_OR_LEFT;
        boolean rightAxis = plot.getRangeAxisCount() > 1 || plot.getRangeAxisLocation() == AxisLocation.TOP_OR_RIGHT || plot.getRangeAxisLocation() == AxisLocation.BOTTOM_OR_RIGHT;
        double contentX = contentRectRaw.getX() - (leftAxis ? offsetX : 0) - border;
        double contentY = contentRectRaw.getY() - border;
        double contentW = contentRectRaw.getWidth() * newScaleX + (leftAxis ? offsetX : 0) + (rightAxis ? offsetX : 0) + 2 * border;
        double contentH = contentRectRaw.getHeight() * newScaleY + 2 * border;

        // compute legend position
        double x;
        double y;
        if(legend.getHorizontalAlignment() == HorizontalAlignment.LEFT) {
            x = contentX;
        } else {
            x = contentX + contentW - w;
        }
        if(legend.getVerticalAlignment() == VerticalAlignment.BOTTOM) {
            y = contentY + contentH - h;
        } else {
            y = contentY;
        }

        //System.out.println(x + "/" + y + ", " + w + "/" + h);
        legend.draw(g2, new Rectangle2D.Double(x, y, w, h));
        
        // check that content area is discovered correctly (save as SVG and view in Inkscape)
        // the legend should cover the content area completely (+border)
        //legend.draw(g2, new Rectangle2D.Double(contentX, contentY, contentW, contentH));
    }

    @Override
    public void draw(Graphics2D g2, Rectangle2D area, ChartRenderingInfo info) {
        super.draw(g2, area, info);
    }

    @Override
    public void draw(Graphics2D g2, Rectangle2D area) {
        super.draw(g2, area);
    }
}
