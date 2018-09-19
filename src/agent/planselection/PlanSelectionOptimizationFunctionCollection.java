package agent.planselection;

import java.util.HashMap;

import org.apache.commons.math3.exception.NotANumberException;


/**
 * Contains collection of Plan Selection Optimization Functions
 * 
 * @author jovan
 *
 */
public class PlanSelectionOptimizationFunctionCollection {
	
	/**
	 * cost + lambda * i/|P| * std
	 */
	public static PlanSelectionOptimizationFunction simpleFunction1 = (HashMap<OptimizationFactor, Object> map) -> {
		double lambda 	= 	(double)	map.get(OptimizationFactor.LAMBDA);
		double cost		=	(double)	map.get(OptimizationFactor.GLOBAL_COST);
		double score	=	(double)	map.get(OptimizationFactor.LOCAL_COST);
		double std		=	(double)	map.get(OptimizationFactor.STD);		
		return cost + lambda * score * std;
	};
	
	/**
	 * (1 - lambda) * cost + lambda * i/|P| * std
	 */
	public static PlanSelectionOptimizationFunction simpleFunction2 = (HashMap<OptimizationFactor, Object> map) -> {
		double lambda 	= 	(double)	map.get(OptimizationFactor.LAMBDA);
		double cost		=	(double)	map.get(OptimizationFactor.GLOBAL_COST);
		double score	=	(double)	map.get(OptimizationFactor.LOCAL_COST);
		double std		=	(double)	map.get(OptimizationFactor.STD);		
		return (1 - lambda) * cost + lambda * score * std;
	};
	
	/**
	 * (1 - lambda) * cost + lambda * i/|P| * mean
	 */
	public static PlanSelectionOptimizationFunction simpleFunction3 = (HashMap<OptimizationFactor, Object> map) -> {
		double lambda 	= 	(double)	map.get(OptimizationFactor.LAMBDA);
		double cost		=	(double)	map.get(OptimizationFactor.GLOBAL_COST);
		double score	=	(double)	map.get(OptimizationFactor.LOCAL_COST);
		double mean		=	(double)	map.get(OptimizationFactor.MEAN);		
		return (1 - lambda) * cost + lambda * score * mean;
	};
	
	/**
	 * (1 - lambda) * cost + lambda * i/|P|
	 */
	public static PlanSelectionOptimizationFunction simpleFunction4 = (HashMap<OptimizationFactor, Object> map) -> {
		double lambda 	= 	(double)	map.get(OptimizationFactor.LAMBDA);
		double cost		=	(double)	map.get(OptimizationFactor.GLOBAL_COST);
		double score	=	(double)	map.get(OptimizationFactor.LOCAL_COST);	
		return (1 - lambda) * cost + lambda * score;
	};
	
	
	public static double unfairness(double discomfortSum, double discomfortSumSqr, double numAgents) {
		double sumOfSquares 	=	discomfortSumSqr/numAgents;
		double squaredSum		=	Math.pow(discomfortSum/numAgents, 2);
		// note that sometimes sumOfSquares can be lower than squaredSum
		double diff = sumOfSquares - squaredSum;
//		System.out.println("discomfortSum = " + discomfortSum + ", discomfortSumSqr = " + discomfortSumSqr);
//		System.out.println("sumOfSquares = " + sumOfSquares + ", squaredSum = " + squaredSum + ", diff = " + diff);
		if(Math.abs(diff) < 1e-9) {
			// some numerical instabilities at 1e-16 can lead to negative number in square root
			diff = 0;
		}
		double discomfortStd 	=	Math.sqrt(diff);
		return discomfortStd;
	}
	
	
	public static double localCost(double discomfortSum, double numAgents) {
		return discomfortSum/numAgents;
	}
	
	
	public static PlanSelectionOptimizationFunction complexFunction1 = (HashMap<OptimizationFactor, Object> map) -> {
		double alpha			=	(double)	map.get(OptimizationFactor.ALPHA);
		double beta				=	(double)	map.get(OptimizationFactor.BETA);
		double discomfortSum	=	(double)	map.get(OptimizationFactor.DISCOMFORT_SUM);
		double discomfortSumSqr =	(double)	map.get(OptimizationFactor.DISCOMFORT_SUM_SQR);
		double global_cost		=	(double)	map.get(OptimizationFactor.GLOBAL_COST);
		double numAgents		=	(double)	map.get(OptimizationFactor.NUM_AGENTS);
		
		double local_cost		=	localCost(discomfortSum, numAgents);
		double unfairness		=	unfairness(discomfortSum, discomfortSumSqr, numAgents); 
		
		if(Double.isNaN(unfairness)) {
			throw new NotANumberException();
		}
//		System.out.println("discomfortSum = " + discomfortSum + ", discomfortSumSqr = " + discomfortSumSqr);
//		System.out.println("global cost = " + global_cost + ", unfairness = " + unfairness + ", local cost = " + local_cost + ", num agents = " + numAgents);
//		System.out.println("alpha = " + alpha + ", beta = " + beta + ", returning: " + ((1 - alpha - beta) * global_cost + alpha * unfairness + beta * local_cost));
		return (1 - alpha - beta) * global_cost + alpha * unfairness + beta * local_cost;
	};

}
