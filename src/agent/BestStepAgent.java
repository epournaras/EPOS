/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package agent;

import agent.IterativeTreeAgent.UpMessage;
import agent.IterativeTreeAgent.DownMessage;
import agent.logging.AgentLoggingProvider;
import data.Plan;
import func.CostFunction;
import func.PlanCostFunction;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import protopeer.Finger;
import protopeer.network.NetworkAddress;
import data.DataType;

/**
 *
 * @author Peter
 */
public class BestStepAgent<V extends DataType<V>> extends IterativeTreeAgent<V, BestStepAgent.Up<V>, BestStepAgent.Down<V>> {

    Plan<V> prevSelectedPlan;
    private int selection;
    private Optimization optimization;

    public BestStepAgent(int numIterations, List<Plan<V>> possiblePlans, CostFunction<V> globalCostFunc, PlanCostFunction<V> localCostFunc, AgentLoggingProvider<? extends IterativeTreeAgent<V, Up<V>, Down<V>>> loggingProvider, long seed) {
        super(numIterations, possiblePlans, globalCostFunc, localCostFunc, loggingProvider, seed);
        optimization = new Optimization(random);
    }

    @Override
    void initPhase() {
        selectedPlan = possiblePlans.get(0);
        prevSelectedPlan = createPlan();
        globalResponse = createValue();
    }

    @Override
    void initIteration() {
        prevSelectedPlan = selectedPlan;
    }

    @Override
    Up<V> up(List<Up<V>> childMsgs) {
        if (iteration == 0) {
            V aggregatedResponse = childMsgs.stream().map(msg -> msg.bestChange).reduce(createValue(), (a, b) -> {
                ((V) a).add(b);
                return a;
            });
            selection = optimization.argmin(globalCostFunc, possiblePlans, aggregatedResponse);
            selectedPlan = possiblePlans.get(selection);
            globalResponse = aggregatedResponse;
            globalResponse.add(possiblePlans.get(selection).getValue());
            
            /*globalResponse = childMsgs.stream().map(msg -> msg.bestChange).reduce(selectedPlan.getValue().cloneThis(), (a, b) -> {
                ((V) a).add(b);
                return a;
            });*/
            return new Up<>(globalResponse, null);
        } else {
            List<V> choices = childMsgs.stream().map(msg -> msg.bestChange).collect(Collectors.toList());
            List<V> possibleChanges = possiblePlans.stream()
                    .map(plan -> {
                        V val = plan.getValue().cloneThis();
                        val.subtract(prevSelectedPlan.getValue());
                        return val;
                    })
                    .collect(Collectors.toList());

            choices.addAll(possibleChanges);

            int selectedChoice = optimization.argmin(globalCostFunc, choices, globalResponse);
            this.setNumTransmitted(this.getNumTransmitted() + choices.size());

            V bestChange = choices.get(selectedChoice);
            NetworkAddress bestAgent;
            if (selectedChoice >= childMsgs.size()) {
                selection = selectedChoice - childMsgs.size();
                bestAgent = getPeer().getNetworkAddress();
            } else {
                bestAgent = childMsgs.get(selectedChoice).bestAgent;
            }

            return new Up<>(bestChange, bestAgent);
        }
    }

    @Override
    Down<V> atRoot(Up<V> rootMsg) {
        if (iteration == 0) {
            globalResponse = rootMsg.bestChange;
        } else {
            globalResponse = globalResponse.cloneThis();
            globalResponse.add(rootMsg.bestChange);
        }

        return new Down<>(globalResponse, rootMsg.bestAgent);
    }

    @Override
    List<Down<V>> down(Down<V> parentMsg) {
        globalResponse = parentMsg.globalResponse;
        if (getPeer().getNetworkAddress().equals(parentMsg.bestAgent)) {
            selectedPlan = possiblePlans.get(selection);
        }

        List<Down<V>> msgs = new ArrayList<>();
        for (Finger child : children) {
            msgs.add(new Down<>(parentMsg.globalResponse, parentMsg.bestAgent));
        }
        return msgs;
    }

    static class Up<V extends DataType<V>> extends UpMessage {

        V bestChange;
        NetworkAddress bestAgent;

        public Up(V bestChange, NetworkAddress bestAgent) {
            this.bestChange = bestChange;
            this.bestAgent = bestAgent;
        }

        @Override
        public int getNumTransmitted() {
            return 1;
        }
    }

    static class Down<V extends DataType<V>> extends DownMessage {

        V globalResponse;
        NetworkAddress bestAgent;

        public Down(V globalResponse, NetworkAddress bestAgent) {
            this.globalResponse = globalResponse;
            this.bestAgent = bestAgent;
        }

        @Override
        public int getNumTransmitted() {
            return 1;
        }
    }

	@Override
	void finalizeDownPhase(Down<V> parentMsg) {	}
}
