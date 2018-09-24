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
import protopeer.measurement.MeasurementLog;

/**
 * An AgentLogger that has no logging logic. It reads the log written by
 * FileWriter and makes the data accessible to other AgentLoggers.
 *
 * @author Peter
 */
public class FileReader extends AgentLogger {

    private String filename;

    /**
     * Creates a new FileReader that reads from the specified file.
     * The data is read from output-data/filename.
     *
     * @param filename the name of the file.
     */
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
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream("output-data/" + filename))) {
            MeasurementLog loaded = (MeasurementLog) ois.readObject();
            log.mergeWith(loaded);
        } catch (IOException | ClassNotFoundException ex) {
            Logger.getLogger(FileReader.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
