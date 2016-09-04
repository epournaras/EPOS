/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package agent;

import agent.IterativeTreeAgent.DownMessage;
import agent.IterativeTreeAgent.UpMessage;
import agent.logging.AgentLoggingProvider;
import data.Plan;
import data.Value;
import func.CostFunction;
import func.PlanCostFunction;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author Peter
 */
public class EposAgent<V extends Value<V>> extends IterativeTreeAgent<V, EposAgent.EposUp<V>, EposAgent.EposDown<V>> {

    // agent info
    List<V> possibleValues;
    V aggregatedResponse;
    private List<Integer> childSelections;

    // misc
    Optimization optimization;

    public EposAgent(List<Plan<V>> possiblePlans, CostFunction<V> globalCostFunc, PlanCostFunction<V> localCostFunc, AgentLoggingProvider<? extends IterativeTreeAgent<V, EposUp<V>, EposDown<V>>> loggingProvider, long seed) {
        super(1, possiblePlans, globalCostFunc, localCostFunc, loggingProvider, seed);
        optimization = new Optimization(random);
    }

    @Override
    void initPhase() {
        possibleValues = possiblePlans.stream().map(plan -> plan.getValue()).collect(Collectors.toList());
    }

    @Override
    void initIteration() {

    }

    @Override
    EposUp<V> up(List<EposUp<V>> childMsgs) {
        if(childMsgs.isEmpty()) {
            aggregatedResponse = createValue();
        } else {
            aggregatedResponse = childMsgs.stream().map(msg -> msg.aggregatedResponse).reduce(createValue(), (a, b) -> {
                a.add(b);
                return a;
            });

            List<List<V>> childPlans = childMsgs.stream().sequential().map(msg -> msg.possiblePlans).collect(Collectors.toList());
            List<V> combinations = optimization.calcAllCombinations(childPlans);
            int selectedCombination = optimization.argmin(globalCostFunc, combinations, aggregatedResponse);
            numComputed += combinations.size();
            childSelections = optimization.combinationToSelections(selectedCombination, childPlans);

            aggregatedResponse.add(combinations.get(selectedCombination));
        }
        return new EposUp<>(possibleValues, aggregatedResponse);
    }

    @Override
    EposDown<V> atRoot(EposUp<V> rootMsg) {
        int selection = optimization.argmin(globalCostFunc, possibleValues, aggregatedResponse);
        numComputed += possiblePlans.size();
        selectedPlan = possiblePlans.get(selection);

        globalResponse = aggregatedResponse.cloneThis();
        globalResponse.add(selectedPlan.getValue());

        return new EposDown<>(globalResponse, selection);
    }

    @Override
    List<EposDown<V>> down(EposDown<V> parentMsg) {
        globalResponse = parentMsg.globalResponse;
        selectedPlan = possiblePlans.get(parentMsg.selection);

        List<EposDown<V>> msgs = new ArrayList<>();
        for (Integer childSelection : childSelections) {
            msgs.add(new EposDown<>(globalResponse, childSelection));
        }
        return msgs;
    }

    static class EposUp<V> extends UpMessage {

        List<V> possiblePlans;
        V aggregatedResponse;

        public EposUp(List<V> possiblePlans, V aggregatedResponse) {
            this.possiblePlans = possiblePlans;
            this.aggregatedResponse = aggregatedResponse;
        }

        @Override
        public int getNumTransmitted() {
            return 1 + possiblePlans.size();
        }
    }

    static class EposDown<V> extends DownMessage {

        V globalResponse;
        int selection;

        public EposDown(V globalResponse, int selection) {
            this.globalResponse = globalResponse;
            this.selection = selection;
        }

        @Override
        public int getNumTransmitted() {
            return 0; // top-down-phase not necessary to find a solution
        }

    }
}
