/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package agent;

import data.Plan;
import func.CostFunction;
import func.PlanCostFunction;
import agent.IeposAgent.DownMessage;
import agent.logging.AgentLoggingProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import data.DataType;

/**
 * This agent performs the I-EPOS algorithm for combinatorial optimization.
 *
 * @author Peter
 * @param <V> the type of the data this agent should handle
 */
public class IeposAgent<V extends DataType<V>> extends IterativeTreeAgent<V, IeposAgent<V>.UpMessage, IeposAgent<V>.DownMessage> {

    // agent info
    Plan<V> 									prevSelectedPlan;
    int 										prevSelectedPlanID;	
    V 											aggregatedResponse;
    V 											prevAggregatedResponse;

    // per child info
    private final List<V> 						subtreeResponses 			= 	new ArrayList<>();
    private final List<V> 						prevSubtreeResponses 		= 	new ArrayList<>();
    private final List<Boolean> 				approvals 					= 	new ArrayList<>();

    // misc
    Optimization 								optimization;
    double 										lambda; 					// parameter for lambda-PREF local cost minimization
    private PlanSelector<IeposAgent<V>, V> 		planSelector;
    

    /**
     * Creates a new IeposAgent. Using the same RNG seed will result in the same
     * execution order in a simulation environment.
     *
     * @param numIterations the number of iterations
     * @param possiblePlans the plans this agent can choose from
     * @param globalCostFunc the global cost function
     * @param localCostFunc the local cost function
     * @param loggingProvider the object that extracts data from the agent and
     * writes it into its log.
     * @param seed a seed for the RNG
     */
    public IeposAgent(int numIterations, List<Plan<V>> possiblePlans, CostFunction<V> globalCostFunc, PlanCostFunction<V> localCostFunc, AgentLoggingProvider<? extends IeposAgent<V>> loggingProvider, long seed) {
        super(numIterations, possiblePlans, globalCostFunc, localCostFunc, loggingProvider, seed);
        this.optimization = new Optimization(this.random);
        this.lambda = 0;
        this.planSelector = new IeposPlanSelector<>();
    }

    /**
     * Sets lambda, the traidoff between global and local cost minimization. A
     * value of 0 indicates pure global cost minimization, while a value of 1
     * indicates pure local cost minimization.
     *
     * @param lambda traidoff between global and local cost minimization
     */
    public void setLambda(double lambda) {
        this.lambda = lambda;
    }

    /**
     * An I-EPOS agent can have different strategies for plan selection. The
     * plan selector decides which plan to select given the current state of the
     * system.
     *
     * @param planSelector the plan selector
     */
    public void setPlanSelector(PlanSelector<IeposAgent<V>, V> planSelector) {
        this.planSelector = planSelector;
    }

    public V getGlobalResponse() {
        return globalResponse.cloneThis();
    }
    
    public Plan getPrevSelectedPlan() {
    	return this.prevSelectedPlan;
    }
    
    public int getPrevSelectedPlanID() {
    	return this.prevSelectedPlanID;
    }
    
    public Optimization getOptimization() {
    	return this.optimization;
    }
    
    /**
     * Sets selected plan and selected plan indice
     * @param i
     */
    public void setSelectedPlan(int i) {
    	if(i == -1) {
    		System.out.println("Node: " + this.getPeer().getIndexNumber() + ", iteration: " + this.getIteration() + " CAN'T FIND PLAN INDEXED WITH -1!");
    	}
    	this.selectedPlan = this.possiblePlans.get(i);
    	this.selectedPlanID = i;
    }

    @Override
    /**
     * NOT INVOKED ANYWHERE FOR NOW!
     * 
     * 1. aggregated response, previous aggregated response and global response:
     *    		is a vector of the same size as a possible plan, with random values in it
     * 2. previous selected plan:
     * 			is a vector of the same size as a possible plan, with random values in it, score and index are copied as is
     */
    void initPhase() {
    	this.aggregatedResponse 	= createValue();
        this.prevAggregatedResponse = createValue();
        this.globalResponse 		= createValue();
        this.prevSelectedPlan 		= createPlan();
        this.prevSelectedPlanID		= -1;
    }

    @Override
    /**
     * The beginning of every iteration:
     * 		- previously selected plan <= selected plan from last epoch
     * 		- previously aggregated response <= aggregated response from last epoch
     * 		- previous subtree responses (selected plans of children) <= selected plans of children from previous epoch
     * 
     * 		- selected plan is nulled, does not exist
     * 		- aggregated responses and selected plans of children are cleared
     * 		- all approvals are cleared
     */
    void initIteration() {
    	if (this.conditionForInitializingIteration()) {
            this.prevSelectedPlan = this.selectedPlan;
            this.prevSelectedPlanID = this.selectedPlanID;
            this.prevAggregatedResponse.set(this.aggregatedResponse);
            this.prevSubtreeResponses.clear();
            this.prevSubtreeResponses.addAll(this.subtreeResponses);
            
            //this.log(Level.FINEST, "IeposAgent::initIteration() passed the condition.");

            this.selectedPlan = null;
            this.aggregatedResponse.reset();
            this.subtreeResponses.clear();
            this.approvals.clear();
        } else {
        	
        }
    }
    
    boolean conditionForInitializingIteration() {
    	return this.iteration > 0;
    }

    @Override
    UpMessage up(List<UpMessage> childMsgs) {
        for (UpMessage msg : childMsgs) {
            subtreeResponses.add(msg.subtreeResponse);
        }
        try {
        	this.aggregate();					// for leaf-nodes nothing happens here
            this.selectPlan();
        } catch(Exception e) {
        	e.printStackTrace();
        }
        return informParent();
    }

    @Override
    DownMessage atRoot(UpMessage rootMsg) {
        return new DownMessage(rootMsg.subtreeResponse, true);
    }

