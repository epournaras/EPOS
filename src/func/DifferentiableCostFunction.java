/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package func;

import data.DataType;

/**
 *
 * @author Peter
 */
public abstract class DifferentiableCostFunction<V extends DataType<V>> extends CostFunction<V> {
    public abstract V calcGradient(V value);
}
