package liveRunUtils.Messages;

import protopeer.network.Message;

import java.io.Serializable;

public class InformUserMessage extends Message implements Serializable {
    public int peerID;
    public int run;
    public String status;
    public int selectedPlanID;
    public double alpha;
    public double beta;

    public InformUserMessage(int index, int currentRun, String stat, int planID){
        peerID = index;
        run = currentRun;
        status = stat;
        selectedPlanID = planID;
    }

    public InformUserMessage(int index, int currentRun, String stat) {
        peerID = index;
        run = currentRun;
        status = stat;
    }

    public InformUserMessage(int index, int currentRun, String stat, double a, double b) {
        peerID = index;
        run = currentRun;
        status = stat;
        alpha = a;
        beta = b;
    }
}
