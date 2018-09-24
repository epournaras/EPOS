/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package func;

import data.Vector;

/**
 * Computes the dot product of a cost vector and the given value.
 * 
 * @author Peter
 */
public class DotCostFunction extends DifferentiableCostFunction<Vector> {
    private Vector costVector;
    
    /**
     * Sets the cost vector.
     * @param costVector the cost vector
     */
    public void setCostVector(Vector costVector) {
        this.costVector = costVector;
    }

    @Override
    public Vector calcGradient(Vector value) {
        return costVector;
    }

    @Override
    public double calcCost(Vector value) {
        return costVector.dot(value);
    }

    @Override
    public String toString() {
        return "dot product";
    }
    
}
