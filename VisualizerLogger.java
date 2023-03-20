package agent.logging;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

import javax.imageio.ImageIO;

import com.google.common.base.Function;

//import org.apache.commons.collections15.Transformer;

import agent.TreeAgent;
import agent.logging.image.ImageFile;
import agent.logging.image.PngFile;
import agent.logging.image.SvgFile;
import config.Configuration;
import data.DataType;
import data.Plan;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.RadialTreeLayout;
import edu.uci.ics.jung.graph.DelegateForest;
import edu.uci.ics.jung.graph.Forest;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Context;
import edu.uci.ics.jung.visualization.DefaultVisualizationModel;
import edu.uci.ics.jung.visualization.VisualizationModel;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.decorators.DirectionalEdgeArrowTransformer;
import edu.uci.ics.jung.visualization.decorators.EdgeShape;
import edu.uci.ics.jung.visualization.util.VertexShapeFactory;
import protopeer.Finger;
import protopeer.measurement.MeasurementLog;
import protopeer.network.NetworkAddress;

/**
 * Visualizes tree structure. The color indicates the index of the selected plan.
 * Darker color indicates higher index.
 * 
 * @author jovan
 *
 * @param <V>
 */
public class VisualizerLogger<V extends DataType<V>> extends AgentLogger<TreeAgent<V>> {
	
	public static int maxPlans = Integer.MIN_VALUE;
	
	public static Color[] colors = {new Color(243, 236, 245),
									new Color(244, 236, 247),			// 1 [1]
									new Color(232, 218, 239),
									new Color(219, 198, 228),
									new Color(210, 180, 222),
									new Color(194, 156, 211),			// 2 [5]
									new Color(187, 143, 206),
									new Color(172, 119, 195),
									new Color(165, 105, 189),
									new Color(142, 68, 173),
									new Color(125, 60, 152),			// 3 [10]
									new Color(108, 52, 131),
									new Color(91, 44, 111),
									new Color(74, 35, 90),
									new Color(64, 8, 87),				// 4 [14]
									new Color(52, 4, 71)
									};
	
	private final double 					vertexSize127 		= 	2.8;
    private final double 					vertexSize255 		= 	1;
    private final double 					vertexSize511 		= 	1;
    private final double 					vertexSize1023 		= 	1;
    private double 							vertexSize 			= 	vertexSize127;
    
    private final Dimension 				size 				= 	new Dimension(1024, 1024);
    private AffineTransform 				shapeTransform 		= 	null;
    
    private VertexShapeFactory<Vertex> 		shapeFactory		=	new VertexShapeFactory<Vertex>();
    private Forest<Vertex, Integer> 		graph;
    private Map<NetworkAddress, Vertex> 	idx2vertex;
    
    private TreeSet<Double> 				scoreSet			=	new TreeSet<Double>();

	@Override
	public void init(TreeAgent<V> agent) { }

	@Override
	/**
	 * For now, logs the following info:
	 * 	-	id as Finger
	 * 	-	list of children
	 * 	-	id of selected plan
	 *  -   the local cost of the selected plan as measured by the local cost function
	 */
	public void log(MeasurementLog log, int epoch, TreeAgent<V> agent) {
		if(agent.getIteration() == agent.getNumIterations()-1) {
			double score = agent.getLocalCostFunction().calcCost(agent.getSelectedPlan());
			
			for(Plan<V> p : agent.getPossiblePlans()) {
				this.scoreSet.add(agent.getLocalCostFunction().calcCost(p));
			}
			
			VisualizerLogger.maxPlans = Math.max(VisualizerLogger.maxPlans, agent.getPossiblePlans().size());
			
			TreeNode node = new TreeNode(agent.getPeer().getFinger(), 
										 agent.getChildren(), 
										 agent.getSelectedPlanID(),
										 score,
										 this.run);			
			log.log(epoch, node, 0.0);
		}
	}
	
	private int getNumAgents(MeasurementLog log) {
		return (int) (log.getTagsOfType(TreeNode.class).size() / Configuration.numSimulations);
	}
	
