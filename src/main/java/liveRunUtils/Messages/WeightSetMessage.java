package liveRunUtils.Messages;

import protopeer.network.Message;
import java.io.Serializable;

public class WeightSetMessage extends Message implements Serializable {
    public String status;
    public double alpha;
    public double beta;

    public WeightSetMessage(String stat){
        status = stat;
    }
    public WeightSetMessage(String stat, double a, double b){
        status = stat;
        alpha = a;
        beta = b;
    }
}
