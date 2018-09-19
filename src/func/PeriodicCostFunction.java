package func;

import org.apache.commons.math3.complex.Complex;

import config.Configuration;
import data.Vector;

public class PeriodicCostFunction extends DifferentiableCostFunction<Vector> {
	
	private enum SignalType {
		GOAL,
		RESPONSE
	}
	
	// goal signal is already in frequency domain
	public static Vector 			goalSignalFreq 			= 	null;
	public static Vector 			goalSignal				=	null;
	
	public static void populateGoalSignal() {
		PeriodicCostFunction.goalSignalFreq = Configuration.goalSignalSupplier.get();
		PeriodicCostFunction.goalSignal = Vector.convertWreal(
											Vector.inverseFourierTransform(
													PeriodicCostFunction.goalSignalFreq.convert2complex()
											)
										  );
	}

	@Override
	public Vector calcGradient(Vector value) {
		System.err.println("I don't have gradient!");
		return null;
	}

	@Override
	public double calcCost(Vector value) {
		Complex[] otherFreq = Vector.forwardFourierTransform(Configuration.normalizer.apply(value));
		Vector otherVectorFreq = Vector.convertWreal(otherFreq);
		return Vector.residualSumOfSquares(otherVectorFreq, PeriodicCostFunction.goalSignalFreq);
	}

}
