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
import protopeer.measurement.MeasurementFileDumper;
import protopeer.measurement.MeasurementLog;
import util.Util;

/**
 * Provides logging capabilities for a network of agents. Supports in-memory as
 * well as out-of-memory logging.
 *
 * @author Peter
 */
public class LoggingProvider<A extends Agent> {

    private MeasurementLog log;
    private List<MeasurementLog> logs;
    private final List<AgentLogger<? super A>> loggers = new ArrayList<>();
    private String experimentLabel = "";

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
            Util.clearDirectory(dir);
            dir.mkdir();
        }
    }

    public void add(AgentLogger<? super A> logger) {
        loggers.add(logger);
    }

    /**
     * Initializes the logger for the following experiment. The given log is
     * merged to the one managed by this LoggingProvider.
     *
     * @param experimentLabel label of the experiment
     * @param initLog the log that should be added
     */
    public void initExperiment(String experimentLabel, MeasurementLog initLog) {
        this.experimentLabel = "/" + experimentLabel;
        if (!isInMemory()) {
            new File(outputDir + this.experimentLabel).mkdir();
        }

        if (isInMemory()) {
            if (logs == null) {
                logs = new ArrayList<>();
            } else {
                logs.add(getExperiment());
                log = new MeasurementLog();
            }
            if (initLog != null) {
                log.mergeWith(initLog);
            }
        } else if (initLog != null) {
            MeasurementFileDumper logger = new MeasurementFileDumper(outputDir + experimentLabel + "/info" + System.currentTimeMillis());
            logger.measurementEpochEnded(initLog, initLog.getMaxEpochNumber() + 1);
        }
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
                log.mergeWith(agentProviders.get(agentId).getInMemoryLog());
            }
            agentProvider = new AgentLoggingProvider<>(loggers, run, null);
            agentProviders.put(agentId, agentProvider);
        } else {
            agentProvider = new AgentLoggingProvider<>(loggers, run, outputDir + experimentLabel + "/" + run + "_" + agentId);
        }
        return agentProvider;
    }

    /**
     * Prints the results for each logger.
     */
    public void print() {
        mergeLogs();
        for (MeasurementLog log : logs) {
            for (AgentLogger<? super A> logger : loggers) {
                logger.print(log);
            }
        }
    }

    private boolean isInMemory() {
        return outputDir == null;
    }

    private MeasurementLog mergeLogs() {
        if (logs == null) {
            logs = new ArrayList<>();
        }
        if (isInMemory()) {
            logs.add(getExperiment());
        } else {
            File directory = new File(outputDir + experimentLabel);
            if (!directory.listFiles()[0].isDirectory()) {
                logs.add(readExperiment(directory));
            } else {
                for (File experimentDir : directory.listFiles()) {
                    logs.add(readExperiment(experimentDir));
                }
            }
        }
        return log;
    }

    private MeasurementLog getExperiment() {
        for (AgentLoggingProvider<? super A> agentProvider : agentProviders.values()) {
            log.mergeWith(agentProvider.getInMemoryLog());
        }
        agentProviders.clear();
        return log;
    }

    private MeasurementLog readExperiment(File experimentDir) {
        MeasurementLog log = new MeasurementLog();

        LogReplayer replayer = new LogReplayer();
        for (File file : experimentDir.listFiles()) {
            try {
                MeasurementLog l = replayer.loadLogFromFile(file.getPath());
                log.mergeWith(l);
            } catch (IOException | ClassNotFoundException ex) {
                Logger.getLogger(LoggingProvider.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return log;
    }
}
