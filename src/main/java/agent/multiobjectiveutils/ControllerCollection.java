package agent.multiobjectiveutils;

import java.util.function.DoubleBinaryOperator;

import agent.MultiObjectiveIEPOSAgent;
import agent.planselection.PlanSelectionOptimizationFunctionCollection;
/**
 * 
 * 
 * @author jovan
 *
 */
public class ControllerCollection {
	
	public static final double localCostLearningRate = 0.15;
	public static final double unfairnessLearningRate = 0.15;
	
	public static double epsilonLocalCost = 0.4;
	public static double epsilonUnfairness = 0.4;
	
	public static final DoubleBinaryOperator plus = (double left, double right) -> {
		return left + right;
	};
	
	public static final DoubleBinaryOperator minus = (double left, double right) -> {
		return left - right;
	};
	
	public static final DoubleBinaryOperator[] forUnfairness = {plus, minus};
	public static final DoubleBinaryOperator[] forLocalCost = {plus, minus};
	
	public static void swapElementsInUnfairness() {
		DoubleBinaryOperator first = forUnfairness[0];
		forUnfairness[0] = forUnfairness[1];
		forUnfairness[1] = first;
	}
	
	public static void swapElementsInLocalCost() {
		DoubleBinaryOperator first = forLocalCost[0];
		forLocalCost[0] = forLocalCost[1];
		forLocalCost[1] = first;
	}
	
	// -------------------------------------------------------------------------------------------------------
	
	public static LocalCostController simpleDecreasingLCController = (MultiObjectiveIEPOSAgent agent) -> {
		double previousLocalCostWeight = agent.getLocalCostWeight();
		double newLocalCostWeight = previousLocalCostWeight - 0.1;
		
		if(newLocalCostWeight >= 0.0) {
			return newLocalCostWeight;
		} else {
			return 0.0;
		}
	};
	
	public static UnfairnessController simpleDecreasingUFController = (MultiObjectiveIEPOSAgent agent) -> {
		double previousUnfairnessWeight = agent.getUnfairnessWeight();
		double newLocalCostWeight = previousUnfairnessWeight - 0.1;
		
		if(newLocalCostWeight >= 0.0) {
			return newLocalCostWeight;
		} else {
			return 0.0;
		}
	};
	
	// -------------------------------------------------------------------------------------------------------
	// -------------------------------------------------------------------------------------------------------
	
		public static LocalCostController simpleOscilatingLCController = (MultiObjectiveIEPOSAgent agent) -> {
			int iter = agent.getIteration();
			
			if(iter % 2 == 0) {
				return 0.5;
			} else {
				return 0;
			}
		};
		
		public static UnfairnessController simpleOscilatingUFController = (MultiObjectiveIEPOSAgent agent) -> {
			int iter = agent.getIteration();
			
			if(iter % 2 == 0) {
				return 0.5;
			} else {
				return 0;
			}
		};
		
		// -------------------------------------------------------------------------------------------------------
		// -------------------------------------------------------------------------------------------------------
		
			public static LocalCostController complexOscilatingLCController = (MultiObjectiveIEPOSAgent agent) -> {
				int iter = agent.getIteration();
				double opt1 = 0.1;
				double opt2 = 0.9;
				double param = agent.getLocalCostWeight();
				
				if(iter % 6 == 0) {
					return param == opt1 ? opt2 : opt1;
				} else {					
					return param;
				}
			};
			
			public static UnfairnessController complexOscilatingUFController = (MultiObjectiveIEPOSAgent agent) -> {
				int iter = agent.getIteration();
				double opt1 = 0.1;
				double opt2 = 0.3;
				double param = agent.getUnfairnessWeight();
				
				if(iter % 6 == 0) {
					return param == opt1 ? opt2 : opt1;
				} else {					
					return param;
				}
			};
			
		// -------------------------------------------------------------------------------------------------------
		// -------------------------------------------------------------------------------------------------------
			
