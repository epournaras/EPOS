/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package func;

import data.Plan;
import data.DataType;

/**
 * A function that assigns a cost value to a given plan.
 * 
 * @author Peter
 * @param <V> the type of the data this cost function should handle
 */
public interface PlanCostFunction<V extends DataType<V>> {

    public double calcCost(Plan<V> plan);
}
