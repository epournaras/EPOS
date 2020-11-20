package liveRunUtils.Messages;

import protopeer.network.Message;
import java.io.Serializable;

public class ChangeGFCMessage extends Message implements Serializable {
    public String status;

    public ChangeGFCMessage(String func){
        status = func;
    }
}
