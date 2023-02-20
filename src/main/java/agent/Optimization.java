/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package agent;

import data.HasValue;
import data.Plan;
import data.Vector;
import func.CostFunction;
import func.PlanCostFunction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import agent.planselection.OptimizationFactor;
import config.Configuration;
import data.DataType;

/**
 *
 * @author Peter P. & Jovan N.
 */
public class Optimization {

    protected Random random;

    public Optimization(Random random) {
        this.random = random;
    }

    public <V extends DataType<V>> List<V> calcAllCombinations(List<List<V>> choicesPerAgent) {
        List<V> combinations = new ArrayList<>();
        
        if(choicesPerAgent.isEmpty() || choicesPerAgent.get(0).isEmpty()) {
            return combinations;
        }
        
        V prototypeValue = choicesPerAgent.get(0).get(0);
        int numCombinations = choicesPerAgent.stream().map(p -> p.size()).reduce(1, (a, b) -> a * b);

        // init combinations
        for (int i = 0; i < numCombinations; i++) {
            combinations.add(prototypeValue.cloneNew());
        }

        // calc all possible combinations
        int factor = 1;
        for (List<V> choices : choicesPerAgent) {
            int numChoices = choices.size();
            for (int i = 0; i < numCombinations; i++) {
                int localIdx = (i / factor) % numChoices;
                combinations.get(i).add(choices.get(localIdx));
            }
            factor *= numChoices;
        }

        return combinations;
    }

    public <V extends DataType<V>> List<Integer> combinationToSelections(int combinationIdx, List<List<V>> choicesPerAgent) {
        List<Integer> selected = new ArrayList<>();

        int factor = 1;
        for (List<V> choices : choicesPerAgent) {
            int numChoices = choices.size();
            selected.add((combinationIdx / factor) % numChoices);
            factor *= numChoices;
        }

        return selected;
    }
    
    public List<Double> calculateAllCombinationsForDiscomfortScores(List<List<Double>> choicesPerChild) {
        List<Double> combinations = new ArrayList<>();
        
        if(choicesPerChild.isEmpty() || choicesPerChild.get(0).isEmpty()) {
            return combinations;
        }
        
        int numCombinations = choicesPerChild.stream().map(p -> p.size()).reduce(1, (a, b) -> a * b);

        for (int i = 0; i < numCombinations; i++) {
            combinations.add(0.0);
        }

        int factor = 1;
        for (List<Double> choices : choicesPerChild) {  			// iterates over children
            int numChoices = choices.size();				// equals to 2 (always!)
            for (int i = 0; i < numCombinations; i++) {
                int localIdx = (i / factor) % numChoices;	// this can be 0 or 1
                											// 0 -> prevSubtreeResponse t_{c}^{\tau - 1}
                											// 1 -> subtreeResponse 	\tilde{t}_{c}^{\tau}
                double prevVal = combinations.get(i);
                prevVal += choices.get(localIdx);
                combinations.set(i, prevVal);                
            }
            factor *= numChoices;	// 1st child: f = 1 so it alternates: 0,1,0,1,0,1,0,1 ...
            						// 2nd child: f = 2 so it alternates: 0,0,1,1,0,0,1,1 ...
            						// 3rd child: f = 4 so it alternates: 0,0,0,0,1,1,1,1 ...
        }
        return combinations;
    }
    
    public <V extends DataType<V>, T extends HasValue<V>> int argmin(CostFunction<V> costFunction, List<T> choices) {
        return argmin(costFunction, choices, null);
    }
    
    public <V extends DataType<V>, T extends HasValue<V>> int argmin(CostFunction<V> costFunction, List<T> choices, double lambda) {
        return argmin(costFunction, choices, null, lambda);
    }

    public <V extends DataType<V>, T extends HasValue<V>> int argmin(CostFunction<V> costFunction, List<T> choices, V constant) {
        return argmin(costFunction, choices, constant, 0);
    }
    
