/*
 * Copyright (C) 2016 peter
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package func;

import config.Configuration;
import data.Vector;

/**
 * A cost function that assigns cost to a vector based on the squared euclidean
 * distance to a target vector.
 *
 * @author peter
 */
public class SqrDistCostFunction implements DifferentiableCostFunction<Vector>, HasGoal{

    private Vector target;

    /**
     * Creates a new cost function that assigns cost to a vector based on the
     * squared euclidean distance to a target vector.
     *
     * @param target the target vector
     */
    public SqrDistCostFunction() {
        
    }
    
    public void setCostVector(Vector target) {
    		this.target = target;
    }
    
    @Override
    public double calcCost(Vector vector) {
        Vector v = vector.cloneThis();
        v.subtract(target);
        v.pow(2);
        return v.sum();
    }

    @Override
    public Vector calcGradient(Vector vector) {
        Vector v = vector.cloneThis();
        v.subtract(target);
        v.multiply(2);
        return v;
    }

    @Override
    public String toString() {
        return "squared distance";
    }

	@Override
	public void populateGoalSignal() {
		this.target = Configuration.goalSignalSupplier.get();
	}
	
	@Override
	public String getLabel() {
		// TODO Auto-generated method stub
		return "SQR";
	}
}
