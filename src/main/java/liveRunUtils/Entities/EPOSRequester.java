package liveRunUtils.Entities;

import liveRunUtils.Messages.EPOSRequestMessage;
import config.Configuration;
import loggers.EventLog;
import org.zeromq.ZMQ;
import pgpersist.PersistenceClient;
import protopeer.measurement.MeasurementLogger;
import protopeer.network.Message;
import protopeer.network.NetworkAddress;
import protopeer.network.NetworkInterface;
import protopeer.network.NetworkListener;
import protopeer.network.zmq.ZMQAddress;
import protopeer.network.zmq.ZMQNetworkInterface;
import protopeer.network.zmq.ZMQNetworkInterfaceFactory;
import protopeer.time.RealClock;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

/*
EPOS requester is the main entity starting the live operations.
It does the following tasks:
1) initialize the gateway (system gateway and manager)
2) initialize the user (representing system users such as IoT devices)
3) request certain number of runs (config.maxRuns) and simulations (config.maxSims) from the gateway
4) tracking current run and simulation as well as the status of the system
5) in case of config changes, announcing the to the gateway and changing the config file
 */
public class EPOSRequester {

    ZMQNetworkInterfaceFactory zmqNetworkInterfaceFactory;
    ZMQNetworkInterface zmqNetworkInterface;
    transient PersistenceClient persistenceClient;
    static String rootPath = System.getProperty("user.dir");
    static String confPath = rootPath + File.separator + "conf" + File.separator + "eposLive.properties";
    static Configuration config;
    static String EPOSRequesterIP;
    static int EPOSRequesterPort;
    static int EPOSRequesterPeerID;
    static String GateWayIP;
    static int GateWayPort;
    static int maxSimulations;
    int currentSim = 0;
    static int maxNumRuns;
    static int numPeers;
    static int persistenceClientOutputQueueSize;
    static int sleepSecondBetweenRuns;

    public static void main(String[] args) throws UnknownHostException {

        EPOSRequester eposRequester = new EPOSRequester();
        eposRequester.readConfig();
        eposRequester.bringUp();
        eposRequester.setUpPersistantClient();
        eposRequester.setUpEventLogger();
        eposRequester.listen();
        eposRequester.startSimulation();
    }

    public void readConfig(){
        // reading the configuration. note that this entity does not need to load any datasets
        config = Configuration.fromFile(confPath,false,true);

        EPOSRequesterIP = config.EPOSRequesterIP;
        EPOSRequesterPort = config.EPOSRequesterPort;
        EPOSRequesterPeerID = config.EPOSRequesterPeerID;
        GateWayIP = config.GateWayIP;
        GateWayPort = config.GateWayPort;

        maxSimulations = config.maxSimulations;
        maxNumRuns = config.maxNumRuns;
        numPeers = config.numAgents;

        sleepSecondBetweenRuns = config.sleepSecondBetweenRuns;
        persistenceClientOutputQueueSize = config.persistenceClientOutputQueueSize;
    }

