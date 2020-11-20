package treestructure.reorganizationstrategies;


import java.util.logging.Level;

import agent.ModifiableIeposAgent;
import config.Configuration;
import data.DataType;

/**
 * 
 * 
 * @author Jovan N.
 *
 * @param <V>
 */
public class ReorganizationConvergence<V extends DataType<V>> implements ReorganizationStrategy {
	
	Configuration config;
	ModifiableIeposAgent agent;
	
	DataType<V> previousGlobalResponse = null;
	
	int planToStartWithID = -1;
	static int numReorganizations = 0;
	
	public ReorganizationConvergence(ModifiableIeposAgent agent) {
		this.agent = agent;
		this.config = this.agent.getConfiguration();		
	}
	
	@Override
    public int getNumReorganizations() {
    	return ReorganizationConvergence.numReorganizations;
    }
    
	@Override
	public void resetCounter() {
		ReorganizationConvergence.numReorganizations = 0;
	}

	@Override
	public void iterationAtRootEndedCallback() {
		DataType<V> globalResponse = this.agent.getGlobalResponse();
		if(this.shouldReorganize(globalResponse)) {
			ReorganizationConvergence.numReorganizations++;
			this.agent.forceReorganization();
		}
		this.previousGlobalResponse = globalResponse.cloneThis();
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
			if(this.planToStartWithID != -1) {
				//System.out.println("USING PRE-SAVED SELECTED PLAN");
				this.agent.setSelectedPlan(this.planToStartWithID);
			}			
		} 
	}
	
	boolean shouldReorganize(DataType<V> globalResponse) {
		return !agent.isIterationAfterReorganization() && globalResponse.equals(this.previousGlobalResponse);
	}
	
	boolean shouldChooseSelectedPlan() {
		return this.agent.getIteration() == 0 || !this.agent.isIterationAfterReorganization();
	}	

	public void log(Level level, String message) {
		this.agent.log(level, message);
	}

	@Override
	public void screenshotAfterDOWNphase() {
		if(this.agent.getIteration() == this.config.memorizationOffset || 
		   this.agent.getIteration() == this.agent.getReorganizationIteration() + this.config.memorizationOffset) {
			//System.out.println("SCREENSHOT TAKEN!");
			if(this.shouldTakeScreenshot()) {
				//System.out.println("SCREENSHOT VALID!");
				this.planToStartWithID = this.agent.getSelectedPlanID();
			} else {
				//System.out.println("SCREENSHOT INVALID!");
				this.planToStartWithID = -1;
			}
		}
	}
	
	boolean shouldTakeScreenshot() {
		boolean cond1 = this.planToStartWithID != -1 && this.planToStartWithID != this.agent.getSelectedPlanID();
		boolean cond2 = this.agent.getIteration() == this.config.memorizationOffset;
		return cond1 || cond2;
	}
	
	@Override
	public void prepareForReorganization() {
		if(this.agent.getIteration() < this.config.memorizationOffset || 
		   this.agent.getIteration() < this.agent.getReorganizationIteration() + this.config.memorizationOffset) {
			//System.out.println("PREPARATION FINISHED!");
			this.planToStartWithID = -1;
		} else {
//			System.out.println("Iteration: " + this.agent.getIteration() + 
//							   ", reorganization iteration: " + this.agent.getReorganizationIteration() + 
//							   ", offset: " + this.config.reorganizationOffset);
		}
	}

}