			public static LocalCostController thresholdLCController = (MultiObjectiveIEPOSAgent agent) -> {
				
				double discomfortSum = agent.getGlobalDiscomfortSum();
				double numAgents = agent.getNumAgents();
				double currentLocalCost = PlanSelectionOptimizationFunctionCollection.localCost(discomfortSum, numAgents);
				double threshold = 0.7; // -energy
//				double threshold = 6; // -gaussian				
				
				if(agent.isRoot()) {
					System.out.println("LC iteration = " + agent.getIteration() + 
									   ", range = (" + ((1.0-epsilonLocalCost)*threshold) + ", " + ((1.0+epsilonLocalCost)*threshold) + ")" +
									   ", current local cost = " + currentLocalCost);
				}			
				
				if(currentLocalCost - threshold > epsilonLocalCost*threshold /** *currentLocalCost */) {
					
//					epsilonLocalCost += 0.5*epsilonLocalCost/(agent.getIteration()==0 ? 1.0 : Math.sqrt(agent.getIteration()));
					
					if(agent.isRoot()) {
						System.out.println("\t local cost - threshold = " + 
											(currentLocalCost - threshold) + 
											" > " + (epsilonLocalCost*threshold));
					}
					
					double modification = (localCostLearningRate / (agent.getIteration()==0 ? 1.0 : Math.sqrt(agent.getIteration()))) * agent.getLocalCostWeight();
					DoubleBinaryOperator operation = forLocalCost[0];
					double computed =  operation.applyAsDouble(agent.getLocalCostWeight(), modification);							
					return computed<0 ? 0.0 : (computed>1.0 ? 1.0 : computed);
					
				} else 
				if(currentLocalCost - threshold < -epsilonLocalCost*threshold /** *currentLocalCost */) {
					
//					epsilonLocalCost += 0.5*epsilonLocalCost/(agent.getIteration()==0 ? 1.0 : Math.sqrt(agent.getIteration()));
					
					if(agent.isRoot()) {
						System.out.println("\t local cost - threshold = " + 
											(currentLocalCost - threshold) + 
											" < " + (-epsilonUnfairness*threshold));
					}
					
					double modification = (localCostLearningRate / (agent.getIteration()==0 ? 1.0 : Math.sqrt(agent.getIteration()))) * agent.getLocalCostWeight();
					DoubleBinaryOperator operation = forLocalCost[1];
					double computed =  operation.applyAsDouble(agent.getLocalCostWeight(), modification);							
					return computed<0 ? 0.0 : (computed>1.0 ? 1.0 : computed);
					
				} else {
					if(agent.isRoot()) {
						System.out.println("\t keeping things the same");
					}
//					epsilonLocalCost -= 0.5*epsilonLocalCost/(agent.getIteration()==0 ? 1.0 : Math.sqrt(agent.getIteration()));
					return agent.getLocalCostWeight();
				}
			};
			
