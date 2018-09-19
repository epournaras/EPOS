package func;

import data.Vector;

/**
 * 
 * 
 * @author jovan
 *
 */
public class ConstraintCostFunction extends DifferentiableCostFunction<Vector> {
	
	public static final double expectedSum = 1264000;

	@Override
	public double calcCost(Vector vector) {
		return Math.abs(vector.sum()-ConstraintCostFunction.expectedSum);
	}

	@Override
	public Vector calcGradient(Vector value) {
		return null;
	}
	
	@Override
    public String toString() {
        return "constraint";
    }

}
