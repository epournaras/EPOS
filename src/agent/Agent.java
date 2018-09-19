/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package agent;

import data.Plan;
import func.CostFunction;
import func.PlanCostFunction;
import agent.logging.AgentLoggingProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import protopeer.BasePeerlet;
import protopeer.measurement.MeasurementLog;
import protopeer.time.Timer;
import protopeer.time.TimerListener;
import protopeer.util.quantities.Time;
import data.DataType;

/**
 * An agent that performs combinatorial optimization.
 * 
 * @author Peter
 * @param <V> the type of the data this agent should handle
 */
public abstract class Agent<V extends DataType<V>> extends BasePeerlet {

    // misc
    final Random 							random 				= 		new Random();

    // logging
    private final AgentLoggingProvider 		loggingProvider;
    Logger 									logger 				= 		Logger.getLogger(Agent.class.getName());
    
    // timings
    private final int						bootstrapPeriod		=	2000;	//ms
    private final int						activeStatePeriod	=	1000;	//ms

    // combinatorial optimization variables
    Plan<V> 								selectedPlan;
    int										selectedPlanID;
    V 										globalResponse;
    final List<Plan<V>> 					possiblePlans 		= 	new ArrayList<>();
    final CostFunction<V> 					globalCostFunc;
    final PlanCostFunction<V> 				localCostFunc;

    // logging stuff
    private int 							numTransmitted;
    private int 							numComputed;
    private int 							cumTransmitted;
    private int 							cumComputed;
    
    int										iterationAfterReorganization =	0;	// iteration at which reorganization was requested and executed

    /**
     * Initializes the agent with the given combinatorial optimization problem
     * definition
     *
     * @param possiblePlans the possible plans of this agent
     * @param globalCostFunc the global cost function
     * @param localCostFunc the local cost function
     * @param loggingProvider the logger for the experiment
     */
    public Agent(List<Plan<V>> possiblePlans, CostFunction<V> globalCostFunc, PlanCostFunction<V> localCostFunc, AgentLoggingProvider<? extends Agent> loggingProvider) {
        this.possiblePlans.addAll(possiblePlans);
//        if(localCostFunc != null) {
//            this.possiblePlans.sort((plan1, plan2) -> (int)Math.signum(localCostFunc.calcCost(plan1) - localCostFunc.calcCost(plan2)));
//        }
        this.globalCostFunc = globalCostFunc;
        this.localCostFunc = localCostFunc;
        this.loggingProvider = loggingProvider;
    }

    /**
     * Initializes the agent with the given combinatorial optimization problem
     * definition
     *
     * @param possiblePlans the possible plans of this agent
     * @param globalCostFunc the global cost function
     * @param localCostFunc the local cost function
     * @param loggingProvider the logger for the experiment
     * @param seed the seed for the RNG used by this agent
     */
    public Agent(List<Plan<V>> possiblePlans, CostFunction<V> globalCostFunc, PlanCostFunction<V> localCostFunc, AgentLoggingProvider<? extends Agent> loggingProvider, long seed) {
        this(possiblePlans, globalCostFunc, localCostFunc, loggingProvider);
        random.setSeed(seed);
    }

    V createValue() {
        return possiblePlans.get(0).getValue().cloneNew();
    }

    Plan<V> createPlan() {
        return possiblePlans.get(0).cloneNew();
    }

    @Override
    public void start() {
        this.runBootstrap();
        scheduleMeasurements();
    }

    @Override
    public void stop() {
    }

    public Plan getSelectedPlan() {
        return selectedPlan;
    }
    
    public int getSelectedPlanID() {
    	return this.selectedPlanID;
    }

    public V getGlobalResponse() {
        return globalResponse;
    }

    public List<Plan<V>> getPossiblePlans() {
        return possiblePlans;
    }

    public CostFunction<V> getGlobalCostFunction() {
        return globalCostFunc;
    }

    public PlanCostFunction<V> getLocalCostFunction() {
        return localCostFunc;
    }

    public int getIteration() {
        return 0;
    }

    public int getNumIterations() {
        return 1;
    }
    
    /**
     * Returns iterations at which reorganization was requested and executed.
     * @return
     */
    public int getIterationAfterReorganization() {
    	return this.iterationAfterReorganization;
    }

    public boolean isRepresentative() {
        return getPeer().getIndexNumber() == 0;
    }

    public int getNumTransmitted() {
        return numTransmitted;
    }
    
    public void setNumTransmitted(int val) {
    	this.numTransmitted = val;
    }

    public int getNumComputed() {
        return numComputed;
    }
    
    public void setNumComputed(int val) {
    	this.numComputed = val;
    }

    public int getCumTransmitted() {
        return cumTransmitted;
    }
    
    public void setCumTransmitted(int val) {
    	this.cumTransmitted = val;
    }

    public int getCumComputed() {
        return cumComputed;
    }
    
    public void setCumComputed(int val) {
    	this.cumComputed = val;
    }

    private void runBootstrap() {
        Timer loadAgentTimer = getPeer().getClock().createNewTimer();
        loadAgentTimer.addTimerListener(new TimerListener() {
            public void timerExpired(Timer timer) {
                runActiveState();
            }
        });
        loadAgentTimer.schedule(Time.inMilliseconds(this.bootstrapPeriod));
    }

    void runActiveState() {
        Timer loadAgentTimer = getPeer().getClock().createNewTimer();
        loadAgentTimer.addTimerListener((Timer timer) -> {
            initPhase();
            runPhase();
        });
        loadAgentTimer.schedule(Time.inMilliseconds(this.activeStatePeriod));
    }

    private void initPhase() {
        loggingProvider.init(this);
        
        this.log(Level.FINER, "initPhase()");

        numTransmitted = 0;
        numComputed = 0;
        cumTransmitted = 0;
        cumComputed = 0;
    }

    abstract void runPhase();

    private void scheduleMeasurements() {
        getPeer().getMeasurementLogger().addMeasurementLoggerListener((MeasurementLog log, int epochNumber) -> {
            loggingProvider.log(log, epochNumber, this);
        });
    }
    
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //																									BY JOVAN: //
    
    public void reset() {
    	this.numTransmitted 	= 	0;
        this.numComputed 		= 	0;
        this.cumTransmitted		= 	0;
        this.cumComputed 		= 	0;
    }
    
    void log(Level level, String message) {
    	this.logger.log(level, "NODE: " + this.getPeer().getIndexNumber() + message);
    }
    
    public boolean isIterationAfterReorganization() {
    	return this.getIteration() == 0;
    }
    
    public int getNumReorganizations() {
    	return 0;
    }
}
