package treestructure;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import java.util.Random;
import java.util.Set;

import dsutil.generic.RankPriority;
import dsutil.protopeer.FingerDescriptor;
import dsutil.protopeer.services.topology.trees.DescriptorType;
import dsutil.protopeer.services.topology.trees.TreeType;
import protopeer.BasePeerlet;
import protopeer.Peer;
import protopeer.network.Message;
import tree.BalanceType;
import tree.centralized.TreeViewReply;
import tree.centralized.TreeViewRequest;
import tree.centralized.server.TreeTopologyGenerator;
import tree.centralized.server.TreeViewFacilitator;

/**
 * The class that computes and maintains tree topology. Main functionalities include:
 *  - handling <code>TreeViewRequests</code> received from other nodes in the network
 *  - generating Tree topology given all the nodes
 *  - broadcasting parents and children via <code>TreViewReply</code> messages to all 
 *    participating nodes.
 * 
 * @author jovan
 *
 */
public class ModifiableTreeServer extends BasePeerlet {
	
	private Logger logger = Logger.getLogger(ModifiableTreeServer.class.getName());
	
    public enum ServerState {
        INIT,
        GATHERING_PEERS,
        WAITING,
        COMPLETED
    }
    
    private Random								random;
    private LinkedHashSet<FingerDescriptor> 	peers;			// it maintains the insertion order!
    private TreeTopologyGenerator 				generator;
    private ServerState 						state;
    private final int 							N;
    private int 								n;
    
    private Set<Entry<FingerDescriptor,TreeViewFacilitator>> views;
    
    
    /**
     * Initializes the server and the topology generator with the required
     * information.
     *
     * @param N 			the number of requests to wait for before starting building the
     * 						 tree topology. Essentially, this should be total number of nodes, no more no less.
     * @param priority 		higher or lower ranks preferred during the sorting. This
     * 						 parameter is fed in the <code>TreeTopologyGenerator</code>.
     * @param descrType 	the descriptor type based on which the sorting is performed: 
     * 						 <code>RANK</code> or <code>NODE_DEGREE</code>. This parameter 
     * 						 is fed in the <code>TreeTopologyGenerator</code>.
     * @param treeType 		the type of tree to be built: <code>RANDOM</code>, <code>SORTED_HtL</code>
     * 						 and <code>SORTED_LtH</code>. This parameter is fed in the <code>TreeTopologyGenerator</code>.
     * @param balanceType 	the balance of the tree to be built: <code>WEIGHT_BALANCED</code> or <code>LIST</code>. 
     * 						 This parameter is fed in the <code>TreeTopologyGenerator</code>.
     * @param random		random number generator to be used for permuting the list.
     */
    public ModifiableTreeServer(int N, 
    		                    RankPriority priority, 
    		                    DescriptorType descrType, 
    		                    TreeType treeType, 
    		                    BalanceType balanceType ,
    		                    Random random
    		                   ){
        this.state = ServerState.INIT;
        this.N = N;
        this.n = 0;
        this.peers = new LinkedHashSet<FingerDescriptor>();
        this.generator = new TreeTopologyGenerator(priority, descrType, treeType, balanceType);
        this.random = random;
    }
    
    /**
     * Initializes the server and the topology generator with the required
     * information.
     *
     * @param N 			the number of requests to wait for before starting building the
     * 						 tree topology. Essentially, this should be total number of nodes, no more no less.
     * @param priority 		higher or lower ranks preferred during the sorting. This
     * 						 parameter is fed in the <code>TreeTopologyGenerator</code>.
     * @param descrType 	the descriptor type based on which the sorting is performed: 
     * 						 <code>RANK</code> or <code>NODE_DEGREE</code>. This parameter 
     * 						 is fed in the <code>TreeTopologyGenerator</code>.
     * @param treeType 		the type of tree to be built: <code>RANDOM</code>, <code>SORTED_HtL</code>
     * 						 and <code>SORTED_LtH</code>. This parameter is fed in the <code>TreeTopologyGenerator</code>.
     * @param balanceType 	the balance of the tree to be built: <code>WEIGHT_BALANCED</code> or <code>LIST</code>. 
     * 						 This parameter is fed in the <code>TreeTopologyGenerator</code>.
     * @param seed			seed value for random numbers generator to be used for permutations.
     */
    public ModifiableTreeServer(int N, 
    		                    RankPriority priority, 
    		                    DescriptorType descrType, 
    		                    TreeType treeType, 
    		                    BalanceType balanceType ,
    		                    long seed
    		                   ){
        this.state = ServerState.INIT;
        this.N = N;
        this.n = 0;
        this.peers = new LinkedHashSet<FingerDescriptor>();
        this.generator = new TreeTopologyGenerator(priority, descrType, treeType, balanceType);
        this.random = new Random(seed);
    }
    
