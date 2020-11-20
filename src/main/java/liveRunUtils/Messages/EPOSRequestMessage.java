package liveRunUtils.Messages;

import protopeer.network.Message;

import java.io.Serializable;

public class EPOSRequestMessage extends Message implements Serializable {
    public int run;
    public int numPeers;
    public int maxRuns = -1;
    public int currentSim;
    public String status;

    public EPOSRequestMessage(int currentRun, int numNodes, String stat){
        run = currentRun;
        numPeers = numNodes;
        status = stat;
    }

    public EPOSRequestMessage(int currentRun, int numNodes, String stat, int maxNumRuns, int sim){
        run = currentRun;
        numPeers = numNodes;
        status = stat;
        maxRuns = maxNumRuns;
        currentSim = sim;
    }
}
