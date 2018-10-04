package func;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.apache.commons.math3.complex.Complex;
import config.Configuration;
import data.Vector;


/**
 * Calculates both cross-correlation coefficient without delay, and also maximal value of cross-correlation with sliding.
 * Cross-correlation with sliding is computed by transforming input signal to frequency domain, calculating cross-correlation
 * in frequency domain, and then the result is inversely transformed back to time domain. As final value, maximal value
 * of resulting signal in time-domain is taken.
 * 
 * @author jovan
 *
 */
public class CrossCorrelationCostFunction implements DifferentiableCostFunction<Vector>, HasGoal{
	
	private enum SignalType {
		GOAL,
		RESPONSE
	}
	
	public static Vector goalSignal = null;
	public static boolean shouldSlide = false;
	
	static Logger logger = Logger.getLogger(CrossCorrelationCostFunction.class.getName());
	
	@Override
	public void populateGoalSignal() {
		try {
			goalSignal = Configuration.goalSignalSupplier.get();
		} catch(NullPointerException e) {
			logger.log(Configuration.loggingLevel, "Goal Signal was not set. Nothing to compare!");
			throw new NullPointerException();
		}
	}
	
	@Override
	public double calcCost(Vector value) {
		if(CrossCorrelationCostFunction.shouldSlide) {
			return crossCorrelationWithSliding(value, CrossCorrelationCostFunction.goalSignal);
		} else {
			return crossCorrelationCoefficient(value, CrossCorrelationCostFunction.goalSignal);
		}
	}
	
	private static double crossCorrelationWithSliding(Vector response, Vector goal) {
		Map<SignalType, Vector> normalized = CrossCorrelationCostFunction.normalize2(response, goal);
		Complex[] otherF = Vector.forwardFourierTransform(normalized.get(SignalType.RESPONSE));
		Complex[] goalF = Vector.forwardFourierTransform(normalized.get(SignalType.GOAL));
		Complex[] crossCorrelationF = Vector.crossCorrelationInFrequencyDomain(goalF, otherF);
		Vector result = Vector.convertWreal(crossCorrelationF);
		return lossFunction(result.max());
	}
	
	/**
	 * Computes zero-normalized cross-correlation coefficient without delaying of signals.
	 * This is cross-correlation and normalization of signals together.
	 * 
	 * As IEPOS is minimization algorithm, and higher values of correlation are preferred,
	 * output of this function is _inverse_ cross-correlation coefficient.
	 * @param response (unnormalized) response vector
	 * @param goal (unnormalized) goal signal
	 * @return squared inverse cross correlation coefficient
	 */
	private static double crossCorrelationCoefficient(Vector response, Vector goal) {
		// Standard deviation of both signals
		double goal_stdev = goal.std();
		double response_stdev = response.std();
		
		Vector goalReplicate = goal.cloneThis();
		Vector responseReplicate = response.cloneThis();
		
		// means of both signals
		double goal_mean = goalReplicate.avg();
		double response_mean = responseReplicate.avg();
		
		goalReplicate.subtract(goal_mean);
		responseReplicate.subtract(response_mean);
		
		// multiplying signals from which their mean was already subtracted
		double total_sum = responseReplicate.dot(goalReplicate);
		double stdevs = response.getNumDimensions() * goal_stdev * response_stdev + 1e-10;
		//                                                                           ^
		//                                                                          added because deviations can be 0!
		
		// note that crosscorrelation should be maximized
		double crosscorrelation = total_sum / stdevs;
		
		return CrossCorrelationCostFunction.lossFunction(crosscorrelation);
	}
	
	private static double lossFunction(double crosscorrelation) {
		return -1.0*crosscorrelation;
	}
	
	
	/**
	 * Performs Procrustes Analysis on 2 signals which includes:
	 *  - translation of data, so that "center-of-mass" is in origin
	 *  - scales data so that values along every dimension fall in range [-1, 1]
	 * @param other vector to be normalized together with goal signal
	 * @return list of normalized vectors, first one is normalized <code>other</code>, 
	 * the second one is normalized <code>goalSignal</code>
	 */
	static Map<SignalType, Vector> normalize(Vector other, Vector goal) {
		Map<SignalType, Vector> translated = CrossCorrelationCostFunction.translate(other, goal);
		return CrossCorrelationCostFunction.scale(translated);		
	}
	
	static Map<SignalType, Vector> normalize2(Vector response, Vector goal) {
		double response_mean = response.avg();
		double goal_mean = goal.avg();
		double response_std = response.std();
		double goal_std = goal.std();
		
		Vector response_n = response.cloneThis();
		response_n.subtract(response_mean);
		response_n.multiply(1/(response_std*goal_std));
		
		Vector goal_n = goal.cloneThis();
		goal_n.subtract(goal_mean);
		goal_n.multiply(1/(response_std*goal_std));
		
		Map<SignalType, Vector> map = new HashMap<SignalType, Vector>();
		map.put(SignalType.RESPONSE, response_n);
		map.put(SignalType.GOAL, goal_n);
		
		return map;
	}
	
	/**
	 * Translated both signals so that center of mass of signal is at 0
	 * @param other
	 * @return
	 */
	static Map<SignalType, Vector> translate(Vector other, Vector goal) {
		double sum = goal.sum();
		sum += other.sum();
		double mean = sum / (2.0*other.getNumDimensions());
		
		Vector goalTranslated = goal.cloneThis();
		goalTranslated.subtract(mean);
		Vector otherTranslated = other.cloneThis();
		otherTranslated.subtract(mean);
		
		HashMap<SignalType, Vector> map = new HashMap<SignalType, Vector>();
		map.put(SignalType.GOAL, goalTranslated);
		map.put(SignalType.RESPONSE, otherTranslated);
		return map;
	}
	
	static Map<SignalType, Vector> scale(Map<SignalType, Vector> translated) {
		// Uniform scaling: square each element in vector
		Vector scalingResponse = translated.get(SignalType.RESPONSE).cloneThis();
		Vector scalingGoal = translated.get(SignalType.GOAL).cloneThis();
		
		scalingResponse.pow(2);
		scalingGoal.pow(2);
		
		// calculating scale
		double sum = scalingResponse.sum();
		sum += scalingGoal.sum();
		//								!!! divide by the number of points
		double scale = Math.sqrt(sum / (2.0*scalingResponse.getNumDimensions()) );
		double inverseScale = 1.0/scale;
		
		// rescaling vectors
		translated.get(SignalType.GOAL).multiply(inverseScale);
		translated.get(SignalType.RESPONSE).multiply(inverseScale);
		
		return translated;
	}	

	@Override
	public Vector calcGradient(Vector value) {
		System.out.println("I DON'T HAVE GRADIENT IMPLEMENTED.");
		return null;
	}
	
	@Override
	public String toString() {
		return "cross-correlation global cost function";
	}
	
	@Override
	public String getLabel() {
		return "XCORR";
	}
}
