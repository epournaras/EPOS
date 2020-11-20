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

import config.Configuration;
import liveRunUtils.Messages.InformGatewayMessage;
import func.*;
import java.util.*;
import java.text.SimpleDateFormat;
import loggers.EventLog;
import pgpersist.PersistenceClient;
import pgpersist.SqlDataItem;
import pgpersist.SqlInsertTemplate;
import protopeer.MainConfiguration;
import protopeer.network.NetworkAddress;
import protopeer.network.zmq.ZMQAddress;


/**
 * An agent that performs combinatorial optimization.
 *
 * @author Peter
 * @param <V> the type of the data this agent should handle
 */
public abstract class Agent<V extends DataType<V>> extends BasePeerlet  implements java.io.Serializable {

    // misc
    final Random 							random 				= 		new Random();

    // logging
    public final transient AgentLoggingProvider 		loggingProvider;
    transient Logger 									logger 				= 		Logger.getLogger(Agent.class.getName());

    // timings
    protected final int						bootstrapPeriod		=	2000;	//ms
    protected final int						activeStatePeriod	=	1000;	//ms

    protected final int						readyPeriod		=	1000;	//ms
    public boolean                          plansAreSet = new Boolean(false);
    public boolean                          readyToRun = new Boolean(false);
    public boolean                          weightsAreSet = new Boolean(true);

    // combinatorial optimization variables
    Plan<V> 								selectedPlan;
    int										selectedPlanID;
    V 										globalResponse;
    final transient List<Plan<V>> 			possiblePlans 		= 	new ArrayList<>();
    transient CostFunction<V> 		globalCostFunc;
    transient PlanCostFunction<V> 	localCostFunc;

    //For DBLogging
    transient PersistenceClient persistenceClient;
    transient public NetworkAddress userAddress;

    // logging stuff
    private int 							numTransmitted;
    public int 						    	numComputed;
    private int 							cumTransmitted;
    private int 							cumComputed;

    int										iterationAfterReorganization =	0;	// iteration at which reorganization was requested and executed
    public int activeRun=-1;
    public int activeSim=-1;
    protected boolean                       alreadyCleanedResponses = new Boolean(false);
    transient ZMQAddress                    GatewayAddress = new ZMQAddress(MainConfiguration.getSingleton().peerZeroIP, Configuration.GateWayPort);


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

    // this is for live implementation
    public Agent(CostFunction<V> globalCostFunc, PlanCostFunction<V> localCostFunc, AgentLoggingProvider<? extends Agent> loggingProvider) {
        this.globalCostFunc = globalCostFunc;
        this.localCostFunc = localCostFunc;
        this.loggingProvider = loggingProvider;
    }

    /**
     * this is for live implementation
     * the main difference is the possiblePlans argument.
     * in the live implementation, each agent is sent its plans by the corresponding user, using network-level messaging
      */
    public Agent(CostFunction<V> globalCostFunc, PlanCostFunction<V> localCostFunc, AgentLoggingProvider<? extends Agent> loggingProvider, long seed) {
        this(globalCostFunc, localCostFunc, loggingProvider);
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
        if(config.Configuration.isLiveRun) {
            /**
             * if the system is live, it needs to initiate the loggers, creating the SQL templates for various loggers
              */
            loggingProvider.init(Agent.this);
            // sets up the event logger
            setUpEventLogger();

            if (MainConfiguration.getSingleton().peerIndex == 0) {
                /**
                 * if this peer is the bootstrap (tree gateway), it informs the gateway that is is running and listening for new peers
                 */
                getPeer().sendMessage(GatewayAddress, new InformGatewayMessage(MainConfiguration.getSingleton().peerIndex, this.activeRun, "bootsrapPeerInitiated", false));
            }
        }
        this.runBootstrap();
        scheduleMeasurements();
    }

    @Override
    public void stop() {
    }

    public void addPersistenceClient( PersistenceClient	persistenceClient )
    {
//        if( persistenceClient == null ) {return;}
        // each peers has a persistent client which takes care of sending the logging messages to the logging gateway (database)
        this.persistenceClient = persistenceClient;
        System.out.println("persistenceClient set");
    }

    public void setActiveRun (int initRun){
        activeRun = initRun;
    }
    public void setActiveSim (int initSim){ activeSim = initSim; }

    public void addPlans(List<Plan<V>> possiblePlans){
        // the plans for each peer is sent via the corresponding user via messaging
        this.possiblePlans.clear();
        this.possiblePlans.addAll(possiblePlans);
        plansAreSet = true;
        // informing the gateway that the plans are set
        getPeer().sendMessage(GatewayAddress, new InformGatewayMessage(MainConfiguration.getSingleton().peerIndex, this.activeRun, "plansSet", true));
    }

