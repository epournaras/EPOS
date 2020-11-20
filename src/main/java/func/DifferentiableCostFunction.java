/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package func;

import data.DataType;

/**
 * A cost function that assigns a cost value to any given data instance. Furthermore,
 * the gradient of the cost function can be computed, indicating the direction
 * of the highest cost increase/decrease.
 *
 * @author Peter, Thomas Asikis
 * @param <V> the type of the data this cost function should handle. Preferably a numerical structure like a vector
 */
public interface DifferentiableCostFunction<V extends DataType<V>> extends CostFunction<V> {
	
	/**
	 * This method returns the gradient of the cost function
	 * in regards to the input value. For numerical inputs, the
	 * gradient is a numeric structure, e.g. a vector, the it returns a vector
	 * with each element containing the partial derivative in regards to it.
	 * @param value
	 * @return
	 */
    public abstract V calcGradient(V value);
}
