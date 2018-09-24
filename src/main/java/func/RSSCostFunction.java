package func;


import java.util.logging.Logger;

import config.Configuration;
import data.Vector;


/**
 * Similarity Cost Function is based on residual sum of squares between the normalized desired signal and 
 * normalized goal signal.
 * 
 * @author Jovan N.
 *
 */
public class RSSCostFunction extends DifferentiableCostFunction<Vector> {
	
	private enum SignalType {
		GOAL,
		RESPONSE
	}
	
	public static Vector 		goalSignal 			= 	null;
	public static Vector 		goalNormalized 		= 	null;
	public static StringBuilder sb 					= 	new StringBuilder();
	
	static Logger logger = Logger.getLogger(RSSCostFunction.class.getName());
	
	public static void populateGoalSignal() {
		RSSCostFunction.goalSignal = Configuration.goalSignalSupplier.get();
		RSSCostFunction.goalNormalized = Configuration.normalizer.apply(RSSCostFunction.goalSignal);
	}	
	
	@Override
	/**
	 * Calculates residual sum of squares between 2 normalized vectors.
	 */
	public double calcCost(Vector value) {
		Vector responseNormalized = Configuration.normalizer.apply(value);
		return Vector.residualSumOfSquares(responseNormalized, RSSCostFunction.goalNormalized);
	}
	
	@Override
	public Vector calcGradient(Vector value) {
		System.err.println("I DON'T HAVE FUCKING GRADIENT!");
		return null;
	}
	
	@Override
	public String toString() {
		return "Residual sum of Squares";
	}

}
