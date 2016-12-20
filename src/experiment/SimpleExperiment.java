package experiment;

import agent.logging.GlobalCostLogger;
import agent.logging.TerminationLogger;
import agent.logging.LoggingProvider;
import agent.logging.AgentLoggingProvider;
import agent.logging.CostViewer;
import agent.dataset.Dataset;
import agent.*;
import agent.dataset.GaussianDataset;
import agent.logging.GraphLogger;
import agent.logging.LocalCostLogger;
import data.Plan;
import data.Vector;
import func.DifferentiableCostFunction;
import func.IndexCostFunction;
import func.PlanCostFunction;
import func.VarCostFunction;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

/**
 *
 * @author Peter
 */
public class SimpleExperiment {

    public static void main(String[] args) {
        Random random = new Random(0);
       
        // dataset
        int numAgents = 127;
        Dataset<Vector> dataset = new GaussianDataset(16, 100, 0, 1, random);

        // optimization functions
        double lambda = 0.1;
        DifferentiableCostFunction globalCostFunc = new VarCostFunction();
        PlanCostFunction localCostFunc = new IndexCostFunction();

        // network
        int numChildren = 2;
       
        // logging
        LoggingProvider<IeposAgent<Vector>> loggingProvider = new LoggingProvider<>();
        loggingProvider.add(new GlobalCostLogger());
        loggingProvider.add(new LocalCostLogger());
        loggingProvider.add(new TerminationLogger());
        loggingProvider.add(new CostViewer());
        loggingProvider.add(new GraphLogger<>(GraphLogger.Type.Change));

        int numSimulations = 1;
        for(int sim = 0; sim < numSimulations; sim++) {
            final int simulationId = sim;
           
            // algorithm
            int numIterations = 20;
            PlanSelector<IeposAgent<Vector>, Vector> planSelector = new IeposPlanSelector();
            Function<Integer, Agent> createAgent = agentIdx -> {
                List<Plan<Vector>> possiblePlans = dataset.getPlans(agentIdx);
                AgentLoggingProvider agentLP = loggingProvider.getAgentLoggingProvider(agentIdx, simulationId);

                IeposAgent newAgent = new IeposAgent(
                        numIterations,
                        possiblePlans,
                        globalCostFunc,
                        localCostFunc,
                        agentLP,
                        random.nextLong());
                newAgent.setLambda(lambda);
                newAgent.setPlanSelector(planSelector);
                return newAgent;
            };

            // start experiment
            IeposExperiment.runSimulation(
                    numChildren,
                    numIterations,
                    numAgents,
                    createAgent);
        }

        loggingProvider.print();
    }
}