    /**
     * Invoked by old I-EPOS
     * @param costFunction
     * @param choices
     * @param constant
     * @param lambda
     * @return
     */
    public <V extends DataType<V>> int argmin(CostFunction<V> costFunction, List<? extends HasValue<? extends V>> choices, V constant, double lambda) {
       
    	double minCost = Double.POSITIVE_INFINITY;
        int selected = -1;
        int numOpt = 0;
        
        // lambda-PREF
        double[] costs = new double[choices.size()];
        double sum = 0;
        double sqrSum = 0;
        double std = 0;

        for (int i = 0; i < choices.size(); i++) {
            V combined;
            if(constant != null) {
                combined = constant.cloneThis();
                combined.add(choices.get(i).getValue());
            } else {
                combined = choices.get(i).getValue();
            }
            
            double cost = costFunction.calcCost(combined);
            costs[i] = cost;
            sum += cost;
            sqrSum += cost*cost;
        }
        
        if(lambda != 0) {
            std = Math.sqrt(sqrSum/choices.size() - (sum/choices.size())*(sum/choices.size()));
            if(!Double.isFinite(std) || std < 0) {
                std = 0;
            }
        }
        
        for(int i = 0; i < choices.size(); i++) {
            double cost = costs[i];
            if (lambda != 0) {
                double score = i/(double)choices.size();
                cost = (1 - lambda) * cost + lambda * score * std;
            }
            
            if (cost < minCost) {
                minCost = cost;
                selected = i;
                numOpt = 1;
            } else if (cost == minCost) {
                numOpt++;
                if (random.nextDouble() <= 1.0 / numOpt) {
                    selected = i;
                }
            }
        }
        
        return selected;
    }
    
    /**
     * Invoked for accepting and/or rejecting changes proposed by descendants in a subtree
     * @param costFunction
     * @param responseCombos
     * @param discomfortSumCombos
     * @param discomfortSumSqrCombos
     * @param responseConst
     * @param discomfortSumConst
     * @param discomfortSumSqrConst
     * @param alpha
     * @param beta
     * @param numAgents
     * @return
     */
    public <V extends DataType<V>> int argmin(
    		CostFunction<V> costFunction,		List<V> responseCombos,
			List<Double> discomfortSumCombos,	List<Double> discomfortSumSqrCombos,
			V responseConst,					double discomfortSumConst,
			double discomfortSumSqrConst,		double alpha,
			double beta,						double numAgents) {

		double[] costs = new double[responseCombos.size()];
		double[] discomfortSums = new double[responseCombos.size()];
		double[] discomfortSumSqrs = new double[responseCombos.size()];

		IntStream.range(0, responseCombos.size()).forEach(i -> {
			V combined;
			if(responseConst != null) {
				combined = responseConst.cloneThis();
				combined.add(responseCombos.get(i).getValue());
			} else {
				combined = responseCombos.get(i).getValue();
			}
			double cost = costFunction.calcCost(combined);
		
			costs[i] = cost;
			discomfortSums[i] = discomfortSumConst + discomfortSumCombos.get(i);
			discomfortSumSqrs[i] = discomfortSumSqrConst + discomfortSumSqrCombos.get(i);
		});

		return this.extendedOptimization(costs, alpha, beta, discomfortSums, discomfortSumSqrs, numAgents);
    }
    
