/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package agent;

import data.HasValue;
import data.Value;
import func.CostFunction;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 *
 * @author Peter
 */
public class Optimization {

    protected Random random;

    public Optimization(Random random) {
        this.random = random;
    }

    public <V extends Value<V>> List<V> calcAllCombinations(List<List<V>> choicesPerAgent) {
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

    public <V extends Value<V>> List<Integer> combinationToSelections(int combinationIdx, List<List<V>> choicesPerAgent) {
        List<Integer> selected = new ArrayList<>();

        int factor = 1;
        for (List<V> choices : choicesPerAgent) {
            int numChoices = choices.size();
            selected.add((combinationIdx / factor) % numChoices);
            factor *= numChoices;
        }

        return selected;
    }
    
    public <V extends Value<V>> int argmin(CostFunction<V> costFunction, List<? extends HasValue<? extends V>> choices) {
        return argmin(costFunction, choices, null);
    }
    
    public <V extends Value<V>> int argmin(CostFunction<V> costFunction, List<? extends HasValue<? extends V>> choices, double lambda) {
        return argmin(costFunction, choices, null, lambda);
    }

    public <V extends Value<V>> int argmin(CostFunction<V> costFunction, List<? extends HasValue<? extends V>> choices, V constant) {
        return argmin(costFunction, choices, constant, 0);
    }
        
    public <V extends Value<V>> int argmin(CostFunction<V> costFunction, List<? extends HasValue<? extends V>> choices, V constant, double lambda) {
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
                cost = cost + lambda * score * std;
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
}
