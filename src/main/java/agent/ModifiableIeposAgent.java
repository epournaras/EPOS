package agent;

import java.util.List;
import java.util.Random;
import java.util.logging.Level;

import agent.ModifiableIeposAgent.AgentState;
import agent.ModifiableIeposAgent.ModifiableDownMessage;
import agent.logging.AgentLoggingProvider;
import config.Configuration;
import data.DataType;
import data.Plan;
import func.CostFunction;
import treestructure.ModifiableTreeClient;
import treestructure.ModifiableTreeProvider;
import treestructure.reorganizationstrategies.ReorganizationConvergence;
import treestructure.reorganizationstrategies.ReorganizationGlobalCostReduction;
import treestructure.reorganizationstrategies.ReorganizationNever;
import treestructure.reorganizationstrategies.ReorganizationPeriodically;
import treestructure.reorganizationstrategies.ReorganizationStrategy;

/**
 * This class supports IEPOS agents that allow for dynamic structural reorganization
 * during runtime of IEPOS. Main functionalities include:
 *  - checking if reorganization should take place from received Down Message
 *  - sets in motion sequence of actions to retrieve it's new place in new tree structure
 *  - after retrieval of new parent and children, becomes operational again, but as if it's iteration 0 again.
 *  
 * This class basically just overrides several methods that implement conditions, and generates DownMessage
 * that has field that broadcasts decision of the root if reorganization should take place.
 * 
 * @author jovan
 *
 * @param <V> type of data this class should operate on
 */
public class ModifiableIeposAgent<V extends DataType<V>> extends MultiObjectiveIEPOSAgent<V> {
	
	public enum AgentState {
        REORGANIZING,
        OPERATIONAL
    }
	
	private int						planToStartWithID;
	private int						reorganizationIteration;
	private boolean					shouldReorganize;
	
	private Configuration			config;
	private AgentState 				state;	
	private ReorganizationStrategy 	strategy;
    

	public ModifiableIeposAgent(Configuration config,								
								List<Plan<V>> possiblePlans, 								
								AgentLoggingProvider<? extends MultiObjectiveIEPOSAgent<V>> loggingProvider
								) {
		
		super(Configuration.numIterations, possiblePlans, (CostFunction<V>) config.globalCostFunc, 
			  config.localCostFunc, loggingProvider, config.simulationRNG.nextLong());
		this.state = AgentState.REORGANIZING;
		this.shouldReorganize = false;
		this.config = config;
		this.initStrategy();
	}
	
	private void initStrategy() {
		switch (this.config.reorganizationStrategy) {
		case PERIODICALLY:
			this.strategy = new ReorganizationPeriodically<V>(this);
			break;
		case ON_CONVERGENCE:
			this.strategy = new ReorganizationConvergence<V>(this);
			break;
		case GLOBAL_COST_REDUCTION:
			this.strategy = new ReorganizationGlobalCostReduction<V>(this);
			break;
		default:
			this.strategy = new ReorganizationNever<V>(this);
			break;
		}
	}
	
	public Configuration getConfiguration() {
		return this.config;
	}
	
	public int getReorganizationIteration() {
		return this.reorganizationIteration;
	}
	
	@Override
	public boolean isIterationAfterReorganization() {
		return this.iterationAfterReorganization == this.iteration;
	}
	
	public boolean isReorganizationIteration() {
		return this.iteration != this.reorganizationIteration;
	}
	
	/**
	 * Requests new parent and children from TreeClient, and sets <code>reorganizationIteration</code>
	 * to current iteration. TreeClient will send request to TreeServer and once new parent and children are received,
	 * agent can continue as if it's first iteration.
	 */
	private void requestNewTreeView() {
		this.reorganizationIteration = this.iteration;
		this.getTreeClient().requestNewTreeView();
	}
	
	public ModifiableTreeProvider getTreeProvider() {
		return (ModifiableTreeProvider) this.getPeer().getPeerletOfType(ModifiableTreeProvider.class);
	}
	
	public ModifiableTreeClient getTreeClient() {
		return (ModifiableTreeClient) this.getPeer().getPeerletOfType(ModifiableTreeClient.class);
	}
	
	@Override
	public int getNumReorganizations() {
//		if(this.strategy instanceof ReorganizationConvergence) {
//			ReorganizationConvergence<V> convStrategy = (ReorganizationConvergence<V>) this.strategy;
//			return convStrategy.getNumReorganizations();
//		} else {
//			return super.getNumReorganizations();
//		}
		return this.strategy.getNumReorganizations();
	}
	
	@Override
	/**
	 * Invoked just before request for new TreeView is sent.
	 * From this point, until new TreeView is received, agent is in reorganization,
	 * and cannot communicate.
	 */
	public void reset() {
		super.reset();
		this.log(Level.FINER, "ModifiableIeposAgent::reset()");
	}
	
