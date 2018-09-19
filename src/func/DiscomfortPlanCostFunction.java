package func;

import data.Plan;

/**
 * Local cost of a plan is its discomfort score.
 * 
 * @author jovan
 *
 */
public class DiscomfortPlanCostFunction implements PlanCostFunction {

	@Override
	public double calcCost(Plan plan) {
		if(Double.isNaN(plan.getScore())) {
			return 0.0;
		} else {
			return plan.getScore();
		}		
	}
	
	@Override
    public String toString() {
        return "discomfort local cost function";
    }

}
