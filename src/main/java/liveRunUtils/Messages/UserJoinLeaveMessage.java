package liveRunUtils.Messages;

import protopeer.network.Message;
import protopeer.network.zmq.ZMQAddress;

import java.io.Serializable;

public class UserJoinLeaveMessage extends Message implements Serializable {
    public int userIndex;
    public int currentRun;
    public ZMQAddress userAddress;
    public ZMQAddress peerAddress;
    public String joinLeaveStatus; // true is join, false is leave

    public UserJoinLeaveMessage(int idx, int run, String stat, ZMQAddress userAddr){
        userIndex = idx;
        currentRun = run;
        joinLeaveStatus = stat;
        userAddress = userAddr;
    }

    public UserJoinLeaveMessage(String stat, int run){
        joinLeaveStatus = stat;
        currentRun = run;
    }
}
