/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package agent;

import data.Vector;
import func.DifferentiableCostFunction;
import func.DotCostFunction;

/**
 *
 * @author Peter
 */
public class IeposAdaptiveGradientPlanSelector implements PlanSelector<IeposAgent<Vector>, Vector>{

    private PlanSelector<? super IeposAgent<Vector>, Vector> initialPlanSelector;
    private DotCostFunction costFunc = new DotCostFunction();
    
    public IeposAdaptiveGradientPlanSelector() {
        this.initialPlanSelector = new IeposPlanSelector<>();
    }
    
    @Override
    public int selectPlan(IeposAgent<Vector> agent) {
        if (agent.iteration == 0) {
            return initialPlanSelector.selectPlan(agent);
        } else {
            DifferentiableCostFunction<Vector> gradientFunction = (DifferentiableCostFunction<Vector>) agent.globalCostFunc;

            Vector otherResponse = agent.globalResponse.cloneThis();
            otherResponse.subtract(agent.prevSelectedPlan.getValue());
            otherResponse.subtract(agent.prevAggregatedResponse);
            otherResponse.add(agent.aggregatedResponse);
            otherResponse.multiply(agent.numAgents / (agent.numAgents - 1));

            Vector gradient = gradientFunction.calcGradient(otherResponse);
            costFunc.setCostVector(gradient);

            return agent.optimization.argmin(costFunc, agent.possiblePlans, agent.lambda);
        }
    }

    @Override
    public int getNumComputations(IeposAgent<Vector> agent) {
        return agent.possiblePlans.size();
    }
    
}
