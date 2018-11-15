package agent.planselection;

import agent.MultiObjectiveIEPOSAgent;
import agent.PlanSelector;
import data.DataType;

/**
 * 
 * 
 * @author jovan
 *
 * @param <V>
 */
public class MultiObjectiveIeposPlanSelector<V extends DataType<V>> implements PlanSelector<MultiObjectiveIEPOSAgent<V>, V> {

    @Override
    public int selectPlan(MultiObjectiveIEPOSAgent<V> agent) {
        V otherResponse = agent.getGlobalResponse().cloneThis();
        otherResponse.subtract(agent.getPrevSelectedPlan().getValue());
        otherResponse.subtract(agent.getPrevAggregatedResponse());
        otherResponse.add(agent.getAggregatedResponse());
        
        double score = agent.getLocalCostFunction().calcCost(agent.getPrevSelectedPlan());
//        if(((Vector)agent.getPrevSelectedPlan().getValue()).sum() == 0.0) {
//        	score = 0.0;
//        } else {
//        	score = agent.getLocalCostFunction().calcCost(agent.getPrevSelectedPlan());
//        }
        
        double currentDiscomfortSum = agent.getGlobalDiscomfortSum() - 
        							  agent.getPrevAggregatedDiscomfortSum() + 
        							  agent.getAggregatedDiscomfortSum() - 
        							  score;
        
//        agent.log(Level.FINER, "globalDiscomfortSum(" + agent.getGlobalDiscomfortSum() + ")" + 
//        						" - prevAggDiscomfortSum(" + agent.getPrevAggregatedDiscomfortSum() + ")" +
//        						" + aggDiscomfortSum(" + agent.getAggregatedDiscomfortSum() + ")" + 
//        						" - prevPlanDiscomfort(" + score + ") = " +
//        						currentDiscomfortSum);
        
        double currentDiscomfortSumSqr = agent.getGlobalDiscomfortSumSqr() - 
        								 agent.getPrevAggregatedDiscomfortSumSqr() +
        								 agent.getAggregatedDiscomfortSumSqr() -
        								 score*score;
        
//        System.out.println("agent: " + agent.getPeer().getIndexNumber() + " globalDiscomfortSum(" + agent.getGlobalDiscomfortSum() + ")" + 
//				" - prevAggDiscomfortSum(" + agent.getPrevAggregatedDiscomfortSum() + ")" +
//				" + aggDiscomfortSum(" + agent.getAggregatedDiscomfortSum() + ")" + 
//				" - prevPlanDiscomfort(" + score + ") = " +
//				currentDiscomfortSum);
//        
//        System.out.println("agent: " + agent.getPeer().getIndexNumber() + " globalDiscomfortSumSqr(" + agent.getGlobalDiscomfortSumSqr() + ")" + 
//				" - prevAggDiscomfortSumSqr(" + agent.getPrevAggregatedDiscomfortSumSqr() + ")" +
//				" + aggDiscomfortSumSqr(" + agent.getAggregatedDiscomfortSumSqr() + ")" + 
//				" - prevPlanDiscomfortSqr(" + score*score + ") = " +
//				currentDiscomfortSumSqr);

        int id =  agent.getOptimization().argmin(agent.getGlobalCostFunction(), 
        									  agent.getLocalCostFunction(),
        									  agent.getPossiblePlans(), 
        									  otherResponse, 
        									  agent.getUnfairnessWeight(),
        									  agent.getLocalCostWeight(),
        									  currentDiscomfortSum,
        									  currentDiscomfortSumSqr,
        									  agent.getNumAgents(),
        									  agent);
        
//        System.out.println("agent: " + agent.getPeer().getIndexNumber() + " Chosen id is: " + id);
        
        return id;
    }

    @Override
    public int getNumComputations(MultiObjectiveIEPOSAgent<V> agent) {
        return agent.getPossiblePlans().size();
    }

}