    /**
     * Initializes the server and the topology generator with the required
     * information.
     *
     * @param N 			the number of requests to wait for before starting building the
     * 						 tree topology
     * @param priority 		higher or lower ranks preferred during the sorting. This
     * 						 parameter is fed in the <code>TreeTopologyGenerator</code>.
     * @param descrType 	the descriptor type based on which the sorting is performed: 
     * 						 <code>RANK</code> or <code>NODE_DEGREE</code>. This parameter 
     * 						 is fed in the <code>TreeTopologyGenerator</code>.
     * @param treeType 		the type of tree to be built: <code>RANDOM</code>, <code>SORTED_HtL</code>
     * 						 and <code>SORTED_LtH</code>. This parameter is fed in the <code>TreeTopologyGenerator</code>.
     */
    public ModifiableTreeServer(int N, 
    		 					RankPriority priority, 
    		 					DescriptorType descrType, 
    		 					TreeType treeType
    		 				   ){
        this(N, priority, descrType, treeType, BalanceType.WEIGHT_BALANCED, new Random(0));
    }
    
    @Override
    public void init(Peer peer) {
        super.init(peer);
    }

    @Override
    public void start() {
        super.start();
        this.runActiveState();
    }
    
    public ServerState getState() {
    	return this.state;
    }
    
    private void runActiveState(){
        this.state=ServerState.GATHERING_PEERS;
    }
    
    //																								ACTIVE STATE  //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //																								PASSIVE STATE //
    
    @Override
    /**
     * Handles only messages of type <code>TreeViewRequest</code>
     */
    public void handleIncomingMessage(Message message) {
        if (message instanceof TreeViewRequest) {
                this.runPassiveState((TreeViewRequest) message);
        }
    }
    
    /**
     * It operates in 2 different modes:
     *  1. if state is <code>GATHERING PEERS</code>:
     *      - waits until all nodes send their requests
     *      - generates topology based on all received requests
     *      - broadcasts all replies to all participating nodes
     *      - sets mode into <code>COMPLETED</code>
     *  2. if state is <code>COMPLETED</code>:
     *      request received in this state indicates dynamic change of structure. Therefore:
     *       - parents and children of the request sender are wrapped in TreeViewReply message
     *       - message is sent to the original agent
     *      Note that prior to reception of any message, nodes have been shuffled to explore different structure.
     * @param request
     */
    private void runPassiveState(TreeViewRequest request) {    	
    	switch(this.state) {
    	case GATHERING_PEERS:
    		this.peers.add(request.sourceDescriptor);
    		//this.logger.log(Level.FINER, "Descriptor received: " + request.sourceDescriptor);
   	     	this.n++;
	   	    if(this.n == this.N){
	   	    	//this.logger.log(Level.INFO, "Number of requests gathered reached expected number: " + this.N);
	            this.generateTreeTopology();
	        }
    		break;
    	case COMPLETED:
    		this.handleSingleMessage(request);
    		break;
    	default:
    		this.logger.log(Level.SEVERE, "Received Tree View Request during WAITING state.");
    		break;
    	}
    }
    
    /**
     * Invoked only in the beginning, during <code>GATHERING PEERS</code> phase.
     * It generates topology for the first time (all peers' descriptors are gathered by now)
     * and then broadcasts TreeViews to all nodes.
     */
    private void generateTreeTopology() {
    	this.views = this.generator.generateTopology(this.peers);
        this.broadcastViews();
        this.state = ServerState.COMPLETED;
        this.shuffleNodes();
        this.n = 0;
    }
    
