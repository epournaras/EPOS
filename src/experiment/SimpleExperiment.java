package experiment;

import agent.logging.GlobalCostLogger;
import agent.logging.TerminationLogger;
import agent.logging.LoggingProvider;
import agent.logging.ProgressIndicator;
import agent.logging.AgentLoggingProvider;
import agent.logging.JFreeChartLogger;
import agent.dataset.Dataset;
import agent.*;
import agent.dataset.GaussianDataset;
import agent.logging.GraphLogger;
import agent.logging.MovieLogger;
import data.Plan;
import data.Vector;
import data.io.VectorIO;
import dsutil.generic.RankPriority;
import dsutil.protopeer.services.topology.trees.DescriptorType;
import dsutil.protopeer.services.topology.trees.TreeType;
import func.DifferentiableCostFunction;
import func.IndexCostFunction;
import func.PlanCostFunction;
import func.SqrDistCostFunction;
import func.VarCostFunction;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
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
public class SimpleExperiment extends SimulatedExperiment {
    
    private final Random random = new Random();
    
    public static void main(String[] args) {
        new SimpleExperiment().run();
    }
    
    public void run() {
        // constants
        int a = 127;
        int p = 16;
        int d = 40;
        int c = 2;
        int t = 20;
        int runs = 1;
        double lambda = 5;
        //DifferentiableCostFunction globalCostFunc = new VarCostFunction();
        DifferentiableCostFunction globalCostFunc;
        try {
            globalCostFunc = new SqrDistCostFunction(VectorIO.readVector(new File("input-data/sample_vector.txt")));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(SimpleExperiment.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        PlanCostFunction localCostFunc = new IndexCostFunction();

        // loggers
        LoggingProvider<IeposAgent<Vector>> loggingProvider = new LoggingProvider<>();
        loggingProvider.add(new ProgressIndicator());
        loggingProvider.add(new GlobalCostLogger());
        //loggingProvider.add(new LocalCostLogger());
        loggingProvider.add(new TerminationLogger());
        loggingProvider.add(new JFreeChartLogger());
        loggingProvider.add(new MovieLogger());
        loggingProvider.add(new GraphLogger<>(GraphLogger.Type.Change, null));
        //loggingProvider.add(new FileWriter("simple.log"));

        // dataset
        //Dataset<Vector> dataset = new GaussianDataset(p, d, 0, 1, new Random(0));
        // network
        TreeArchitecture architecture = new TreeArchitecture();
        architecture.balance = BalanceType.WEIGHT_BALANCED;
        architecture.maxChildren = c;
        architecture.priority = RankPriority.HIGH_RANK;
        architecture.rank = DescriptorType.RANK;
        architecture.rankGenerator = (idx, agent) -> (double) idx;
        architecture.type = TreeType.SORTED_HtL;
        
        for (int r = 0; r < runs; r++) {
            final int run = r;
            
            random.setSeed(r);
            Dataset<Vector> dataset = new GaussianDataset(p, d, 0, 1, new Random(random.nextLong()));
            
            SimpleExperiment.initEnvironment();
            init();
            
            PeerFactory peerFactory = new PeerFactory() {
                
                @Override
                public Peer createPeer(int peerIndex, Experiment e) {
                    List<Plan<Vector>> possiblePlans = dataset.getPlans(peerIndex);
                    AgentLoggingProvider agentLP = loggingProvider.getAgentLoggingProvider(peerIndex, run);
                    
                    IeposAgent newAgent = new IeposAgent(t, possiblePlans, globalCostFunc, localCostFunc, agentLP, random.nextLong());
                    //newAgent.setLambda(lambda);
                    //newAgent.setPlanSelector(new IeposIndividualGradientPlanSelector());
                    //Agent newAgent = new CohdaAgent(t, possiblePlans, globalCostFunc, localCostFunc, agentLP, random.nextLong());
                    //Agent newAgent = new EposAgent(possiblePlans, globalCostFunc, localCostFunc, agentLP, random.nextLong());
                    //Agent newAgent = new BestStepAgent(t, possiblePlans, globalCostFunc, localCostFunc, agentLP, random.nextLong());
                    Peer newPeer = new Peer(peerIndex);
                    
                    architecture.addPeerlets(newPeer, newAgent, peerIndex, a);
                    
                    return newPeer;
                }
            };
            initPeers(0, a, peerFactory);
            startPeers(0, a);
            
            runSimulation(Time.inSeconds(3 + t));
        }
        
        loggingProvider.print();
    }
}
