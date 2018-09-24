/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package agent;

import data.Plan;
import func.CostFunction;
import func.PlanCostFunction;
import agent.logging.AgentLoggingProvider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import protopeer.Finger;
import protopeer.network.Message;
import protopeer.time.Timer;
import protopeer.util.quantities.Time;
import data.DataType;

/**
 * An agent that performs combinatorial optimization in a tree network over
 * multiple iterations. Each iteration consists of a bottom-up phase followed by
 * a top-down phase.
 *
 * @author Peter P. & Jovan N.
 * @param <V> the type of the data this agent should handle
 * @param <UP> the type of message for the bottom-up phase
 * @param <DOWN> the type of message for the top-down phase
 */
public abstract class IterativeTreeAgent<V 		extends DataType<V>, 
										 UP 	extends IterativeTreeAgent.UpMessage, 
										 DOWN 	extends IterativeTreeAgent.DownMessage> extends TreeAgent<V> {

    int numAgents;

    int numIterations;
    int iteration;

    private final Map<Finger, UP> messageBuffer = new HashMap<>();

    /**
     * Initializes the agent with the given combinatorial optimization problem
     * definition
     *
     * @param numIterations number of iterations
     * @param possiblePlans the possible plans of this agent
     * @param globalCostFunc the global cost function
     * @param localCostFunc the local cost function
     * @param loggingProvider the logger for the experiment
     * @param seed the seed for the RNG used by this agent
     */
    public IterativeTreeAgent(int numIterations, List<Plan<V>> possiblePlans, CostFunction<V> globalCostFunc, PlanCostFunction<V> localCostFunc, AgentLoggingProvider<? extends IterativeTreeAgent<V, UP, DOWN>> loggingProvider, long seed) {
        super(possiblePlans, globalCostFunc, localCostFunc, loggingProvider, seed);
        this.numIterations = numIterations;
        this.iteration = numIterations;
    }

    @Override
    public int getIteration() {
        return iteration;
    }

    @Override
    public int getNumIterations() {
        return numIterations;
    }

    @Override
    void runActiveState() {
        if (iteration < numIterations - 1) {
            Timer loadAgentTimer = getPeer().getClock().createNewTimer();
            loadAgentTimer.addTimerListener((Timer timer) -> {
                runIteration();
            });
            loadAgentTimer.schedule(Time.inMilliseconds(1000));
        } else {
            super.runActiveState();
        }
    }

    @Override
    final void runPhase() {
        iteration = -1;

        initPhase();
        runIteration();
    }

    private void runIteration() {
        iteration++;
        
        if(this.isIterationAfterReorganization()) {
        	this.reset();
        }        
    	
        this.setNumComputed(0);
        this.setNumTransmitted(0);

        if (this.conditionToStartNewIteration()) {
            this.doIfConditionToStartNewIterationIsMet();
        } else {
        	this.doIfConditionToStartNewIterationIsNOTMet();
        }
        
    }
    
    /**
     * This condition should control how many iterations IEPOS runs for.
     * It should prevent program to run indefinitely long.
     * @return true if current iteration is exclusively lower than max allowed number of iterations
     */
    boolean conditionToStartNewIteration() {
    	return this.iteration < this.numIterations;
    }
    
    void doIfConditionToStartNewIterationIsMet() {
//    	this.log(Level.FINER, "IterativeTreeAgent::doIfConditionToStartNewIterationIsMet()");
    	this.initIteration();
        if (this.isLeaf()) {
            this.goUp();
        }
        this.runActiveState();
    }
    
    void doIfConditionToStartNewIterationIsNOTMet() { }

    @Override
    /**
     * In case of <code>UP Message</code>:
     * 	1. put message in the buffer
     * 	2. invokes <code>this.goUp()</code> when <code>this.children.size() <= this.messageBuffer.size()</code>
     *     Number of children is constant (for now), and <code>messageBuffer</code> is only growing. The moment it
     *     reaches size of <code>children</code>, UP phase begins
     * 
     * In case of <code>DOWN Message</code>:
     * 	1. invokes <code>this.goDown()</code>
     * @param message
     */
    public void handleIncomingMessage(Message message) {
        if (message instanceof UpMessage) {
            UP msg = (UP) message;
            messageBuffer.put(msg.child, msg);
            if (children.size() <= messageBuffer.size()) {
                goUp();
            }
        } else if (message instanceof DownMessage) {
            goDown((DOWN) message);
        }
    }

    /**
     * 1. Collect all messages received from children (and clear this.messageBuffer)
     * 2. this.numAgents = SUM{c.numAgents | c is child of 'this'}
     * 3. this.cumTransmitted = this.numTransmitted + MAX{c.cumTransmitted | c is child of 'this'}
     * 4. this.cumComputed = MAX{c.cumComputed | c is child of 'this'}
     * 5. actual UP message is created:
     *    - aggregation
     *    - selecting a plan
     *    - aggregated response
     * 6. counters are updated and message is sent to parent
     */
    private void goUp() {
        List<UP> orderedMsgs = new ArrayList<>();
        for (Finger child : children) {
            orderedMsgs.add(messageBuffer.get(child));
        }
        messageBuffer.clear();

        if (iteration == 0) {
            numAgents = 1 + orderedMsgs.stream().map(msg -> msg.numAgents).reduce(0, (a, b) -> a + b);
        }
        
        this.setNumTransmitted(orderedMsgs.stream().map(msg -> msg.getNumTransmitted()).reduce(0, (a, b) -> a + b));
        this.setNumComputed(0);
        this.setCumTransmitted(this.getNumTransmitted() + orderedMsgs.stream().map(msg -> msg.cumTransmitted).reduce(0, (a, b) -> Math.max(a, b)));
        this.setCumComputed(orderedMsgs.stream().map(msg -> msg.cumComputed).reduce(0, (a, b) -> Math.max(a, b)));
        
        this.setCumComputed(this.getCumComputed() - this.getNumComputed());
        UP msg = up(orderedMsgs);
        this.setCumComputed(this.getCumComputed() + this.getNumComputed());
        
        msg.child = getPeer().getFinger();
        if (isRoot()) {
            goDown(atRoot(msg));
        } else {
            msg.numAgents = numAgents;
            msg.cumTransmitted = this.getCumTransmitted();
            msg.cumComputed = this.getCumComputed();
            this.setNumTransmitted(this.getNumTransmitted() + msg.getNumTransmitted());
            this.setCumTransmitted(this.getCumTransmitted() + msg.getNumTransmitted());
            getPeer().sendMessage(parent.getNetworkAddress(), msg);
        }
    }

    /**
     * 1. updates counters
     * 2. - updates global response received from parent
     *    - approve or reject children's selected plan. rules:
     *         i) if parent's selected plan was not approved, then none of descendants of the parent cannot have their selected plans approved
     *         ii) if parent's selected plan was approved, then preliminary approval becomes effective. In other words, selected plan of a child
     *             will be approved iff parent's selected plan was approved and child's selected plan was preliminary approved during BOTTOM-UP phase
     * @param parentMsg
     */
    private void goDown(DOWN parentMsg) {
        if (!isRoot()) {
            numAgents = parentMsg.numAgents;
            this.setNumTransmitted(this.getNumTransmitted() + parentMsg.getNumTransmitted());
            this.setCumTransmitted(parentMsg.getNumTransmitted() + parentMsg.cumTransmitted);
            this.setCumComputed(parentMsg.cumComputed);
        }

        this.setCumComputed(this.getCumComputed() - this.getNumComputed());
        List<DOWN> msgs = down(parentMsg);
        this.setCumComputed(this.getCumComputed() + this.getNumComputed());

        for (int i = 0; i < msgs.size(); i++) {
            DOWN msg = msgs.get(i);
            msg.numAgents = numAgents;
            msg.cumTransmitted = this.getCumTransmitted();
            msg.cumComputed = this.getCumComputed();
            this.setNumTransmitted(this.getNumTransmitted() + msg.getNumTransmitted());
            this.setCumTransmitted(this.getCumTransmitted() + msg.getNumTransmitted());
            
            getPeer().sendMessage(children.get(i).getNetworkAddress(), msg);
        }
        
        this.finalizeDownPhase(parentMsg);
    }

    abstract void initPhase();

    abstract void initIteration();
    
    abstract void finalizeDownPhase(DOWN parentMsg);

    /**
     * As side-effect, this method should update this.numComputed to value equal to size of possible plans!
     * @param childMsgs - list of messages received from children
     * @return UP message to be sent to the parent
     */
    abstract UP up(List<UP> childMsgs);

    abstract DOWN atRoot(UP rootMsg);

    abstract List<DOWN> down(DOWN parentMsg);

    static abstract class UpMessage extends Message {

        public Finger child;
        public int numAgents;
        public int cumTransmitted;
        public int cumComputed;

        public abstract int getNumTransmitted();
    }

    static abstract class DownMessage extends Message {

        public int numAgents;
        public int cumTransmitted;
        public int cumComputed;

        public abstract int getNumTransmitted();
    }
    
    @Override
    public void log(Level level, String message) {
    	this.logger.log(level, "NODE: " + this.getPeer().getIndexNumber() + ", iter: " + this.iteration + ", " + message);
    }
}
