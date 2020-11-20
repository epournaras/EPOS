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
package treestructure;

import dsutil.generic.RankPriority;
import dsutil.protopeer.services.topology.trees.DescriptorType;
import dsutil.protopeer.services.topology.trees.TreeProvider;
import dsutil.protopeer.services.topology.trees.TreeType;
import agent.Agent;
import config.Configuration;

import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.logging.Logger;
import protopeer.Experiment;
import protopeer.Peer;
import protopeer.servers.bootstrap.SimplePeerIdentifierGenerator;
import tree.BalanceType;
import tree.centralized.client.TreeClient;
import tree.centralized.server.TreeServer;

/**
 * Defines tree architecture and assignment of agents to vertices of the tree-graph.
 *  - RANK PRIORITY: defines ascending or descending order
 *  - DESCRIPTOR TYPE: can be RANK or NODE DEGREE
 *  - TREE TYPE: with respect to rank of nodes, agents can be RANDOMly assigned to vertices in the tree or SORTED:
 *               SORTED_HtL -> from HIGH to LOW
 *               SORTED_LtH -> from LOW to HIGH
 *  - BALANCE TYPE: can be either WEIGHT_BALANCED or LIST
 * @author Peter
 */
public class TreeArchitecture implements Cloneable {
	
    public RankPriority 	priority;
    public DescriptorType 	rank;
    public TreeType 		type;
    public BalanceType 		balance;
    public int 				maxChildren;
    
    public BiFunction<Integer, Agent, Double> rankGenerator = (idx, agent) -> (double) idx;
    
    public TreeArchitecture() {
        this.maxChildren = Configuration.numChildren;
        this.priority = Configuration.priority;
        this.rank = Configuration.rank;
        this.type = Configuration.type;
        this.balance = Configuration.balance;
    }
    
    public void addPeerlets(Peer peer, Agent agent, int peerIndex, int numNodes) {
        if (peerIndex == 0) {
            peer.addPeerlet(new TreeServer(numNodes, priority, rank, type, balance));
        }
        peer.addPeerlet(new TreeClient(Experiment.getSingleton().getAddressToBindTo(0), 
        		                       new SimplePeerIdentifierGenerator(), 
        		                       rankGenerator.apply(peerIndex, agent), 
        		                       maxChildren+1)
        		        );        
        peer.addPeerlet(new TreeProvider());
        peer.addPeerlet(agent);
    }
    
    @Override
    public TreeArchitecture clone() {
        try {
            return (TreeArchitecture) super.clone();
        } catch (CloneNotSupportedException ex) {
            Logger.getLogger(TreeArchitecture.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    @Override
    public String toString() {
        return "Tree";
    }
}
