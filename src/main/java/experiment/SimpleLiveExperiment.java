package experiment;

import agent.logging.GlobalCostLogger;
import agent.logging.TerminationLogger;
import agent.logging.LoggingProvider;
import agent.logging.ProgressIndicator;
import agent.logging.AgentLoggingProvider;
import agent.logging.CostViewer;
import agent.dataset.Dataset;
import agent.*;
import agent.dataset.GaussianDataset;
import data.Plan;
import data.Vector;
import dsutil.generic.RankPriority;
import dsutil.protopeer.services.topology.trees.DescriptorType;
import dsutil.protopeer.services.topology.trees.TreeType;
import func.DifferentiableCostFunction;
import func.IndexCostFunction;
import func.PlanCostFunction;
import func.VarCostFunction;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import protopeer.Experiment;
import protopeer.LiveExperiment;
import protopeer.MainConfiguration;
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
public class SimpleLiveExperiment extends LiveExperiment {

    private final int idx;
    private final Random random = new Random();

    public static void main(String[] args) throws IOException, InterruptedException {
        int a = 2;
        int idx = 0;
        List<Process> processes = new ArrayList<>();

        if (args.length == 0) {
            for (int i = 1; i < a; i++) {
                ProcessBuilder b = new ProcessBuilder("java", "-jar", "dist" + File.separator + "IEPOS.jar", Integer.toString(i));
                b.inheritIO();
                processes.add(b.start());
            }
        } else {
            idx = Integer.parseInt(args[0]);
        }

        System.out.println("start " + idx);

        SimpleLiveExperiment exp = new SimpleLiveExperiment(idx);

        SimpleLiveExperiment.initEnvironment();
        synchronized (MainConfiguration.getSingleton()) {
            MainConfiguration.getSingleton().peerIndex = idx;
            MainConfiguration.getSingleton().peerIP = InetAddress.getLocalHost();
            MainConfiguration.getSingleton().peerZeroIP = InetAddress.getLocalHost();
            MainConfiguration.getSingleton().peerPort = 2000 + idx;
            MainConfiguration.getSingleton().peerZeroPort = 2000;
        }

        exp.init();
        System.out.println("peer address: " + exp.getLocalPeerAddress() + " peer index: " + exp.getLocalPeerIndex()
                + " zero address: " + exp.getPeerZeroAddress());

        exp.run();
        if (idx == 0) {
            for (Process p : processes) {
                p.destroy();
            }
        }
    }

    public SimpleLiveExperiment(int idx) {
        this.idx = idx;
    }

    public void run() {
        // constants
        int a = 2;
        int p = 16;
        int d = 100;
        int c = 2;
        int t = 1;
        double lambda = 5;
        DifferentiableCostFunction globalCostFunc = new VarCostFunction();
        PlanCostFunction localCostFunc = new IndexCostFunction();

        // loggers
        LoggingProvider<IeposAgent<Vector>> loggingProvider = new LoggingProvider<>();
        //loggingProvider.add(new ProgressIndicator());
        loggingProvider.add(new GlobalCostLogger());
        //loggingProvider.add(new LocalCostLogger());
        //loggingProvider.add(new TerminationLogger());
        //loggingProvider.add(new JFreeChartLogger());
        //loggingProvider.add(new FileWriter("simple.log"));

        // dataset
        Dataset<Vector> dataset = new GaussianDataset(p, d, 0, 1, new Random(random.nextLong()));

        // network
        TreeArchitecture architecture = new TreeArchitecture();

        PeerFactory peerFactory = new PeerFactory() {

            @Override
            public Peer createPeer(int peerIndex, Experiment e) {
                List<Plan<Vector>> possiblePlans = dataset.getPlans(peerIndex);
                AgentLoggingProvider agentLP = loggingProvider.getAgentLoggingProvider(peerIndex, 0);

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
        initPeers(idx, 1, peerFactory);
        startPeers(idx, 1);

        if (idx == 0) {
            try {
                Thread.sleep(100000);
            } catch (InterruptedException ex) {
                Logger.getLogger(SimpleLiveExperiment.class.getName()).log(Level.SEVERE, null, ex);
            }
            loggingProvider.print();
        }
    }
}
