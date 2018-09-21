package treestructure.reorganizationstrategies;

import java.util.logging.Level;

import agent.ModifiableIeposAgent;
import data.DataType;
import func.CostFunction;

/**
 * This strategy forces reorganization when percent in change of global cost is lower than some threshold.
 * Consequently, clear convergence is not observable. Selected plan following reorganization is the same plan
 * that was just chosen in last iteration prior to reorganization. Also note that for reorganization to happen,
 * there are actually 2 conditions that both should be met:
 *  - relative change between global !costs! in 2 consecutive iterations should be lower than certain threshold
 *  - relative change should be strictly greater than 0. This is to prevent from pointless reorganization that
 *    don't lead to any change.
 * 
 * @author Jovan N.
 *
 * @param <V>
 */
public class ReorganizationGlobalCostReduction<V extends DataType<V>> extends ReorganizationConvergence<V> {
	
	private static double				previousGlobalCost	=	0;
	private CostFunction<V>				globalCostFunc;
	

	public ReorganizationGlobalCostReduction(ModifiableIeposAgent agent) {
		super(agent);
		this.globalCostFunc = agent.getGlobalCostFunction();
	}
	
	@Override
	public void iterationAtRootEndedCallback() {
		DataType globalResponse = this.agent.getGlobalResponse();
		double globalCost = this.globalCostFunc.calcCost((V)globalResponse);
		if(this.shouldReorganize(globalCost)) {
			this.numReorganizations++;
			this.agent.forceReorganization();
		}
		//System.out.println("Global cost: " + globalCost + ", prev global cost: " + previousGlobalCost + ", % of change: " + (previousGlobalCost == 0 ? 0 : (previousGlobalCost-globalCost)/previousGlobalCost*100));
		ReorganizationGlobalCostReduction.previousGlobalCost = globalCost;
	}
	
	@Override
	public void selectPlan() {
		if(this.shouldChooseSelectedPlan()) {
			int selected = this.agent.getPlanSelector().selectPlan(this.agent);
			this.agent.setNumComputed(this.agent.getNumComputed() + this.agent.getPlanSelector().getNumComputations(this.agent));
	        this.agent.setSelectedPlan(selected);	        
			this.log(Level.FINER, "ModifiableIeposAgent:: preliminary selected plan equal to previous: " + 
						(this.agent.getSelectedPlanID() == this.agent.getPrevSelectedPlanID())); 	        
		} else {
		
		} 
	}
	
	boolean shouldReorganize(double globalCost) {
		return !agent.isIterationAfterReorganization() && this.globalCostProximity(globalCost);
	}
	
	@Override
	boolean shouldReorganize(DataType<V> globalResponse) {
		return false;
	}
	
	@Override
	boolean shouldChooseSelectedPlan() {
		return this.agent.getIteration() == 0 || !this.agent.isIterationAfterReorganization();
	}
	
	@Override
	public void screenshotAfterDOWNphase() { }
	
	boolean shouldTakeScreenshot() {
		return false;
	}
	
	private boolean globalCostProximity(double globalCost) {
		if(ReorganizationGlobalCostReduction.previousGlobalCost == 0) {
			return false;
		}
		double percentOfChange = Math.abs(ReorganizationGlobalCostReduction.previousGlobalCost - globalCost) / ReorganizationGlobalCostReduction.previousGlobalCost;
		return percentOfChange <= this.config.convergenceTolerance && percentOfChange > 0;
	}
	
	@Override
	public void prepareForReorganization() { }

}
