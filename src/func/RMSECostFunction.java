package func;

import config.Configuration;
import data.Vector;

public class RMSECostFunction extends DifferentiableCostFunction<Vector> {
	
	public static Vector 		goalSignal 			= 	null;
	public static double		goalMean;
	public static double		goalStd;
	
	
	public static void populateGoalSignal() {
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
		System.err.println("I DON'T HAVE FUCKING GRADIENT!");
		return null;
	}

}
