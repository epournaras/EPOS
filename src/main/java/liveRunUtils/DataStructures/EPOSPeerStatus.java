package liveRunUtils.DataStructures;

import protopeer.network.NetworkAddress;

public class EPOSPeerStatus {
    public int index;
    public int run;
    public int initRun = 0;
    public int leaveRun=Integer.MAX_VALUE;
    public String status;
    public boolean isleaf;
    public NetworkAddress address;
    public int peerPort;

    public EPOSPeerStatus(int idx, int currentRun, String stat, boolean leaf, NetworkAddress addr, int port){
        index = idx;
        run = currentRun;
        status = stat;
        isleaf = leaf;
        address = addr;
        peerPort = port;
    }
}
