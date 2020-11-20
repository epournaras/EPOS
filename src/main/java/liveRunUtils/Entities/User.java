package liveRunUtils.Entities;

import liveRunUtils.Messages.*;
import config.Configuration;
import data.Plan;
import data.Vector;
import liveRunUtils.DataStructures.UserStatus;
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
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.*;

import static org.apache.commons.math3.util.Precision.round;

/*
The user class acts on behalf of the users (e.g., IoT Devices), and takes care of the following tasks: simulation
1) initiates n = numAgents users
2) registers each user with the gateway, and in turn receives the address of the corresponding EPOS peer
3) select the plans for each user from the dataset, and sends them to the peer over the network
4) informs the peer if there are changes in the plans or the weights
4) informs the peer if it is leaving the network at the end of the current run
5) informs the gateway if there are new users joining the network at the end of the current run
6) keeps track of the status of each peer during the runs and simulations
** the user class is initialized by the EPOSRequester, at the beginning of each
 */

public class User {

    private ZMQNetworkInterfaceFactory zmqNetworkInterfaceFactory;
    private ZMQNetworkInterface zmqNetworkInterface;
    transient PersistenceClient persistenceClient;
    private String UserIP;
    private int userPort;
    private ZMQAddress UserAddress;
    private String gateWayIP;
    private int gateWayPort;
    private ZMQAddress gateWayAddress;
    private Configuration config;
    private List<UserStatus> Users;
    private List<Integer> numUsersPerRun;
    private int usersWithAssignedPeer;
    private int finishedPeers=0;
    private int currentRun = 0;
    private int currentSim=0;
    private int finishedRun=-1;
    private int usersWithAssignedPeerRunning =0;

    private int maxNumRuns;

    private Boolean userChange;
    private boolean userChangeProcessed = new Boolean(false);
    private int joinLeaveRate;
    private int userChangeProb;
    private int maxNumPeers;
    private int minNumPeers;

    private Boolean planChange;
    private int newPlanProb;
    private Boolean weightChange;
    private int newWeightProb;

    private int dataSetSize;
    private List<Integer> numberList;
    private Boolean randomiseUsers;
    private List<Integer> userDatasetIndices;

    int persistenceClientOutputQueueSize;
    int UserPeerID;

    public User(){
        String rootPath = System.getProperty("user.dir");
        String confPath = rootPath + File.separator + "conf" + File.separator + "eposLive.properties";
        config = Configuration.fromFile(confPath,true,true );
        numUsersPerRun = new ArrayList<Integer>();

        UserIP = config.UserIP;
        userPort = config.UserPort;
        gateWayIP = config.GateWayIP;
        gateWayPort = config.GateWayPort;
        UserPeerID = config.UserPeerID;

        maxNumRuns = config.maxNumRuns;
        //dynamic settings
        userChange = new Boolean(config.userChange);
        planChange = new Boolean(config.planChange);
        weightChange = new Boolean(config.weightChange);
        randomiseUsers = new Boolean(config.randomiseUsers);

        joinLeaveRate = config.joinLeaveRate;
        userChangeProb = config.userChangeProb;
        maxNumPeers = config.maxNumPeers;
        minNumPeers = config.minNumPeers;
        newPlanProb = config.newPlanProb;
        newWeightProb = config.newWeightProb;

        dataSetSize = config.dataSetSize;
        numberList = new ArrayList<>();
        for(int i=0; i<=dataSetSize; i++){ numberList.add(i); }
        userDatasetIndices = new ArrayList<Integer>(); // mapping of the users to the agents in the dataset

        numUsersPerRun = new ArrayList<Integer>();
        numUsersPerRun = new ArrayList<Integer>(Collections.nCopies(maxNumRuns+2, 0));
        numUsersPerRun.set(0, Configuration.numAgents);

        persistenceClientOutputQueueSize = config.persistenceClientOutputQueueSize;
    }

    public static void main(String[] args) {
        User user = new User();
        user.currentSim = Integer.parseInt(args[0]);
        // setting up persistant client, for logging over the network
        user.setUpPersistantClient();
        user.setUpEventLogger();
        user.createInterface();
        /*
         select and initiate users and match them to an agent in the dataset
         relevant if numAgents < datasetSize
         also, if randomiseUsers = true, the users are assigned to an agent randomly
         */
        user.selectUsers(user.numUsersPerRun.get(0), user.randomiseUsers);
        user.initiateUsers();
        user.registerUsers();
    }

