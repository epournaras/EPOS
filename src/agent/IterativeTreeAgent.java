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
 * @author Peter
 * @param <V> the type of the data this agent should handle
 * @param <UP> the type of message for the bottom-up phase
 * @param <DOWN> the type of message for the top-down phase
 */
public abstract class IterativeTreeAgent<V extends DataType<V>, UP extends IterativeTreeAgent.UpMessage, DOWN extends IterativeTreeAgent.DownMessage> extends TreeAgent<V> {

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
                runActiveState();
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

        numTransmitted = 0;
        numComputed = 0;

        if (iteration < numIterations) {
            initIteration();
            if (isLeaf()) {
                goUp();
            }
        }
    }

    @Override
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

    private void goUp() {
        List<UP> orderedMsgs = new ArrayList<>();
        for (Finger child : children) {
            orderedMsgs.add(messageBuffer.get(child));
        }
        messageBuffer.clear();

        if (iteration == 0) {
            numAgents = 1 + orderedMsgs.stream().map(msg -> msg.numAgents).reduce(0, (a, b) -> a + b);
        }
        numTransmitted = orderedMsgs.stream().map(msg -> msg.getNumTransmitted()).reduce(0, (a, b) -> a + b);
        numComputed = 0;
        cumTransmitted = numTransmitted + orderedMsgs.stream().map(msg -> msg.cumTransmitted).reduce(0, (a, b) -> Math.max(a, b));
        cumComputed = orderedMsgs.stream().map(msg -> msg.cumComputed).reduce(0, (a, b) -> Math.max(a, b));

        cumComputed -= numComputed;
        UP msg = up(orderedMsgs);
        cumComputed += numComputed;

        msg.child = getPeer().getFinger();
        if (isRoot()) {
            goDown(atRoot(msg));
        } else {
            msg.numAgents = numAgents;
            msg.cumTransmitted = cumTransmitted;
            msg.cumComputed = cumComputed;
            numTransmitted += msg.getNumTransmitted();
            cumTransmitted += msg.getNumTransmitted();
            getPeer().sendMessage(parent.getNetworkAddress(), msg);
        }
    }

    private void goDown(DOWN parentMsg) {
        if (!isRoot()) {
            numAgents = parentMsg.numAgents;
            numTransmitted += parentMsg.getNumTransmitted();
            cumTransmitted = parentMsg.getNumTransmitted() + parentMsg.cumTransmitted;
            cumComputed = parentMsg.cumComputed;
        }

        cumComputed -= numComputed;
        List<DOWN> msgs = down(parentMsg);
        cumComputed += numComputed;

        for (int i = 0; i < msgs.size(); i++) {
            DOWN msg = msgs.get(i);
            msg.numAgents = numAgents;
            msg.cumTransmitted = cumTransmitted;
            msg.cumComputed = cumComputed;
            numTransmitted += msg.getNumTransmitted();
            cumTransmitted += msg.getNumTransmitted();
            getPeer().sendMessage(children.get(i).getNetworkAddress(), msg);
        }
    }

    abstract void initPhase();

    abstract void initIteration();

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
}
