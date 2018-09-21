package treestructure.reorganizationstrategies;

import agent.ModifiableIeposAgent;
import data.DataType;

/**
 * Implements Reorganization strategy NEVER. This strategy disables reorganization, 
 * the code runs as old IEPOS.
 * 
 * @author jovan
 *
 * @param <V>
 */
public class ReorganizationNever<V extends DataType<V>> implements ReorganizationStrategy {	
	
	private ModifiableIeposAgent agent;
	
	public ReorganizationNever(ModifiableIeposAgent agent) {
		this.agent = agent;	
	}
	

	@Override
	public void iterationAtRootEndedCallback() { }

	@Override
	/**
	 * Classic (old) way of setting selected plan
	 */
	public void selectPlan() {
		int selected = this.agent.getPlanSelector().selectPlan(this.agent);		
		this.agent.setNumComputed(this.agent.getNumComputed() + this.agent.getPlanSelector().getNumComputations(this.agent));
        this.agent.setSelectedPlan(selected);
	}

	@Override
	public void screenshotAfterDOWNphase() { }


	@Override
	public void prepareForReorganization() { }


	@Override
	public int getNumReorganizations() {
		return 0;
	}
	@Override
	public void resetCounter() { }

}
