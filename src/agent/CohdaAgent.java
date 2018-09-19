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
package agent;

import agent.logging.AgentLoggingProvider;
import data.Plan;
import func.CostFunction;
import func.PlanCostFunction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import protopeer.Finger;
import protopeer.network.Message;
import protopeer.network.NetworkAddress;
import protopeer.time.Timer;
import protopeer.util.quantities.Time;
import data.DataType;

/**
 * This agent performs the COHDA algorithm for combinatorial optimization.
 * Note that this algorithm was developed as a baseline algorithm and does not
 * support all the features that IeposAgent does. For example, COHDA does not
 * support local cost minimization in this implementation.
 * 
 * @author Peter
 * @param <V> the type of the data this agent should handle
 */
public class CohdaAgent<V extends DataType<V>> extends TreeAgent<V> {

    private int age;
    private int step;
    private int numSteps;
    private KnowledgeBase<V> best;
    private KnowledgeBase<V> current;
    private boolean somethingChanged;

    // network
    private List<Finger> neighbours = new ArrayList<>();

    // misc
    private Optimization optimization;

    private V zero;

    @Override
    public int getIteration() {
        return step;
    }

    @Override
    public int getNumIterations() {
        return numSteps;
    }

    @Override
    public V getGlobalResponse() {
        return best.global();
    }

    /**
     * Creates a new CohdaAgent. Using the same RNG seed will result in the same
     * execution order in a simulation environment.
     *
     * @param numSteps the number of steps
     * @param possiblePlans the plans this agent can choose from
     * @param globalCostFunc the global cost function
     * @param localCost the local cost function
     * @param loggingProvider the object that extracts data from the agent and
     * writes it into its log.
     * @param seed a seed for the RNG
     */
    public CohdaAgent(int numSteps, List<Plan<V>> possiblePlans, CostFunction<V> globalCostFunc, PlanCostFunction<V> localCost, AgentLoggingProvider<? extends TreeAgent<V>> loggingProvider, long seed) {
        super(possiblePlans, globalCostFunc, localCost, loggingProvider, seed);
        this.numSteps = numSteps;
        this.step = numSteps;
        this.optimization = new Optimization(random);
    }

    @Override
    void runActiveState() {
        if (step < numSteps - 1) {
            Timer loadAgentTimer = getPeer().getClock().createNewTimer();
            loadAgentTimer.addTimerListener((Timer timer) -> {
                runStep();
                runActiveState();
            });
            loadAgentTimer.schedule(Time.inMilliseconds(1000));
        } else {
            super.runActiveState();
        }
    }

    @Override
    final void runPhase() {
        step = -1;

        initPhase();
        runStep();
    }

    private void runStep() {
        step++;

        if (step < numSteps) {
            initStep();
            if (somethingChanged) {
                publish();
            }
        }
    }

    private void initPhase() {
        neighbours.clear();
        neighbours.addAll(children);
        if (parent != null) {
            neighbours.add(parent);
        }
        zero = createValue();
        current = new KnowledgeBase<>();
        best = new KnowledgeBase<>();
        age = 0;
        somethingChanged = true;

        update(null);
    }

    private void initStep() {
    	this.setNumComputed(0);
    	this.setNumTransmitted(0);
    	this.setCumComputed(0);
    	this.setCumTransmitted(0);
    }

    @Override
    public void handleIncomingMessage(Message message) {
        if (CohdaMessage.class.equals(message.getClass())) {
            CohdaMessage msg = (CohdaMessage) message;
            this.setNumTransmitted(msg.getNumTransmitted() + this.getNumTransmitted());
            this.setCumTransmitted(this.getCumTransmitted() + msg.getNumTransmitted() + Math.max(this.getCumTransmitted(), msg.cumTransmitted));
            this.setCumComputed(Math.max(this.getCumComputed(), msg.cumComputed));
            update(msg);
        }
    }

    private void update(CohdaMessage msg) {
        somethingChanged = false;
        if (msg == null) {
            somethingChanged = true;
        } else {
            somethingChanged = current.updateWith(msg.current);
            if (betterThanBest(msg.best)) {
                best = msg.best;
                somethingChanged = true;
            }
        }
        if (somethingChanged) {
            choose();
        }
    }

