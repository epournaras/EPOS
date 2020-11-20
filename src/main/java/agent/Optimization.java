/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package agent;

import data.HasValue;
import data.Plan;
import func.CostFunction;
import func.PlanCostFunction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
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

		IntStream.range(0, choices.size()).forEach(i -> {
			V combined;
			if(constant != null) {
				combined = constant.cloneThis();
				combined.add(choices.get(i).getValue());
			} else {
				combined = choices.get(i).getValue();
			}
			double cost = costFunction.calcCost(combined);

			costs[i] = cost;
			double score = localCostFunction.calcCost(choices.get(i));
			discomfortSums[i] = discomfortSumConstant + score;
			discomfortSumSqrs[i] = discomfortSumSqrConstant + score*score;
			//System.out.print("agent: " + agent.getPeer().getIndexNumber() + ", SumConst = " + discomfortSumConstant + ", Sum2const = " + discomfortSumSqrConstant);
			//System.out.println("Sum is " + discomfortSums[i] + ", sum^2 is " + discomfortSumSqrs[i] + ", num agents = " + numAgents);
		});

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
    
}
