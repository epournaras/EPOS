package treestructure.reorganizationstrategies;


/**
 * Interface that provides functionality of handling all reorganization operations based
 * on reorganization strategy
 * 
 * @author jovan
 *
 */
public interface ReorganizationStrategy {
	
	public enum ReorganizationStrategyType {
		PERIODICALLY,
		ON_CONVERGENCE,
		PREDEFINED,
		CONVERGENCE_SHORT,
		NEVER
	}
	
	public void iterationAtRootEndedCallback();
	
	public void selectPlan();
	
	public void screenshotAfterDOWNphase();
	
	public void prepareForReorganization();

}