    /**
     * Invoked for plan selection
     * @param costFunction
     * @param localCostFunction
     * @param choices
     * @param constant
     * @param alpha
     * @param beta
     * @param discomfortSumConstant
     * @param discomfortSumSqrConstant
     * @param numAgents
     * @param agent
     * @return
     */
    public <V extends DataType<V>> int argmin(
    		CostFunction<V> costFunction,		PlanCostFunction<V> localCostFunction,
			List<Plan<V>> choices, 				V constant, 
			double alpha,						double beta,
			double discomfortSumConstant,		double discomfortSumSqrConstant,	
			int numAgents, 						MultiObjectiveIEPOSAgent agent) {

		double[] costs = new double[choices.size()];
		double[] discomfortSums = new double[choices.size()];
		double[] discomfortSumSqrs = new double[choices.size()];

        boolean isFirstIter = (agent.getIteration() == 0); // First iteration
        boolean isPlanConst = (Objects.equals(Configuration.hardConstraint, "PLAN"));
        boolean isDoubleConst = (Objects.equals(Configuration.hardConstraint, "PLAN_DOUBLE"));
        boolean isCostConst = (Objects.equals(Configuration.hardConstraint, "COST"));
        AtomicInteger countsOfHardViolated = new AtomicInteger(0);

		IntStream.range(0, choices.size()).forEach(i -> {
			V combined;
			if(constant != null) {
				combined = constant.cloneThis();
				combined.add(choices.get(i).getValue());
			} else {
				combined = choices.get(i).getValue();
			}
			double cost = costFunction.calcCost(combined);

			double score = localCostFunction.calcCost(choices.get(i));
			discomfortSums[i] = discomfortSumConstant + score;
			discomfortSumSqrs[i] = discomfortSumSqrConstant + score*score;

            if (isPlanConst) {
                DataType<Vector> taskV = (DataType<Vector>) combined.getValue();
                double[] response = taskV.getValue().getArray();
                if (isFirstIter) {
                    // calculate the initial plan that satisfy the hard constraint in an extreme way
                    cost = costOfHardConstraint(response);

                } else {
                    // exclude the plan that violates the hard constraint
                    if (agent.getIteration() > (Configuration.numIterations / 2)
                            && isHardConstraintViolated(response, false, response.length)) {
                        countsOfHardViolated.getAndIncrement();
                        cost = Configuration.numAgents * cost;
                    }
                }
            }

            // PLAN_DOUBLE: double constraints that the plan should be within two thresholds
            if (isDoubleConst) {
                DataType<Vector> taskV = (DataType<Vector>) combined.getValue();
                double[] response = taskV.getValue().getArray();
                if (isFirstIter) {
                    // calculate the initial plan that satisfy the hard constraint in an extreme way
//                    cost = 1/costOfDoubleHardConstraint(response);

                } else {
                    // exclude the plan that violates the hard constraint
                    int length = Math.max(response.length * (agent.getIteration()-5) / (Configuration.numIterations / 2), 0);
                    if (isHardConstraintViolated(response, true, Math.min(length, response.length))) {
                        countsOfHardViolated.getAndIncrement();
                        cost = Configuration.numAgents * cost;
                    }
                }
            }

            // COST constraint: each agent selects the plan with the lowest cost in the first iteration
            if (isCostConst) {
//                int agent_idx = agent.getPeer().getIndexNumber();
                double[] local = new double[] {discomfortSums[i] / Configuration.numAgents};
                if (isFirstIter) {
                    // calculate the initial plan that satisfy the hard constraint in an extreme way
//                    if (score == 1) {
//                        cost = Configuration.numAgents * cost;
//                    }
                    cost = costOfHardConstraint(local);

                } else {
                    // exclude the plan that violates the hard constraint
                    if (isHardConstraintViolated(local, false, local.length)) {
                        countsOfHardViolated.getAndIncrement();
                        cost = Configuration.numAgents * cost;
                    }
                }
            }

            costs[i] = cost;
			//System.out.print("agent: " + agent.getPeer().getIndexNumber() + ", SumConst = " + discomfortSumConstant + ", Sum2const = " + discomfortSumSqrConstant);
			//System.out.println("Sum is " + discomfortSums[i] + ", sum^2 is " + discomfortSumSqrs[i] + ", num agents = " + numAgents);
		});

        // If all elements violate, report that agents cannot follow hard constraint and change to soft constraint (variance)
        if (countsOfHardViolated.get() == choices.size()) {
            return agent.prevSelectedPlanID;
        }

		return this.extendedOptimization(costs, alpha, beta, discomfortSums, discomfortSumSqrs, numAgents);

    }    
    private <V extends DataType<V>> int extendedOptimization(double[] costs,				double alpha,
			 												 double beta,					double[] discomfortSums, 
			 												 double[] discomfortSumSqrs,	double numAgents) {

			double minCost = Double.POSITIVE_INFINITY;
			int selected = -1;
			int numOpt = 0;
			
			try {
				for(int i = 0; i < costs.length; i++) {
					double cost = costs[i];
					if(alpha > 0 || beta > 0) {
						HashMap<OptimizationFactor, Object> parameters = new HashMap<OptimizationFactor, Object>();
						parameters.put(OptimizationFactor.GLOBAL_COST, costs[i]);
						parameters.put(OptimizationFactor.DISCOMFORT_SUM, discomfortSums[i]);
						parameters.put(OptimizationFactor.DISCOMFORT_SUM_SQR, discomfortSumSqrs[i]);
						parameters.put(OptimizationFactor.ALPHA, alpha);
						parameters.put(OptimizationFactor.BETA, beta);
						parameters.put(OptimizationFactor.NUM_AGENTS, numAgents);
						cost = Configuration.planOptimizationFunction.apply(parameters);
					}
				
					if (cost < minCost) {
						minCost = cost;
						selected = i;
						numOpt = 1;
					} else if (cost == minCost) {
						numOpt++;
						if (random.nextDouble() <= 1.0 / numOpt) {
							selected = i;
						}
					}
				}
			} catch(Exception e) {
				e.printStackTrace();
			}		
			return selected;		
	}
    
