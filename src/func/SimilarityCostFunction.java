package func;


import java.util.logging.Logger;

import config.Configuration;
import data.Vector;


/**
 * Similarity Cost Function is based on residual sum of squares between the normalized desired signal and 
 * normalized goal signal.
 * 
 * @author jovan
 *
 */
public class SimilarityCostFunction extends DifferentiableCostFunction<Vector> {
	
	private enum SignalType {
		GOAL,
		RESPONSE
	}
	
	public static Vector 		goalSignal 			= 	null;
	public static Vector 		goalNormalized 		= 	null;
	public static StringBuilder sb 					= 	new StringBuilder();
	
	static Logger logger = Logger.getLogger(SimilarityCostFunction.class.getName());
	
	public static void populateGoalSignal() {
//		try {
			SimilarityCostFunction.goalSignal = Configuration.goalSignalSupplier.get();
			SimilarityCostFunction.goalNormalized = Configuration.normalizer.apply(SimilarityCostFunction.goalSignal);
//		} catch(NullPointerException e) {
//			logger.log(Configuration.loggingLevel, "Goal Signal was not set. Nothing to compare!");
//			throw new NullPointerException();
//		}
	}	
	
	@Override
	/**
	 * Calculates residual sum of squares between 2 normalized vectors.
	 */
	public double calcCost(Vector value) {
		// Procrustes Analysis
//		Map<SignalType, Vector> normalized = this.normalize(value);
//		return Vector.residualSumOfSquares(normalized.get(SignalType.RESPONSE), normalized.get(SignalType.GOAL));
		
		Vector responseNormalized = Configuration.normalizer.apply(value);
		return Vector.residualSumOfSquares(responseNormalized, SimilarityCostFunction.goalNormalized);
//		return Vector.residualSumOfSquares(responseNormalized, SimilarityCostFunction.goalSignal);
	}
	
//	/**
//	 * Performs Procrustes Analysis on 2 signals which includes:
//	 *  - translation of data, so that "center-of-mass" is in origin
//	 *  - scales data so that values along every dimension fall in range [-1, 1]
//	 * @param other vector to be normalized together with goal signal
//	 * @return list of normalized vectors, first one is normalized <code>other</code>, 
//	 * the second one is normalized <code>goalSignal</code>
//	 */
//	Map<SignalType, Vector> normalize(Vector other) {
//		Map<SignalType, Vector> translated = this.translate(other);
//		return this.scale(translated);		
//	}
	
//	/**
//	 * Translated both signals so that center of mass of signal is at 0
//	 * @param other
//	 * @return
//	 */
//	Map<SignalType, Vector> translate(Vector other) {
//		double sum = SimilarityCostFunction.goalSignal.sum();
//		sum += other.sum();
//		double mean = sum / (2.0*other.getNumDimensions());
//		
//		Vector goalTranslated = SimilarityCostFunction.goalSignal.cloneThis();
//		goalTranslated.subtract(mean);
//		Vector otherTranslated = other.cloneThis();
//		otherTranslated.subtract(mean);
//		
//		HashMap<SignalType, Vector> map = new HashMap<SignalType, Vector>();
//		map.put(SignalType.GOAL, goalTranslated);
//		map.put(SignalType.RESPONSE, otherTranslated);
//		return map;
//	}
	
//	/**
//	 * Uniformly scales both signals.
//	 * @param translated
//	 * @return
//	 */
//	Map<SignalType, Vector> scale(Map<SignalType, Vector> translated) {
//		// Uniform scaling: square each element in vector
//		Vector scalingResponse = translated.get(SignalType.RESPONSE).cloneThis();
//		Vector scalingGoal = translated.get(SignalType.GOAL).cloneThis();
//		
//		scalingResponse.pow(2);
//		scalingGoal.pow(2);
//		
//		// calculating scale
//		double sum = scalingResponse.sum();
//		sum += scalingGoal.sum();
//		//								!!! divide by the number of points
//		double scale = Math.sqrt(sum / (2.0*scalingResponse.getNumDimensions()) );
//		double inverseScale = 1.0/scale;
//		
//		// rescaling vectors
//		translated.get(SignalType.GOAL).multiply(inverseScale);
//		translated.get(SignalType.RESPONSE).multiply(inverseScale);
//		
//		return translated;
//	}
	
	@Override
	public Vector calcGradient(Vector value) {
		System.err.println("I DON'T HAVE FUCKING GRADIENT!");
		return null;
	}
	
	@Override
	public String toString() {
		return "Residual sum of Squares";
	}
	
	
	
//	/**
//	 * testing only
//	 * @param args
//	 */
//	public static void main(String[] args) {
//		SimilarityCostFunction.populateGoalSignal();
//		
//		Vector other = new Vector(Configuration.numDimensions);
//		IntStream.range(0, 20).forEach(i -> other.setValue(i, 2));
//		IntStream.range(20, 80).forEach(i -> other.setValue(i, i));
//		IntStream.range(80, Configuration.numDimensions).forEach(i -> other.setValue(i, 5));
//		
////		StringBuilder sb = new StringBuilder();
//		String goalOriginal = SimilarityCostFunction.goalSignal.toString();
//		String otherOriginal = other.toString();
//		
//		sb.append(goalOriginal.substring(1, goalOriginal.length()-1)).append(System.lineSeparator());
//		System.out.println("added goal original vector");
//		sb.append(otherOriginal.substring(1, otherOriginal.length()-1)).append(System.lineSeparator());
//		System.out.println("added other original vector");
//		
//		SimilarityCostFunction scf = new SimilarityCostFunction();
//		Map<SignalType, Vector> normalized = scf.translate(other);
//		
//		String goalNormalized = normalized.get(SignalType.GOAL).toString();
//		String otherNormalized = normalized.get(SignalType.RESPONSE).toString();
//		
//		sb.append(goalNormalized.substring(1, goalNormalized.length()-1)).append(System.lineSeparator());
//		System.out.println("added goal translated vector");
//		sb.append(otherNormalized.substring(1, otherNormalized.length()-1)).append(System.lineSeparator());
//		System.out.println("added other translated vector");
//		
//		normalized = scf.scale(normalized);
//		
//		goalNormalized = normalized.get(SignalType.GOAL).toString();
//		otherNormalized = normalized.get(SignalType.RESPONSE).toString();
//		
//		sb.append(goalNormalized.substring(1, goalNormalized.length()-1)).append(System.lineSeparator());
//		System.out.println("added goal scaled vector");
//		sb.append(otherNormalized.substring(1, otherNormalized.length()-1)).append(System.lineSeparator());
//		System.out.println("added goal scaled vector");
//		
//		try (PrintWriter out = new PrintWriter(new BufferedWriter(new java.io.FileWriter("RESULTS/similarity/test_results.csv", false)))) { 
//			System.out.println("P");
//            out.append(sb.toString());
//        } catch (FileNotFoundException ex) {
//            Logger.getLogger(GlobalCostLogger.class.getName()).log(Level.SEVERE, null, ex);
//        } catch(IOException e) {
//        	Logger.getLogger(GlobalCostLogger.class.getName()).log(Level.SEVERE, null, e);
//        }
//	}	

}
