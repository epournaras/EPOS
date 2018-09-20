package experiment;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import agent.Agent;
import agent.IeposAgent;
import agent.IeposPlanSelector;
import agent.MultiObjectiveIEPOSAgent;
import agent.PlanSelector;
import agent.logging.AgentLogger;
import agent.logging.AgentLoggingProvider;
import agent.logging.DiscomfortLogger;
import agent.logging.GlobalComplexCostLogger;
import agent.logging.GlobalCostLogger;
import agent.logging.GlobalResponseVectorLogger;
import agent.logging.GraphLogger;
import agent.logging.GraphLogger.Type;
import agent.logging.instrumentation.CustomFormatter;
import agent.multiobjectiveutils.ControllerCollection;
import agent.logging.LocalCostMultiObjectiveLogger;
import agent.logging.LoggingProvider;
import agent.logging.PlanFrequencyLogger;
import agent.logging.SelectedPlanLogger;
import agent.logging.TerminationLogger;
import agent.logging.UnfairnessLogger;
import agent.logging.WeightsLogger;
import agent.planselection.MultiObjectiveIeposPlanSelector;
import config.CommandLineArgumentReader;
import config.Configuration;
import data.Plan;
import data.Vector;
import func.CrossCorrelationCostFunction;
import protopeer.Experiment;
import protopeer.Peer;
import protopeer.PeerFactory;
import protopeer.SimulatedExperiment;
import protopeer.util.quantities.Time;
import util.TreeArchitecture;

/**
 * This class describes multi-objective optimization with I-EPOS
 * 
 * @author Jovan N.
 *
 */
public class MultiObjectiveIEPOSExperiment {
	
	private static LoggingProvider<MultiObjectiveIEPOSAgent<Vector>> generateLoggers(Configuration config) {
		
		LoggingProvider<MultiObjectiveIEPOSAgent<Vector>>	loggingProvider = 	new LoggingProvider<MultiObjectiveIEPOSAgent<Vector>>();        
        GlobalCostLogger<Vector> 				GCLogger 		= 	new GlobalCostLogger<Vector>(Configuration.getGlobalCostPath());
        LocalCostMultiObjectiveLogger<Vector> 	LCLogger  		= 	new LocalCostMultiObjectiveLogger<Vector>(Configuration.getLocalCostPath());
        TerminationLogger<Vector> 				TLogger 		= 	new TerminationLogger<Vector>(Configuration.getTerminationPath());
        SelectedPlanLogger<Vector> 				SPLogger		=	new SelectedPlanLogger<Vector>(Configuration.getSelectedPlansPath(), config.numAgents);
        GlobalResponseVectorLogger<Vector>  	GRVLogger		=	new GlobalResponseVectorLogger<Vector>(Configuration.getGlobalResponsePath());
        PlanFrequencyLogger<Vector> 			DstLogger		=	new PlanFrequencyLogger<Vector>(Configuration.getDistributionPath());
        UnfairnessLogger<Vector> 				ULogger			=	new UnfairnessLogger<Vector>(Configuration.getUnfairnessPath());
        GlobalComplexCostLogger<Vector> 		GCXLogger		=	new GlobalComplexCostLogger<Vector>(Configuration.getGlobalComplexCostPath());
        WeightsLogger<Vector>					WLogger			=	new WeightsLogger<Vector>(Configuration.getWeightsPath());
        
        
        GCLogger.setRun(Configuration.permutationID);
        LCLogger.setRun(Configuration.permutationID);
        TLogger.setRun(Configuration.permutationID);        
        SPLogger.setRun(Configuration.permutationID);
        GRVLogger.setRun(Configuration.permutationID);
        DstLogger.setRun(Configuration.permutationID);
        ULogger.setRun(Configuration.permutationID);
        GCXLogger.setRun(Configuration.permutationID);
        WLogger.setRun(Configuration.permutationID);
        
        loggingProvider.add(GCLogger);
        loggingProvider.add(LCLogger);
        loggingProvider.add(TLogger);
        loggingProvider.add(SPLogger);
        loggingProvider.add(GRVLogger);
        loggingProvider.add(DstLogger);
        loggingProvider.add(ULogger);
        loggingProvider.add(GCXLogger);
        loggingProvider.add(WLogger);
		
        return loggingProvider;
	}
	
