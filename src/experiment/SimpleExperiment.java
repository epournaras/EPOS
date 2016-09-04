package experiment;

import agent.Agent;
import agent.EposAgent;
import agent.logging.GlobalCostLogger;
import agent.logging.TerminationLogger;
import agent.logging.LoggingProvider;
import agent.logging.ProgressIndicator;
import agent.logging.AgentLoggingProvider;
import agent.logging.JFreeChartLogger;
import agent.dataset.Dataset;
import agent.dataset.AgentDataset;
import agent.dataset.NoiseDataset;
import agent.*;
import data.Plan;
import data.Vector;
import dsutil.generic.RankPriority;
import dsutil.protopeer.services.topology.trees.DescriptorType;
import dsutil.protopeer.services.topology.trees.TreeType;
import func.DifferentiableCostFunction;
import func.IndexCostFunction;
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
public class SimpleExperiment extends SimulatedExperiment {

    private Random random = new Random();

    public static void main(String[] args) {
        new SimpleExperiment().run();
    }

    public void run() {
        // constants
        int a = 1000;
        int p = 16;
        int d = 100;
        int c = 2;
        int t = 100;
        int runs = 5;
        double lambda = 5;
        DifferentiableCostFunction globalCostFunc = new VarCostFunction();
        PlanCostFunction localCostFunc = new IndexCostFunction();

        // loggers
        LoggingProvider<IeposAgent<Vector>> loggingProvider = new LoggingProvider<>();
        loggingProvider.add(new ProgressIndicator());
        loggingProvider.add(new GlobalCostLogger());
        //loggingProvider.add(new LocalCostLogger());
        loggingProvider.add(new TerminationLogger());
        loggingProvider.add(new JFreeChartLogger());
        //loggingProvider.add(new FileWriter("simple.log"));

        // dataset
        Dataset<Vector> dataset = new NoiseDataset(p, p, d, 0, 1, d, d, null);
        //Dataset<Vector> dataset = new SchedulingDataset(d, 50, 5, 1, 1, (x, y) -> Double.compare(x.getDiscomfort(), y.getDiscomfort()));
        //Dataset<Vector> dataset = new SchedulingDataset(d, 50, 5, 0, 0, (x,y) -> Double.compare(x.getDiscomfort(), y.getDiscomfort()));

        // network
        TreeArchitecture architecture = new TreeArchitecture();
        architecture.balance = BalanceType.WEIGHT_BALANCED;
        architecture.maxChildren = c;
        architecture.priority = RankPriority.HIGH_RANK;
        architecture.rank = DescriptorType.RANK;
        architecture.rankGenerator = (idx, agent) -> (double) idx;
        architecture.type = TreeType.SORTED_HtL;

        loggingProvider.initExperiment("children" + architecture.maxChildren, null);
        for (int r = 0; r < runs; r++) {
            final int run = r;
            dataset.init(r);
            random.setSeed(r);

            SimpleExperiment.initEnvironment();
            init();

            List<? extends AgentDataset<Vector>> agentDatasets = dataset.getAgentDatasets(a);
            int n = agentDatasets.size();

            PeerFactory peerFactory = new PeerFactory() {

                @Override
                public Peer createPeer(int peerIndex, Experiment e) {
                    AgentDataset<Vector> agentDataset = agentDatasets.get(peerIndex);
                    List<Plan<Vector>> possiblePlans = agentDataset.getPlans(agentDataset.getPhases().get(0));
                    AgentLoggingProvider agentLP = loggingProvider.getAgentLoggingProvider(peerIndex, run);

                    IeposAgent newAgent = new IeposAgent(t, possiblePlans, globalCostFunc, localCostFunc, agentLP, random.nextLong());
                    //newAgent.setLambda(lambda);
                    //newAgent.setPlanSelector(new IeposIndividualGradientPlanSelector());
                    //Agent newAgent = new CohdaAgent(t, possiblePlans, globalCostFunc, localCostFunc, agentLP, random.nextLong());
                    //Agent newAgent = new EposAgent(possiblePlans, globalCostFunc, localCostFunc, agentLP, random.nextLong());
                    //Agent newAgent = new BestStepAgent(t, possiblePlans, globalCostFunc, localCostFunc, agentLP, random.nextLong());
                    Peer newPeer = new Peer(peerIndex);

                    architecture.addPeerlets(newPeer, newAgent, peerIndex, n);

                    return newPeer;
                }
            };
            initPeers(0, n, peerFactory);
            startPeers(0, n);

            runSimulation(Time.inSeconds(3 + t));
        }

        loggingProvider.print();
    }
}
