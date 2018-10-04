
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package func;

import data.Plan;

/**
 * Uses the score of a plan as it's cost value.
 *
 * @author Peter P. & Jovan N.
 */
public class PlanDiscomfortFunction implements PlanCostFunction {

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
        return "score as a cost plan cost function.";
    }
    
    @Override
    public String getLabel() {
    		return "DISC";
    }
}
