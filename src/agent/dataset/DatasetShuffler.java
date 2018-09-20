package agent.dataset;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import config.Configuration;


/**
 * Provides various agent-2-vertex mappings.
 * 
 * @author Jovan N.
 *
 */
public class DatasetShuffler {
	
	public static int row = -1;
	
	/**
	 * Default mapping is 0->0, 1->1, 2->2, ..
	 * @param config
	 * @return
	 */
	public static Map<Integer, Integer> getDefaultMapping(Configuration config) {
		
		HashMap<Integer, Integer> mapping	= 	new HashMap<Integer, Integer>();		
		ArrayList<Integer> agents			= 	new ArrayList<Integer>();
		
		IntStream.range(0, Configuration.numAgents)		.forEach(i -> mapping.put(i, i));	
		
		return mapping;
	}
	
	/**
	 * Returns mapping vertex-2-agent. Keys are identifications of vertices and values
	 * are agent identifications. Takes into account initial permutation of agents,
	 * if any is specified.
	 * 
	 * @return Map that maps vertex id to agent id
	 */
	public static Map<Integer, Integer> getMappingByShuffling(Configuration config) {
				
		ArrayList<Integer> agents			= 	new ArrayList<Integer>();
		Random random						=	new Random(config.permutationSeed);
		
		long timeBefore = System.currentTimeMillis();
		
		IntStream.range(0, Configuration.numAgents)			.forEach(i -> agents.add(Configuration.mapping.get(i)));
		IntStream.range(0, Configuration.permutationID)		.forEach(i -> Collections.shuffle(agents, random));
		IntStream.range(0, agents.size())					.forEach(i -> Configuration.mapping.put(i, agents.get(i)));
		
		long timeAfter = System.currentTimeMillis();
		
		System.out.println("Permuting time: " + ((timeAfter-timeBefore)/1000) + " seconds.");		
		System.out.println(Configuration.mapping.toString());
		
		return Configuration.mapping;
	}
	
	public static Map<Integer, Integer> getMappingForRepetitiveExperiments(Configuration config) {
				
		ArrayList<Integer> agents			= 	new ArrayList<Integer>();
		Random random						=	new Random(config.permutationSeed);
		
		long timeBefore = System.currentTimeMillis();
		
		IntStream.range(0, Configuration.numAgents)		.forEach(i -> agents.add(Configuration.mapping.get(i)));
		Collections.shuffle(agents, random);
		IntStream.range(0, agents.size())				.forEach(i -> Configuration.mapping.put(i, agents.get(i)));		
		
		long timeAfter = System.currentTimeMillis();
		
		System.out.println("Permuting time: " + ((timeAfter-timeBefore)/1000) + " seconds.");
		
		return Configuration.mapping;
	}
	
//	public static Map<Integer, Integer> getMappingFromFile() {
//		HashMap<Integer, Integer> mapping = new HashMap<Integer, Integer>();
//		
//		try(Stream<String> stream = Files.lines(Paths.get(Configuration.permutationFile))) {
//			
//			ArrayList<String[]> thelist = stream.filter(line -> line.startsWith(Configuration.chosenMetric))
//			                          		 	.map(line -> line.substring(Configuration.chosenMetric.length()+1, line.length()))
//			                          		 	.map(line -> line.split(","))
//			                          		 	.collect(Collectors.toCollection(ArrayList<String[]>::new));
//			
//			IntStream.range(0, thelist.get(0).length)
//			         .forEach(idx -> {
//			        	 int pid = Integer.parseInt(thelist.get(0)[idx]);
//						 mapping.put(idx, pid);
//			         });
//			
//		} catch(Exception e) {
//			Logger.getLogger(DatasetShuffler.class.getName()).log(Level.SEVERE, null, e);
//		}
//		
//		System.out.println(mapping.toString());
//		
//		return mapping;
//	}
	
	/**
	 * Reads mapping from a one-column file, no header.
	 * @return
	 */
	public static Map<Integer, Integer> getMappingFromFile() {
		HashMap<Integer, Integer> mapping = new HashMap<Integer, Integer>();
		
		try(Stream<String> stream = Files.lines(Paths.get(Configuration.permutationFile))) {
			
			ArrayList<Integer> thelist = stream.map(line -> line.replace(',', ' ').trim())
												.map(line -> Integer.parseInt(line))
			                          		 	.collect(Collectors.toCollection(ArrayList<Integer>::new));
			
			IntStream.range(0, thelist.size())
			         .forEach(idx -> mapping.put(idx, thelist.get(idx)));
			
		} catch(Exception e) {
			Logger.getLogger(DatasetShuffler.class.getName()).log(Level.SEVERE, null, e);
		}
		
		System.out.println(mapping.toString());		
		return mapping;
	}
	
	public static void main(String[] args) {
		Map<Integer, Integer> mapping = DatasetShuffler.getMappingFromFile();
		System.out.println(mapping);
	}
	
	

}
