package config;

import java.util.Properties;
import java.util.logging.Level;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import func.SimilarityCostFunction;
import func.goalsignals.GoalSignalsCollection;
import treestructure.reorganizationstrategies.ReorganizationPredefined;
import treestructure.reorganizationstrategies.ReorganizationStrategy.ReorganizationStrategyType;

/**
 * Reads, parses and sets parameters from command line into configuration object
 * 
 * @author jovan
 *
 */
public class CommandLineArgumentReader {
	
	private static Options generateOptions() {
		Options opts = new Options();
		
		opts.addOption("h","show helptext");
		
		opts.addOption("dataset", true, "dataset name to be used");
		opts.addOption("numSim", true, "number of simulations");
		opts.addOption("iterations", true, "number of iterations");
		
		opts.addOption("lambda", true, "lambda preference level");
		opts.addOption("alpha", true, "weight of unfairness in plan selection process");
		opts.addOption("beta", true, "weight of local cost in plan selection process");
		opts.addOption("gamma", true, "weight of unfairness in preliminary subtree choices");
		opts.addOption("delta", true, "weight of local cost in preliminary subtree choices");		
		
		opts.addOption("numAgents", true, "number of participating agents");
		opts.addOption("numPlans", true, "number of possible plans per agent");
		opts.addOption("planDim", true, "dimension of every possible plan of every agent");
		opts.addOption("nodeDegree", true, "number of children of each inner node");
		
		opts.addOption("permOffset", true, "structure permutation offset");
		opts.addOption("permID", true, "structure permutation id");
		
		opts.addOption("reorganizationSeed", true, "seed to be used in Random generator the shuffling is based on");
		
		opts.addOption("enableNEVERstrategy", false, "enabling reorganization strategy NEVER");
		opts.addOption("enablePERIODICALLYstrategy", true, "enabling reorganization strategy PERIODICALLY with period");
		opts.addOption("enableCONVERGENCEstrategy", true, "enabling reorganization strategy ON_CONVERGENCE with offset from reorganization to memorize selected plan");
		opts.addOption("enablePREDEFINEDstrategy", false, "enabling starting IEPOS with predefined selected plans.");
		opts.addOption("enableCONVERGENCESHORTstrategy", true, "enabling reorganization strategy based on CONVERGENCE_SHORT strategy with tolerance level in [0, 1]");
		
		opts.addOption("readInitialStructure", true, "initial structure is given as permutation ID number. Value >= 1 passed here says which row to read from a fixed file datasets/initial-tree-<dataset>.csv");
		opts.addOption("sortingOrder", true, "ascending/asc/a or descending/desc/d, case INsensitive");
		opts.addOption("goalSignalType", true, "Type of the goal signal, paired with Similarity Global Cost Function. Ignored if global cost function is not Similarity Cost Function.");
		
		opts.addOption("logLevel", true, "log level: SEVERE, ALL, INFO, WARNING, FINE, FINER, FINEST");
		
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
		
		/*
		 * -dataset energy -numSim 4 -iterations 50 -lambda 7 -numAgents 888 -numPlans 33 -planDim 12 -nodeDegree 5
		 * -permOffset 11 -permID 22 -enableNEVERstrategy
		 * -enablePERIODICALLYstrategy 44
		 * -enableCONVERGENCEstrategy 55
		 * -logLevel FINEST
		 */
		
		if (argMap.get("dataset") != null) {
			String dataset = (String) argMap.get("dataset");
			for(int i = 0; i < Configuration.datasets.length; i++) {
				if(Configuration.datasets[i].getDatasetName().equalsIgnoreCase(dataset)) {
					Configuration.selectedDataset = Configuration.datasets[i];
					Configuration.dataset = Configuration.selectedDataset.getDatasetName();
					Configuration.numDimensions	= Configuration.selectedDataset.getDimensionality();
					Configuration.numAgents = Configuration.selectedDataset.getTotalNumAgentsAvailable();
					Configuration.numPlans = Configuration.selectedDataset.getNumPlansAvailable();
					
					break;
				}
			}
		}
		if (argMap.get("numSim") != null) {
			Configuration.numSimulations = Integer.parseInt((String) argMap.get("numSim"));
		}
		if (argMap.get("iterations") != null) {
			Configuration.numIterations = Integer.parseInt((String) argMap.get("iterations"));
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
		if (argMap.get("gamma") != null) {
			config.gamma = Double.parseDouble((String) argMap.get("gamma"));
		}
		if (argMap.get("delta") != null) {
			config.delta = Double.parseDouble((String) argMap.get("delta"));
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
		if (argMap.get("nodeDegree") != null) {
			Configuration.numChildren = Integer.parseInt((String) argMap.get("nodeDegree"));
		}
		if (argMap.get("permOffset") != null) {
			Configuration.permutationOffset = Integer.parseInt((String) argMap.get("permOffset"));
		}
		if (argMap.get("permID") != null) {
			Configuration.permutationID = Integer.parseInt((String) argMap.get("permID"));
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
		if (argMap.get("enablePREDEFINEDstrategy") != null) {
			config.reorganizationType = ReorganizationStrategyType.PREDEFINED;
			ReorganizationPredefined.readPredefinedSelectedPlans(Configuration.selectedPlanFilename);
		}
		if (argMap.get("enableCONVERGENCESHORTstrategy") != null) {
			config.reorganizationType = ReorganizationStrategyType.CONVERGENCE_SHORT;
			config.convergenceTolerance = Double.parseDouble((String) argMap.get("enableCONVERGENCESHORTstrategy"));
		}
		if (argMap.get("readInitialStructure") != null) {
			int row = Integer.parseInt((String) argMap.get("readInitialStructure"));
			Configuration.chosenMetric = Configuration.tags[row-1];
		}
		if (argMap.get("sortingOrder") != null) {
			String sortingOrder = (String) argMap.get("sortingOrder");
			if (argMap.get("dataset") != null) {
				Configuration.dataset = (String) argMap.get("dataset");
			}
			if(sortingOrder.equalsIgnoreCase("asc") ||
			   sortingOrder.equalsIgnoreCase("ascending") ||
			   sortingOrder.equalsIgnoreCase("a")) {
				Configuration.initialSortingOrder = "ascending";	
				Configuration.permutationFile = Configuration.initialStructureBaseFilename + Configuration.dataset +
                        Configuration.initialStructureSuffix + Configuration.initialSortingOrder + ".csv";
			} else if(sortingOrder.equalsIgnoreCase("desc") ||
					  sortingOrder.equalsIgnoreCase("descending") ||
					  sortingOrder.equalsIgnoreCase("d")) {
						Configuration.initialSortingOrder = "descending";
						Configuration.permutationFile = Configuration.initialStructureBaseFilename + Configuration.dataset +
		                        Configuration.initialStructureSuffix + Configuration.initialSortingOrder + ".csv";
			}
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
				if(Configuration.goalSignalFilename.startsWith("MON") || 
				   Configuration.goalSignalFilename.startsWith("TUE") || 
				   Configuration.goalSignalFilename.startsWith("WED") ||
				   Configuration.goalSignalFilename.startsWith("THU") ||
				   Configuration.goalSignalFilename.startsWith("FRI") ||
				   Configuration.goalSignalFilename.startsWith("SAT") || 
				   Configuration.goalSignalFilename.startsWith("SUN") ||
				   Configuration.goalSignalFilename.startsWith("weekly")) {
					Configuration.goalSignalSupplier = GoalSignalsCollection.fromOnelinerFile;
				} else {
					Configuration.goalSignalSupplier = GoalSignalsCollection.fromFile;
				}				
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
