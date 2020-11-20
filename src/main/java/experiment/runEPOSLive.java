package experiment;

import agent.Agent;
import agent.IterativeTreeAgent;
import agent.TreeAgent;
import agent.logging.AgentLoggingProvider;
import agent.logging.instrumentation.CustomFormatter;
import config.Configuration;
import config.LiveConfiguration;
import data.Plan;
import data.Vector;
import loggers.EventLog;
import loggers.MemLog;
import loggers.RawLog;
import org.github.jamm.MemoryMeter;
import org.zeromq.ZMQ;
import pgpersist.PersistenceClient;
import protopeer.*;
import protopeer.servers.bootstrap.BootstrapClient;
import treestructure.ModifiableTreeArchitecture;
import treestructure.ModifiableTreeClient;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class runEPOSLive extends ZMQExperiment {
    public void runEPOS(final LiveConfiguration liveConf, final protopeer.MainConfiguration protopeer_conf,
                        final ZMQ.Context zmqContext,
                        int numChildren, // number of children for each middle node
                        int numIterations, // total number of iterations to run for
                        int numAgents, // total number of nodes in the network
                        int initRun,
                        int initSim,
                        Function<Integer, Agent> createAgent, // lambda expression that creates an agent
                        Configuration config)
    {

        Experiment.initEnvironment();		// reads conf/protopeer.conf  (amongst other things)

        synchronized (MainConfiguration.getSingleton()) {
            // get Protopeer configuration values
            MainConfiguration.getSingleton().peerIndex = liveConf.myIndex;
            MainConfiguration.getSingleton().peerPort = liveConf.myPort;
            MainConfiguration.getSingleton().peerZeroPort = config.bootstrapPort;
            System.out.println("Hi, I'm peer with the index " + MainConfiguration.getSingleton().peerIndex);
            // set IP addresses
            try {
                MainConfiguration.getSingleton().peerIP = InetAddress.getByName(liveConf.myIP);
                MainConfiguration.getSingleton().peerZeroIP = InetAddress.getByName(liveConf.bootstrapIP);

                System.out.println("runDias::peerIP = " + MainConfiguration.getSingleton().peerIP);
                System.out.println("runDias::peerZeroIP = " + MainConfiguration.getSingleton().peerZeroIP);

            } catch (UnknownHostException e) {
                System.out.println("runDias::excepton setting host name : " + e);
                e.printStackTrace();
            }
        }

        this.init();
        System.out.println("peer address: " + this.getLocalPeerAddress() + " peer index: " + this.getLocalPeerIndex() + " zero address: " + this.getPeerZeroAddress());
        int myIndex = liveConf.myIndex;

        ModifiableTreeArchitecture architecture = new ModifiableTreeArchitecture(config);

        PeerFactory peerFactory = new PeerFactory() {

            public Peer createPeer(int peerIndex, Experiment e) {

                Peer newPeer = new Peer(peerIndex);
                // create a class for persisting message to PostgreSQL
                final int persistenceClientOutputQueueSize = 1000;
                final int diasNetworkId = 0;
                final String daemonConnectString = "tcp://" + config.persistenceDaemonIP + ":" + config.persistenceDaemonPort;
                PersistenceClient persistenceClient = null;
                final boolean persistenceActive = liveConf.persistenceActive,		// all perisistence will be disabled if this is false
                        persistMessages = liveConf.persistMessages,
                        eventLogging = liveConf.eventLogging,
                        vizPersistence = liveConf.vizPersistence;
                final boolean persistAggregationEvent = liveConf.persistAggregationEvent,
                        rawLog = liveConf.rawLog,
                        persistPSSSamples = liveConf.persistPSSSamples;
                final int rawLogLevel = liveConf.rawLogLevel;

                System.out.println( "peerIndex: " + peerIndex );
                System.out.println( "persistenceActive: " + persistenceActive );
                System.out.println( "eventLogging: " + eventLogging );
                System.out.println( "vizPersistence: " + vizPersistence );
                System.out.println( "rawLog: " + rawLog );
                System.out.println( "rawLogLevel: " + rawLogLevel );

                // connect to PostgreSQL logging daemon
                if( persistenceActive ) {
                    persistenceClient = new PersistenceClient( zmqContext, daemonConnectString, persistenceClientOutputQueueSize );
                    System.out.println( "persistenceClient created" );
                }

                newPeer.SetQueueSizeIn(protopeer_conf.queueSizeIn);
                newPeer.SetQueueSizeOut(protopeer_conf.queueSizeOut);
                newPeer.SetOnFullQueueWait(protopeer_conf.onFullQueueWait);
                newPeer.SetQueueVerbose(protopeer_conf.queueVerbose);
                newPeer.addPersistenceClient(persistenceClient, diasNetworkId);
                Agent newAgent = createAgent.apply(myIndex);
                newAgent.addPersistenceClient(persistenceClient);
                newAgent.setActiveRun(initRun);
                newAgent.setActiveSim(initSim);
                architecture.addPeerlets(newPeer, newAgent, myIndex, numAgents);

                return newPeer;
            }
        };

        Logger rootLogger = LogManager.getLogManager().getLogger("");
        rootLogger.setLevel(config.loggingLevel);
        for (Handler h : rootLogger.getHandlers()) {
            h.setLevel(config.loggingLevel);
            h.setFormatter(new CustomFormatter());
        }

        this.initPeers(myIndex, 1, peerFactory);
        this.startPeers(myIndex, 1);

        Peer thisPeer = this.getPeers().elementAt(myIndex);

        // send Bootstrap hello
        if (true) {
            System.out.println("Waiting before sending BootstrapHello");
            try {
                Thread.currentThread().sleep(2000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            ((ModifiableTreeClient) thisPeer.getPeerletOfType(ModifiableTreeClient.class)).requestNewTreeView();
        }

        //set plans
//        List<Plan<Vector>> possiblePlans =config.getDataset(Configuration.dataset).getPlans(Configuration.mapping.get(myIndex));
//        ((Agent) thisPeer.getPeerletOfType(Agent.class)).addPlans(possiblePlans);
    }
}
