package agent.planselection;

import java.util.HashMap;

public interface PlanSelectionOptimizationFunction {
	
	public double apply(HashMap<OptimizationFactor, Object> parameters);

}
