package liveRunUtils.Messages;

import protopeer.network.Message;

import java.io.Serializable;
import java.util.List;

public class InformBootstrap extends Message implements Serializable {
    public int currentRun;
    public int numPeers;
    public String status;
    public List<Integer> activePeers;

    public InformBootstrap(int run, String stat, int newNumPeers, List<Integer> actPeers){
        currentRun = run;
        status = stat;
        numPeers = newNumPeers;
        activePeers = actPeers;
    }

    public InformBootstrap(int run, String stat){
        currentRun = run;
        status = stat;
    }
}