    public void startSimulation() {

        try {
            // initializing gateway and user
            Runtime.getRuntime().exec("screen -S GateWay -d -m java -Xmx1024m -jar GateWay.jar");
            Runtime.getRuntime().exec("screen -S Users -d -m java -Xmx2048m -jar IEPOSUsers.jar "+currentSim);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // creating the EPOS request message, to be send to gateway
    public EPOSRequestMessage createMessage(Configuration conf, int numNodes) throws UnknownHostException {
        EPOSRequestMessage eposRequestMessage = new EPOSRequestMessage(1,numNodes,"EPOSRunRequested", maxNumRuns, currentSim);
        return eposRequestMessage;
    }

    // bringing up, and binding the gateway to a network address and port (based on config)
    public void bringUp(){
        RealClock clock=new RealClock();
        MeasurementLogger measurementLogger=new MeasurementLogger(clock);
        zmqNetworkInterfaceFactory=new ZMQNetworkInterfaceFactory(measurementLogger);

        ZMQAddress EPOSRequesterAddress = new ZMQAddress(EPOSRequesterIP,EPOSRequesterPort);
        System.out.println("zmqAddress : " + EPOSRequesterAddress );

        zmqNetworkInterface=(ZMQNetworkInterface)zmqNetworkInterfaceFactory.createNewNetworkInterface(measurementLogger, EPOSRequesterAddress);
    }

    // listening to the messages from network
    public void listen(){
        zmqNetworkInterface.addNetworkListener(new NetworkListener()
        {
            public void exceptionHappened(NetworkInterface networkInterface, NetworkAddress remoteAddress, Message message, Throwable cause) {
                System.out.println( "ZmqTestClient::exceptionHappened" + cause );
                cause.printStackTrace();
            }

            public void interfaceDown(NetworkInterface networkInterface) {
                System.out.println( "ZmqTestClient::interfaceDown" );
            }

            public void messageReceived(NetworkInterface networkInterface, NetworkAddress sourceAddress, Message message) {
                if (message instanceof EPOSRequestMessage){
                    EPOSRequestMessage eposRequestMessage = (EPOSRequestMessage) message;
                    if (eposRequestMessage.status.equals("usersRegistered")){
                        // informed by gateway that all users are registered
                        System.out.println("EPOS users registered! Run: "+eposRequestMessage.run+" simulation: "+currentSim);
                        try {
                            EventLog.logEvent("EPOSRequester", "messageReceived", "usersRegistered" , eposRequestMessage.run+"-"+currentSim);
                            requestEPOS(createMessage(config, numPeers));
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        }
                    }
                    if (eposRequestMessage.status.equals("finished")){
                        // informed by gateway that an EPOS run is finished
                        System.out.println("EPOS finished successfully! Run: "+eposRequestMessage.run+" simulation: "+currentSim);
                        EventLog.logEvent("EPOSRequester", "messageReceived", "EPOSFinished" , eposRequestMessage.run+"-"+currentSim);
                    }
                    if (eposRequestMessage.status.equals("maxRunReached")){
                        // informed by gateway that all runs in a simulation is over
                        System.out.println("---");
                        System.out.println("SIMULATION: "+currentSim+" Over");
                        System.out.println("---");

                        if (currentSim == maxSimulations){
                            // if all simulations are done, terminate the system
                            EventLog.logEvent("EPOSRequester", "messageReceived", "ALL SIMULATIONS Done" , eposRequestMessage.run+"-"+currentSim);
                            try {
                                terminate();
                                TimeUnit.SECONDS.sleep(sleepSecondBetweenRuns);
                                System.exit(0);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }

                        EventLog.logEvent("EPOSRequester", "messageReceived", "SIMULATION Over" , eposRequestMessage.run+"-"+currentSim);
                        currentSim++;
                        try {
                            terminate();
                            // checking for config change, between runs and simulations
                            checkConfigChanges();
                            TimeUnit.SECONDS.sleep(sleepSecondBetweenRuns);
                            startSimulation();
                        } catch (InterruptedException | IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            public void messageSent(NetworkInterface networkInterface, NetworkAddress destinationAddress, Message message) {
//                System.out.println("Message sent: + " +destinationAddress + " message: "+ message + " messageSize: " + message.getMessageSize());
            }


            public void interfaceUp(NetworkInterface networkInterface) {
                System.out.println( "ZmqTestClient::interfaceUp" );
            }
        });

        zmqNetworkInterface.bringUp();
    }

    // requesting EPOS service
    public void requestEPOS(EPOSRequestMessage ERM){
        ZMQAddress destination = new ZMQAddress(GateWayIP,GateWayPort);
        zmqNetworkInterface.sendMessage(destination, ERM);
    }

    // this is a template of changing configs depending on the simulation
    public void checkConfigChanges() throws IOException {
        if (currentSim%3 == 0) {
            config.changeConfig(confPath,"globalCostFunction","RMSE");
            config.changeConfig(confPath,"userChangeProb","9");
            config.changeConfig(confPath,"GCFChangeProb","9");
            config.changeConfig(confPath,"newWeightProb","9");
            config.changeConfig(confPath,"newPlanProb","9");
        }
        if (currentSim%3 == 1) {
            config.changeConfig(confPath,"globalCostFunction","RMSE");
            config.changeConfig(confPath,"userChangeProb","4");
            config.changeConfig(confPath,"GCFChangeProb","4");
            config.changeConfig(confPath,"newWeightProb","4");
            config.changeConfig(confPath,"newPlanProb","4");
        }
        if (currentSim%3 == 2) {
            config.changeConfig(confPath,"globalCostFunction","RMSE");
            config.changeConfig(confPath,"userChangeProb","2");
            config.changeConfig(confPath,"GCFChangeProb","2");
            config.changeConfig(confPath,"newWeightProb","2");
            config.changeConfig(confPath,"newPlanProb","2");
        }
        readConfig();
    }

    public void terminate(){
        try {
            Runtime.getRuntime().exec("./killAll.sh");
            EventLog.logEvent("EPOSRequester", "terminate", "terminate", String.valueOf(currentSim));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // setting up the local event logger
    public void setUpEventLogger(){
        EventLog.setPeristenceClient(persistenceClient);
        EventLog.setPeerId(EPOSRequesterPeerID);
        EventLog.setDIASNetworkId(0);
    }

    public void setUpPersistantClient(){
        ZMQ.Context zmqContext = ZMQ.context(1);
        String daemonConnectString = "tcp://" + config.persistenceDaemonIP + ":" + config.persistenceDaemonPort;
        persistenceClient = new PersistenceClient( zmqContext, daemonConnectString, persistenceClientOutputQueueSize );
        System.out.println( "persistenceClient created" );
    }
}