    public void setReadyToRun(){
        // ready to run (start iterations), as told by the gateway
        this.readyToRun = true;
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

    public void changeGlobalCostFunc(String func){
        // changing the global cost function, as instructed by the gateway
        if (func.equals("VAR")){
            this.globalCostFunc = (CostFunction<V>) new VarCostFunction();
            ((HasGoal) globalCostFunc).populateGoalSignal();
        }
        else if (func.equals("RMSE")){
            this.globalCostFunc = (CostFunction<V>) new RMSECostFunction();
            ((HasGoal) globalCostFunc).populateGoalSignal();
        }
    }

    protected abstract boolean checkMethodExistence(Class<? extends PlanCostFunction> cl, String getLabel);

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

    public PersistenceClient getPersistenceClient() {return persistenceClient; }

    // has over ride methods in extended classes
    void runBootstrap() {
        Timer loadAgentTimer = getPeer().getClock().createNewTimer();
        loadAgentTimer.addTimerListener(new TimerListener() {
            public void timerExpired(Timer timer) {
                runActiveState();
            }
        });
        loadAgentTimer.schedule(Time.inMilliseconds(this.bootstrapPeriod));
    }

    void runActiveState() {
        // has over ride methods in extended classes
        if(!config.Configuration.isLiveRun) {
            Timer loadAgentTimer = getPeer().getClock().createNewTimer();
            loadAgentTimer.addTimerListener((Timer timer) -> {
                initPhase();
                runPhase();
            });
            loadAgentTimer.schedule(Time.inMilliseconds(this.activeStatePeriod));
        }
    }

    private void initPhase() {
        if(!config.Configuration.isLiveRun) {
            // the logging provider init for non-live
            loggingProvider.init(this);
        }
        this.log(Level.FINER, "initPhase()");
        numTransmitted = 0;
        numComputed = 0;
        cumTransmitted = 0;
        cumComputed = 0;
    }

    abstract void runPhase();

    private void scheduleMeasurements() {
        if (!Configuration.isLiveRun) {
            getPeer().getMeasurementLogger().addMeasurementLoggerListener((MeasurementLog log, int epochNumber) -> {
                loggingProvider.log(log, epochNumber, this);
            });
        }
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
    	return (this.getIteration() == 0);
    }
    
    public int getNumReorganizations() {
    	return 0;
    }

    // testing the custom logger
    public void testCustomLog(){

        // -------------------
        // -- Custom Logger --
        // -------------------

        System.out.println("hereNow");

        // step 1. create the table in SQL -> sql/definitions/customlog.sql
        // step 2.  send the template to the Peristence daemon, so that it knows how to write the data to the database
        final String 				sql_insert_template_custom  = "INSERT INTO customlog(dt,run,iteration,dim_0,dim_1) VALUES({dt}, {run}, {iteration}, {dim_0}, {dim_1});";
        // step 3. send that string to the daemon
        persistenceClient.sendSqlInsertTemplate( new SqlInsertTemplate( "custom", sql_insert_template_custom ) );


        // -----------
        // -- start --
        // -----------



        // MockClient
        // requires 3 arguments:
        // 1. port for sending messages to daemon

        // parse arguments
        // parse listen port
        int sleepTimeMilliSeconds = 10;
        final ArrayList<String>				someList = new ArrayList<String>();
        long					counter = 0L;

        boolean 				b_loop = true;

        LinkedHashMap<String,String> outputMap = new LinkedHashMap<String,String> ();
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Random randomNumberGenerator = new Random();

        // TODO: create a list and add to memlog

        // Start
//        RawLog.print(1,"Hello!");
//        EventLog.logEvent("TestBasicLog", "main", "loop start");


        while( b_loop )
        {
            ++counter;

            // wait
            try
            {
//                RawLog.print(1,"Hello, oupss... any btw, my counter is " + Long.toString(counter));
//                EventLog.logEvent("TestBasicLog", "main", "counter", Long.toString(counter));

                // send some data to the custom log
                // fields: dt,run,iteration,dim_0,dim_1
                LinkedHashMap<String,String>          record = new LinkedHashMap<String,String>();

                record.put("dt", "'" + dateFormatter.format( System.currentTimeMillis() ) + "'" );
                record.put("run", Long.toString(counter) );
                record.put("iteration", Long.toString(counter + 1000));
                record.put("dim_0", "100" );
                record.put("dim_1", "101" );

                persistenceClient.sendSqlDataItem( new SqlDataItem( "custom", record ) );


                // add some data to the list, so that we can see it's memory increasing
                someList.add(Long.toString(counter));

                // wait a bit
                Thread.currentThread().sleep(sleepTimeMilliSeconds);
            }
            catch (InterruptedException e)
            {
                b_loop = false;
            }


        }// whi
    }

    public void setUpEventLogger(){
        EventLog.setPeristenceClient(persistenceClient);
        EventLog.setPeerId(MainConfiguration.getSingleton().peerIndex);
        EventLog.setDIASNetworkId(0);
    }
}
