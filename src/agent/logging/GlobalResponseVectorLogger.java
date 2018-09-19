package agent.logging;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import agent.Agent;
import config.Configuration;
import data.DataType;
import data.Vector;
import func.PeriodicCostFunction;
import func.SimilarityCostFunction;
import protopeer.measurement.MeasurementLog;

/**
 * 
 * 
 * @author jovan
 *
 * @param <V>
 */
public class GlobalResponseVectorLogger<V extends DataType<V>> extends AgentLogger<Agent<V>> {
	
	private String 					filepath;
	
	
	/**
     * Outputs the global response to the specified file.
     *
     * @param filename the output file
     */
    public GlobalResponseVectorLogger(String filename) {
        this.filepath = filename;
    }

	@Override
	public void init(Agent<V> agent) { }

	@Override
	public void log(MeasurementLog log, int epoch, Agent<V> agent) {
		if (agent.isRepresentative()) {
            V globalResponse = agent.getGlobalResponse();
            Entry<V> e = new Entry<V>();
            e.iteration = agent.getIteration();
            e.globalResponse = globalResponse.cloneThis();
            log.log(epoch, e, 0.0);
        }		
	}

	@Override
	public void print(MeasurementLog log) {
		String outcome = this.extractResponses(log);
    	
        if (this.filepath == null) {
            System.out.print(outcome);
        } else {
            try (PrintWriter out = new PrintWriter(new BufferedWriter(new java.io.FileWriter(this.filepath, false)))) {   
                out.append(outcome);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(GlobalCostLogger.class.getName()).log(Level.SEVERE, null, ex);
            } catch(IOException e) {
            	Logger.getLogger(GlobalCostLogger.class.getName()).log(Level.SEVERE, null, e);
            }
        }
	}
	
	public String extractResponses(MeasurementLog log) {
		Set<Object> entries = log.getTagsOfType(Entry.class);
		
		Set<Object> sortedEntries = new TreeSet<>((x, y) -> Integer.compare(((GlobalResponseVectorLogger.Entry) x).iteration,
																			((GlobalResponseVectorLogger.Entry) y).iteration));
		sortedEntries.addAll(entries);
		
		StringBuilder sb = new StringBuilder();
		if(Configuration.goalSignalSupplier != null) {
			Vector globalSignal = Configuration.goalSignalSupplier.get();
			sb.append("-1,").append(globalSignal.toString()).append(System.lineSeparator());
		}
		
//		Vector globalSignal = Vector.convertWreal(
//			Vector.inverseFourierTransform(
//					Configuration.goalSignalSupplier.get().convert2complex()
//			)
//		);		
		
		sortedEntries.forEach(obj -> {
			GlobalResponseVectorLogger.Entry entry = (GlobalResponseVectorLogger.Entry) obj;
			sb.append(entry.iteration).append(",").append(entry.globalResponse.toString()).append(System.lineSeparator());
		});
		
		return sb.toString();
	}
	
	private class Entry<V> {

        public int iteration;
        public V globalResponse;

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Entry other = (Entry) obj;
            if (this.iteration != other.iteration) {
                return false;
            }
            return true;
        }
    }

}
