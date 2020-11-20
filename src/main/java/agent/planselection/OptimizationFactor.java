package agent.planselection;

/**
 * Plan selection function uses various constants, variables and functions.
 * These enumeration types are used only as keys to hashmap when passing all arguments
 * to optimization function
 * 
 * @author jovan
 *
 */
public enum OptimizationFactor {
	LAMBDA,
	GLOBAL_COST,
	LOCAL_COST,
	STD,
	MEAN,
	ALPHA,
	BETA,
	DISCOMFORT_SUM,
	DISCOMFORT_SUM_SQR,
	NUM_AGENTS
}
