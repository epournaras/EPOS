
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
 * @author Peter
 */
public class PlanScoreCostFunction implements PlanCostFunction {

    @Override
    public double calcCost(Plan plan) {
        return plan.getScore();
    }

    @Override
    public String toString() {
        return "score";
    }
}
