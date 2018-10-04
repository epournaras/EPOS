package func;

import java.util.logging.Level;
import java.util.logging.Logger;

import config.Configuration;
import data.Vector;

/**
 * A cost function that calculates the RMSE between a given goal vector and other vectors.
 * @author Jovan N. Thomas Asikis
 *
 */
public class RMSECostFunction implements DifferentiableCostFunction<Vector>, HasGoal {

	public static Vector goalSignal = null;
	public static double goalMean;
	public static double goalStd;
	
	static Logger logger = Logger.getLogger(RMSECostFunction.class.getName());

	/**
	 * This method calculates the goal signal based on which the cost finction is calculated.
	 */
	@Override
	public void populateGoalSignal() {
		RMSECostFunction.goalSignal = Configuration.goalSignalSupplier.get();
		RMSECostFunction.goalMean = RMSECostFunction.goalSignal.avg();
		RMSECostFunction.goalStd = RMSECostFunction.goalSignal.std();
	}

	@Override
	public double calcCost(Vector value) {
		double otherMean = value.avg();
		double otherStd = value.std();
		Vector goalReplica = RMSECostFunction.goalSignal.cloneThis();
		goalReplica.subtract(RMSECostFunction.goalMean);
		double multiplicativeFactor = otherStd / (RMSECostFunction.goalStd + 1e-10);
		goalReplica.multiply(multiplicativeFactor);
		goalReplica.add(otherMean);
		return goalReplica.rootMeanSquareError(value);
	}

	@Override
	public Vector calcGradient(Vector value) {
		double length = value.getNumDimensions();

		double inverse = 1 / calcCost(value);
		Vector difference = value.cloneThis();
		difference.subtract(goalSignal);

		difference.multiply(inverse * length);
		
		logger.log(Level.WARNING, "Untested functionality and correctness");
		//FIXME no sanity check done in the math
		//FIXME check if the gradient affects things 
		return difference;
	}
	
	@Override
	public String getLabel() {
		// TODO Auto-generated method stub
		return "RMSE";
	}
}
