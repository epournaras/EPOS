package liveRunUtils.Messages;

import protopeer.network.Message;
import protopeer.network.zmq.ZMQAddress;

import java.io.Serializable;

public class UserRegisterMessage extends Message implements Serializable {
    public int index;
    public int currentRun;
    public String status;
    public ZMQAddress userAddress;
    public ZMQAddress assignedPeerAddress;

    public UserRegisterMessage(int idx, int run, String stat, ZMQAddress userAddr){
        index = idx;
        currentRun = run;
        status = stat;
        userAddress = userAddr;
    }

    public UserRegisterMessage(int idx, int run, String stat, ZMQAddress userAddr, ZMQAddress assignedPeerAddr){
        index = idx;
        currentRun = run;
        status = stat;
        userAddress = userAddr;
        assignedPeerAddress = assignedPeerAddr;
    }
}
