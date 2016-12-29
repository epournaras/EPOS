package experiment;

import agent.logging.GlobalCostLogger;
import agent.logging.LoggingProvider;
import agent.logging.AgentLoggingProvider;
import agent.logging.CostViewer;
import agent.*;
import agent.dataset.Dataset;
import agent.dataset.FileVectorDataset;
import agent.dataset.GaussianDataset;
import agent.logging.GraphLogger;
import agent.logging.LocalCostLogger;
import agent.logging.GlobalResponseLogger;
import agent.logging.ProgressIndicator;
import data.Plan;
import data.Vector;
import data.io.VectorIO;
import func.DifferentiableCostFunction;
import func.PlanScoreCostFunction;
import func.PlanCostFunction;
import func.SqrDistCostFunction;
import func.VarCostFunction;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import protopeer.Experiment;
import protopeer.Peer;
import protopeer.PeerFactory;
import protopeer.SimulatedExperiment;
import protopeer.util.quantities.Time;
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

    private static final Map<String, PlanSelector<IeposAgent<Vector>, Vector>> ALGORITHM_MAP = new HashMap<>();

    static {
        ALGORITHM_MAP.put("I-EPOS", new IeposPlanSelector<>());
        ALGORITHM_MAP.put("Global Gradient", new IeposPlanSelector<>());
        ALGORITHM_MAP.put("Individual Gradient", new IeposPlanSelector<>());
        ALGORITHM_MAP.put("Adaptive Gradient", new IeposPlanSelector<>());
    }

    private Dataset<Vector> dataset;
    private DifferentiableCostFunction globalCostFunc;
    private double lambda = 0;
    private PlanCostFunction<Vector> localCostFunc;
    private int numChildren = 2;
    private PlanSelector<IeposAgent<Vector>, Vector> algorithm;
    private int numIterations = 20;
    private long seed = 0;
    private int numAgents = 100;
    
    private Consumer<Double> onProgressDo = (x) -> x = x;
    private CostViewer<Vector> globalCostPlot;
    private GlobalResponseLogger globalResponsePlot;
    private GraphLogger<Vector> agentChangesPlot;

    public static void main(String[] args) {
        new ExperimentGUI().run();
    }

    public void setDataset(String datasetDir) {
        if (datasetDir == null) {
            dataset = null;
        } else {
            dataset = new FileVectorDataset(datasetDir);
        }
    }

    public void setGlobalCostFunc(String targetFile) {
        if (targetFile == null) {
            globalCostFunc = new VarCostFunction();
        } else {
            try {
                globalCostFunc = new SqrDistCostFunction(VectorIO.readVector(new File(targetFile)));
            } catch (FileNotFoundException ex) {
                Logger.getLogger(ExperimentGUI.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void setLambda(double lambda) {
        this.lambda = lambda;
    }

    public void setNumChildren(int numChildren) {
        this.numChildren = numChildren;
    }

    public void setAlgorithm(String algorithmName) {
        algorithm = ALGORITHM_MAP.get(algorithmName);
    }
    
    public int getNumIterations() {
        return numIterations;
    }

    public void setNumIterations(int numIterations) {
        this.numIterations = numIterations;
    }

    public void setSeed(long seed) {
        this.seed = seed;
    }
    
    public void onProgressDo(Consumer<Double> action) {
        onProgressDo = action;
    }
    
    public BufferedImage getGlobalCostPlot(int width, int height) {
       return globalCostPlot.getPlotImage(width, height);
    }
    
    public BufferedImage getGlobalResponsePlot(int width, int height, int iteration) {
       return globalResponsePlot.getPlotImage(width, height, iteration);
    }
    
    public BufferedImage getAgentChangesPlot(int width, int height, int iteration) {
       return agentChangesPlot.getPlotImage(width, height, iteration);
    }

    public void run() {
        //////////////
        // parameters
        //////////////

        if (dataset == null) {
            dataset = new GaussianDataset(16, 100, 0, 1, random);
        }
        if (globalCostFunc == null) {
            globalCostFunc = new VarCostFunction();
        }
        if (localCostFunc == null) {
            localCostFunc = new PlanScoreCostFunction();
        }
        if (numChildren < 1) {
            numChildren = 2;
        }
        if (algorithm == null) {
            algorithm = new IeposPlanSelector<>();
        }
        if (numIterations < 1) {
            numIterations = 20;
        }
        if (numAgents < 1) {
            numAgents = 100;
        }

        random.setSeed(seed);

        if (dataset instanceof FileVectorDataset) {
            numAgents = ((FileVectorDataset) dataset).getNumAgents();
        }

        ////////////////////
        // output definiton
        ////////////////////
        LoggingProvider<IeposAgent<Vector>> loggingProvider = new LoggingProvider<>();
        loggingProvider.add(new GlobalCostLogger());
        if (lambda > 0) {
            loggingProvider.add(new LocalCostLogger());
        }
        
        // notify progress bar about progress
        loggingProvider.add(new ProgressIndicator(onProgressDo));
        
        globalCostPlot = new CostViewer<>(false);
        loggingProvider.add(globalCostPlot); // presents global and local cost (if applicable)

        globalResponsePlot = new GlobalResponseLogger();
        loggingProvider.add(globalResponsePlot); // writes the output signal of the network to stdout in MATALB readable format

        agentChangesPlot = new GraphLogger<>(GraphLogger.Type.Change, false);
        loggingProvider.add(agentChangesPlot); // presents the changes in the network

        ///////////
        // network
        ///////////
        TreeArchitecture architecture = new TreeArchitecture(numChildren);

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

                IeposAgent newAgent = new IeposAgent(numIterations, possiblePlans, globalCostFunc, localCostFunc, agentLP, random.nextLong());
                newAgent.setLambda(lambda);
                newAgent.setPlanSelector(algorithm);
                Peer newPeer = new Peer(peerIndex);

                architecture.addPeerlets(newPeer, newAgent, peerIndex, numAgents);

                return newPeer;
            }
        };
        initPeers(0, numAgents, peerFactory);
        startPeers(0, numAgents);

        runSimulation(Time.inSeconds(3 + numIterations));

        ////////////////
        // Show results
        ////////////////
        loggingProvider.print();
        
        getAgentChangesPlot(500, 500, 1);
    }
}
