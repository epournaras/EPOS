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

import dsutil.generic.RankPriority;
import dsutil.protopeer.services.topology.trees.DescriptorType;
import dsutil.protopeer.services.topology.trees.TreeProvider;
import dsutil.protopeer.services.topology.trees.TreeType;
import agent.Agent;
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
 *
 * @author Peter
 */
public class TreeArchitecture implements Cloneable {
    public RankPriority priority = RankPriority.HIGH_RANK;
    public DescriptorType rank = DescriptorType.RANK;
    public TreeType type = TreeType.SORTED_HtL;
    public BalanceType balance = BalanceType.WEIGHT_BALANCED;
    public int maxChildren = 2;
    public BiFunction<Integer, Agent, Double> rankGenerator = (idx, agent) -> (double) idx;
    
    public TreeArchitecture(int maxChildren) {
        this.maxChildren = maxChildren;
    }
    
    public void addPeerlets(Peer peer, Agent agent, int peerIndex, int numNodes) {
        if (peerIndex == 0) {
            peer.addPeerlet(new TreeServer(numNodes, priority, rank, type, balance));
        }
        peer.addPeerlet(new TreeClient(Experiment.getSingleton().getAddressToBindTo(0), new SimplePeerIdentifierGenerator(), rankGenerator.apply(peerIndex, agent), maxChildren+1));
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
