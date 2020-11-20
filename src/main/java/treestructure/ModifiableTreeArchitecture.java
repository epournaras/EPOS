package treestructure;

import java.net.InetAddress;
import java.net.UnknownHostException;
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
import protopeer.MainConfiguration;
import protopeer.Peer;
import protopeer.network.IntegerNetworkAddress;
import protopeer.network.NetworkAddress;
import protopeer.network.zmq.ZMQAddress;
import protopeer.servers.bootstrap.SimplePeerIdentifierGenerator;
import protopeer.util.NetworkAddressPair;
import sun.applet.Main;
import sun.nio.ch.Net;
import tree.BalanceType;

public class ModifiableTreeArchitecture implements Cloneable {
	
	public RankPriority 	priority;
    public DescriptorType 	rank;
    public TreeType 		type;
    public BalanceType 		balance;
    public int 				maxChildren;
    private Configuration	config;
    private NetworkAddress bootstrap;
    
    public BiFunction<Integer, Agent, Double> rankGenerator = (idx, agent) -> (double) idx;
    
    public ModifiableTreeArchitecture(Configuration config) {
        this.maxChildren = Configuration.numChildren;
        this.priority = Configuration.priority;
        this.rank = Configuration.rank;
        this.type = Configuration.type;
        this.balance = Configuration.balance;
        this.config = config;
    }
    
    public void addPeerlets(Peer peer, Agent agent, int peerIndex, int numNodes) {
    	
        if (peerIndex == 0) {
            peer.addPeerlet(new ModifiableTreeServer(numNodes, 
            										 priority, 
            										 rank, 
            										 type, 
            										 balance,  
            										 new Random(this.config.reorganizationSeed)));
        }

        // depending on the live vs sim operation, the address for the bootstrap differs (in live run, it is an IP read from the config)
        if (config.isLiveRun){ bootstrap = new ZMQAddress(MainConfiguration.getSingleton().peerZeroIP,MainConfiguration.getSingleton().peerZeroPort);}
        else {bootstrap = Experiment.getSingleton().getAddressToBindTo(0);}

        peer.addPeerlet(new ModifiableTreeClient(bootstrap,
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
