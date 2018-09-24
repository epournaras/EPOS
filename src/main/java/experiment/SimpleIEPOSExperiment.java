package experiment;

import agent.logging.GlobalCostLogger;
import agent.logging.TerminationLogger;
import config.Configuration;
import agent.logging.LoggingProvider;
import agent.logging.AgentLoggingProvider;
import agent.logging.CostViewer;
import agent.dataset.Dataset;
import agent.*;
import agent.dataset.GaussianDataset;
import agent.logging.GraphLogger;
import agent.logging.LocalCostMultiObjectiveLogger;
import data.Plan;
import data.Vector;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

/**
 *
 * @author Peter
 */
public class SimpleIEPOSExperiment {

    public static void main(String[] args) {
        Random random = new Random(0);

        Dataset<Vector> dataset = new GaussianDataset(16, 100, 0, 1, random);
       
        // logging
        LoggingProvider<IeposAgent<Vector>> loggingProvider = new LoggingProvider<>();
        loggingProvider.add(new GlobalCostLogger());
        loggingProvider.add(new LocalCostMultiObjectiveLogger());
        loggingProvider.add(new TerminationLogger());
        loggingProvider.add(new CostViewer());
        loggingProvider.add(new GraphLogger<>(GraphLogger.Type.Change));

        int numSimulations = 1;
        for(int sim = 0; sim < numSimulations; sim++) {
            final int simulationId = sim;
           
            // algorithm
            PlanSelector<IeposAgent<Vector>, Vector> planSelector = new IeposPlanSelector();
            Function<Integer, Agent> createAgent = agentIdx -> {
                List<Plan<Vector>> possiblePlans = dataset.getPlans(agentIdx);
                AgentLoggingProvider agentLP = loggingProvider.getAgentLoggingProvider(agentIdx, simulationId);

                IeposAgent newAgent = new IeposAgent(
								                        Configuration.numIterations,
								                        possiblePlans,
								                        Configuration.globalCostFunc,
								                        Configuration.localCostFunc,
								                        agentLP,
								                        random.nextLong());
                newAgent.setLambda(Configuration.lambda);
                newAgent.setPlanSelector(planSelector);
                return newAgent;
            };

            // start experiment
            IeposExperiment.runSimulation(
                    Configuration.numChildren,
                    Configuration.numIterations,
                    Configuration.numAgents,
                    createAgent);
        }

        loggingProvider.print();
    }
}