    private double costOfHardConstraint(double[] array) {

        double sum = 0;

        for (int j = 0; j < array.length; j++) {
            // 0: not influence;
            if (Configuration.hardArray[1][j] == 0) continue;
            // 1: smaller than hard constraint
            if (Configuration.hardArray[1][j] == 1)
                sum += Configuration.hardArray[0][j] - array[j];
            // 2: larger than hard constraint
            if (Configuration.hardArray[1][j] == 2)
                sum += array[j] - Configuration.hardArray[0][j];
            // 3: must equal
            if (Configuration.hardArray[1][j] == 3)
                sum += Math.abs(Configuration.hardArray[0][j] - array[j]) * 2;
        }

        return 1 / Math.exp(sum / Configuration.numAgents);
    }

    private double costOfDoubleHardConstraint(double[] array) {
        double sum = 0;

        for (int j = 0; j < array.length; j++) {
            int compare1 = (int) Configuration.hardArray[1][j];
            int compare2 = (int) Configuration.hardArray[3][j];
            double hard1 = Configuration.hardArray[0][j];
            double hard2 = Configuration.hardArray[2][j];
            if (compare1 == 0 && compare2 == 0)
                continue;
            
            sum += Math.abs((hard1 + hard2) / 2 - array[j]);
        }

        return sum;
    }

    private boolean isHardConstraintViolated(double[] array, boolean isDoubleConst, int length) {


        for (int i = 0; i < length; i++) {
            // 0: not influenced by hard constraint
            if (Configuration.hardArray[1][i] == 0) continue;
            // 1: no larger than hard constraint
            if (Configuration.hardArray[1][i] == 1 && Configuration.hardArray[0][i] < array[i]) return true;
            // 2: no smaller than hard constraint
            if (Configuration.hardArray[1][i] == 2 && Configuration.hardArray[0][i] > array[i]) return true;
            // 3: must equal to hard constraint
            if (Configuration.hardArray[1][i] == 3 && Configuration.hardArray[0][i] != array[i]) return true;
        }

        if (isDoubleConst) {
            for (int i = 0; i < length; i++) {
                // 0: not influenced by hard constraint
                if (Configuration.hardArray[3][i] == 0) continue;
                // 1: no larger than hard constraint
                if (Configuration.hardArray[3][i] == 1 && Configuration.hardArray[2][i] < array[i]) return true;
                // 2: no smaller than hard constraint
                if (Configuration.hardArray[3][i] == 2 && Configuration.hardArray[2][i] > array[i]) return true;
                // 3: must equal to hard constraint
                if (Configuration.hardArray[3][i] == 3 && Configuration.hardArray[2][i] != array[i]) return true;
            }
        }

        return false;
    }
    
}
