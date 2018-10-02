package config;

import java.util.Properties;
import java.util.logging.Level;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import agent.dataset.DatasetDescriptor;
import data.Vector;
import func.CrossCorrelationCostFunction;
import func.IndexCostFunction;
import func.PlanDiscomfortFunction;
import func.PlanPreferenceFunction;
import func.RMSECostFunction;
import func.RSSCostFunction;
import func.VarCostFunction;
import func.goalsignals.GoalSignalsCollection;
import treestructure.reorganizationstrategies.ReorganizationStrategy.ReorganizationStrategyType;

/**
 * Reads, parses and sets parameters from command line into configuration object
 * 
 * @author Jovan N.
 *
 */
public class CommandLineArgumentReader {
	
	/*
	 * -dataset gaussian -numSim 4 -numIterations 50 -alpha 0.2 -beta 0.3 -numAgents 888 -numPlans 33 -planDim 12 -numChildren 5
	 * -shuffle 11 -reorganizationSeed 2 -shuffleFile "permutation.csv" -enableNEVERstrategy -goalSignalType 19 -setGlobalCostFunc XCORR
	 * -setScaling MIN-MAX -setLocalCostFunc INDEX
	 * -logLevel FINEST
	 */
	
	private static Options generateOptions() {
		Options opts = new Options();
		
		opts.addOption("h","show helptext");
		
		opts.addOption("dataset",		true, "Dataset name to be used. This is the name of the directory located inside datasets/ directory thta contains all data. No default value, must be set.");
		opts.addOption("numSim", 		true, "Number of simulations. Default is 1.");
		opts.addOption("numIterations",	true, "Number of iterations. Default is 40.");
		
		opts.addOption("lambda", 		true, "Lambda preference level. Lambda > 0. Used only for old I-EPOS. Default is 0.");
		opts.addOption("alpha", 		true, "Weight of unfairness in multi-objective function. 0<= Alpha <= 1. Default is 0.");
		opts.addOption("beta", 			true, "Weight of local cost in multi-objective function. 0<= Beta <= 1. Default is 0.");	
		
		opts.addOption("numAgents", 	true, "Number of participating agents. Default is 100.");
		opts.addOption("numPlans", 		true, "The maximum number of possible plans per agent. Lower than this is possible. If more exist in the file, only first numPlan rows are read. Default is 16.");
		opts.addOption("planDim", 		true, "Dimension of every possible plan of every agent. Must be equal across the agents and must correspond to the dataset. Default to -1 and must be set!");
		opts.addOption("numChildren", 	true, "Number of children of each inner node. Default is 2.");
	
		opts.addOption("shuffle", 				true, "The number of the shuffles to make before assigning agents to the tree hierarchy. Default is 0.");
		opts.addOption("shuffleFile", 			true, "The path to a file containing already shuffled agents in one column, no header. Default is null.");
		
		opts.addOption("reorganizationSeed", 				true, "The seed to be used in Random generator the shuffling is based on. Default is 0.");
		opts.addOption("enableNEVERstrategy", 				false,	"Enabling reorganization strategy NEVER. Works only for ModifiableIEPOSAgent. This is default.");
		opts.addOption("enablePERIODICALLYstrategy", 		true, 	"Enabling reorganization strategy PERIODICALLY with indicated period. Default period is 3, but default strategy is to NEVER reorganize.");
		opts.addOption("enableCONVERGENCEstrategy", 		true, 	"Enabling reorganization strategy ON_CONVERGENCE with indicated memorization offset. Default is 5, but default strategy is to NEVER reorganize.");
		opts.addOption("enableGLOBALCOSTREDUCTIONstrategy", true, 	"Enabling reorganization strategy based on GLOBAL_COST_REDUCTION strategy with indicated tolerance level. 0 <= Tolerane Level <= 1. Default is 0.5, but default strategy is to NEVER reorganize.");
		
		opts.addOption("goalSignalType", 		true, 	"The reference signal, paired with RSS, XCORR or RMSE global cost function, otherwise ignored. Options are: either an integer from [1, 19] to use predetermined signals, or path to a file with the signal in one column. The length of the signal from the file must correspond planDim option. Default is signal type 1.");
		opts.addOption("setGlobalCostFunc", 	true, 	"The global cost function to be used. Options are (case-sensitive): VAR for variance function, XCORR for negative cross-correlation, RSS for residual sum of squares and RMSE for residual mean square error. Default is VAR. XCORR uses standard normalization by definition, RMSE has its own way of scaling and RSS uses standard normalization by default.");
		opts.addOption("setScaling", 			true, 	"The scaling technique to be used with RSS function, ignored otherwise. Options are (case-sensitive): STD for standard normalization, MIN-MAX for min-max scaling and UNIT-lENGTH for unit-length scaling. Default is STD.");
		opts.addOption("setLocalCostFunc", 		true, 	"The local cost function. Options are (case-sensitive): COST for cost plan score, PREF for preference plan score, which is converted to COST by 1 - PREF, INDEX for plan indicies to be used as costs. Default is COST.");
		
		opts.addOption("logLevel", 				true, 	"The log level. Options are (case-sensitive): SEVERE, ALL, INFO, WARNING, FINE, FINER, FINEST");
		
		return opts;
	}
	
