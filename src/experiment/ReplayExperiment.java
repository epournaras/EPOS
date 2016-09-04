/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package experiment;

import agent.logging.FileReader;
import agent.logging.GlobalCostLogger;
import agent.logging.JFreeChartLogger;
import agent.logging.LoggingProvider;
import agent.logging.TerminationLogger;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import protopeer.measurement.LogReplayer;
import protopeer.measurement.MeasurementLog;

/**
 *
 * @author Peter
 */
public class ReplayExperiment {

    public static void main(String[] args) {
        String filename = "simple.log";

        LoggingProvider loggingProvider = new LoggingProvider();
        loggingProvider.add(new FileReader(filename));
        loggingProvider.add(new GlobalCostLogger());
        loggingProvider.add(new TerminationLogger());
        loggingProvider.add(new JFreeChartLogger());
        
        loggingProvider.print();
    }
}