			public static UnfairnessController thresholdUFController = (MultiObjectiveIEPOSAgent agent) -> {
				
				double discomfortSum = agent.getGlobalDiscomfortSum();
				double discomfortSumSqr = agent.getGlobalDiscomfortSumSqr();
				double numAgents = agent.getNumAgents();
				double currentUnfairness = PlanSelectionOptimizationFunctionCollection.unfairness(discomfortSum, discomfortSumSqr, numAgents);
				double threshold = 0.5; // -energy
//				double threshold = 2.5; // -gaussian	

				if(agent.isRoot()) {
					System.out.println("UF iteration = " + agent.getIteration() + 
									   ", range = (" + ((1.0-epsilonLocalCost)*threshold) + ", " + ((1.0+epsilonLocalCost)*threshold) + ")" +
									   ", current unfairness = " + currentUnfairness);
				}	
				
				if(currentUnfairness - threshold > epsilonUnfairness*threshold /** *currentUnfairness*/ ) {
					
//					epsilonUnfairness += 0.5*epsilonUnfairness/(agent.getIteration()==0 ? 1.0 : Math.sqrt(agent.getIteration()));
					
					if(agent.isRoot()) {
						System.out.println("\t unfairness - threshold = " + 
											(currentUnfairness - threshold) + 
											" > " + (epsilonUnfairness*threshold));
					}					
					
					double modification = (unfairnessLearningRate / (agent.getIteration()==0 ? 1.0 : Math.sqrt(agent.getIteration()))) * agent.getUnfairnessWeight();
					DoubleBinaryOperator operation = forUnfairness[0];
					double computed = operation.applyAsDouble(agent.getUnfairnessWeight(), modification);							
					return computed<0 ? 0.0 : (computed>1.0 ? 1.0 : computed);
					
				} else 
				if(currentUnfairness - threshold < -epsilonUnfairness*threshold /** *currentUnfairness */) {
					
//					epsilonUnfairness += 0.5*epsilonUnfairness/(agent.getIteration()==0 ? 1.0 : Math.sqrt(agent.getIteration()));
					
					if(agent.isRoot()) {
						System.out.println("\t unfairness - threshold = " + 
											(currentUnfairness - threshold) + 
											" < " + (-epsilonUnfairness*threshold));
					}
					
					double modification = (unfairnessLearningRate / (agent.getIteration()==0 ? 1.0 : Math.sqrt(agent.getIteration()))) * agent.getUnfairnessWeight();
					DoubleBinaryOperator operation = forUnfairness[1];
					double computed = operation.applyAsDouble(agent.getUnfairnessWeight(), modification);							
					return computed<0 ? 0.0 : (computed>1.0 ? 1.0 : computed);
					
				} else {
					if(agent.isRoot()) {
						System.out.println("\t keeping things the same");
					}
//					epsilonUnfairness -= epsilonUnfairness/(agent.getIteration()==0 ? 1.0 : Math.sqrt(agent.getIteration()));
					return agent.getUnfairnessWeight();
				}
			};
				
			// -------------------------------------------------------------------------------------------------------
			// -------------------------------------------------------------------------------------------------------
			
				public static LocalCostController simpleFuncLCController = (MultiObjectiveIEPOSAgent agent) -> {
					int iter = agent.getIteration();
					double n = 0;
					double x1 = 0, y1 = n;
					double x2 = 100, y2 = 0.9;
					double k = (y2-y1)/(x2-x1);
					double newparam = k*iter + n;
					if(newparam > y2) return y2;
					else if(newparam < 0) return 0;
					else return newparam;
					
				};
				
				public static UnfairnessController simpleFuncUFController = (MultiObjectiveIEPOSAgent agent) -> {
					int iter = agent.getIteration();
					double n = 0.9;
					double x1 = 0, y1 = n;
					double x2 = 100, y2 = 0;
					double k = (y2-y1)/(x2-x1);
					double newparam = k*iter + n;
					if(newparam > y1) return y1;
					else if(newparam < 0) return 0;
					else return newparam;
				};
				
			// -------------------------------------------------------------------------------------------------------
			
			// -------------------------------------------------------------------------------------------------------				
				public static LocalCostController simpleExpFuncLCController = (MultiObjectiveIEPOSAgent agent) -> {
					int iter = agent.getIteration();
					double n = 1.7;
					double y = 1.0;
					double newparam = Math.exp((10-(double)iter)/10.0) - n;
					// set this one initially to 0.9 always
					
					if(newparam > y) return y;
					else if(newparam < 0) return 0;
					else 
						return newparam;					
				};
				
				public static UnfairnessController simpleExpFuncUFController = (MultiObjectiveIEPOSAgent agent) -> {
					int iter = agent.getIteration();
					double n = 1.0;
					double y = 1.0;
					double newparam = Math.exp((double)iter/10.0) - n;
					// set this one initially to 0.0 always
					
					if(newparam > y) return y;
					else if(newparam < 0) return 0;
					else 
						return newparam;					
				};				
			// -------------------------------------------------------------------------------------------------------

}
