package liveRunUtils.Messages;

import protopeer.network.Message;

import java.io.Serializable;

public class TreeViewChangeMessage extends Message implements Serializable {
    public int currentRun;
    public int numPeers;
    public String status;

    public TreeViewChangeMessage(int run, String stat, int newNumPeers){
        currentRun = run;
        status = stat;
        numPeers = newNumPeers;
    }

    public TreeViewChangeMessage(int run, String stat){
        currentRun = run;
        status = stat;
    }
}