    @Override
    List<DownMessage> down(DownMessage parentMsg) {
    	this.updateGlobalResponse(parentMsg);
        this.approveOrRejectChanges(parentMsg);
        this.processDownMessageMore(parentMsg);
        return this.informChildren();
    }
    
    @Override
    void finalizeDownPhase(DownMessage parentMsg) { }
    
    void processDownMessageMore(DownMessage parentMsg) { }

    /**
     *  1. in first iteration, all children's actions are approved
     *  2. for iteration > 0 calculates preliminary approvals (delta values):
     *     - calculate all possible combinations of accepting/rejecting children's subtree
     *     - choose the combination that minimizes current global cost
     *     - increases <code>numComputed</code> by the number of combinations (that algorithm had to see to choose the minimal one)
     *     - computed preliminary delta values, 1 if approved, 0 if rejected. Preliminary selected plan of a child is approved if
     *       it appeared in chosen combination, and it is rejected if it's previous selected plan appeared in chosen combination.
     *  3. preliminary selected plans of children are set. Preliminary aggregated response represent sum of all preliminary selected plans.
     */
    void aggregate() {
        if (iteration == 0) {
            for (int i = 0; i < children.size(); i++) {
                approvals.add(true);
            }
        } else if (children.size() > 0) {
            List<List<V>> choicesPerAgent = new ArrayList<>();
            for (int i = 0; i < children.size(); i++) {
                List<V> choices = new ArrayList<>();
                choices.add(prevSubtreeResponses.get(i));
                choices.add(subtreeResponses.get(i));
                choicesPerAgent.add(choices);
            }
            List<V> combinations = optimization.calcAllCombinations(choicesPerAgent);

            V othersResponse = globalResponse.cloneThis();
            for (V prevSubtreeResponce : prevSubtreeResponses) {
                othersResponse.subtract(prevSubtreeResponce);
            }
            int selectedCombination = optimization.argmin(globalCostFunc, combinations, othersResponse);
            this.setNumComputed(this.getNumComputed() + combinations.size());

            List<Integer> selections = optimization.combinationToSelections(selectedCombination, choicesPerAgent);
            for (int selection : selections) {
                approvals.add(selection == 1);
            }
        }
        for (int i = 0; i < children.size(); i++) {
            V prelSubtreeResponse = approvals.get(i) ? subtreeResponses.get(i) : prevSubtreeResponses.get(i);
            subtreeResponses.set(i, prelSubtreeResponse);
            aggregatedResponse.add(prelSubtreeResponse);
        }
    }

    void selectPlan() {
    	int selected = this.planSelector.selectPlan(this);
    	this.setNumComputed(this.getNumComputed() + planSelector.getNumComputations(this));
	    this.setSelectedPlan(selected);
    }

    /**
     * Computes final subtree response of the agent for UP phase. It consists of:
     *  - preliminary selected plan of the agent in UP phase
     *  - accepted subtree responses from all of its children
     * In other words, everything that parent of this node receives from this agent is:
     *   aggregated responses from all children of this agent + selected plan of this agent
     * @return
     */
    private UpMessage informParent() {
        V subtreeResponse = aggregatedResponse.cloneThis();
        subtreeResponse.add(selectedPlan.getValue());
        return new UpMessage(subtreeResponse);
    }

    private void updateGlobalResponse(DownMessage parentMsg) {
        globalResponse.set(parentMsg.globalResponse);
    }

    void approveOrRejectChanges(DownMessage parentMsg) {
        if (!parentMsg.approved) {
            selectedPlan = prevSelectedPlan;
            aggregatedResponse.set(prevAggregatedResponse);
            subtreeResponses.clear();
            subtreeResponses.addAll(prevSubtreeResponses);
            Collections.fill(approvals, false);
        }
    }
    
    @Override
    /**
     * Clears the following:
     *  - parent is set to null
     *  - list of children is cleared
     *  - numTransmitted, numComputed, cumTransmitted and cumComputed are all set to 0
     *  - aggregatedResponse and prevAggregatedResponse are re-initialized
     *  - previousSelectedPlan is re-initialized
     *  - globalResponse is re-initialized
     *  - subtreeResponses and prevSubtreeResponses are cleared
     *  - approvals are cleared
     *  
     * Note that selectedPlan stays as is, it will be chosen as next selectedPlan
     * in the beginning of new iteration after reorganization.
     */
    public void reset() {
    	super.reset();
    	
    	this.globalResponse			=	createValue();
    	this.aggregatedResponse 	= 	createValue();
        this.prevAggregatedResponse = 	createValue();
        this.globalResponse 		= 	createValue();
        this.prevSelectedPlan 		= 	createPlan();
        this.prevSelectedPlanID		=	-1;
        
        this.subtreeResponses.clear();
        this.prevSubtreeResponses.clear();
        this.approvals.clear(); 	
    }

    private List<DownMessage> informChildren() {
        List<DownMessage> msgs = new ArrayList<>();
        for (int i = 0; i < children.size(); i++) {
            msgs.add(new DownMessage(globalResponse, approvals.get(i)));
        }
        return msgs;
    }

    // message classes
    class UpMessage extends IterativeTreeAgent.UpMessage {

        public V subtreeResponse;

        public UpMessage(V subtreeResponse) {
            this.subtreeResponse = subtreeResponse;
        }

        @Override
        public int getNumTransmitted() {
            return 1;
        }
    }

    class DownMessage extends IterativeTreeAgent.DownMessage {

        public V globalResponse;
        public boolean approved;

        public DownMessage(V globalResponse, boolean approved) {
            this.globalResponse = globalResponse;
            this.approved = approved;
        }

        @Override
        public int getNumTransmitted() {
            return 1;
        }
    }
}
