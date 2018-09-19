package treestructure;

import java.util.Random;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.logging.Logger;

import agent.Agent;
import config.Configuration;
import data.Vector;
import dsutil.generic.RankPriority;
import dsutil.protopeer.services.topology.trees.DescriptorType;
import dsutil.protopeer.services.topology.trees.TreeType;
import protopeer.Experiment;
import protopeer.Peer;
import protopeer.servers.bootstrap.SimplePeerIdentifierGenerator;
import tree.BalanceType;
import util.TreeArchitecture;

public class ModifiableTreeArchitecture implements Cloneable {
	
	public RankPriority 	priority 	= 	RankPriority.HIGH_RANK;
    public DescriptorType 	rank 		= 	DescriptorType.RANK;
    public TreeType 		type 		= 	TreeType.SORTED_LtH;			// LOW TO HIGH!
    public BalanceType 		balance 	= 	BalanceType.WEIGHT_BALANCED;
    public int 				maxChildren = 	2;
    
    public Configuration	config		=	null;
    
    public BiFunction<Integer, Agent, Double> rankGenerator = (idx, agent) -> (double) idx;
    
    public ModifiableTreeArchitecture(int maxChildren, Configuration config) {
        this.maxChildren = maxChildren;
        this.config = config;
    }
    
    public void addPeerlets(Peer peer, Agent agent, int peerIndex, int numNodes) {
    	
        if (peerIndex == 0) {
            peer.addPeerlet(new ModifiableTreeServer(numNodes, 
            										 priority, 
            										 rank, 
            										 type, 
            										 balance,  
            										 new Random(config.reorganizationSeed)));
        }
        
        peer.addPeerlet(new ModifiableTreeClient(Experiment.getSingleton().getAddressToBindTo(0), 
        		                       			 new SimplePeerIdentifierGenerator(), 
        		                       			 rankGenerator.apply(peerIndex, agent), 
        		                       			 maxChildren+1)
        		        );        
        peer.addPeerlet(new ModifiableTreeProvider());
        peer.addPeerlet(agent);
    }
    
    @Override
    public ModifiableTreeArchitecture clone() {
        try {
            return (ModifiableTreeArchitecture) super.clone();
        } catch (CloneNotSupportedException ex) {
            Logger.getLogger(TreeArchitecture.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    @Override
    public String toString() {
        return "Modifiable-Tree";
    }

}
