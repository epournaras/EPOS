package func;


import java.util.logging.Level;
import java.util.logging.Logger;

import config.Configuration;
import data.Vector;


/**
 * Similarity Cost Function is based on residual sum of squares between the normalized desired signal and 
 * normalized goal signal.
 * 
 * @author Jovan N., Thomas Asikis
 *
 */
public class RSSCostFunction implements DifferentiableCostFunction<Vector>, HasGoal {
	

	private enum SignalType {
		GOAL,
		RESPONSE
	}
	
	public static Vector 		goalSignal 			= 	null;
	public static Vector 		goalNormalized 		= 	null;
	public static StringBuilder sb 					= 	new StringBuilder();
	
	static Logger logger = Logger.getLogger(RSSCostFunction.class.getName());
	
	@Override
	public void populateGoalSignal() {
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
		//FIXME test amth and functionality to see that the gradient is properly calculated.
		logger.log(Level.WARNING, "Untested functionality and math, might lead to errorneous behavior when gradient is used");
		
		Vector result = value.cloneThis();
		result.subtract(goalSignal);
		result.multiply(2);
		return result;
	}
	
	@Override
	public String toString() {
		return "Residual sum of Squares";
	}
	
	@Override
	public String getLabel() {
		return "RSS";
	}
}
