/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package agent;

import data.DataType;

/**
 *
 * @author Peter
 */
public interface PlanSelector<A extends Agent<V>, V extends DataType<V>> {
    public int selectPlan(A agent);
    public int getNumComputations(A agent);
}
