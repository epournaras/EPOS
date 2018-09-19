package func;

import data.Plan;

/**
 * 
 * This local (plan) cost function should be used when scores
 * in the dataset denote preference that should be maximized,
 * and not discomfort that should be minimized.
 * 
 * Therefore, this function transforms preference score into
 * discomfort score.
 * 
 * @author jovan
 *
 */
public class PreferencePlanCostFunction implements PlanCostFunction  {

	@Override
	public double calcCost(Plan plan) {
		if(Double.isNaN(plan.getScore())) {
			return 0.0;
		} else {
			return 1 - plan.getScore();
		}		
	}
	
	@Override
    public String toString() {
        return "preference local cost function";
    }

}
