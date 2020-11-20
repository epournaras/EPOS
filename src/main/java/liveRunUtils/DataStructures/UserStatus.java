package liveRunUtils.DataStructures;

import protopeer.network.zmq.ZMQAddress;

public class UserStatus {
    public int index;
    public int run;
    public int leaveRun = Integer.MAX_VALUE;
    public String status;
    public String planStatus = "needPlans";
    public String weightStatus = "noNewWeights";
    public ZMQAddress assignedPeerAddress;
    public ZMQAddress userAddress;

    public UserStatus(int idx, int currentRun, String stat, ZMQAddress userAddr){
        index = idx;
        run = currentRun;
        status = stat;
        userAddress = userAddr;
    }

    public UserStatus(int idx, int currentRun, String stat, ZMQAddress userAddr, ZMQAddress peerAddress){
        index = idx;
        run = currentRun;
        status = stat;
        userAddress = userAddr;
        assignedPeerAddress = peerAddress;
    }
}
