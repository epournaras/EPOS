package experiment;

import agent.Agent;
import agent.ModifiableIeposAgent;
import agent.MultiObjectiveIEPOSAgent;
import agent.PlanSelector;
import agent.dataset.Dataset;
import agent.dataset.GaussianDataset;
import agent.logging.AgentLogger;
import agent.logging.AgentLoggingProvider;
import agent.logging.LoggingProvider;
import agent.planselection.MultiObjectiveIeposPlanSelector;
import config.Configuration;
import config.LiveConfiguration;
import data.Vector;
import org.zeromq.ZMQ;
import protopeer.ZMQExperiment;
import java.io.File;
import java.util.function.Function;

import static config.Configuration.numChildren;
import static config.Configuration.numIterations;


public class LiveRun extends ZMQExperiment {

    static int idx=0;
    static int peerPort=0;
    static int numAgents=0;
    static int initRun=0;
    static int initSim=0;

    public static void main(String[] args) {

        idx = Integer.parseInt(args [0]);
        peerPort = (args.length >= 2 ? Integer.parseInt(args [1]) : 0 );
        numAgents = (args.length >= 3 ? Integer.parseInt(args [2]) : 0 );
        initRun = (args.length >= 4 ? Integer.parseInt(args [3]) : 0 );
        initSim = (args.length >= 5 ? Integer.parseInt(args [4]) : 0 );
        liveRun(idx,peerPort);
        }

        public static void liveRun(int index, int port){
            // common ZeroMQ context for the entire application
            ZMQ.Context zmqContext = ZMQ.context(1);
            runEPOSLive EPOSapp = new runEPOSLive();

            String rootPath = System.getProperty("user.dir");
            String confPath = rootPath + File.separator + "conf" + File.separator + "eposLive.properties";
            Configuration config = Configuration.fromFile(confPath,false,true);
            config.printConfiguration();

            LiveConfiguration liveConf = new LiveConfiguration();
            String[] args = new String[2];
            args[0] = String.valueOf(index);
            args[1]= String.valueOf(port);
            liveConf.readConfiguration(args);

            // set arguments
            liveConf.myIndex = index;
            liveConf.myPort = port;

            System.out.println("my index = " + liveConf.myIndex  + "(" + index + ")");
            System.out.println("my port = " + liveConf.myPort  + "(" + port + ")");
            System.out.println("my IP = " + liveConf.myIP);
            System.out.println("\n---- Configuration ---\n" );
//            liveConf.printParameterFile();
            System.out.println("\n---- End Configuration ---\n" );

            protopeer.MainConfiguration			protopeer_conf = protopeer.MainConfiguration.getSingleton();


            LoggingProvider<MultiObjectiveIEPOSAgent<Vector>> loggingProvider = new LoggingProvider<>();

            for (AgentLogger logger : config.loggers) {
                loggingProvider.add(logger);
            }

            for (AgentLogger al : loggingProvider.getLoggers()) {
                al.setRun(1);
            }


            PlanSelector<MultiObjectiveIEPOSAgent<Vector>, Vector> planSelector = new MultiObjectiveIeposPlanSelector<Vector>();

            Function<Integer, Agent> createAgent = agentIdx -> {

                AgentLoggingProvider<ModifiableIeposAgent<Vector>> agentLP = loggingProvider.getAgentLoggingProvider(agentIdx, 1);
                ModifiableIeposAgent<Vector> newAgent = new ModifiableIeposAgent<Vector>(config, agentLP);
                newAgent.setUnfairnessWeight(Double.parseDouble(config.weights[0]));
                newAgent.setLocalCostWeight(Double.parseDouble(config.weights[1]));
                newAgent.setPlanSelector(planSelector);
                return newAgent;

            };

            EPOSapp.runEPOS(liveConf,protopeer_conf,zmqContext,numChildren,numIterations,numAgents,initRun,initSim,createAgent,config);
//        loggingProvider.print();
        }

    }
