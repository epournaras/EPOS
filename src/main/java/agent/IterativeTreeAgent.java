/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package agent;

import config.Configuration;
import liveRunUtils.Messages.InformGatewayMessage;
import liveRunUtils.Messages.InformUserMessage;
import protopeer.MainConfiguration;
import protopeer.time.TimerListener;
import protopeer.network.zmq.ZMQAddress;
import java.io.Serializable;

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

    // init config for live run
    int latestDownMessage = -10;
    boolean listenForDownMessage = new Boolean(false);
    boolean inBetweenRuns = new Boolean(false);
    boolean moveToReadyToActive = new Boolean(false);

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

    // constructor for live run, the possible plans are not needed, as the user will send it to the agent via a network message
    public IterativeTreeAgent(int numIterations, CostFunction<V> globalCostFunc, PlanCostFunction<V> localCostFunc, AgentLoggingProvider<? extends IterativeTreeAgent<V, UP, DOWN>> loggingProvider, long seed) {
        super(globalCostFunc, localCostFunc, loggingProvider, seed);
        this.numIterations = numIterations;
        this.iteration = 0;
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
    void runBootstrap() {
        if(config.Configuration.isLiveRun) {
            /**
             * if live, the following is done:
             * check for the treeView (given by the bootstrap)
             * check for the plans being set (sent by the user)
             * sending ready to run to the gateway, and proceeding to readytoRunActiveState
             */
            Timer loadAgentTimer = getPeer().getClock().createNewTimer();
            loadAgentTimer.addTimerListener(new TimerListener() {
                public void timerExpired(Timer timer) {
                    if (treeViewIsSet && plansAreSet && weightsAreSet) {
                        System.out.println("tree view and plans are set for: " + getPeer().getNetworkAddress());
                        if (!readyToRun) {
                            System.out.println("sending the ready to run for: " + getPeer().getNetworkAddress());
                            //inform gateway of running status
                            ZMQAddress dest = new ZMQAddress(MainConfiguration.getSingleton().peerZeroIP, Configuration.GateWayPort);
                            getPeer().sendMessage(dest, new InformGatewayMessage(MainConfiguration.getSingleton().peerIndex, activeRun, "ready", isLeaf()));
                            readytoRunActiveState();
                        } else if (readyToRun) {
                            readytoRunActiveState();
                        } else {
                            runBootstrap();
                        }
                    } else runBootstrap();
                }
            });
            loadAgentTimer.schedule(Time.inMilliseconds(this.bootstrapPeriod));
        }
        else{
            // if not live
            super.runBootstrap();}
    }

    protected void readytoRunActiveState() {
        inBetweenRuns = new Boolean(false);
        if (!isLeaf()){
            // if not the leaf, go to runActiveState and wait to receive a message from the childern
            runActiveState();
        }
        else {
            Timer loadAgentTimer = getPeer().getClock().createNewTimer();
            loadAgentTimer.addTimerListener((Timer timer) -> {
                if (readyToRun){
                    // if leaf and readyToRun, start the iterations by sending up message to the parent
                    System.out.println("waiting over, I am a leaf: "+isLeaf()+", moving to active state for: "+getPeer().getNetworkAddress());
                    listenForDownMessage = new Boolean(false);
                    runActiveState();}
                else {
                    readytoRunActiveState();
                }
            });
            loadAgentTimer.schedule(Time.inMilliseconds(readyPeriod));
        }
    }

    @Override
    void runActiveState() {
        if(config.Configuration.isLiveRun) {
            if (iteration <= numIterations - 1) {
                Timer loadAgentTimer = getPeer().getClock().createNewTimer();
                loadAgentTimer.addTimerListener((Timer timer) -> {
                    runIteration();
                });
                loadAgentTimer.schedule(Time.inMilliseconds(this.activeStatePeriod));
            }
        } else {
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
    }

    @Override
    final void runPhase() {
        if(!config.Configuration.isLiveRun) {
            iteration = -1;
            initPhase();
            runIteration();
        }
    }

    private void runIteration() {
        if(config.Configuration.isLiveRun) {
            // synchronizing the iterations
            if (this.isIterationAfterReorganization() && alreadyCleanedResponses == false && !inBetweenRuns) {
                // clearing previous responses
                this.reset();
                this.initIteration();
                alreadyCleanedResponses = new Boolean(true);
                if (!isLeaf()) {
                    // if the peer is an inner node, it lets gateway know if is running (not used anymore)
                    ZMQAddress dest = new ZMQAddress(MainConfiguration.getSingleton().peerZeroIP, Configuration.GateWayPort);
                    getPeer().sendMessage(dest, new InformGatewayMessage(MainConfiguration.getSingleton().peerIndex, this.activeRun, "innerRunning", isLeaf()));
                }
            }

            this.setNumComputed(0);
            this.setNumTransmitted(0);

            doIfConditionToStartNewIterationIsMet();
        }
        else{
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
        if(config.Configuration.isLiveRun) {
            if (this.isLeaf()) {
                // due to network partitions and delays, the peer needs to wait for the previous iteration to complete before moving to the next one
                // the listenForDownMessage flag takes care of this
                if (iteration == 0 && !listenForDownMessage) {
                    this.goUp();
                } else if (iteration == latestDownMessage + 1 && iteration != numIterations && !listenForDownMessage) {
                    this.goUp();
                }
            }
            this.runActiveState();
        }
        else{
            //    	this.log(Level.FINER, "IterativeTreeAgent::doIfConditionToStartNewIterationIsMet()");
            this.initIteration();
            if (this.isLeaf()) {
                this.goUp();
            }
            this.runActiveState();
        }
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
//            EventLog.logEvent("IterativeTreeAgent", "handleIncomingMessage", "upMessage received" ,String.valueOf(iteration));
            UP msg = (UP) message;
            messageBuffer.put(msg.child, msg);
            if (children.size() <= messageBuffer.size()) {
                goUp();
            }
        } else if (message instanceof DownMessage) {
//            EventLog.logEvent("IterativeTreeAgent", "handleIncomingMessage", "downMessage received" ,String.valueOf(iteration));
            if (config.Configuration.isLiveRun) {
                // checking if the last down message (iteration) is complete
                latestDownMessage = this.iteration;
            }
            goDown((DOWN) message);
            if (config.Configuration.isLiveRun) {
                iteration++;
                if (iteration == numIterations) {
                    System.out.println("EPOS finished for: " + getPeer().getNetworkAddress() + " for run: " + this.activeRun + " at iteration: " + iteration + ". Sending message now");
                    ZMQAddress dest = new ZMQAddress(MainConfiguration.getSingleton().peerZeroIP, Configuration.GateWayPort);
                    // inform gateway
                    getPeer().sendMessage(dest, new InformGatewayMessage(MainConfiguration.getSingleton().peerIndex, this.activeRun, "finished", isLeaf()));
                    // inform user
                    getPeer().sendMessage(userAddress, new InformUserMessage(MainConfiguration.getSingleton().peerIndex, this.activeRun, "finished", this.selectedPlanID));
                    // move to the next run
                    this.newRun();
                } else {
                    initIteration();
                    listenForDownMessage = new Boolean(false);
                }
            }
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
//        EventLog.logEvent("IterativeTreeAgent", "goUp", "goUp started" ,String.valueOf(iteration));
        if(config.Configuration.isLiveRun){
            // up message is being sent, get ready to receive the down message
            listenForDownMessage = new Boolean(true);
        }

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
//                System.out.println("----------");
//                System.out.println("I am the root, going down "+getPeer().getNetworkAddress()+" at iteration: "+getIteration());
//                System.out.println("----------");
                goDown(atRoot(msg));
                if(config.Configuration.isLiveRun) {
                    iteration++;
                    if (iteration == numIterations) {
                        System.out.println("EPOS finished for: " + getPeer().getNetworkAddress() + " for run: " + this.activeRun + " at iteration: " + iteration + ". Sending message now");
                        ZMQAddress dest = new ZMQAddress(MainConfiguration.getSingleton().peerZeroIP, Configuration.GateWayPort);
                        // inform gateway
                        getPeer().sendMessage(dest, new InformGatewayMessage(MainConfiguration.getSingleton().peerIndex, this.activeRun, "finished", isLeaf()));
                        // inform user
                        getPeer().sendMessage(userAddress, new InformUserMessage(MainConfiguration.getSingleton().peerIndex, this.activeRun, "finished", this.selectedPlanID));
                        // move to the next run
                        this.newRun();
                    } else {
                        initIteration();
                    }
                }
            } else {
                msg.numAgents = numAgents;
                msg.cumTransmitted = this.getCumTransmitted();
                msg.cumComputed = this.getCumComputed();
                this.setNumTransmitted(this.getNumTransmitted() + msg.getNumTransmitted());
                this.setCumTransmitted(this.getCumTransmitted() + msg.getNumTransmitted());
//                System.out.println("sending the up message, I am: "+this.getPeer().getNetworkAddress()+" at iteration: "+iteration+" message goes to: "+parent.getNetworkAddress());
                getPeer().sendMessage(parent.getNetworkAddress(), msg);
//                EventLog.logEvent("IterativeTreeAgent", "goUp", "upMessage send" ,String.valueOf(iteration));
            }
//        EventLog.logEvent("IterativeTreeAgent", "goUp", "goUp ended" ,String.valueOf(iteration));
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
//        EventLog.logEvent("IterativeTreeAgent", "goDown", "goDown started" ,String.valueOf(iteration));
        if (!isRoot()) {
//            System.out.println("going down, I am: "+getPeer().getNetworkAddress()+" "+getIteration());
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
//            EventLog.logEvent("IterativeTreeAgent", "goDown", "downMessage send" ,String.valueOf(iteration));
        }
        this.finalizeDownPhase(parentMsg);
//        EventLog.logEvent("IterativeTreeAgent", "goDown", "goDown ended" ,String.valueOf(iteration));
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

    static abstract class UpMessage extends Message implements Serializable {

        public Finger child;
        public int numAgents;
        public int cumTransmitted;
        public int cumComputed;

        public abstract int getNumTransmitted();
    }

    static abstract class DownMessage extends Message implements Serializable {

        public int numAgents;
        public int cumTransmitted;
        public int cumComputed;

        public abstract int getNumTransmitted();
    }

    @Override
    public void log(Level level, String message) {
    	this.logger.log(level, "NODE: " + this.getPeer().getIndexNumber() + ", iter: " + this.iteration + ", " + message);
    }

    public void newRun(){
        // resetting parameters for a new run
        inBetweenRuns = new Boolean(true);
        readyToRun = new Boolean(false);
        iteration =0;
        alreadyCleanedResponses = new Boolean(false);
        latestDownMessage = -10;
        activeRun++;
        treeViewIsSet = new Boolean(false);

        Timer loadAgentTimer = getPeer().getClock().createNewTimer();
        loadAgentTimer.addTimerListener((Timer timer) -> {
            // check to see if the user is leaving
            checkForUserChanges();
            // check for plan changes
            checkForNewPlans();
            // check for new weights
            checkForNewWeights();
            System.out.println("----------");
            System.out.println("run "+this.activeRun +" started for: "+getPeer().getNetworkAddress());
            System.out.println("----------");
            this.runBootstrap();
        });
        loadAgentTimer.schedule(Time.inMilliseconds(readyPeriod+4000));
    }

    public void checkForUserChanges(){
//        System.out.println("checking for user changes for : "+getPeer().getNetworkAddress());
        getPeer().sendMessage(GatewayAddress, new InformGatewayMessage(MainConfiguration.getSingleton().peerIndex, this.activeRun, "checkUserChanges", isLeaf()));
    }

    public void checkForNewPlans(){
        if (plansAreSet == false){
//        System.out.println("checking for new plans for: "+getPeer().getNetworkAddress());
        getPeer().sendMessage(userAddress, new InformUserMessage(MainConfiguration.getSingleton().peerIndex, this.activeRun, "checkNewPlans"));}
    }

    // over ride method in multiObjectiveAgent
    abstract void checkForNewWeights();
}