    public void createInterface(){
        RealClock clock=new RealClock();
        MeasurementLogger measurementLogger=new MeasurementLogger(clock);
        zmqNetworkInterfaceFactory=new ZMQNetworkInterfaceFactory(measurementLogger);

        UserAddress = new ZMQAddress(UserIP,userPort);
        System.out.println("user entity address : " + UserAddress);

        gateWayAddress = new ZMQAddress(gateWayIP,gateWayPort);
        System.out.println("gateway address : " + UserAddress);

        zmqNetworkInterface = (ZMQNetworkInterface) zmqNetworkInterfaceFactory.createNewNetworkInterface(measurementLogger, UserAddress);
        zmqNetworkInterface.addNetworkListener(new NetworkListener()
        {
            public void exceptionHappened(NetworkInterface networkInterface, NetworkAddress remoteAddress,
                                          Message message, Throwable cause) {
                System.out.println( "ZmqTestClient::exceptionHappened" + cause );
                cause.printStackTrace();
            }

            public void interfaceDown(NetworkInterface networkInterface) {
                System.out.println( "ZmqTestClient::interfaceDown" );
            }

            public void messageReceived(NetworkInterface networkInterface, NetworkAddress sourceAddress, Message message) {
                synchronized (this) {
                    if (message instanceof InformUserMessage) {
                        InformUserMessage informUserMessage = (InformUserMessage) message;
                        if (informUserMessage.status.equals("assignedPeerRunning")) {
                            // user is registered, the assigned peer is running and ready to receive the plans
                            Users.get(informUserMessage.peerID).status = "assignedPeerRunning";
                            usersWithAssignedPeerRunning++;
                            if (usersWithAssignedPeerRunning == numUsersPerRun.get(currentRun)) {
                                // all users have been assigned to peers, and the peers are running
                                usersWithAssignedPeerRunning = 0;
                                numUsersPerRun.set(currentRun + 1, numUsersPerRun.get(currentRun));
                                System.out.println("all users have their assigned peers running for run: " + currentRun + " numPeers: " + numUsersPerRun.get(currentRun));
                                // userChangeProcessed tracks of the ongoing changes during an iteration.
                                // While the changes in the previous iteration are not completely processed, no new changes are allowed
                                if (!userChangeProcessed && userChange) {
                                    userChangeProcessed = new Boolean(true);
                                    usersJoiningOrLeaving();
                                }
                            }
                        }
                        if (informUserMessage.status.equals("finished")) {
                            // EPOS run finished
                            checkCorrectRun(informUserMessage);
                            if (finishedPeers == numUsersPerRun.get(currentRun)) {
                                finishedPeers = 0;
                                System.out.println("---");
                                System.out.println("EPOS FINISHED! Run: " + currentRun + " numPeers: " + numUsersPerRun.get(currentRun));
                                System.out.println("---");
                                finishedRun = currentRun;
                                currentRun++;
                                userChangeProcessed = new Boolean(false);;;
                                resetPerRun();
                            }
                        }
                        if (informUserMessage.status.equals("checkNewPlans")) {
                            // a peer checking if it has new plans. This happens at the end of each run
                            checkForNewPlans(informUserMessage);
                        }
                        if (informUserMessage.status.equals("checkNewWeights")) {
                            // a peer checking if it has new weights. This happens at the end of each run
                            checkForNewWeights(informUserMessage);
                        }
                    } else if (message instanceof UserRegisterMessage) {
                        // user registered by the gateway, and a peer is assigned (the address of the assigned peer is given
                        UserRegisterMessage userRegisterMessage = (UserRegisterMessage) message;
                        Users.get(userRegisterMessage.index).status = "peerAssigned";
                        Users.get(userRegisterMessage.index).assignedPeerAddress = userRegisterMessage.assignedPeerAddress;
                        if (Users.get(userRegisterMessage.index).planStatus.equals("needPlans")){
                            // if the peer has no plans set yet, or needs plans
                            sendPlans(userRegisterMessage.index, userRegisterMessage.assignedPeerAddress);
                        }
                        usersWithAssignedPeer++;
                        if (usersWithAssignedPeer == numUsersPerRun.get(currentRun)) {
                            // peers have their treeView set
                            usersWithAssignedPeer = 0;
                            System.out.println("all peers are assigned treeView: " + currentRun + " numPeers: " + numUsersPerRun.get(currentRun));
                        }
                    }
                }
            }

            public void messageSent(NetworkInterface networkInterface, NetworkAddress destinationAddress, Message message) {
                if (message instanceof PlanSetMessage){
                    PlanSetMessage planSetMessage = (PlanSetMessage) message;
                    System.out.println(planSetMessage.status+" for: "+message.getDestinationAddress());
                }
            }


            public void interfaceUp(NetworkInterface networkInterface) {
                System.out.println( "ZmqTestClient::interfaceUp" );
            }
        });
        zmqNetworkInterface.bringUp();
    }

