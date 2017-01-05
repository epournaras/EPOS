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
import java.util.Collections;
import java.util.List;
import data.DataType;

/**
 * This agent performs the I-EPOS algorithm for combinatorial optimization.
 *
 * @author Peter
 * @param <V> the type of the data this agent should handle
 */
public class IeposAgent<V extends DataType<V>> extends IterativeTreeAgent<V, IeposAgent<V>.UpMessage, IeposAgent<V>.DownMessage> {

    // agent info
    Plan<V> prevSelectedPlan;
    V aggregatedResponse;
    V prevAggregatedResponse;

    // per child info
    private final List<V> subtreeResponses = new ArrayList<>();
    private final List<V> prevSubtreeResponses = new ArrayList<>();
    private final List<Boolean> approvals = new ArrayList<>();

    // misc
    Optimization optimization;
    double lambda; // parameter for lambda-PREF local cost minimization
    private PlanSelector<IeposAgent<V>, V> planSelector;

    /**
     * Creates a new IeposAgent. Using the same RNG seed will result in the same
     * execution order in a simulation environment.
     *
     * @param numIterations the number of iterations
     * @param possiblePlans the plans this agent can choose from
     * @param globalCostFunc the global cost function
     * @param localCostFunc the local cost function
     * @param loggingProvider the object that extracts data from the agent and
     * writes it into its log.
     * @param seed a seed for the RNG
     */
    public IeposAgent(int numIterations, List<Plan<V>> possiblePlans, CostFunction<V> globalCostFunc, PlanCostFunction<V> localCostFunc, AgentLoggingProvider<? extends IeposAgent<V>> loggingProvider, long seed) {
        super(numIterations, possiblePlans, globalCostFunc, localCostFunc, loggingProvider, seed);
        this.optimization = new Optimization(random);
        this.lambda = 0;
        this.planSelector = new IeposPlanSelector<>();
    }

    /**
     * Sets lambda, the traidoff between global and local cost minimization. A
     * value of 0 indicates pure global cost minimization, while a value of 1
     * indicates pure local cost minimization.
     *
     * @param lambda traidoff between global and local cost minimization
     */
    public void setLambda(double lambda) {
        this.lambda = lambda;
    }

    /**
     * An I-EPOS agent can have different strategies for plan selection. The
     * plan selector decides which plan to select given the current state of the
     * system.
     *
     * @param planSelector the plan selector
     */
    public void setPlanSelector(PlanSelector<IeposAgent<V>, V> planSelector) {
        this.planSelector = planSelector;
    }

    public V getGlobalResponse() {
        return globalResponse.cloneThis();
    }

    @Override
    void initPhase() {
        aggregatedResponse = createValue();
        prevAggregatedResponse = createValue();
        globalResponse = createValue();
        prevSelectedPlan = createPlan();
    }

    @Override
    void initIteration() {
        if (iteration > 0) {
            prevSelectedPlan = selectedPlan;
            prevAggregatedResponse.set(aggregatedResponse);
            prevSubtreeResponses.clear();
            prevSubtreeResponses.addAll(subtreeResponses);

            selectedPlan = null;
            aggregatedResponse.reset();
            subtreeResponses.clear();
            approvals.clear();
        }
    }

    @Override
    UpMessage up(List<UpMessage> childMsgs) {
        for (UpMessage msg : childMsgs) {
            subtreeResponses.add(msg.subtreeResponse);
        }
        aggregate();
        selectPlan();
        return informParent();
    }

    @Override
    DownMessage atRoot(UpMessage rootMsg) {
        return new DownMessage(rootMsg.subtreeResponse, true);
    }

    @Override
    List<DownMessage> down(DownMessage parentMsg) {
        updateGlobalResponse(parentMsg);
        approveOrRejectChanges(parentMsg);
        return informChildren();
    }

    private void aggregate() {
        if (iteration == 0) {
            for (int i = 0; i < children.size(); i++) {
                approvals.add(true);
            }
        } else if (children.size() > 0) {
            List<List<V>> choicesPerAgent = new ArrayList<>();
            for (int i = 0; i < children.size(); i++) {
                List<V> choices = new ArrayList<>();
                choices.add(prevSubtreeResponses.get(i));
                choices.add(subtreeResponses.get(i));
                choicesPerAgent.add(choices);
            }
            List<V> combinations = optimization.calcAllCombinations(choicesPerAgent);

            V othersResponse = globalResponse.cloneThis();
            for (V prevSubtreeResponce : prevSubtreeResponses) {
                othersResponse.subtract(prevSubtreeResponce);
            }
            int selectedCombination = optimization.argmin(globalCostFunc, combinations, othersResponse);
            numComputed += combinations.size();

            List<Integer> selections = optimization.combinationToSelections(selectedCombination, choicesPerAgent);
            for (int selection : selections) {
                approvals.add(selection == 1);
            }
        }
        for (int i = 0; i < children.size(); i++) {
            V prelSubtreeResponse = approvals.get(i) ? subtreeResponses.get(i) : prevSubtreeResponses.get(i);
            subtreeResponses.set(i, prelSubtreeResponse);
            aggregatedResponse.add(prelSubtreeResponse);
        }
    }

    void selectPlan() {
        int selected = planSelector.selectPlan(this);
        numComputed += planSelector.getNumComputations(this);
        selectedPlan = possiblePlans.get(selected);
    }

    private UpMessage informParent() {
        V subtreeResponse = aggregatedResponse.cloneThis();
        subtreeResponse.add(selectedPlan.getValue());
        return new UpMessage(subtreeResponse);
    }

    private void updateGlobalResponse(DownMessage parentMsg) {
        globalResponse.set(parentMsg.globalResponse);
    }

    private void approveOrRejectChanges(DownMessage parentMsg) {
        if (!parentMsg.approved) {
            selectedPlan = prevSelectedPlan;
            aggregatedResponse.set(prevAggregatedResponse);
            subtreeResponses.clear();
            subtreeResponses.addAll(prevSubtreeResponses);
            Collections.fill(approvals, false);
        }
    }

    private List<DownMessage> informChildren() {
        List<DownMessage> msgs = new ArrayList<>();
        for (int i = 0; i < children.size(); i++) {
            msgs.add(new DownMessage(globalResponse, approvals.get(i)));
        }
        return msgs;
    }

    // message classes
    class UpMessage extends IterativeTreeAgent.UpMessage {

        public V subtreeResponse;

        public UpMessage(V subtreeResponse) {
            this.subtreeResponse = subtreeResponse;
        }

        @Override
        public int getNumTransmitted() {
            return 1;
        }
    }

    class DownMessage extends IterativeTreeAgent.DownMessage {

        public V globalResponse;
        public boolean approved;

        public DownMessage(V globalResponse, boolean approved) {
            this.globalResponse = globalResponse;
            this.approved = approved;
        }

        @Override
        public int getNumTransmitted() {
            return 1;
        }
    }
}
