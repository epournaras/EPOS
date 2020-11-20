package liveRunUtils.Messages;

import protopeer.network.Message;

import java.io.Serializable;

public class InformGatewayMessage extends Message implements Serializable {
    public int peerID;
    public int run;
    public String status;
    public boolean isLeaf;
    public InformGatewayMessage(int index, int currentRun, String stat, boolean leaf){
        peerID = index;
        run = currentRun;
        status =stat;
        isLeaf = leaf;
    }
}
