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
 * Shuffles vertex-2-agent assignment. 
 * Permutations are reproducible by always giving the same seed.
 * Permutation in n-th run is obtained by n times shuffling the same list,
 * with same random generator.
 * 
 * @author jovan
 *
 */
public class DatasetShuffler {
	
	public static int row = -1;
	
	/**
	 * Returns mapping vertex-2-agent. Keys are identifications of vertices and values
	 * are agent identifications mapped to that vertex.
	 * 
	 * @return Map that maps vertex id to agent id
	 */
	public static Map<Integer, Integer> getMapping(Configuration config) {
		
		HashMap<Integer, Integer> mapping	= 	new HashMap<Integer, Integer>();		
		ArrayList<Integer> agents			= 	new ArrayList<Integer>();
		Random random						=	new Random(config.permutationSeed);
		
		long timeBefore = System.currentTimeMillis();
		
		IntStream.range(0, Configuration.numAgents)			.forEach(i -> agents.add(i));
		IntStream.range(0, Configuration.permutationOffset)	.forEach(i -> Collections.shuffle(agents, random));
		IntStream.range(0, Configuration.permutationID)		.forEach(i -> Collections.shuffle(agents, random));
		IntStream.range(0, agents.size())					.forEach(i -> mapping.put(i, agents.get(i)));
		
		long timeAfter = System.currentTimeMillis();
		
		System.out.println("Permuting time: " + ((timeAfter-timeBefore)/1000) + " seconds.");
		
		System.out.println(mapping.toString());
		
		return mapping;
	}
	
	public static Map<Integer, Integer> getMappingForRepetitiveExperiments(Configuration config) {
		
		HashMap<Integer, Integer> mapping	= 	new HashMap<Integer, Integer>();		
		ArrayList<Integer> agents			= 	new ArrayList<Integer>();
		Random random						=	new Random(config.permutationSeed);
		
		long timeBefore = System.currentTimeMillis();
		
		IntStream.range(0, Configuration.numAgents).forEach(i -> agents.add(i));
		Collections.shuffle(agents, random);
		IntStream.range(0, agents.size()).forEach(i -> mapping.put(i, agents.get(i)));		
		
		long timeAfter = System.currentTimeMillis();
		
		System.out.println("Permuting time: " + ((timeAfter-timeBefore)/1000) + " seconds.");
		
		return mapping;
	}
	
	public static Map<Integer, Integer> getMappingFromFile() {
		HashMap<Integer, Integer> mapping = new HashMap<Integer, Integer>();
		
		try(Stream<String> stream = Files.lines(Paths.get(Configuration.permutationFile))) {
			
			ArrayList<String[]> thelist = stream.filter(line -> line.startsWith(Configuration.chosenMetric))
			                          		 	.map(line -> line.substring(Configuration.chosenMetric.length()+1, line.length()))
			                          		 	.map(line -> line.split(","))
			                          		 	.collect(Collectors.toCollection(ArrayList<String[]>::new));
			
			IntStream.range(0, thelist.get(0).length)
			         .forEach(idx -> {
			        	 int pid = Integer.parseInt(thelist.get(0)[idx]);
						 mapping.put(idx, pid);
			         });
			
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
