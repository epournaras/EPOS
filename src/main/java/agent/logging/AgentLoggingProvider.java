/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package agent.logging;

import agent.Agent;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import protopeer.measurement.MeasurementFileDumper;
import protopeer.measurement.MeasurementLog;

/**
 * Provides logging capabilities to an agent. Supports in-memory as well as
 * out-of-memory logging.
 *
 * @author Peter
 * @param <A> the agent type this logger is used for
 */
public class AgentLoggingProvider<A extends Agent> {

    // loggers
    private final List<AgentLogger<? super A>> loggers = new ArrayList<>();

    // out-of-memory log
    private final MeasurementFileDumper measurementDumper;

    // in-memory
    private MeasurementLog inMemoryLog;

    /**
     * Creates an agent logging provider that provides the given logging
     * capabilities.
     *
     * @param loggers the loggers of this AgentLoggingProvider; the loggers are
     * cloned in this constructor
     * @param outputFile filename specifying where the log is stored; null means
     * the log is in-memory
     */
    public AgentLoggingProvider(List<AgentLogger<? super A>> loggers, int run, String outputFile) {
        if (loggers != null) {
            for (AgentLogger<? super A> logger : loggers) {
                AgentLogger<? super A> clone = logger.clone();
                clone.setRun(run);
                this.loggers.add(clone);
            }
        }
        if (outputFile != null) {
            measurementDumper = new MeasurementFileDumper(outputFile);
        } else {
            measurementDumper = null;
        }
    }

    protected MeasurementLog getInMemoryLog() {
        return inMemoryLog;
    }

    /**
     * Initializes the loggers.
     *
     * @param agent the agent that is be logged
     */
    public void init(Agent agent) {
        if (measurementDumper == null) {
            inMemoryLog = agent.getPeer().getMeasurementLogger().getMeasurementLog();
        }
        for (AgentLogger logger : loggers) {
            logger.init(agent);
        }
    }

    /**
     * Logs information of the agent.
     *
     * @param log the log where measurements are written to
     * @param epochNumber the epoch used to log measurements
     * @param agent the agent that is logged
     */
    public void log(MeasurementLog log, int epochNumber, Agent agent) {
        if (epochNumber >= 2) {
            for (AgentLogger logger : loggers) {
                logger.log(log, epochNumber, agent);
            }

            boolean dataAvailable = true;
            try {
                log.getSubLog(epochNumber, epochNumber + 1).getMinEpochNumber();
            } catch (NoSuchElementException e) {
                dataAvailable = false;
            }

            if (dataAvailable && measurementDumper != null) {
                measurementDumper.measurementEpochEnded(log, epochNumber);
            }
            if (measurementDumper != null) {
                log.shrink(epochNumber, epochNumber + 1);
            }
        } else {
            log.shrink(epochNumber, epochNumber + 1);
        }
    }
}
