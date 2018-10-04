/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package agent.logging;

import agent.Agent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import protopeer.measurement.LogReplayer;
import protopeer.measurement.MeasurementLog;

/**
 * Provides logging capabilities for a network of agents. Supports in-memory as
 * well as out-of-memory logging.
 *
 * @author Peter
 */
public class LoggingProvider<A extends Agent> {

    private MeasurementLog log;
    private final List<AgentLogger<? super A>> loggers = new ArrayList<>();

    // directory where the logs are stored in case of out-of-memory logging
    private final String outputDir;

    // links to the agent providers for in-memory logging
    private final Map<Integer, AgentLoggingProvider<? super A>> agentProviders = new HashMap<>();

    /**
     * Creates a LoggingProvider object.
     */
    public LoggingProvider() {
        this(null);
    }

    /**
     * Creates a LoggingProvider object that writes the logs to the given output
     * directory
     *
     * @param outputDir directory path specifying where the logs are stored;
     * null means the logs are in-memory
     */
    public LoggingProvider(String outputDir) {
        this.outputDir = outputDir;

        if (isInMemory()) {
            log = new MeasurementLog();
        } else {
            log = null;
            File dir = new File(outputDir);
            util.Helper.clearDirectory(dir);
            dir.mkdir();
        }
    }

    public void add(AgentLogger<? super A> logger) {
        loggers.add(logger);
    }

    /**
     * Returns the AgentLoggingProvider for the agent with the given agent id.
     *
     * @param agentId the id of the agent
     * @return the AgentLoggingProvider for the specified agent
     */
    public AgentLoggingProvider getAgentLoggingProvider(int agentId, int run) {
        AgentLoggingProvider<? super A> agentProvider;
        if (isInMemory()) {
            if (agentProviders.containsKey(agentId)) {
                MeasurementLog prevLog = agentProviders.get(agentId).getInMemoryLog();
                if (prevLog != null) {
                    log.mergeWith(prevLog);
                }
            }
            agentProvider = new AgentLoggingProvider<>(loggers, run, null);
            agentProviders.put(agentId, agentProvider);
        } else {
            agentProvider = new AgentLoggingProvider<>(loggers, run, outputDir + "/" + run + "_" + agentId);
        }
        return agentProvider;
    }
    
    public List<AgentLogger<? super A>> getLoggers() {
    	return this.loggers;
    }

    /**
     * Prints the results for each logger.
     */
    public void print() {
        mergeLogs();
        for (AgentLogger<? super A> logger : loggers) {
            logger.print(log);
        }
    }

    private boolean isInMemory() {
        return outputDir == null;
    }

    private void mergeLogs() {
        if (isInMemory()) {
            log = getExperiment();
        } else {
            log = readExperiment(new File(outputDir));
        }
    }

    private MeasurementLog getExperiment() {
        for (AgentLoggingProvider<? super A> agentProvider : agentProviders.values()) {
            MeasurementLog agentLog = agentProvider.getInMemoryLog();
            if (agentLog != null) {
                log.mergeWith(agentProvider.getInMemoryLog());
            }
        }
        agentProviders.clear();
        return log;
    }

    private MeasurementLog readExperiment(File experimentDir) {
        MeasurementLog log = new MeasurementLog();
        
        
        LogReplayer replayer = new LogReplayer();
        for (File file : experimentDir.listFiles()) {
            try {
                System.out.println("yolo " + file.getPath());
                MeasurementLog l = replayer.loadLogFromFile(file.getPath());
                log.mergeWith(l);
            } catch (IOException | ClassNotFoundException ex) {
                Logger.getLogger(LoggingProvider.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return log;
    }
}
