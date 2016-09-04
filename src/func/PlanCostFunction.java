/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package func;

import data.Plan;
import data.Value;

/**
 *
 * @author Peter
 */
public interface PlanCostFunction<V extends Value<V>> {

    public double calcCost(Plan<V> plan);

    public String getMetric();
}