    private void choose() {
        int selected = optimization.argmin(globalCostFunc, possiblePlans, current.aggregate(this));
        this.setNumComputed(this.getNumComputed() + possiblePlans.size());
        this.setCumComputed(this.getCumComputed() + possiblePlans.size());

        Plan<V> selectedPlan = possiblePlans.get(selected);
        if (selectedPlan.equals(current.getLocal(this)) && current.size() <= best.size()) {
            current = new KnowledgeBase(best);
            selectedPlan = current.getLocal(this);
        }
        this.selectedPlan = selectedPlan;
        current.updateLocal(this,selectedPlan);

        if (betterThanBest(current)) {
            best = new KnowledgeBase(current);
        }
    }

    private void publish() {
        for (Finger neighbour : neighbours) {
            CohdaMessage msg = new CohdaMessage();
            msg.best = new KnowledgeBase(best);
            msg.current = new KnowledgeBase(current);
            msg.cumTransmitted = this.getCumTransmitted();
            msg.cumComputed = this.getCumComputed();
            this.setNumTransmitted(this.getNumTransmitted() + msg.getNumTransmitted());
            this.setCumTransmitted(this.getCumTransmitted() + msg.getNumTransmitted());
            getPeer().sendMessage(neighbour.getNetworkAddress(), msg);
        }
    }

    private boolean betterThanBest(KnowledgeBase<V> other) {
        if (best.size() < other.size()) {
            return true;
        } else if (best.size() == other.size()) {
            return globalCostFunc.calcCost(best.global()) > globalCostFunc.calcCost(other.global());
        } else {
            return false;
        }
    }

    private static class KnowledgeBase<V extends DataType<V>> {

        private Map<NetworkAddress, Weight<V>> weights = new HashMap<>();
        private V global;

        public KnowledgeBase() {
        }

        public KnowledgeBase(KnowledgeBase other) {
            set(other);
        }

        public void set(KnowledgeBase<V> newKb) {
            weights.clear();
            weights.putAll(newKb.weights);
            global = newKb.global;
        }

        public boolean updateWith(KnowledgeBase<V> other) {
            boolean changed = false;
            global = null;

            for (NetworkAddress agent : other.weights.keySet()) {
                Weight weight = weights.get(agent);
                Weight otherWeight = other.weights.get(agent);

                if (weight == null || weight.age < otherWeight.age) {
                    weights.put(agent, otherWeight);
                    changed = true;
                }
            }

            return changed;
        }

        public void updateLocal(CohdaAgent agent, Plan<V> newPlan) {
            global = null;
            NetworkAddress key = agent.getPeer().getNetworkAddress();

            Weight<V> prevWeight = weights.get(key);

            Weight<V> newWeight = new Weight();
            if (prevWeight != null) {
                newWeight.age = agent.age++;
            }
            newWeight.weight = newPlan;

            weights.put(key, newWeight);
        }

        public Plan<V> getLocal(CohdaAgent agent) {
            Weight<V> weight = weights.get(agent.getPeer().getNetworkAddress());
            if (weight != null) {
                return weight.weight;
            } else {
                return null;
            }
        }

        public V global() {
            if (global == null) {
                V newVal = weights.values().iterator().next().weight.getValue().cloneNew();
                global = weights.values().stream()
                        .map(x -> x.weight.getValue())
                        .reduce(newVal, (x, y) -> {
                            x.add(y);
                            return x;
                        });
            }
            return global;
        }

        public V aggregate(CohdaAgent<V> agent) {
            V aggregate = weights.entrySet().stream()
                    .filter(x -> !agent.getPeer().getNetworkAddress().equals(x.getKey()))
                    .map(x -> x.getValue().weight.getValue())
                    .reduce(agent.createValue(), (x, y) -> {
                        x.add(y);
                        return x;
                    });
            return aggregate;
        }

        public int size() {
            return weights.size();
        }
    }

    private static class Weight<V extends DataType<V>> {

        public Plan<V> weight;
        public int age;
    }

    private final class CohdaMessage extends Message {

        public KnowledgeBase best;
        public KnowledgeBase current;
        public int cumComputed;
        public int cumTransmitted;

        public int getNumTransmitted() {
            return best.size() + current.size();
        }
    }
}