    /**
     * Shuffles the list of peers using random number generator and <code>Collections.shuffle()</code>.
     * Then, <code>DescriptorType.RANK</code> is updated according to index of the descriptor in the list.
     * Because of this, during topology generation, in which they are sorted according to their RANK, this
     * permutation is maintained. Finally, it sets <code>this.views</code>.
     */
    public void shuffleNodes() {
    	this.state = ServerState.WAITING;
    	ArrayList<FingerDescriptor> listForShuffling = new ArrayList<>();
    	this.peers.forEach(fd -> listForShuffling.add(fd));
    	Collections.shuffle(listForShuffling, this.random);
    	this.peers.clear();
    	IntStream.range(0, listForShuffling.size()).forEach(i -> {
    		listForShuffling.get(i).replaceDescriptor(DescriptorType.RANK, (double)i);
    		this.peers.add(listForShuffling.get(i));
    	});
    	this.logger.log(Level.INFO, "Nodes shuffled!");
    	try {
    		this.views = this.generator.generateTopology(this.peers);
    	} catch(Exception e) {
    		e.printStackTrace();
    	}    	
    	this.state = ServerState.COMPLETED;
    }
    
    /**
     * Sends reply containing parent and children to every node in the network.
     * If views is <code>null</code>, nothing is sent to the nodes.
     * 
     * @param views set of pairs: Descriptor of the receiving agent and it's assigned parent and children
     */
    private void broadcastViews() {
    	if(this.views == null) {
    		this.logger.log(Level.SEVERE, "View received from topology generator is null!");
    		return;
    	}
    	this.views.stream().map(entry -> {
    						 return new SimpleEntry<FingerDescriptor, TreeViewReply>(entry.getKey(), 
    								                                          		 this.createReplyMessage(entry));
    					   })
    	              .forEach(entry -> {
    	            	  //this.logger.log(Level.FINER, "Reply sent from server to agent " + entry.getKey().getNetworkAddress());
    	            	  this.getPeer().sendMessage(entry.getKey().getNetworkAddress(), entry.getValue());
    	              });
    }
    
    /**
     * Generates reply to node's request for TreeView update.
     * @param request
     */
    private void handleSingleMessage(TreeViewRequest request) {
    	//this.logger.log(Level.INFO, "handling single request!");
    	FingerDescriptor sender = request.sourceDescriptor;
    	if(sender == null) {
    		this.logger.log(Level.SEVERE, "Sender of TreeViewRequest is null!");
    		return;
    	}
    	if(!this.peers.contains(sender)) {
    		this.logger.log(Level.SEVERE, "TreeViewRequest sent from unknown node!");
    		return;
    	}
		Entry<FingerDescriptor, TreeViewFacilitator> sendersEntry = 
				this.views.stream().filter(entry -> entry.getKey().equals(sender))	// equals() of FingerDescriptors compares fingers only, not descriptors!
						   		   .findFirst()										// generally, there should be one and only
						   		   .orElse(null);
		if(sendersEntry == null) {
			this.logger.log(Level.SEVERE, "Filtering in handling single message returns null!");
			return;
		}
		TreeViewReply reply = this.createReplyMessage(sendersEntry);
		this.getPeer().sendMessage(sender.getNetworkAddress(), reply);
		this.n++;
		if(this.n == this.N) {
			this.shuffleNodes();
			this.n = 0;
		}
    }
    
    /**
     * Creates <code>TreViewReply</code> message and sets parent and children from <code>entry</code>.
     * 
     * @param entry contains parent and children in the tree topology.
     * @return <code>TreViewReply</code> message that is ready to be sent
     */
    private TreeViewReply createReplyMessage(Entry<FingerDescriptor, TreeViewFacilitator> entry) {
    	TreeViewReply reply = new TreeViewReply();
    	reply.parent = entry.getValue().getParent();
    	reply.children = entry.getValue().getChildren();
    	return reply;
    }

}
