/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package func;

import config.Configuration;
import data.Vector;

/**
 * Computes the dot product of a cost vector and the given input vector in 
 * a non thread safe way. This can be used when the objective includes the calculation of
 * he dot product between vectors.
 * 
 * @author Peter, Thomas Asikis
 */
public class DotCostFunction implements DifferentiableCostFunction<Vector>, HasGoal {
    private Vector costVector;
    
    /**
     * Sets the cost vector in a non thread safe way.
     * This cost vector will be used to calculate the 
     * dot product each time the {@code calcCost} is called.
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

	@Override
	public void populateGoalSignal() {
		this.costVector = Configuration.goalSignalSupplier.get();
	}
	
	@Override
	public String getLabel() {
		return "DOT";
	}
    
}