	private static void runSimulation(int numChildren, 						// number of children for each middle node
									  int numIterations, 					// total number of iterations to run for
									  int numAgents, 						// total number of nodes in the network
									  Function<Integer, Agent> createAgent,	// lambda expression that creates an agent
									  Configuration config) 	
	{
	
		SimulatedExperiment 		experiment 		= 	new SimulatedExperiment() {};
		TreeArchitecture 			architecture 	= 	new TreeArchitecture(numChildren);
		
		SimulatedExperiment.initEnvironment();
		experiment.init();
	
		PeerFactory peerFactory = new PeerFactory() {
	
			@Override
			public Peer createPeer(int peerIndex, Experiment e) {
				Agent newAgent = createAgent.apply(peerIndex);
				Peer newPeer = new Peer(peerIndex);
				
				architecture.addPeerlets(newPeer, newAgent, peerIndex, numAgents);
				
				return newPeer;
			}
		};
	
		Logger rootLogger = LogManager.getLogManager().getLogger("");
		rootLogger.setLevel(config.loggingLevel);
		
		for (Handler h : rootLogger.getHandlers()) {			
			h.setLevel(config.loggingLevel);
			h.setFormatter(new CustomFormatter());
		}
		
		experiment.initPeers(0, numAgents, peerFactory);
		experiment.startPeers(0, numAgents);		
		experiment.runSimulation(Time.inSeconds(3 + numIterations));
	}
	
	private static void runOneSimulation(Configuration config, Function<Integer, Agent> createAgent) {
        long timeBefore = System.currentTimeMillis();
        MultiObjectiveIEPOSExperiment.runSimulation(Configuration.numChildren,
        								Configuration.numIterations,
        								Configuration.numAgents,
        								createAgent,
        								config
                					  );
        long timeAfter = System.currentTimeMillis();
        System.out.println("IEPOS Finished! It took: " + ((timeAfter-timeBefore)/1000) + " seconds.");
	}
	
	private static Configuration generateConfiguration(String[] args) {
		Configuration config = 	new Configuration();
		CommandLineArgumentReader.setConfiguration(config, args);		
		config.printConfiguration();
		
		return config;
	}
	
	public static void main(String[] args) {
		
		Configuration config = MultiObjectiveIEPOSExperiment.generateConfiguration(args);    	
    	LoggingProvider<MultiObjectiveIEPOSAgent<Vector>> loggingProvider = 
    								MultiObjectiveIEPOSExperiment.generateLoggers(config);
		
		for(int sim = 0; sim < Configuration.numSimulations; sim++) {
			
			System.out.println("Simultaion " + (sim+1));
			
			final int simulationId = sim;
			config.permutationSeed = sim;
			
			for(AgentLogger al : loggingProvider.getLoggers()) {
				al.setRun(sim);
			}
			
			if(Configuration.numSimulations > 1 && sim > 0) {
				Configuration.mapping = config.generateMappingForRepetitiveExperiments.apply(config);
			}
	        
	        PlanSelector<MultiObjectiveIEPOSAgent<Vector>, Vector> planSelector = 
	        									new MultiObjectiveIeposPlanSelector<Vector>();
	        
	        /**
	         * Function that creates an Agent given the id of it's vertex in tree graph.
	         * First type is input argument, second type is type of return value.
	         */
	        Function<Integer, Agent> createAgent = agentIdx -> {
	        	
	            List<Plan<Vector>> possiblePlans = config.getDataset(Configuration.dataset).getPlans(Configuration.mapping.get(agentIdx));

	            AgentLoggingProvider<MultiObjectiveIEPOSAgent<Vector>> agentLP = loggingProvider.getAgentLoggingProvider(agentIdx, simulationId);

	            MultiObjectiveIEPOSAgent<Vector> newAgent = new MultiObjectiveIEPOSAgent<Vector>(
	            													 Configuration.numIterations,
											                         possiblePlans,
											                         config.globalCostFunc,
											                         config.localCostFunc,
											                         agentLP,											                         
											                         config.simulationSeed);
	            
	            newAgent.setUnfairnessWeight(config.alpha);
	            newAgent.setLocalCostWeight(config.beta);
	            newAgent.setPlanSelector(planSelector);
	            return newAgent;
	            
	        };
	        
			MultiObjectiveIEPOSExperiment.runOneSimulation(config, createAgent);
		}
		
		loggingProvider.print();        
	}

}
