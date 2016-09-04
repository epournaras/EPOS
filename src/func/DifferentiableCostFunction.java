/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package func;

import data.Value;
import data.Vector;

/**
 *
 * @author Peter
 */
public abstract class DifferentiableCostFunction<V extends Value<V>> extends CostFunction<V> {
    public abstract V calcGradient(V value);
}
