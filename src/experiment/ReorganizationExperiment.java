package experiment;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import agent.Agent;
import agent.ModifiableIeposAgent;
import agent.MultiObjectiveIEPOSAgent;
import agent.PlanSelector;
import agent.logging.AgentLoggingProvider;
import agent.logging.GlobalCostLogger;
import agent.logging.LocalCostLogger;
import agent.logging.LoggingProvider;
import agent.logging.ReorganizationLogger;
import agent.logging.TerminationLogger;
import agent.logging.instrumentation.CustomFormatter;
import agent.planselection.MultiObjectiveIeposPlanSelector;
import config.CommandLineArgumentReader;
import config.Configuration;
import data.Plan;
import data.Vector;
import protopeer.Experiment;
import protopeer.Peer;
import protopeer.PeerFactory;
import protopeer.SimulatedExperiment;
import protopeer.util.quantities.Time;
import treestructure.ModifiableTreeArchitecture;

public class ReorganizationExperiment {
	
	private static LoggingProvider<ModifiableIeposAgent<Vector>> generateLoggers(Configuration config) {
		
		LoggingProvider<ModifiableIeposAgent<Vector>>	loggingProvider = 	new LoggingProvider<ModifiableIeposAgent<Vector>>();        
        GlobalCostLogger<Vector> 			GCLogger 		= 	new GlobalCostLogger<Vector>(config.getGlobalCostPath());
        LocalCostLogger<Vector> 			LCLogger  		= 	new LocalCostLogger<Vector>(config.getLocalCostPath());
        TerminationLogger<Vector> 			TLogger 		= 	new TerminationLogger<Vector>(config.getTerminationPath());
        ReorganizationLogger<Vector> 		RLogger			=	new ReorganizationLogger<Vector>(config.getReorganizationPath());
        
        GCLogger.setRun(Configuration.permutationOffset + Configuration.permutationID);
        LCLogger.setRun(Configuration.permutationOffset + Configuration.permutationID);
        TLogger.setRun(Configuration.permutationOffset + Configuration.permutationID);        
        RLogger.setRun(Configuration.permutationOffset + Configuration.permutationID);
        
        loggingProvider.add(GCLogger);
        loggingProvider.add(LCLogger);
        loggingProvider.add(TLogger);
        loggingProvider.add(RLogger);
		
        return loggingProvider;
	}
	
	public static void runSimulation(int numChildren, 						// number of children for each middle node
			 int numIterations, 					// total number of iterations to run for
			 int numAgents, 						// total number of nodes in the network
			 Function<Integer, Agent> createAgent,	// lambda expression that creates an agent
			 Configuration config) 	
	{
	
		SimulatedExperiment 		experiment 		= 	new SimulatedExperiment() {};
		ModifiableTreeArchitecture 	architecture 	= 	new ModifiableTreeArchitecture(numChildren, config);
		
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
        ReorganizationExperiment.runSimulation(Configuration.numChildren,
        								Configuration.numIterations,
        								Configuration.numAgents,
        								createAgent,
        								config
                					  );
        long timeAfter = System.currentTimeMillis();
        System.out.println("IEPOS Finished! It took: " + ((timeAfter-timeBefore)/1000) + " seconds.");
	}
	
	public static void main(String[] args) {
		
		Configuration config 								= 	new Configuration();
		Configuration.populateDatasets();
		CommandLineArgumentReader.setConfiguration(config, args);
		config.printConfiguration();	
    	
    	LoggingProvider<ModifiableIeposAgent<Vector>> loggingProvider = 	ReorganizationExperiment.generateLoggers(config);    
    	
    	Map<Integer, Integer> mapping;
    	if(!Configuration.shouldReadInitialPermutationFromFile()) {
    		mapping = config.generateMapping.apply(config);
    	} else {
    		mapping = config.readMapping.apply(config);
    	}
		
		for(int sim = 0; sim < Configuration.numSimulations; sim++) {
			
			final int simulationId = Configuration.permutationID;
	        
			PlanSelector<MultiObjectiveIEPOSAgent<Vector>, Vector> planSelector = 
					new MultiObjectiveIeposPlanSelector<Vector>();	        
	        
	        /**
	         * Function that creates an Agent given the id of it's vertex in tree graph.
	         * First type is input argument, second type is type of return value.
	         */
	        Function<Integer, Agent> createAgent = agentIdx -> {
	        	
	            List<Plan<Vector>> possiblePlans 							= config.getDataset(Configuration.dataset).getPlans(mapping.get(agentIdx));
	            AgentLoggingProvider<ModifiableIeposAgent<Vector>> agentLP 	= loggingProvider.getAgentLoggingProvider(agentIdx, simulationId);

	            ModifiableIeposAgent<Vector> newAgent 						= new ModifiableIeposAgent<Vector>(config, possiblePlans, agentLP);
	            
	            newAgent.setUnfairnessWeight(config.alpha);
	            newAgent.setLocalCostWeight(config.beta);
	            newAgent.setPlanSelector(planSelector);
	            return newAgent;
	            
	        };
	        
	        ReorganizationExperiment.runOneSimulation(config, createAgent);
		}
		
		loggingProvider.print();
        
	}

}
