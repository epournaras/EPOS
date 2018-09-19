package treestructure.reorganizationstrategies;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import agent.ModifiableIeposAgent;
import agent.dataset.DatasetShuffler;
import data.DataType;

/**
 * Used to test whether convergence selected plans of one structure cause IEPOS
 * to be stuck in local minimum from which it cannot get out.
 * 
 * In iteration 0 it reads selected plan from a file, after that it decides on it's own,
 * tree structure is reorganized on convergence in the same way as before.
 * 
 * @author Jovan N.
 *
 * @param <V> data structure to operate on
 */
public class ReorganizationPredefined<V extends DataType<V>> extends ReorganizationConvergence<V> {
	
	public static final HashMap<Integer, List<Integer>> predefinedSelectedPlans = 
			new HashMap<Integer, List<Integer>>();
	
	public static void readPredefinedSelectedPlans(String filename) {
		try(Stream<String> stream = Files.lines(Paths.get(filename))) {
			
			ArrayList<String[]> thelist = stream.filter(line -> !line.startsWith("Iteration"))
			                          		 	.map(line -> line.split(","))
			                          		 	.collect(Collectors.toCollection(ArrayList<String[]>::new));
			
			String[] convergencePlans = thelist.get(thelist.size()-1);
			for(int i = 1; i < convergencePlans.length; i++) {
				ArrayList<Integer> plans = new ArrayList<Integer>();
				plans.add(Integer.parseInt(convergencePlans[i]));
				ReorganizationPredefined.predefinedSelectedPlans.put(i-1, plans);
			}
			
		} catch(Exception e) {
			Logger.getLogger(DatasetShuffler.class.getName()).log(Level.SEVERE, null, e);
		}
	}
	
	public ReorganizationPredefined(ModifiableIeposAgent agent) {
		super(agent);
	}

	@Override
	public void selectPlan() {
		
		if(this.agent.getIteration() == 0) {
			int selected = ReorganizationPredefined.predefinedSelectedPlans.get(this.agent.getPeer().getIndexNumber()).get(0);
			this.agent.setSelectedPlan(selected);
		} else if(this.shouldChooseSelectedPlan()) {
			int selected = this.agent.getPlanSelector().selectPlan(this.agent);
			this.agent.setNumComputed(this.agent.getNumComputed() + this.agent.getPlanSelector().getNumComputations(this.agent));
	        this.agent.setSelectedPlan(selected);	        
			this.log(Level.FINER, "ModifiableIeposAgent:: preliminary selected plan equal to previous: " + 
						(this.agent.getSelectedPlanID() == this.agent.getPrevSelectedPlanID())); 	        
		} else {
			if(this.planToStartWithID != -1) {
				//System.out.println("USING PRE-SAVED SELECTED PLAN");
				this.agent.setSelectedPlan(this.planToStartWithID);
			}			
		}	
	}
	
	@Override
	boolean shouldChooseSelectedPlan() {
		return !this.agent.isIterationAfterReorganization();
	}

}
