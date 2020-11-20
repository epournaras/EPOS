package liveRunUtils.Messages;

import data.Plan;
import data.Vector;
import protopeer.network.Message;
import java.io.Serializable;
import java.util.List;

public class PlanSetMessage extends Message implements Serializable {
    public String status;
    public List<Plan<Vector>> possiblePlans;

    public PlanSetMessage(String stat){
        status = stat;
    }
}
