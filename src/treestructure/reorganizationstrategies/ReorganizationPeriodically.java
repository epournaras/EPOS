package treestructure.reorganizationstrategies;

import java.util.logging.Level;
import java.util.logging.Logger;

import agent.ModifiableIeposAgent;
import config.Configuration;
import data.DataType;

/**
 * 
 * @author Jovan N.
 *
 * @param <V>
 */
public class ReorganizationPeriodically<V extends DataType<V>> implements ReorganizationStrategy {
	
	private Configuration 				config;
	private ModifiableIeposAgent 		agent;
	static int							numReorganizations = 0;
	
	public ReorganizationPeriodically(ModifiableIeposAgent agent) {
		this.agent = agent;
		this.config = this.agent.getConfiguration();		
	}

	@Override
	public void iterationAtRootEndedCallback() {
		if(this.agent.getIteration() != 0	&&
		   this.agent.getIteration() % this.config.reorganizationPeriod == 0) {
			ReorganizationPeriodically.numReorganizations++;
			this.agent.forceReorganization();
		}
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
			// do nothing			
		}
	}
	
	private boolean shouldChooseSelectedPlan() {
		return this.agent.getIteration() == 0 || !this.agent.isIterationAfterReorganization();
	}

	public void log(Level level, String message) {
		this.agent.log(level, message);
	}

	@Override
	public void screenshotAfterDOWNphase() { }

	@Override
	public void prepareForReorganization() { }

	@Override
	public int getNumReorganizations() {
		return ReorganizationPeriodically.numReorganizations;
	}

	@Override
	public void resetCounter() {
		ReorganizationPeriodically.numReorganizations = 0;
	}

}
