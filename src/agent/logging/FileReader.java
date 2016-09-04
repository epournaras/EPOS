/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package agent.logging;

import agent.Agent;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import protopeer.measurement.LogReplayer;
import protopeer.measurement.MeasurementLog;

/**
 *
 * @author Peter
 */
public class FileReader extends AgentLogger {
    private String filename;
    
    public FileReader(String filename) {
        this.filename = filename;
    }

    @Override
    public void init(Agent agent) {
    }

    @Override
    public void log(MeasurementLog log, int epoch, Agent agent) {
    }

    @Override
    public void print(MeasurementLog log) {
        try(ObjectInputStream ois = new ObjectInputStream(new FileInputStream("output-data/" + filename))){
            MeasurementLog loaded = (MeasurementLog) ois.readObject();
            log.mergeWith(loaded);
        } catch (IOException | ClassNotFoundException ex) {
            Logger.getLogger(FileReader.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
