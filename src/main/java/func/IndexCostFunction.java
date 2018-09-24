/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package func;

import data.Plan;

/**
 * Uses the index of a plan as it's cost. E.g. the plan with index 0 has 0 cost,
 * while the plan with index 1 has cost 1.
 *
 * @author Peter
 */
public class IndexCostFunction implements PlanCostFunction {

    @Override
    public double calcCost(Plan plan) {
        return plan.getIndex();
    }

    @Override
    public String toString() {
        return "index";
    }

}
