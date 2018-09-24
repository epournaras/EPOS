/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package agent.logging;

import agent.Agent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import protopeer.measurement.MeasurementLog;

/**
 * An AgentLogger that has no logging logic. It writes the log to a file. This
 * file can be read by FileReader.
 *
 * @author Peter
 */
public class FileWriter extends AgentLogger {

    private String filename;

    /**
     * Creates a new FileWriter that writes to the specified file.
     * The data is written to output-data/filename.
     *
     * @param filename the name of the file.
     */
    public FileWriter(String filename) {
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
        new File("output-data").mkdir();
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("output-data/" + filename))) {
            oos.writeObject(log);
        } catch (IOException ex) {
            Logger.getLogger(FileWriter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
