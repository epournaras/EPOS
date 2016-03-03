/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package experiments;

import edu.uci.ics.jung.algorithms.layout.RadialTreeLayout;
import edu.uci.ics.jung.algorithms.layout.TreeLayout;
import edu.uci.ics.jung.graph.DelegateForest;
import edu.uci.ics.jung.graph.Forest;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Tree;
import edu.uci.ics.jung.graph.util.Context;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.decorators.DirectionalEdgeArrowTransformer;
import edu.uci.ics.jung.visualization.decorators.EdgeShape;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import org.apache.commons.collections15.Transformer;
import dsutil.protopeer.FingerDescriptor;
import protopeer.network.NetworkAddress;
import protopeer.util.NetworkAddressPair;

/**
 *
 * @author Evangelos
 */
public class EPOSVisualizer {
     private JFrame frame;
     private Container content;

     private Forest<FingerDescriptor,NetworkAddressPair> graph;
     private TreeLayout<FingerDescriptor,NetworkAddressPair> treeLayout;
     private RadialTreeLayout<FingerDescriptor,NetworkAddressPair> radialLayout;
     private VisualizationViewer<FingerDescriptor,NetworkAddressPair> viewer;

     

     public EPOSVisualizer(){
         this.init();
     }

     public void init(){
         frame = new JFrame("AETOS Vis - Adaptive Epidemic Tree Overlay Service Visualizer");
         frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
     }

     public void drawGraph(){
         if(this.viewer!=null){
             frame.getContentPane().remove(viewer);
         }
         this.treeLayout=new TreeLayout<FingerDescriptor,NetworkAddressPair>(this.graph);
         this.radialLayout=new RadialTreeLayout<FingerDescriptor,NetworkAddressPair>(graph);
         this.radialLayout.setSize(new Dimension(3648,2736));
         this.viewer=new VisualizationViewer<FingerDescriptor,NetworkAddressPair>(radialLayout);
         this.viewer.setPreferredSize(new Dimension(3648,2736));
         this.viewer.setBackground(Color.white);
         this.viewer.getRenderContext().setEdgeShapeTransformer(new EdgeShape.Line());
         Transformer<Context<Graph<FingerDescriptor,NetworkAddressPair>,NetworkAddressPair>,Shape> edgeArrowTransformer =
            new DirectionalEdgeArrowTransformer<FingerDescriptor,NetworkAddressPair>(0, 0, 0);
         this.viewer.getRenderContext().setEdgeArrowTransformer(edgeArrowTransformer);
         final Tree<FingerDescriptor, NetworkAddressPair> body=findTreeBody(graph.getTrees());
         Transformer<FingerDescriptor,Paint> vertexPaint = new Transformer<FingerDescriptor,Paint>() {
            public Paint transform(FingerDescriptor vertex) {
                if(body.containsVertex(vertex)){
                    return Color.BLUE;
                }
                return Color.RED;
            }
         };
         this.viewer.getRenderContext().setVertexFillPaintTransformer(vertexPaint);
//         this.viewer.getRenderContext().setVertexLabelTransformer(new ToStringLabeller());
         DefaultModalGraphMouse gm = new DefaultModalGraphMouse();
         gm.setMode(ModalGraphMouse.Mode.TRANSFORMING);
         this.viewer.setGraphMouse(gm);
         frame.getContentPane().add(this.viewer);
         frame.pack();
         frame.setVisible(true);
     }
     
     

     public void buildGraph(Set edges, Map<NetworkAddress, FingerDescriptor> vertices){
         this.graph=new DelegateForest();
         for(FingerDescriptor vertex: vertices.values()){
             this.graph.addVertex(vertex);
         }
         Iterator it=edges.iterator();
         while(it.hasNext()){
             NetworkAddressPair edge=(NetworkAddressPair)it.next();
             this.graph.addEdge(edge, vertices.get(edge.getAddress1()), vertices.get(edge.getAddress2()));
         }
     }

     public double getConnectedness(){
         Collection<Tree<FingerDescriptor, NetworkAddressPair>> trees=this.graph.getTrees();
         Tree<FingerDescriptor, NetworkAddressPair> body=this.findTreeBody(trees);
         int disconnected=0;
         for(Tree tree:trees){
             if(!tree.equals(body)){
                 disconnected=disconnected+tree.getVertexCount();
             }
         }
         double connectivity=100-(disconnected*100.0/this.graph.getVertexCount());
         return connectivity;
     }

     private Tree<FingerDescriptor, NetworkAddressPair> findTreeBody(Collection<Tree<FingerDescriptor, NetworkAddressPair>> trees){
         Tree<FingerDescriptor, NetworkAddressPair> body=null;
         for(Tree tree:trees){
             if(body==null){
                 body=tree;
             }
             else{
                 if(tree.getVertexCount()>body.getVertexCount()){
                     body=tree;
                 }
             }
         }
         return body;
     }

     public void captureImage(String visDir, int epochNumber){
         this.viewer.setDoubleBuffered(false);
         BufferedImage im=new BufferedImage(this.viewer.getWidth(), this.viewer.getHeight(), ColorSpace.TYPE_CMYK);
         Graphics2D g=(Graphics2D)im.createGraphics();
         this.viewer.getRootPane().paintComponents(g);
         try{
             ImageIO.write(im, "png", new File(visDir+"/"+epochNumber+".png"));
         }
         catch(IOException e){

         }
         this.viewer.setDoubleBuffered(true);
     }


}