	private void setVertexSize(int numAgents) {
		if (numAgents <= 127) {
            vertexSize = vertexSize127;
        } else if (numAgents <= 255) {
            vertexSize = vertexSize255;
        } else if (numAgents <= 511) {
            vertexSize = vertexSize511;
        } else if (numAgents <= 1023) {
            vertexSize = vertexSize1023;
        } else {
            vertexSize = vertexSize1023;
        }
		this.shapeTransform = AffineTransform.getScaleInstance(vertexSize, vertexSize);
	}
	
	private void populateGraph(MeasurementLog log, int current_run) {
		graph = new DelegateForest<>();
		
		this.idx2vertex = new HashMap<>();
		
        for (Object agentObj : log.getTagsOfType(TreeNode.class)) {
            TreeNode agent = (TreeNode) agentObj;
            
            if(agent.run != current_run) continue;
            
            Vertex vertex = new Vertex(agent);
            this.idx2vertex.put(agent.id.getNetworkAddress(), vertex);
            graph.addVertex(vertex);
        }       
        
        int edge = 0;
        for (Vertex node : graph.getVertices()) {
            for (Finger f : node.agent.children) {
                Vertex child = this.idx2vertex.get(f.getNetworkAddress());
                graph.addEdge(edge++, node, child);
            }
        }
	}

	@Override
	public void print(MeasurementLog log) {
		int numAgents = this.getNumAgents(log);
		this.setVertexSize(numAgents);
		
		for(int currentRun = 0; currentRun < Configuration.numSimulations; currentRun++) {
			this.populateGraph(log, currentRun);
			
			File file = new File(Configuration.outputDirectory + "/selected-plans-graph-run-" + currentRun + ".png");
			BufferedImage image = this.getPlotImage();
			
			try {
			    ImageIO.write(image, "png", file);
			} catch (IOException e) {
			    e.printStackTrace();
			}
		}		
	}
	
	private void writeCurrentImage(VisualizationViewer<Vertex, Integer> viewer, File outputFile) {
        ImageFile img = null;
        if (outputFile.getName().endsWith(".png")) {
            img = new PngFile(outputFile, viewer.getWidth(), viewer.getHeight());
        } else if (outputFile.getName().endsWith(".svg")) {
            img = new SvgFile(outputFile, viewer.getWidth(), viewer.getHeight());
        }

        viewer.setDoubleBuffered(false);
        viewer.getRootPane().paintComponents(img.createGraphics());
        viewer.setDoubleBuffered(true);

        img.write();
    }
	
	private BufferedImage getPlotImage() {
		VisualizationModel<Vertex, Integer> model = new DefaultVisualizationModel(this.getLayout(this.graph));
        VisualizationViewer<Vertex, Integer> viewer = this.visualize(model);
        viewer.setVisible(true);
        AffineTransform at = new AffineTransform();
        at.scale(1, 1);
        AffineTransformOp atOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
		
        BufferedImage tmp = new BufferedImage(viewer.getWidth(), viewer.getHeight(), BufferedImage.TYPE_INT_ARGB);
        BufferedImage outputImg = new BufferedImage(viewer.getWidth(), viewer.getHeight(), BufferedImage.TYPE_INT_ARGB);       
        
        viewer.paint(tmp.getGraphics());        
        outputImg = atOp.filter(tmp, outputImg);        
        return outputImg;
    }
	
	/**
	 * Builds viewer for given graph layout.
	 * @param model
	 * @return
	 */
	private VisualizationViewer<Vertex, Integer> visualize(VisualizationModel<Vertex, Integer> model) {		
        VisualizationViewer<Vertex, Integer> viewer = new VisualizationViewer<Vertex, Integer>(model);
        viewer.setPreferredSize(new Dimension(this.size.width, this.size.height));
        viewer.setSize(this.size.width, this.size.height);
        viewer.setBackground(Color.white);
        viewer.getRenderContext().setEdgeShapeTransformer(this.getEdgeShapeTransformer());
        viewer.getRenderContext().setEdgeArrowTransformer(this.getEdgeArrowTransformer());
        viewer.getRenderContext().setVertexFillPaintTransformer(this.getVertexFillPaintTransformer());
        viewer.getRenderContext().setVertexShapeTransformer(this.getVertexShapeTransformer());
        viewer.setGraphMouse(this.getGraphMouse());
        return viewer;
    }
	
