/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package func;

import data.DataType;

/**
 * A function that assigns a cost value to any given data instance. Furthermore,
 * the gradient of the cost function can be computed, indicating the direction
 * of the largest cost increase.
 *
 * @author Peter
 * @param <V> the type of the data this cost function should handle
 */
public abstract class DifferentiableCostFunction<V extends DataType<V>> extends CostFunction<V> {

    public abstract V calcGradient(V value);
}