	private static CommandLine parseCommandLine(String[] args) {	
		Options opts = CommandLineArgumentReader.generateOptions();
		CommandLineParser clp = new DefaultParser();
		try {
			CommandLine line = clp.parse(opts, args);
			if(line.hasOption("h")) {
				new HelpFormatter().printHelp( "java IEPOS [OPTIONS]", opts );
				System.exit(1);
			}
			return line;
		} catch (Exception e) {
			System.err.println("Could not parse command line: " + e.getMessage());
			System.exit(1);
		}
		return null;
	}
	
	public static void setConfiguration(Configuration config, String[] args) {
		Properties argMap = new Properties();
		CommandLine line = CommandLineArgumentReader.parseCommandLine(args);
		
		for(Option o : line.getOptions()) {
			if(o.getValue() != null) {
				argMap.put(o.getOpt(), o.getValue());
			} else {
				argMap.put(o.getOpt(), true);
			}			
		}
		
		
		if (argMap.get("numSim") != null) {
			Configuration.numSimulations = Integer.parseInt((String) argMap.get("numSim"));
		}
		if (argMap.get("numIterations") != null) {
			Configuration.numIterations = Integer.parseInt((String) argMap.get("numIterations"));
		}
		if (argMap.get("numAgents") != null) {
			Configuration.numAgents = Integer.parseInt((String) argMap.get("numAgents"));
		}
		if (argMap.get("numPlans") != null) {
			Configuration.numPlans = Integer.parseInt((String) argMap.get("numPlans"));
		}
		if (argMap.get("planDim") != null) {
			Configuration.numDimensions = Integer.parseInt((String) argMap.get("planDim"));
		}
		if (argMap.get("dataset") != null) {
			String dataset = (String) argMap.get("dataset");
			Configuration.dataset = dataset;
			Configuration.selectedDataset = new DatasetDescriptor(dataset, Configuration.numDimensions, Configuration.numAgents, Configuration.numPlans);
		}		
		if (argMap.get("lambda") != null) {
			config.lambda = Double.parseDouble((String) argMap.get("lambda"));
		}
		if (argMap.get("alpha") != null) {
			config.alpha = Double.parseDouble((String) argMap.get("alpha"));
		}
		if (argMap.get("beta") != null) {
			config.beta = Double.parseDouble((String) argMap.get("beta"));
		}		
		if (argMap.get("numChildren") != null) {
			Configuration.numChildren = Integer.parseInt((String) argMap.get("numChildren"));
		}
		if(argMap.get("shuffleFile") != null) {
			Configuration.permutationFile = (String) argMap.get("shuffleFile");
			Configuration.mapping = config.readMapping.apply(config);
		} else {
			Configuration.mapping = config.generateDefaultMapping.apply(config);
		}
		if (argMap.get("shuffle") != null) {
			Configuration.permutationID = Integer.parseInt((String) argMap.get("shuffle"));
			Configuration.mapping = config.generateShuffledMapping.apply(config);
		}		
		if (argMap.get("enableNEVERstrategy") != null) {
			config.reorganizationType = ReorganizationStrategyType.NEVER;
		}
		if (argMap.get("enablePERIODICALLYstrategy") != null) {
			config.reorganizationType = ReorganizationStrategyType.PERIODICALLY;
			config.reorganizationPeriod = Integer.parseInt((String) argMap.get("enablePERIODICALLYstrategy"));
		}
		if (argMap.get("enableCONVERGENCEstrategy") != null) {
			config.reorganizationType = ReorganizationStrategyType.ON_CONVERGENCE;
			config.reorganizationOffset = Integer.parseInt((String) argMap.get("enableCONVERGENCEstrategy"));
		}
		if (argMap.get("enableGLOBALCOSTREDUCTIONstrategy") != null) {
			config.reorganizationType = ReorganizationStrategyType.GLOBAL_COST_REDUCTION;
			config.convergenceTolerance = Double.parseDouble((String) argMap.get("enableGLOBALCOSTREDUCTIONstrategy"));
		}		
		if (argMap.get("reorganizationSeed") != null) {
			config.reorganizationSeed = Long.parseLong((String) argMap.get("reorganizationSeed"));
		}
		if (argMap.get("goalSignalType") != null) {
			try {
				int type = Integer.parseInt((String) argMap.get("goalSignalType"));
				switch(type) {
				case 1:
					Configuration.goalSignalSupplier = GoalSignalsCollection.sine_a100_o0;
					break;
				case 2:
					Configuration.goalSignalSupplier = GoalSignalsCollection.sine_a100_o500;
					break;
				case 3:
					Configuration.goalSignalSupplier = GoalSignalsCollection.sine_a200_o1000;
					break;
				case 4:
					Configuration.goalSignalSupplier = GoalSignalsCollection.singleImpulse_a100;
					break;
				case 5:
					Configuration.goalSignalSupplier = GoalSignalsCollection.singleImpulse_a500;
					break;
				case 6:
					Configuration.goalSignalSupplier = GoalSignalsCollection.singleImpulse_a1000;
					break;
				case 7:
					Configuration.goalSignalSupplier = GoalSignalsCollection.singleImpulse_a100_b50;
					break;
				case 8:
					Configuration.goalSignalSupplier = GoalSignalsCollection.singleImpulse_a500_b250;
					break;
				case 9:
					Configuration.goalSignalSupplier = GoalSignalsCollection.singleImpulse_a1000_b500;
					break;
				case 10:
					Configuration.goalSignalSupplier = GoalSignalsCollection.singleImpulse_a2000_b1000;
					break;
				case 11:
					Configuration.goalSignalSupplier = GoalSignalsCollection.monotonousDecrease_400_50;
					break;
				case 12:
					Configuration.goalSignalSupplier = GoalSignalsCollection.monotonousDecrease_800_100;
					break;
				case 13:
					Configuration.goalSignalSupplier = GoalSignalsCollection.monotonousDecrease_1600_200;
					break;	
				case 14:
					Configuration.goalSignalSupplier = GoalSignalsCollection.monotonousIncrease_m10_10;
					break;
				case 15:
					Configuration.goalSignalSupplier = GoalSignalsCollection.monotonousIncrease_m100_100;
					break;
				case 16:
					Configuration.goalSignalSupplier = GoalSignalsCollection.monotonousIncrease_150_250;
					break;
				case 17:
					Configuration.goalSignalSupplier = GoalSignalsCollection.monotonousIncrease_1300_1500;
					break;
				case 18:
					Configuration.goalSignalSupplier = GoalSignalsCollection.camelImpulse_50_100;
					break;
				case 19:
					Configuration.goalSignalSupplier = GoalSignalsCollection.gaussian_mixture_impulse;
					System.out.println("Chosen goal signal: " + 19);
					break;
				case 20:
					Configuration.goalSignalSupplier = GoalSignalsCollection.frequencyGoal;
					break;
				default:
					Configuration.goalSignalSupplier = null;
					break;
				}
			} catch(NumberFormatException e) {
				Configuration.goalSignalFilename = (String) argMap.get("goalSignalType");
				Configuration.goalSignalSupplier = GoalSignalsCollection.fromFile;				
			}			
		}
		if(argMap.get("setScaling") != null) {
			String func = (String) argMap.get("setScaling");
			switch (func) {
			case "STD":
				Configuration.normalizer = Vector.standard_normalization;
				break;
			case "MIN-MAX":
				Configuration.normalizer = Vector.min_max_normalization;
				break;
			case "UNIT-LENGTH":
				Configuration.normalizer = Vector.unit_length_normalization;
				break;
			default:
				break;
			}
		}
		if(argMap.get("setGlobalCostFunc") != null) {
			String func = (String) argMap.get("setGlobalCostFunc");
			switch (func) {
			case "VAR":
				Configuration.globalCostFunc = new VarCostFunction();
				break;
			case "RSS":
				Configuration.globalCostFunc = new RSSCostFunction();
				RSSCostFunction.populateGoalSignal();
				break;
			case "RMSE":
				Configuration.globalCostFunc = new RMSECostFunction();
				RMSECostFunction.populateGoalSignal();
				break;
			case "XCORR":
				Configuration.globalCostFunc = new CrossCorrelationCostFunction();
				CrossCorrelationCostFunction.populateGoalSignal();
				break;
			default:
				break;
			}
		}
		if(argMap.get("setLocalCostFunc") != null) {
			String func = (String) argMap.get("setLocalCostFunc");
			switch (func) {
			case "COST":
				Configuration.localCostFunc = new PlanDiscomfortFunction();
				break;
			case "PREF":
				Configuration.localCostFunc = new PlanPreferenceFunction();
				break;
			case "INDEX":
				Configuration.localCostFunc = new IndexCostFunction();
				break;
			default:
				break;
			}
		}				
		if (argMap.get("logLevel") != null) {
			String level = (String) argMap.get("logLevel");
			switch(level) {
			case "SEVERE":
				Configuration.loggingLevel = Level.SEVERE;
				break;
			case "ALL":
				Configuration.loggingLevel = Level.ALL;
				break;
			case "INFO":
				Configuration.loggingLevel = Level.INFO;
				break;
			case "WARNING":
				Configuration.loggingLevel = Level.WARNING;
				break;
			case "FINE":
				Configuration.loggingLevel = Level.FINE;
				break;
			case "FINER":
				Configuration.loggingLevel = Level.FINER;
				break;
			case "FINEST":
				Configuration.loggingLevel = Level.FINEST;
				break;
			default:
				Configuration.loggingLevel = Level.SEVERE;
				break;
			}
		}		
	}

}

