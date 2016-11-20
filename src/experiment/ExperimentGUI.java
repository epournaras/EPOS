package experiment;

import agent.logging.GlobalCostLogger;
import agent.logging.LoggingProvider;
import agent.logging.AgentLoggingProvider;
import agent.logging.JFreeChartLogger;
import agent.*;
import agent.dataset.FileVectorDataset;
import agent.logging.GraphLogger;
import agent.logging.LocalCostLogger;
import agent.logging.MovieLogger;
import data.Plan;
import data.Vector;
import dsutil.generic.RankPriority;
import dsutil.protopeer.services.topology.trees.DescriptorType;
import dsutil.protopeer.services.topology.trees.TreeType;
import func.DifferentiableCostFunction;
import func.DiscomfortCostFunction;
import func.PlanCostFunction;
import func.VarCostFunction;
import java.util.List;
import java.util.Random;
import protopeer.Experiment;
import protopeer.Peer;
import protopeer.PeerFactory;
import protopeer.SimulatedExperiment;
import protopeer.util.quantities.Time;
import tree.BalanceType;
import util.TreeArchitecture;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Peter
 */
public class ExperimentGUI extends SimulatedExperiment {

    private final Random random = new Random();

    public static void main(String[] args) {
        new ExperimentGUI().run();
    }

    public void run() {
        //////////////
        // parameters
        //////////////

        String datasetDir = "input-data/gaussian";
        // TODO: create dropdown menu to select the dataset (only one available atm)

        int t = 20;
        // TODO: parameter "number of iterations" (min 1; max 100; default 20)

        long seed = 0;
        random.setSeed(seed);
        // TODO: parameter "seed", the seed for the RNG (can be any long value)

        DifferentiableCostFunction globalCostFunc = new VarCostFunction();
        /*DifferentiableCostFunction globalCostFunc;
        try {
            globalCostFunc = new SqrDistCostFunction(VectorIO.readVector(new File("input-data/sample_vector.txt")));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ExperimentGUI.class.getName()).log(Level.SEVERE, null, ex);
        }*/
        // TODO: dropdown box to select between VarCostFunction and SqrDistCostFunction
        // be aware, that the vector file has to be consistent with the dataset (i.e. same number of dimensions)

        double lambda = 0;
        // TODO: parameter that controls the tradeoff between local and global cost minimization
        // add a slider for lambda (min 0; max 1; default 0)

        PlanCostFunction localCostFunc = new DiscomfortCostFunction();
        // leave this parameter as is

        PlanSelector planSelector = new IeposPlanSelector(); // standard I-EPOS
        //PlanSelector planSelector = new IeposGlobalGradientPlanSelector(); // gradient descent variation
        //PlanSelector planSelector = new IeposIndividualGradientPlanSelector(); // gradient descent variation
        //PlanSelector planSelector = new IeposAdaptiveGradientPlanSelector(); // gradient descent variation
        // TODO: dropdown box to choose from the four above
        
        
        ///////////
        // network
        ///////////
        
        int c = 2;
        // TODO: make this (number of children) configurable (min 1; max 5; default 2)
        
        TreeArchitecture architecture = new TreeArchitecture();
        architecture.balance = BalanceType.WEIGHT_BALANCED;
        architecture.maxChildren = c;
        architecture.priority = RankPriority.HIGH_RANK;
        architecture.rank = DescriptorType.RANK;
        architecture.rankGenerator = (idx, agent) -> (double) idx;
        architecture.type = TreeType.SORTED_HtL;

        
        ////////////////////
        // output definiton
        ////////////////////
        
        LoggingProvider<IeposAgent<Vector>> loggingProvider = new LoggingProvider<>();
        loggingProvider.add(new GlobalCostLogger());
        if (lambda > 0) {
            loggingProvider.add(new LocalCostLogger());
        }
        loggingProvider.add(new JFreeChartLogger()); // presents global and local cost (if applicable)
        // TODO: integrate into output window (exists once for each simulation)

        loggingProvider.add(new MovieLogger()); // writes the output signal of the network to stdout in MATALB readable format
        // TODO: change this logger to render the vector into the output window
        // Hint: only consider the stuff labeled as "D(...)="
        // each iteration generates one output!

        loggingProvider.add(new GraphLogger<>(GraphLogger.Type.Change, null)); // presents the graph
        // TODO: integrate into output window
        // each iteration generates one output graph!
        
        
        ///////////
        // dataset
        ///////////
        
        FileVectorDataset dataset = new FileVectorDataset(datasetDir);
        int a = dataset.getNumAgents();

        
        ////////////////////
        // Simulation setup
        ////////////////////
        
        ExperimentGUI.initEnvironment();
        init();

        PeerFactory peerFactory = new PeerFactory() {

            @Override
            public Peer createPeer(int peerIndex, Experiment e) {
                List<Plan<Vector>> possiblePlans = dataset.getPlans(peerIndex);
                AgentLoggingProvider agentLP = loggingProvider.getAgentLoggingProvider(peerIndex, 0);

                IeposAgent newAgent = new IeposAgent(t, possiblePlans, globalCostFunc, localCostFunc, agentLP, random.nextLong());
                newAgent.setLambda(lambda);
                newAgent.setPlanSelector(planSelector);
                Peer newPeer = new Peer(peerIndex);

                architecture.addPeerlets(newPeer, newAgent, peerIndex, a);

                return newPeer;
            }
        };
        initPeers(0, a, peerFactory);
        startPeers(0, a);

        runSimulation(Time.inSeconds(3 + t));

        
        ////////////////
        // Show results
        ////////////////
        
        loggingProvider.print();
    }
}
