package agent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.IntStream;

import agent.logging.AgentLoggingProvider;
import agent.planselection.MultiObjectiveIeposPlanSelector;
import config.Configuration;
import data.DataType;
import data.Plan;
import func.CostFunction;
import func.PlanCostFunction;

/**
 * 
 * @author jovan
 *
 * @param <V>
 */
public class MultiObjectiveIEPOSAgent<V extends DataType<V>> extends IterativeTreeAgent<V, 
																						MultiObjectiveIEPOSAgent<V>.UpMessage, 
																						MultiObjectiveIEPOSAgent<V>.DownMessage> {

	// agent info
    Plan<V> 											prevSelectedPlan;
    int 												prevSelectedPlanID;
    V 													aggregatedResponse;
    V 													prevAggregatedResponse;
    
    double												globalDiscomfortSum				=	0;
    double												globalDiscomfortSumSqr			=	0;
    double												aggregatedDiscomfortSum			=	0;
    double												aggregatedDiscomfortSumSqr		=	0;
    double												prevAggregatedDiscomfortSum		=	0;
    double 												prevAggregatedDiscomfortSumSqr	=	0;
    
    // per child info
    final List<V> 										subtreeResponses 				= 	new ArrayList<>();
    final List<V> 										prevSubtreeResponses 			= 	new ArrayList<>();
    final List<Boolean> 								approvals 						= 	new ArrayList<>();
    
    final List<Double>									subtreeDiscomfortSum			=	new ArrayList<>();
    final List<Double>									subtreeDiscomfortSumSqr			=	new ArrayList<>();
    final List<Double>									prevSubtreeDiscomfortSum		=	new ArrayList<>();
    final List<Double>									prevSubtreeDiscomfortSumSqr		=	new ArrayList<>();

    // misc
    Optimization 										optimization;
    double 												lambda;
    double												alpha;
    double 												beta;
    double												gamma;
    double												delta;
    PlanSelector<MultiObjectiveIEPOSAgent<V>, V> 		planSelector;
    
    
    private boolean										convergenceReached				=	false;

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
    public MultiObjectiveIEPOSAgent(int numIterations, 
    								List<Plan<V>> possiblePlans, 
    								CostFunction<V> globalCostFunc, 
    								PlanCostFunction<V> localCostFunc, 
    								AgentLoggingProvider<? extends MultiObjectiveIEPOSAgent<V>> loggingProvider, 
    								long seed) {
        super(numIterations, possiblePlans, globalCostFunc, localCostFunc, loggingProvider, seed);
        this.optimization = new Optimization(this.random);
        this.lambda = 0;
        this.alpha = 0;
        this.beta = 0;
        this.planSelector = new MultiObjectiveIeposPlanSelector<>();
    }
    
    public void setLocalCostWeight(double beta) {
    	this.beta = beta;
    	this.delta = beta;
    }
    
    public void setUnfairnessWeight(double alpha) {
    	this.alpha = alpha;
    	this.gamma = alpha;
    }
    
    public double getLocalCostWeight() {
    	return this.beta;
    }
    
    public double getUnfairnessWeight() {
    	return this.alpha;
    }

    /**
     * An I-EPOS agent can have different strategies for plan selection. The
     * plan selector decides which plan to select given the current state of the
     * system.
     *
     * @param planSelector the plan selector
     */
    public void setPlanSelector(PlanSelector<MultiObjectiveIEPOSAgent<V>, V> planSelector) {
        this.planSelector = planSelector;
    }
    
    public PlanSelector<MultiObjectiveIEPOSAgent<V>, V> getPlanSelector() {
    	return this.planSelector;
    }

    public V getGlobalResponse() {
        return globalResponse.cloneThis();
    }
    
    public Plan<V> getPrevSelectedPlan() {
    	return this.prevSelectedPlan;
    }
    
    public V getAggregatedResponse() {
    	return this.aggregatedResponse;
    }
    
    public V getPrevAggregatedResponse() {
    	return this.prevAggregatedResponse;
    }
    
    public int getPrevSelectedPlanID() {
    	return this.prevSelectedPlanID;
    }
    
    public Optimization getOptimization() {
    	return this.optimization;
    }
    
    public double getGlobalDiscomfortSum() {
    	return this.globalDiscomfortSum;
    }
    
    public double getGlobalDiscomfortSumSqr() {
    	return this.globalDiscomfortSumSqr;
    }
    
    public double getAggregatedDiscomfortSum() {
    	return this.aggregatedDiscomfortSum;
    }
    
    public double getAggregatedDiscomfortSumSqr() {
    	return this.aggregatedDiscomfortSumSqr;
    }
    
    public double getPrevAggregatedDiscomfortSum() {
    	return this.prevAggregatedDiscomfortSum;
    }
    
    public double getPrevAggregatedDiscomfortSumSqr() {
    	return this.prevAggregatedDiscomfortSumSqr;
    }
    
    public int getNumAgents() {
    	return this.numAgents;
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
//    	this.log(Level.FINER, "MultiObjectiveIeposAgent::initPhase()");
        this.aggregatedResponse 	= createValue();
        this.prevAggregatedResponse = createValue();
        this.globalResponse 		= createValue();
        this.prevSelectedPlan 		= createPlan();
        this.prevSelectedPlanID		= -1;
        
        this.globalDiscomfortSum	= 0;
        this.globalDiscomfortSumSqr	= 0;
        
//        this.log(Level.FINER, "prevSelectedPlan's score is: " + this.prevSelectedPlan.getScore());
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
    	this.log(Level.FINER, "MultiObjectiveIeposAgent::initIteration()");
    	if(!this.isLeaf()) {
    		if(this.children.size() > 1) {
    			this.log(Level.FINER, "Children: " + this.children.get(0) + ", " + this.children.get(1));
    		}    		
    	}
    	
        if (this.conditionForInitializingIteration()) {
        	
            this.prevSelectedPlan = this.selectedPlan;
            this.prevSelectedPlanID = this.selectedPlanID;
            this.prevAggregatedResponse.set(this.aggregatedResponse);
            this.prevSubtreeResponses.clear();
            this.prevSubtreeResponses.addAll(this.subtreeResponses);

            this.selectedPlan = null;
            this.aggregatedResponse.reset();
            this.subtreeResponses.clear();            
            this.approvals.clear();            
            
            this.prevSubtreeDiscomfortSum.clear();
            this.prevSubtreeDiscomfortSum.addAll(this.subtreeDiscomfortSum);
            this.prevSubtreeDiscomfortSumSqr.clear();
            this.prevSubtreeDiscomfortSumSqr.addAll(this.subtreeDiscomfortSumSqr);
            this.subtreeDiscomfortSum.clear();
            this.subtreeDiscomfortSumSqr.clear();
            
            this.prevAggregatedDiscomfortSum = this.aggregatedDiscomfortSum;
            this.prevAggregatedDiscomfortSumSqr = this.aggregatedDiscomfortSumSqr;
            this.aggregatedDiscomfortSum = 0;
            this.aggregatedDiscomfortSumSqr = 0;
            
            this.convergenceReached = false;
            
            this.log(Level.FINER, "initIteration:");
            if(this.isLeaf()) {
            	this.log(Level.FINER, "prevSubtreeDiscomfortSum:" + " empty = " + this.prevSubtreeDiscomfortSum.isEmpty());
            	this.log(Level.FINER, "prevSubtreeDiscomfortSumSqr:" + " empty = " + this.prevSubtreeDiscomfortSumSqr.isEmpty());
            } else {
            	if(this.children.size() > 1) {
            		this.log(Level.FINER, "prevSubtreeDiscomfortSum: " + this.prevSubtreeDiscomfortSum.get(0) + ", " + this.prevSubtreeDiscomfortSum.get(1));
                    this.log(Level.FINER, "prevSubtreeDiscomfortSumSqr: " + this.prevSubtreeDiscomfortSumSqr.get(0) + ", " + this.prevSubtreeDiscomfortSumSqr.get(1));
            	} else {
            		this.log(Level.FINER, "prevSubtreeDiscomfortSum: " + this.prevSubtreeDiscomfortSum.get(0));
                    this.log(Level.FINER, "prevSubtreeDiscomfortSumSqr: " + this.prevSubtreeDiscomfortSumSqr.get(0));
            	}                
            }
            this.log(Level.FINER, "subtreeDiscomfortSum:" + " empty = " + this.subtreeDiscomfortSum.isEmpty());
        	this.log(Level.FINER, "subtreeDiscomfortSumSqr:" + " empty = " + this.subtreeDiscomfortSumSqr.isEmpty());
        	this.log(Level.FINER, "prevAggregatedDiscomfortSum: " + this.prevAggregatedDiscomfortSum);
        	this.log(Level.FINER, "prevAggregatedDiscomfortSumSqr: " + this.prevAggregatedDiscomfortSumSqr);
        	this.log(Level.FINER, "aggregatedDiscomfortSum: " + this.aggregatedDiscomfortSum);
        	this.log(Level.FINER, "aggregatedDiscomfortSumSqr: " + this.aggregatedDiscomfortSumSqr);
        	this.log(Level.FINER, "globalDiscomfortSum: " + this.globalDiscomfortSum);
        	this.log(Level.FINER, "globalDiscomfortSumSqr: " + this.globalDiscomfortSumSqr);
            
        } else {
        	this.initAtIteration0();
        }
    }
    
    boolean conditionForInitializingIteration() {
    	return this.iteration > 0;
    }
    
    void initAtIteration0() { }

    @Override
    UpMessage up(List<UpMessage> childMsgs) {
        for (UpMessage msg : childMsgs) {
            this.subtreeResponses.add(msg.subtreeResponse);
            this.subtreeDiscomfortSum.add(msg.discomfortSum);
            this.subtreeDiscomfortSumSqr.add(msg.discomfortSumSqr);
        }
        this.log(Level.FINER, "up:");
//        this.log(Level.FINER, "Number of agents is: " + this.numAgents);
        try {
        	if(!this.isLeaf()) {
        		if(this.children.size() > 1) {
        			this.log(Level.FINER, "subtreeDiscomfortSum: " + this.subtreeDiscomfortSum.get(0) + ", " + this.subtreeDiscomfortSum.get(1));
                    this.log(Level.FINER, "subtreeDiscomfortSumSqr: " + this.subtreeDiscomfortSumSqr.get(0) + ", " + this.subtreeDiscomfortSumSqr.get(1));
        		} else {
        			this.log(Level.FINER, "subtreeDiscomfortSum: " + this.subtreeDiscomfortSum.get(0));
                    this.log(Level.FINER, "subtreeDiscomfortSumSqr: " + this.subtreeDiscomfortSumSqr.get(0));
        		}
            	
            } else {
            	this.log(Level.FINER, " leaf!");
            }  
            
            this.log(Level.FINER, " ^&^&^&^&^&");
        } catch(Exception e) {
        	e.printStackTrace();
        }        
        
        try {
        	this.aggregateExtended();
            this.selectPlan();
        } catch(Exception e) {
        	e.printStackTrace();
        }        
        return this.informParent();
    }

    @Override
    DownMessage atRoot(UpMessage rootMsg) {
        return new DownMessage(rootMsg.subtreeResponse, true, rootMsg.discomfortSum, rootMsg.discomfortSumSqr);   // root always accepts
    }

    @Override
    List<DownMessage> down(DownMessage parentMsg) {
        this.updateGlobalResponse(parentMsg);
        this.updateGlobalDiscomfortScores(parentMsg);
        this.approveOrRejectChanges(parentMsg);
        this.processDownMessageMore(parentMsg);
        return this.informChildren();
    }
    
    @Override
    void finalizeDownPhase(DownMessage parentMsg) { 
    		//TODO for further implementations.
    		this.log(Level.WARNING, "Non implemented.");
    }
    
    void processDownMessageMore(DownMessage parentMsg) { }
    
    private void aggregateExtended() {
//    	System.out.println("EXTENDED!");
        if (this.isIterationAfterReorganization()) {
            for (int i = 0; i < this.children.size(); i++) {
                approvals.add(true);
            }
        } else if (children.size() > 0) {
            List<List<V>> 			responsesPerChild			=	new ArrayList<>();
            List<List<Double>> 		discomfortSumPerChild		=	new ArrayList<>();
            List<List<Double>>		discomfortSumSqrPerChild	=	new ArrayList<>();
            
            for (int i = 0; i < this.children.size(); i++) {
                List<V> responseChoices = new ArrayList<>();
                List<Double> discomfortSumChoices = new ArrayList<>();
                List<Double> discomfortSumSqrChoices = new ArrayList<>();
                
                responseChoices.add(this.prevSubtreeResponses.get(i));
                responseChoices.add(this.subtreeResponses.get(i));
                discomfortSumChoices.add(this.prevSubtreeDiscomfortSum.get(i));
                discomfortSumChoices.add(this.subtreeDiscomfortSum.get(i));
                discomfortSumSqrChoices.add(this.prevSubtreeDiscomfortSumSqr.get(i));
                discomfortSumSqrChoices.add(this.subtreeDiscomfortSumSqr.get(i));
                
                responsesPerChild.add(responseChoices);
                discomfortSumPerChild.add(discomfortSumChoices);
                discomfortSumSqrPerChild.add(discomfortSumSqrChoices);
            }
            
            List<V> 		responseCombinations 			= this.optimization.calcAllCombinations(responsesPerChild);
            List<Double> 	discomfortSumCombinations		= this.optimization.calculateAllCombinationsForDiscomfortScores(discomfortSumPerChild);
            List<Double>	discomfortSumSqrCombinations	= this.optimization.calculateAllCombinationsForDiscomfortScores(discomfortSumSqrPerChild);
            
            
            V 		othersResponse 			= this.globalResponse.cloneThis();
            double 	othersDiscomfortSum		= this.globalDiscomfortSum;
            double	othersDiscomfortSumSqr	= this.globalDiscomfortSumSqr;
            
            for (V prevSubtreeResponce : this.prevSubtreeResponses) {
                othersResponse.subtract(prevSubtreeResponce);
            }
            for(Double prevDiscomfortResponse : this.prevSubtreeDiscomfortSum) {
            	othersDiscomfortSum -= prevDiscomfortResponse;
            }
            for(Double prevDiscomfortResponse : this.prevSubtreeDiscomfortSumSqr) {
            	othersDiscomfortSumSqr -= prevDiscomfortResponse;
            }
                       
            
            int selectedCombination = this.optimization.argmin(globalCostFunc, 
            												   responseCombinations,
            												   discomfortSumCombinations,
            												   discomfortSumSqrCombinations, 
            												   othersResponse,
            												   othersDiscomfortSum,
            												   othersDiscomfortSumSqr,
            												   this.getUnfairnessWeight(),
            												   this.getLocalCostWeight(),
            												   this.numAgents);
            
            this.setNumComputed(this.getNumComputed() + responseCombinations.size());
            
            List<Integer> selections = this.optimization.combinationToSelections(selectedCombination, responsesPerChild);
            for (int selection : selections) {
                this.approvals.add(selection == 1);
            }
            
        } else {
        	// children.size() <= 0
        }
        
        this.log(Level.FINER, "aggregate:");
        if(this.isLeaf()) {
        	
        } else {
        	if(!this.prevSubtreeDiscomfortSum.isEmpty()) {
        		
//        		if(this.getPeer().getIndexNumber() == 189) {
//        			System.out.println("NUMBER OF CHILDREN >>>-->>> : " + this.children.size());
//        			System.out.println("NUMBER OF CHILDREN >>>-->>> : " + this.prevSubtreeDiscomfortSum.size());
//        			System.out.println("NUMBER OF CHILDREN >>>-->>> : " + this.subtreeDiscomfortSum.size());
//        			System.out.println("NUMBER OF CHILDREN >>>-->>> : " + this.prevSubtreeResponses.size());
//        			System.out.println("NUMBER OF CHILDREN >>>-->>> : " + this.subtreeResponses.size());
//        		}
        		
        		if(this.children.size() > 1) {
        			this.log(Level.FINER, "prevSubtreeDiscomfortSum: " + this.prevSubtreeDiscomfortSum.get(0) + ", " + this.prevSubtreeDiscomfortSum.get(1));
        		} else {
        			this.log(Level.FINER, "prevSubtreeDiscomfortSum: " + this.prevSubtreeDiscomfortSum.get(0));
        		}
        		
//                this.log(Level.FINER, "prevSubtreeDiscomfortSumSqr: " + this.prevSubtreeDiscomfortSumSqr.get(0) + ", " + this.prevSubtreeDiscomfortSumSqr.get(1));
        	}     
        	if(this.children.size() > 1) {
                this.log(Level.FINER, "newsubtreeDiscomfortSum: " + this.subtreeDiscomfortSum.get(0) + ", " + this.subtreeDiscomfortSum.get(1));
        	} else {
                this.log(Level.FINER, "newsubtreeDiscomfortSum: " + this.subtreeDiscomfortSum.get(0));
        	}
//            this.log(Level.FINER, "newsubtreeDiscomfortSumSqr: " + this.subtreeDiscomfortSumSqr.get(0) + ", " + this.subtreeDiscomfortSumSqr.get(1));
        }
         
        for (int i = 0; i < this.children.size(); i++) {
            V prelSubtreeResponse = this.approvals.get(i) ? 
            						this.subtreeResponses.get(i) : 
            						this.prevSubtreeResponses.get(i);
            double prelDiscomfortSum = this.approvals.get(i) ? 
            						   this.subtreeDiscomfortSum.get(i) : 
            						   this.prevSubtreeDiscomfortSum.get(i);
            double prelDiscomfortSumSqr = this.approvals.get(i) ? 
            							  this.subtreeDiscomfortSumSqr.get(i) : 
            							  this.prevSubtreeDiscomfortSumSqr.get(i);
            							  
            this.subtreeResponses.set(i, prelSubtreeResponse);
            this.aggregatedResponse.add(prelSubtreeResponse);
            
            this.subtreeDiscomfortSum.set(i, prelDiscomfortSum);
            this.aggregatedDiscomfortSum += prelDiscomfortSum;
            
            this.subtreeDiscomfortSumSqr.set(i, prelDiscomfortSumSqr);
            this.aggregatedDiscomfortSumSqr += prelDiscomfortSumSqr;
        }
        
        if(this.isLeaf()) {
        	
        } else {
        	if(this.children.size() > 1) {
        		this.log(Level.FINER, "approvals: " + this.approvals.get(0) + ", " + this.approvals.get(1));
                this.log(Level.FINER, "preliminary approved SubtreeDiscomfortSum: " + this.subtreeDiscomfortSum.get(0) + ", " + this.subtreeDiscomfortSum.get(1));
        	} else {
        		this.log(Level.FINER, "approvals: " + this.approvals.get(0));
                this.log(Level.FINER, "preliminary approved SubtreeDiscomfortSum: " + this.subtreeDiscomfortSum.get(0));
        	}
        	
//            this.log(Level.FINER, "preliminary approved SubtreeDiscomfortSumSqr: " + this.subtreeDiscomfortSumSqr.get(0) + ", " + this.subtreeDiscomfortSumSqr.get(1));
        }             
    }

    /**
     * Based on <code>planSelector</code> chooses the plan and sets <code>selectedPlan</code> variable.
     * Moreover, <code>numComputed</code> is increased by the number of computations it took to select a plan
     */
    void selectPlan() {    	
		int selected = this.planSelector.selectPlan(this);
		this.setNumComputed(this.getNumComputed() + this.planSelector.getNumComputations(this));
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
        V subtreeResponse = this.aggregatedResponse.cloneThis();
        subtreeResponse.add(this.selectedPlan.getValue());
        double score = this.localCostFunc.calcCost(this.selectedPlan);
        double totalDiscomfortSum = this.aggregatedDiscomfortSum + score;
        double totalDiscomfortSumSqr = this.aggregatedDiscomfortSumSqr + score*score;
        this.log(Level.FINER, "informParent:");
        this.log(Level.FINER, "Discomfort of selected plan " + this.selectedPlanID + " (" + score + ") is added to sum and SumSqr");
        return new UpMessage(subtreeResponse, totalDiscomfortSum, totalDiscomfortSumSqr);
    }

    private void updateGlobalResponse(DownMessage parentMsg) {
    	this.setConvergenceReachedFlag(this.globalResponse, parentMsg.globalResponse);
        this.globalResponse.set(parentMsg.globalResponse);
    }
    
    private void setConvergenceReachedFlag(V oldGlobalResponse, V newGlobalResponse) {
    	double oldglobalcost = this.getGlobalCostFunction().calcCost(oldGlobalResponse);
    	double newglobalcost = this.getGlobalCostFunction().calcCost(newGlobalResponse);
    	this.convergenceReached = oldglobalcost == newglobalcost;
    }
    
    public boolean hasConverged() {
    	return this.convergenceReached;
    }
    
    private void updateGlobalDiscomfortScores(DownMessage parentMsg) {
    	this.globalDiscomfortSum = parentMsg.globalDiscomfortSum;
    	this.globalDiscomfortSumSqr = parentMsg.globalDiscomfortSumSqr;
    	this.log(Level.FINER, "Received global Discomfort Sum: " + this.globalDiscomfortSum + " and SumSqr: " + this.globalDiscomfortSumSqr);
    }

    /**
     * Approvals remain the same if parent's selected plan was approved.
     * Otherwise, all approvals are set to <code>false</code>, and state is reverted to the last previously approved state:
     *   - aggregated response
     *   - selected plan
     * @param parentMsg
     */
    private void approveOrRejectChanges(DownMessage parentMsg) {
    	// if parent's selected plan is not approved, revert to previously approved state
    	//    also, preliminary approvals are cleared and all are set to false
        if (!parentMsg.approved) {
            this.selectedPlan = this.prevSelectedPlan;
            this.selectedPlanID = this.prevSelectedPlanID;
            this.aggregatedResponse.set(this.prevAggregatedResponse);
            this.subtreeResponses.clear();
            this.subtreeResponses.addAll(this.prevSubtreeResponses);
            
            this.aggregatedDiscomfortSum = this.prevAggregatedDiscomfortSum;
            this.aggregatedDiscomfortSumSqr = this.prevAggregatedDiscomfortSumSqr;
            this.subtreeDiscomfortSum.clear();
            this.subtreeDiscomfortSumSqr.clear();
            
            this.subtreeDiscomfortSum.addAll(this.prevSubtreeDiscomfortSum);
            this.subtreeDiscomfortSumSqr.addAll(this.prevSubtreeDiscomfortSumSqr);
            
            Collections.fill(approvals, false);
            
            this.log(Level.FINER, "NOT ACCEPTED.");
        } else {
        	
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
        
        this.prevSubtreeDiscomfortSum.clear();
        this.prevSubtreeDiscomfortSumSqr.clear();
        
        this.approvals.clear(); 	
        
        this.subtreeDiscomfortSum.clear();
        this.subtreeDiscomfortSumSqr.clear();
        this.aggregatedDiscomfortSum = 0;
        this.aggregatedDiscomfortSumSqr = 0;
        this.prevAggregatedDiscomfortSum = 0;
        this.prevAggregatedDiscomfortSumSqr = 0;
        this.globalDiscomfortSum = 0;
        this.globalDiscomfortSumSqr = 0;
    }

    /**
     * global response and effective approvals for children are sent via DOWN messages
     * @return list of DOWN messages with global response and approvals set
     */
    private List<DownMessage> informChildren() {
        List<DownMessage> msgs = new ArrayList<>();
        for (int i = 0; i < children.size(); i++) {
            msgs.add(this.generateDownMessage(i));
        }
        return msgs;
    }
    
    DownMessage generateDownMessage(int i) {
    	return new DownMessage(this.globalResponse, approvals.get(i), this.globalDiscomfortSum, this.globalDiscomfortSumSqr);
    }

    // message classes
    public class UpMessage extends IterativeTreeAgent.UpMessage {

        public V subtreeResponse;					// basically aggregated response from the subtree rooted at this agent
        public double discomfortSum;				// sum of all discomforts from both subtrees and from sending node
        public double discomfortSumSqr;				// sum of squared discomforts from both subtrees and from sending node
        
        public UpMessage(V subtreeResponse, double discomfortSum, double discomfortSumSqr) {
            this.subtreeResponse = subtreeResponse;
            this.discomfortSum = discomfortSum;
            this.discomfortSumSqr = discomfortSumSqr;
        }

        @Override
        public int getNumTransmitted() {
            return 1;								// is this 1 message, or 1 selected plan transmitted?
            										// what is the idea, what is actually counted?
        }
    }

    public class DownMessage extends IterativeTreeAgent.DownMessage {

        public V globalResponse;					// global response from the root
        public double globalDiscomfortSum;			// sum of all discomfort scores of all selected plans
        public double globalDiscomfortSumSqr;		// sum of all sqared discomfort scores of all selected plans
        public boolean approved;					// Delta value!

        public DownMessage(V globalResponse, boolean approved, double globalDiscomfortSum, double globalDiscomfortSumSqr) {
            this.globalResponse = globalResponse;
            this.globalDiscomfortSum = globalDiscomfortSum;
            this.globalDiscomfortSumSqr = globalDiscomfortSumSqr;
            this.approved = approved;
        }

        @Override
        public int getNumTransmitted() {
            return 1;								// is this 1 message, or 1 selected plan transmitted?
													// what is the idea, what is actually counted?
        }
    }
}