    // initiating users
    public void initiateUsers(){
        Users = new ArrayList<UserStatus>(numUsersPerRun.get(currentRun));
        for (int i=0;i<numUsersPerRun.get(currentRun);i++){
            UserStatus user = new UserStatus(i,0,"initiated", UserAddress);
            Users.add(user);
        }
    }

    // selecting agents from the dataset, and assigning their plans to the users
    public void selectUsers(int size, boolean randomise){
        List<Integer> defaultMapping = new ArrayList<Integer>(size);
        for(int j=0; j<size; j++){
            defaultMapping.add(this.numberList.get(j));
        }
        if (randomise){ Collections.shuffle(defaultMapping,new Random(config.permutationSeed));}
        userDatasetIndices.addAll(defaultMapping);
    }

    // register users with the gateway
    public void registerUsers(){
        for (UserStatus user: Users) {
            zmqNetworkInterface.sendMessage(gateWayAddress, new UserRegisterMessage(user.index, currentRun,user.status,user.userAddress));
        }
        System.out.println("user register message send for all of the users: "+currentRun);
    }

    public void sendPlans(int idx, ZMQAddress address){

        PlanSetMessage psm = null;
        try {
            psm = createPlanMessage(config,idx);
            EventLog.logEvent("User", "sendPlans", "set user plans" , idx+"-"+userDatasetIndices.get(idx)+"-"+currentSim);
            sendPlansMessage(psm,address);
            Users.get(idx).planStatus = "noNewPlans";
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public PlanSetMessage createPlanMessage(Configuration conf, int Index) throws UnknownHostException {
        PlanSetMessage planSetMessage = new PlanSetMessage("setPlans");
        planSetMessage.possiblePlans = generatePlans(Index);
        return planSetMessage;
    }

    public void sendPlansMessage(PlanSetMessage planSetMessage, ZMQAddress destination){
        zmqNetworkInterface.sendMessage(destination, planSetMessage);
    }

    // read the agent plans from the dataset
    public List<Plan<Vector>> generatePlans(int peerIdx){
        List<Plan<Vector>> possiblePlans = config.getDataset(Configuration.dataset).getPlans(userDatasetIndices.get(peerIdx));
        return possiblePlans;
    }

    // at the end of each run, with probability 1/(1+newPlanProb), it is given new plans
    public void usersHavingNewPlans(UserStatus user){
        SecureRandom random = new SecureRandom();
        if( (random.nextInt(newPlanProb) + 1) == 1 && user.leaveRun > (currentRun+1)){
            user.planStatus = "hasNewPlans";
            System.out.println("user: "+user.index+" has new plans");
            zmqNetworkInterface.sendMessage(user.assignedPeerAddress, new PlanSetMessage("hasNewPlans"));
            EventLog.logEvent("User", "usersHavingNewPlans", "hasNewPlans" , user.index+"-"+currentRun+"-"+currentSim);
        }
    }

    // if a user status is "hasNewPlans" it is sent new plans over the network by the user
    public void checkForNewPlans(InformUserMessage informUserMessage){
        if (Users.get(informUserMessage.peerID).planStatus.equals("hasNewPlans")){
            PlanSetMessage planSetMessage = new PlanSetMessage("changePlans");
            // generates new plans for the user
            SecureRandom randomGenerator = new SecureRandom();
            int randomInt = randomGenerator.nextInt(dataSetSize) + 1;
            while (userDatasetIndices.contains(randomInt)){
                randomInt = randomGenerator.nextInt(dataSetSize) + 1;
            }
            userDatasetIndices.set(informUserMessage.peerID,randomInt);
            planSetMessage.possiblePlans = generatePlans(informUserMessage.peerID);
            sendPlansMessage(planSetMessage,Users.get(informUserMessage.peerID).assignedPeerAddress);
            Users.get(informUserMessage.peerID).planStatus = "noNewPlans";
        }
        else {
//            zmqNetworkInterface.sendMessage(Users.get(informUserMessage.peerID).assignedPeerAddress, new PlanSetMessage("noNewPlans"));
        }
    }

    public void usersHavingNewWeights(UserStatus user){
        SecureRandom random = new SecureRandom();
        if( (random.nextInt(newWeightProb) + 1) == 1){
            user.weightStatus = "hasNewWeights";
            zmqNetworkInterface.sendMessage(user.assignedPeerAddress, new PlanSetMessage("hasNewWeights"));
            EventLog.logEvent("User", "usersHavingNewPlans", "hasNewWeights" , user.index+"-"+currentRun+"-"+currentSim);
        }
    }

    // if a user status is "hasNewWeights" it is sent new weights over the network by the user
    public void checkForNewWeights(InformUserMessage informUserMessage){
        if (Users.get(informUserMessage.peerID).weightStatus.equals("hasNewWeights")){
            boolean val = new SecureRandom().nextInt(2)==0;
            double oldAlpha = informUserMessage.alpha;
            double oldBeta = informUserMessage.beta;
            if (val){
                // increase alpha
                double newAlpha = round(( new SecureRandom().nextInt(20)*(0.05) ),2);
                if ( !( (newAlpha+oldBeta) < 1) ){
                    double newBeta = round(oldBeta - Math.abs(1-(newAlpha+oldBeta)),2);
                    zmqNetworkInterface.sendMessage(Users.get(informUserMessage.peerID).assignedPeerAddress, new WeightSetMessage("setNewWeights",newAlpha,newBeta));
//                    System.out.println("(inc alpha) user: "+informUserMessage.peerID+ " has new weights of alpha: "+newAlpha+" beta: "+newBeta);
                }
                else if ( ( (newAlpha+oldBeta) < 1)) {
                    zmqNetworkInterface.sendMessage(Users.get(informUserMessage.peerID).assignedPeerAddress, new WeightSetMessage("setNewWeights",newAlpha,oldBeta));
//                    System.out.println("(inc alpha) user: "+informUserMessage.peerID+ " has new weights of alpha: "+newAlpha+" beta: "+oldBeta);
                }
            }
            else {
                // increase beta
                double newBeta = round(( new SecureRandom().nextInt(20)*(0.05) ),2);
                if ( !( (oldAlpha+newBeta) < 1) ){
                    double newAlpha = round(oldAlpha - Math.abs(1-(oldAlpha+newBeta)),2);
                    zmqNetworkInterface.sendMessage(Users.get(informUserMessage.peerID).assignedPeerAddress, new WeightSetMessage("setNewWeights",newAlpha,newBeta));
//                    System.out.println("(inc beta) user: "+informUserMessage.peerID+ " has new weights of alpha: "+newAlpha+" beta: "+newBeta);
                }
                else if ( (oldAlpha+newBeta) < 1) {
                    zmqNetworkInterface.sendMessage(Users.get(informUserMessage.peerID).assignedPeerAddress, new WeightSetMessage("setNewWeights", oldAlpha, newBeta));
//                    System.out.println("(inc beta) user: " + informUserMessage.peerID + " has new weights of alpha: " + oldAlpha + " beta: " + newBeta);
                }
            }
            Users.get(informUserMessage.peerID).weightStatus = "noNewWeights";
        }
//        else { zmqNetworkInterface.sendMessage(Users.get(informUserMessage.peerID).assignedPeerAddress, new WeightSetMessage("noNewWeights"));}
    }

    // informing the gateway about new number of users
    public void addRemoveUsers(){
        SecureRandom random = new SecureRandom();
        if (random.nextInt(2) == 0 && numUsersPerRun.get(currentRun) < maxNumPeers){
            int countJoined=0;
            for (int r=0;r<numUsersPerRun.get(currentRun)/joinLeaveRate;r++){
                UserStatus user = new UserStatus(Users.size(),currentRun+1,"added", UserAddress);
                Users.add(user);

                SecureRandom randomGenerator = new SecureRandom();
                int randomInt = randomGenerator.nextInt(dataSetSize) + 1;
                while (userDatasetIndices.contains(randomInt)){randomInt = randomGenerator.nextInt(dataSetSize) + 1; }
                userDatasetIndices.add(randomInt);

                zmqNetworkInterface.sendMessage(gateWayAddress, new UserJoinLeaveMessage(Users.size()-1,currentRun+1,"join",this.UserAddress));
                System.out.println("users: "+(Users.size()-1)+" will join the system at run: "+(currentRun+1));
                countJoined++;
                EventLog.logEvent("User", "addRemoveUsers", "userJoin" , (Users.size()-1)+"-"+currentRun+"-"+currentSim);
            }
            numUsersPerRun.set(currentRun+1,numUsersPerRun.get(currentRun)+countJoined);
        }
        else if (numUsersPerRun.get(currentRun) > minNumPeers) {
            int countLeft=0;
            Set<Integer> indices = new HashSet<Integer>();
            SecureRandom newRand = new SecureRandom();
            for (int r=0;r<numUsersPerRun.get(currentRun)/joinLeaveRate;r++){
                indices.add(newRand.nextInt(Users.size()-1));
            }
            Iterator<Integer> it = indices.iterator();
            while (it.hasNext()){
                int index = it.next();
                if (index !=0 && (currentRun < Users.get(index).leaveRun) ){
                    Users.get(index).run = currentRun+1;
                    Users.get(index).leaveRun = currentRun+1;
                    userDatasetIndices.set(index,-1);
                    zmqNetworkInterface.sendMessage(gateWayAddress, new UserJoinLeaveMessage(index, currentRun+1,"leave",this.UserAddress));
                    System.out.println("users: "+index+" will leave the system at run: "+(currentRun+1));
                    countLeft++;
                    EventLog.logEvent("User", "addRemoveUsers", "userLeave" , index+"-"+currentRun+"-"+currentSim);
                }
            }
            numUsersPerRun.set(currentRun+1,numUsersPerRun.get(currentRun)-countLeft);
        }
    }

    // at the end of each run, users join/leave with probability 1/(1+userChangeProb)
    public void usersJoiningOrLeaving(){
        boolean val = new SecureRandom().nextInt(userChangeProb)==0;
        if( val && currentRun>0){
            addRemoveUsers();
        }
        else {
            zmqNetworkInterface.sendMessage(gateWayAddress, new UserJoinLeaveMessage("noChange",currentRun));}
    }

    public void resetPerRun(){
        finishedPeers=0;
        usersWithAssignedPeerRunning =0;
        usersWithAssignedPeer=0;
    }

    public void checkCorrectRun(InformUserMessage informUserMessage){
        if (informUserMessage.run == finishedRun+1){
            Users.get(informUserMessage.peerID).status = "finished";
            Users.get(informUserMessage.peerID).run = informUserMessage.run;
            if (planChange) {usersHavingNewPlans(Users.get(informUserMessage.peerID));}
            if (weightChange) {usersHavingNewWeights(Users.get(informUserMessage.peerID));}
            finishedPeers++;
        }
        else {
            // checking to see if all peers have correctly finished their current run
            EventLog.logEvent("User", "checkCorrectRun", "incorrectFinish" , informUserMessage.peerID+"-"+currentRun+"-"+currentSim);
            System.out.println("incorrect finish message received from peer"+informUserMessage.peerID+
                    " reported run: "+informUserMessage.run+" numPeers for incorrect run: "+numUsersPerRun.get(informUserMessage.run));
            System.out.println("current run: "+currentRun+" correct numPeers: "+numUsersPerRun.get(currentRun));
            if (numUsersPerRun.get(currentRun) != numUsersPerRun.get(informUserMessage.run)) {System.exit(1);}
        }
    }

    public void setUpEventLogger(){
        EventLog.setPeristenceClient(persistenceClient);
        EventLog.setPeerId(UserPeerID);
        EventLog.setDIASNetworkId(0);
    }

    public void setUpPersistantClient(){
        ZMQ.Context zmqContext = ZMQ.context(1);
        String daemonConnectString = "tcp://" + config.persistenceDaemonIP + ":" + config.persistenceDaemonPort;
        persistenceClient = new PersistenceClient( zmqContext, daemonConnectString, persistenceClientOutputQueueSize );
        System.out.println( "persistenceClient created" );
    }

}