//for(int i = 0; i < Configuration.datasets.length; i++) {
//	if(Configuration.datasets[i].getDatasetName().equalsIgnoreCase(dataset)) {
//		Configuration.selectedDataset = Configuration.datasets[i];
//		Configuration.dataset = Configuration.selectedDataset.getDatasetName();
//		Configuration.numDimensions	= Configuration.selectedDataset.getDimensionality();
//		Configuration.numAgents = Configuration.selectedDataset.getTotalNumAgentsAvailable();
//		Configuration.numPlans = Configuration.selectedDataset.getNumPlansAvailable();
//		
//		break;
//	}
//}
//
//if (argMap.get("sortingOrder") != null) {
//	String sortingOrder = (String) argMap.get("sortingOrder");
//	if (argMap.get("dataset") != null) {
//		Configuration.dataset = (String) argMap.get("dataset");
//	}
//	if(sortingOrder.equalsIgnoreCase("asc") ||
//	   sortingOrder.equalsIgnoreCase("ascending") ||
//	   sortingOrder.equalsIgnoreCase("a")) {
//		Configuration.initialSortingOrder = "ascending";	
//		Configuration.permutationFile = Configuration.initialStructureBaseFilename + Configuration.dataset +
//                Configuration.initialStructureSuffix + Configuration.initialSortingOrder + ".csv";
//	} else if(sortingOrder.equalsIgnoreCase("desc") ||
//			  sortingOrder.equalsIgnoreCase("descending") ||
//			  sortingOrder.equalsIgnoreCase("d")) {
//				Configuration.initialSortingOrder = "descending";
//				Configuration.permutationFile = Configuration.initialStructureBaseFilename + Configuration.dataset +
//                        Configuration.initialStructureSuffix + Configuration.initialSortingOrder + ".csv";
//	}
//}