	/**
	 * Returns the type of line that will be drawn between vertices of the graph.
	 * 	-	Line renders as strainght line
	 * 	-	CubicCurve has shape of ~
	 * @return
	 */	
	private Function<Integer, Shape> getEdgeShapeTransformer() {
		return EdgeShape.line(graph);
    }
	
	
	/**
	 * Returns the type of arrow to be used.
	 * Because all 0 are passed, no arrow is printed.
	 * @return
	 */
	private <V, E> Function<Context<Graph<V, E>, E>, Shape> getEdgeArrowTransformer() {
        return new DirectionalEdgeArrowTransformer<>(0, 0, 0);
    }
	
	/**
	 * Gets the color of each vertex.
	 * @return
	 */
	private Function<Vertex, Paint> getVertexFillPaintTransformer() {
        return (Vertex vertex) -> {
            return vertex.getMyColor();
        };
    }
	
	/**
	 * Gets the shape of each vertex:
	 * 	-	Ellipse with getEllipse()
	 * 	-	Rectangle with getRectangle()
	 * 	-	RoundRectangle with getRoundRectangle()
	 * 	-	RegularStar with getRegularStar()
	 * 
	 * Note that shape is scaled!
	 * @return
	 */
	private Function<Vertex, Shape> getVertexShapeTransformer() {
        return (Vertex vertex) -> {
            Shape shape = this.shapeFactory.getEllipse(vertex);
            shape = shapeTransform.createTransformedShape(shape);
            return shape;
        };
    }
	
	/**
	 * I actually have no idea what this thing does
	 * @return
	 */
	private VisualizationViewer.GraphMouse getGraphMouse() {
        DefaultModalGraphMouse graphMouse = new DefaultModalGraphMouse();
        graphMouse.setMode(ModalGraphMouse.Mode.TRANSFORMING);
        return graphMouse;
    }
	
	/**
	 * Creates radial layout of vertices
	 * @param graph
	 * @return
	 */
	private <V, E> Layout<V, E> getLayout(Forest<V, E> graph) {
        Layout<V, E> layout = new RadialTreeLayout<>(graph);
        layout.setSize(size);
        return layout;
    }
	
	
	public Color getColor(VisualizerLogger.TreeNode node) {		
		int val = node.selectedPlan;
		int mapping = (val * VisualizerLogger.colors.length) / VisualizerLogger.maxPlans;		
		return VisualizerLogger.colors[mapping];		
	}
	
	private class Vertex {
		public final TreeNode agent;
        private final Color color;
        
        public Vertex(TreeNode agent) {
        	this.agent = agent;
        	this.color = getColor(this.agent);
        }
        
        public Color getMyColor() {
        	return this.color;
        }

        @Override
        public int hashCode() {
            int hash = 3;
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
            final Vertex other = (Vertex) obj;
            if (!Objects.equals(this.agent, other.agent)) {
                return false;
            }
            return true;
        }
	}
	
	
	/**
	 * This class holds all necessary information to be logged in Measurement Logger
	 * 
	 * @author jovan
	 *
	 */
	private class TreeNode implements Serializable {

		public final int run;
        public final Finger id;
        public final List<Finger> children;
        public final int selectedPlan;
        public final double localCost;

        public TreeNode(Finger id, List<Finger> children, int selectedPlan, double localCost, int run) {
            this.id = id;
            this.children = children;
            this.selectedPlan = selectedPlan;
            this.localCost = localCost;
            this.run = run;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 97 * hash + Objects.hashCode(this.id);
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
            final TreeNode other = (TreeNode) obj;
            if (!Objects.equals(this.id, other.id)) {
                return false;
            }
            if (!Objects.equals(this.children, other.children)) {
                return false;
            }
            if (this.run != other.run) {
            	return false;
            }
            return true;
        }
    }

}
