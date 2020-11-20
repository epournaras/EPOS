/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package agent;

import liveRunUtils.Messages.InformGatewayMessage;
import config.Configuration;
import data.Plan;
import dsutil.protopeer.services.topology.trees.TreeApplicationInterface;
import func.CostFunction;
import func.PlanCostFunction;
import agent.logging.AgentLoggingProvider;

import java.util.ArrayList;
import java.util.List;

import protopeer.Finger;
import data.DataType;
import protopeer.MainConfiguration;
import protopeer.network.zmq.ZMQAddress;

/**
 * An agent that performs combinatorial optimization in a tree network.
 * 
 * @author Peter
 * @param <V> the type of the data this agent should handle
 */
public abstract class TreeAgent<V extends DataType<V>> extends Agent<V> implements TreeApplicationInterface, java.io.Serializable {

    // tree properties
    Finger 					parent 			= 	null;
    final List<Finger> 		children 		= 	new ArrayList<>();
    public boolean treeViewIsSet = false;

    /**
     * Initializes the agent with the given combinatorial optimization problem
     * definition
     *
     * @param possiblePlans the possible plans of this agent
     * @param globalCostFunc the global cost function
     * @param localCost the local cost function
     * @param loggingProvider the logger for the experiment
     * @param seed the seed for the RNG used by this agent
     */
    public TreeAgent(List<Plan<V>> possiblePlans, CostFunction<V> globalCostFunc, PlanCostFunction<V> localCost, AgentLoggingProvider<? extends TreeAgent<V>> loggingProvider, long seed) {
        super(possiblePlans, globalCostFunc, localCost, loggingProvider, seed);
    }

    /**
     * Initializes the agent with the given combinatorial optimization problem
     * definition
     *  @param globalCostFunc the global cost function
     * @param localCost the local cost function
     * @param loggingProvider the logger for the experiment
     * @param seed the seed for the RNG used by this agent
     */
    public TreeAgent(CostFunction<V> globalCostFunc, PlanCostFunction<V> localCost, AgentLoggingProvider<? extends TreeAgent<V>> loggingProvider, long seed) {
        super(globalCostFunc, localCost, loggingProvider, seed);
    }

    /**
     * Root is the agent without parent and always has at least 1 child
     * @return
     */
    public boolean isRoot() {
        return parent == null && !children.isEmpty();
    }

    /**
     * Leaf is the agent that has a parent and has no children
     * @return
     */
    public boolean isLeaf() {
        return parent != null && children.isEmpty();
    }
    
    /**
     * Inner node is the agent that has a parent and has at least 1 child
     * @return
     */
    public boolean isInnerNode() {
        return parent != null && !children.isEmpty();
    }

    /**
     * Node is considered disconnected iff it has no parent and has no children
     * @return
     */
    public boolean isDisconnected() {
        return parent == null && children.isEmpty();
    }

    @Override
    public void setParent(Finger parent) {
        if (parent != null) {
            this.parent = parent;
        }
        else {
        }
    }

    @Override
    public void setChildren(List<Finger> list) {
        children.addAll(list);
    }

    @Override
    public void setTreeView(Finger parent, List<Finger> children) {
        if(Configuration.isLiveRun) {
            // in case of a new run, the tree structure might change. Hence the treeView is reset
            resetTreeView();
        }
        this.setParent(parent);
        this.setChildren(children);
        if(!Configuration.isLiveRun) {treeViewIsSet();}
        treeViewIsSet = true;
        if(Configuration.isLiveRun) {
            // informing the gateway that the peer has its treeView set
            ZMQAddress dest = new ZMQAddress(MainConfiguration.getSingleton().peerZeroIP, Configuration.GateWayPort);
            getPeer().sendMessage(dest, new InformGatewayMessage(MainConfiguration.getSingleton().peerIndex, this.activeRun, "treeViewSet", isLeaf()));
        }
    }

    void resetTreeView() {
        this.parent = null;
        this.children.clear();
    }

    void treeViewIsSet(){};

    public List<Finger> getChildren() {
        return children;
    }

    @Override
    /**
     * Agent is 'representative' iff it is root.
     */
    public boolean isRepresentative() {
        return isRoot();
    }
    
    @Override
    public void reset() {
    	super.reset();
    }

    void runBootstrap(){
        super.runBootstrap();
    }
}
