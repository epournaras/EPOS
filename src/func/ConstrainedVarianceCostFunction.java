package func;

import data.Vector;

/**
 * 
 * 
 * @author jovan
 *
 */
public class ConstrainedVarianceCostFunction extends DifferentiableCostFunction<Vector> {
	
	public static final double expectedSum = 1264000;

	@Override
	public Vector calcGradient(Vector vector) {
		Vector v = vector.cloneThis();
        v.subtract(v.avg());
        v.multiply(2.0 / (v.getNumDimensions() - 1));
        return v;
	}

	@Override
	public double calcCost(Vector vector) {
    	double value = vector.variance() + Math.abs(vector.sum()-ConstrainedVarianceCostFunction.expectedSum);
    	return value;
	}
	
	@Override
    public String toString() {
        return "constrained variance";
    }

}
