/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package experiment;

import agent.logging.FileReader;
import agent.logging.GlobalCostLogger;
import agent.logging.CostViewer;
import agent.logging.GraphLogger;
import agent.logging.LocalCostLogger;
import agent.logging.LoggingProvider;
import agent.logging.TerminationLogger;

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
        loggingProvider.add(new LocalCostLogger());
        loggingProvider.add(new TerminationLogger());
        loggingProvider.add(new CostViewer());
        loggingProvider.add(new GraphLogger(GraphLogger.Type.Change));
        
        loggingProvider.print();
    }
}