	@Override
	/**
	 * Callback method that notifies that new parent and children are received and set.
	 * Agent is back in operational mode and continues IEPOS algorithm by keeping in mind that
	 * in the iteration immediately after reorganization, agent should act as if it's iteration 0.
	 */
	void treeViewIsSet() {
		if(this.isReorganizationIteration() && this.iteration != this.numIterations) {
			this.log(Level.SEVERE, "Iteration count has increased during reorganization from: " + 
					 this.reorganizationIteration + " to " + this.iteration);
		}
		this.state = AgentState.OPERATIONAL;
		this.shouldReorganize = false;
		this.iterationAfterReorganization = (this.iteration == this.numIterations) ? 0 : this.iteration + 1;		
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//																								UP PHASE

	@Override
	/**
	* New iterations should be initialized if both of the following conditions are met:
	*  - iteration is not 0 iteration. In 0 iteration, there is no previous info, so re-initializing makes no sense
	*  - iteration is not first iteration of reorganized tree. Again, all info is reset during reorganization,
	*     and agent acts in this iteration as if it's iteration 0.
	*/
	boolean conditionForInitializingIteration() {
		boolean old = super.conditionForInitializingIteration();
		boolean newCond = !this.isIterationAfterReorganization();
		boolean condition = old & newCond;
		//this.log(Level.FINEST, "ModifiableIeposAgent:: condition for initializing iteration is: " + condition);
		return condition;
	}
	
	@Override
	void initAtIteration0() {
		if(this.isRoot()  && this.iteration == 0) {
			this.strategy.resetCounter();
		}		
	}

	void doIfConditionToStartNewIterationIsNOTMet() { }

	@Override
	/**
	* New iterations should be started if both following conditions are met:
	*  - iteration does not exceed maximal allowed number of iterations
	*  - node is in OPERATIONAL mode
	*/
	boolean conditionToStartNewIteration() {
		boolean old = super.conditionToStartNewIteration();
		boolean condition = old & this.state.equals(AgentState.OPERATIONAL);
		return condition;
	}
	
	@Override
	void selectPlan() {
		this.strategy.selectPlan();        
    }
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//																								DOWN PHASE

	@Override
	DownMessage atRoot(UpMessage rootMsg) {
		this.globalResponse.set(rootMsg.subtreeResponse);
		this.iterationEndedAtRoot();
		return new ModifiableDownMessage(rootMsg.subtreeResponse, true, rootMsg.discomfortSum, rootMsg.discomfortSumSqr, this.shouldReorganize());
	}

	/**
	* By the end of iteration at root (once new global response is known), it performs following actions:
	*  - if Reorganization Strategy Peerlet is present, then:
	*    - strategy calculates whether reorganization is to take place, and if so, sets shouldReorganize flag!
	*/
	private void iterationEndedAtRoot() {
		if(this.isRoot()) {
			this.strategy.iterationAtRootEndedCallback();
		}		
	}

	@Override
	/**
	* WARNING: global response here is cloned, even though it wasn't done originally!
	*/
	DownMessage generateDownMessage(int i) {
		return new ModifiableDownMessage(this.globalResponse.cloneThis(), this.approvals.get(i), this.globalDiscomfortSum, this.globalDiscomfortSumSqr, this.shouldReorganize());
	}

	@Override
	/**
	* This method processes received Down Message additionally.
	* Concretely, here it extracts info if reorganization should take place.
	*/
	void processDownMessageMore(DownMessage parentMsg) {
		if(parentMsg instanceof ModifiableIeposAgent.ModifiableDownMessage) {
			ModifiableDownMessage msgFromParent = (ModifiableDownMessage)parentMsg;
			this.shouldReorganize = msgFromParent.shouldReorganize();
		}
	}

	@Override
	/**
	* Finalizes processing in DOWN phase.
	* Concretely, it sets in motion sequence of actions to find it's place in reorganized tree structure,
	* if reorganization should take place. This method is invoked after all messages to children are sent.
	*/
	void finalizeDownPhase(DownMessage parentMsg) {
		if(parentMsg instanceof ModifiableIeposAgent.ModifiableDownMessage) {
			ModifiableDownMessage msgFromParent = (ModifiableDownMessage)parentMsg;
			this.strategy.screenshotAfterDOWNphase();
			if(msgFromParent.shouldReorganize()) {
				this.actionsToReorganize();
			} else {
				// do nothing
			}
		}
	}

	/**
	* Sequence of actions each node takes when notified that reorganization is to take place. It includes:
	*  - handling saved plans if 
	*/
	private void actionsToReorganize() {
		this.strategy.prepareForReorganization();
		this.parent = null;
		this.children.clear();
		this.state = AgentState.REORGANIZING;
		this.requestNewTreeView();
	}

	/**
	* Gives the flag that is passed on during DOWN phase from root to children
	* and that indicates if reorganization of the tree structure is about to take place
	* @return
	*/
	private boolean shouldReorganize() {
		return this.shouldReorganize;
	}

	/**
	* When Reorganization Strategy Peerlet decides structural reorganization is to take place,
	* this is callback method it invokes.
	*/
	public void forceReorganization() {
		if(this.isRoot()) {
			this.shouldReorganize = true;
			this.log(Level.INFO, "ModifiableIeposAgent:: reorganization forced!)");
		}
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////


	public class ModifiableDownMessage extends MultiObjectiveIEPOSAgent.DownMessage {

		boolean reorganization = false;
	
		public ModifiableDownMessage(V globalResponse, 				boolean approved, 
									 double globalDiscomfortSum, 	double globalDiscomfortSumSqr, 	boolean reorganization) {
			super(globalResponse, approved, globalDiscomfortSum, globalDiscomfortSumSqr);
			this.reorganization = reorganization;
		}
	
		public boolean shouldReorganize() {
			return this.reorganization;
		}

	}

}
