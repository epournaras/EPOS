package config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.netlib.util.Util;

import agent.dataset.Dataset;
import agent.dataset.DatasetDescriptor;
import agent.dataset.DatasetShuffler;
import agent.dataset.FileVectorDataset;
import agent.dataset.GaussianDataset;
import agent.planselection.PlanSelectionOptimizationFunction;
import agent.planselection.PlanSelectionOptimizationFunctionCollection;
import data.Vector;
import dsutil.generic.RankPriority;
import dsutil.protopeer.services.topology.trees.DescriptorType;
import dsutil.protopeer.services.topology.trees.TreeType;
import func.CrossCorrelationCostFunction;
import func.DifferentiableCostFunction;
import func.IndexCostFunction;
import func.PlanCostFunction;
import func.PlanDiscomfortFunction;
import func.PlanPreferenceFunction;
import func.RMSECostFunction;
import func.RSSCostFunction;
import func.VarCostFunction;
import func.goalsignals.GoalSignalsCollection;
import tree.BalanceType;
import treestructure.reorganizationstrategies.ReorganizationStrategy.ReorganizationStrategyType;
import util.Helper;

/**
 * Class that keeps all global parameters for IEPOS run.
 * 
 * @author Jovan N.
 */
public class Configuration {

	/*
	 * -dataset energy -numSim 4 -iterations 50 -lambda 7 -numAgents 888 -numPlans
	 * 33 -planDim 12 -nodeDegree 5 -permOffset 11 -permID 22 -enableNEVERstrategy
	 * -enablePERIODICALLYstrategy 44 -enableCONVERGENCEstrategy 55 -logLevel FINEST
	 * -readInitialStructure 3 -reorganizationSeed 81
	 */

	public static Logger log = Logger.getLogger(Configuration.class.getName());
	public static DatasetDescriptor[] datasets = null;
	public static DatasetDescriptor selectedDataset = null;
	public static String dataset = null;
	public static int planDim = -1;
	public static int numAgents = 100;
	public static int numPlans = 16;
	public static Map<Integer, Integer> mapping = null;

	public static RankPriority priority = RankPriority.HIGH_RANK;
	public static DescriptorType rank = DescriptorType.RANK;
	public static TreeType type = TreeType.SORTED_HtL;
	public static BalanceType balance = BalanceType.WEIGHT_BALANCED;

	public static int numSimulations = 1;
	public static int numIterations = 40;
	public static int numChildren = 2;

	public static double lambda = 0;
	public double alpha = 0;
	public double beta = 0;

	public static int permutationID = 0;
	public static String permutationFile = null;

	public static DifferentiableCostFunction<Vector> globalCostFunc = new VarCostFunction();
	public static PlanCostFunction localCostFunc = new IndexCostFunction();
	public static Supplier<Vector> goalSignalSupplier = GoalSignalsCollection.sine_a100_o0;
	public static UnaryOperator<Vector> normalizer = Vector.standard_normalization;
	public static PlanSelectionOptimizationFunction planOptimizationFunction = PlanSelectionOptimizationFunctionCollection.complexFunction1;

	////////////////////////////////////////////////////////////////////////////////////////////////////
	// SEEDS:
	public long reorganizationSeed = 0;
	public long permutationSeed = 0;
	public long simulationSeed = 0;
	public Random simulationRNG = new Random(this.simulationSeed);

	////////////////////////////////////////////////////////////////////////////////////////////////////
	// REGARDING REORGANIZATION:
	public int reorganizationPeriod = 3;
	public int memorizationOffset = 5;
	public ReorganizationStrategyType reorganizationStrategy = ReorganizationStrategyType.NEVER;
	public double convergenceTolerance = 0.5;

	///////////////////////////////////////////////////////////////////////////////////////////////////
	// LOGGING INSTRUMENTATION:
	public static Level loggingLevel = Level.SEVERE;
	public static final String logDirectory = null; // "outputLogs";
	public static final String outputDirectory = "output";
	public static final String pathDelimiter = "/";

	public static final String globalCostFilename = "global-cost.csv";
	public static final String localCostFilename = "local-cost.csv";
	public static final String terminationFilename = "termination.csv";
	public static final String selectedPlanFilename = "selected-plans.csv";
	public static final String numReorganizationsFilename = "num-reorganizations.csv";
	public static final String globalResponseFilename = "global-response.csv";
	public static final String fairnessFilename = "fairness-distribution.csv";
	public static final String distributionFilename = "indexes-histogram.csv";
	public static final String initialStructureBaseFilename = "metric_permutations/"; // "datasets/initial-full-tree-";
	public static final String unfairnessFilename = "unfairness.csv";
	public static final String repetetiveVectorFilename = "repetetive-vector.csv";
	public static final String repetetiveFairnessFilename = "repetetive-fairness.csv";
	public static final String initialStructureSuffix = "-all-permutations-";
	public static final String globalComplexCostFilename = "global-complex-cost.csv";
	public static final String globalWeightsFilename = "weights-alpha-beta.csv";
	public static String initialSortingOrder = "ASC";
	public static String goalSignalFilename = "TIS-GENERATION-FAILURE.txt";

	/**
	 * Default mapping is 0->0, 1->1, 2->2, ...
	 */
	public Function<Configuration, Map<Integer, Integer>> generateDefaultMapping = config -> {
		return DatasetShuffler.getDefaultMapping(config);
	};

	/**
	 * Shuffles the initial mapping (default or the one read from a file)
	 * <code>permutationID</code> times
	 */
	public Function<Configuration, Map<Integer, Integer>> generateShuffledMapping = config -> {
		return DatasetShuffler.getMappingByShuffling(config);
	};

	/**
	 * Returns vertex ID -> agent Plans ID Shuffles the list of agents once using
	 * random generator initialized with <code>permutationSeed</code> seed.
	 */
	public Function<Configuration, Map<Integer, Integer>> generateMappingForRepetitiveExperiments = config -> {
		return DatasetShuffler.getMappingForRepetitiveExperiments(config);
	};

	/**
	 * Returns vertex ID -> agent Plans ID
	 */
	public Function<Configuration, Map<Integer, Integer>> readMapping = config -> {
		return DatasetShuffler.getMappingFromFile();
	};

	public Dataset<Vector> getDataset(Random random) {
		return new GaussianDataset(numPlans, planDim, 0, 1, random);
	}

	public Dataset<Vector> getDataset(String datasetName) {
		FileVectorDataset dataset = new FileVectorDataset("datasets/" + datasetName + "/");
		return dataset;
	}

	public static boolean shouldReadInitialPermutationFromFile() {
		return Configuration.permutationFile != null;
	}

	public Configuration() {
		this.makeDirectory(Configuration.logDirectory);
		this.makeDirectory(Configuration.outputDirectory);
	}

	private void makeDirectory(String path) {
		if (path != null) {
			File dir = new File(path);
			if (!dir.exists()) {
				dir.mkdirs();
			}
		}
	}

	public static String getGlobalCostPath() {
		return Configuration.outputDirectory + Configuration.pathDelimiter + Configuration.globalCostFilename;
	}

	public static String getLocalCostPath() {
		return Configuration.outputDirectory + Configuration.pathDelimiter + Configuration.localCostFilename;
	}

	public static String getTerminationPath() {
		return Configuration.outputDirectory + Configuration.pathDelimiter + Configuration.terminationFilename;
	}

	public static String getSelectedPlansPath() {
		return Configuration.outputDirectory + Configuration.pathDelimiter + Configuration.selectedPlanFilename;
	}

	public static String getReorganizationPath() {
		return Configuration.outputDirectory + Configuration.pathDelimiter + Configuration.numReorganizationsFilename;
	}

	public static String getFairnessPath() {
		return Configuration.outputDirectory + Configuration.pathDelimiter + Configuration.fairnessFilename;
	}

	public static String getDistributionPath() {
		return Configuration.outputDirectory + Configuration.pathDelimiter + Configuration.distributionFilename;
	}

	public static String getGlobalResponsePath() {
		return Configuration.outputDirectory + Configuration.pathDelimiter + Configuration.globalResponseFilename;
	}

	public static String getUnfairnessPath() {
		return Configuration.outputDirectory + Configuration.pathDelimiter + Configuration.unfairnessFilename;
	}

	public static String getRepetetiveVectorPath() {
		return Configuration.outputDirectory + Configuration.pathDelimiter + Configuration.repetetiveVectorFilename;
	}

	public static String getRepetetiveFairnessPath() {
		return Configuration.outputDirectory + Configuration.pathDelimiter + Configuration.repetetiveFairnessFilename;
	}

	public static String getLeftTermPath() {
		return Configuration.outputDirectory + Configuration.pathDelimiter + "left-term.txt";
	}

	public static String getRightTermPath() {
		return Configuration.outputDirectory + Configuration.pathDelimiter + "right-term.txt";
	}

	public static String getGlobalComplexCostPath() {
		return Configuration.outputDirectory + Configuration.pathDelimiter + Configuration.globalComplexCostFilename;
	}

	public static String getWeightsPath() {
		return Configuration.outputDirectory + Configuration.pathDelimiter + Configuration.globalWeightsFilename;
	}

	public static String getGoalSignalPath() {
		if (Configuration.selectedDataset.getDatasetGroup().equalsIgnoreCase("energy2")) {
			return "datasets/energy2/" + "incentive-signals/" + Configuration.goalSignalFilename;
		} else {
			// StringTokenizer st = new
			// StringTokenizer(Configuration.selectedDataset.getDatasetName(), "\\");
			// st.nextToken();
			// String filename = st.nextToken();
			return "datasets/EPOS-ELECTRIC-VEHICLES/" + "price_signals/" + Configuration.goalSignalFilename;
		}
	}

	public void printConfiguration() {
		StringBuilder sb = new StringBuilder();
		sb.append("CONFIGURATION:").append(System.lineSeparator());
		sb.append("==============").append(System.lineSeparator());
		sb.append("numSimulations = ").append(Configuration.numSimulations).append(System.lineSeparator());
		sb.append("dataset = ").append(Configuration.dataset).append(System.lineSeparator());
		sb.append("numAgents = ").append(Configuration.numAgents).append(System.lineSeparator());
		sb.append("numPlans = ").append(Configuration.numPlans).append(System.lineSeparator());
		sb.append("planDim = ").append(Configuration.planDim).append(System.lineSeparator());
		sb.append("numIterations = ").append(Configuration.numIterations).append(System.lineSeparator());
		sb.append("numChildren = ").append(Configuration.numChildren).append(System.lineSeparator());
		sb.append("--------------").append(System.lineSeparator());
		sb.append("alpha = ").append(this.alpha).append(System.lineSeparator());
		sb.append("beta = ").append(this.beta).append(System.lineSeparator());
		sb.append("global cost function = ").append(Configuration.globalCostFunc.toString())
				.append(System.lineSeparator());
		sb.append("local cost function = ").append(Configuration.localCostFunc.toString())
				.append(System.lineSeparator());
		sb.append("goal signal supplier = ")
				.append(Configuration.goalSignalSupplier == null ? "null" : Configuration.goalSignalSupplier.toString())
				.append(System.lineSeparator());
		sb.append("--------------").append(System.lineSeparator());
		sb.append("permutationID = ").append(Configuration.permutationID).append(System.lineSeparator());
		sb.append("reorganizationSeed = ").append(this.reorganizationSeed).append(System.lineSeparator());
		sb.append("permutationSeed = ").append(this.permutationSeed).append(System.lineSeparator());
		sb.append("permutationFile = ").append(Configuration.permutationFile).append(System.lineSeparator());
		sb.append("reorganizationPeriod = ").append(this.reorganizationPeriod).append(System.lineSeparator());
		sb.append("memorizationOffset = ").append(this.memorizationOffset).append(System.lineSeparator());
		sb.append("reorganizationStrategy = ").append(this.reorganizationStrategy).append(System.lineSeparator());
		sb.append("convergenceTolerance = ").append(this.convergenceTolerance).append(System.lineSeparator());
		sb.append("--------------").append(System.lineSeparator());
		sb.append("loggingLevel = ").append(Configuration.loggingLevel).append(System.lineSeparator());

		System.out.println(sb.toString());
	}

	public static void main(String args[]) {
		Configuration config = new Configuration();
		CommandLineArgumentReader.setConfiguration(config, args);
		config.printConfiguration();
	}

	public static void fromFile(String path) {
		Configuration config = new Configuration();

		Properties argMap = new Properties();

		try (InputStream input = new FileInputStream(new File(path))) {
			argMap.load(input);
		} catch (IOException e1) {
			Configuration.log.log(Level.SEVERE, e1.getMessage());
			throw new IllegalStateException(e1);
		}

		if (argMap.get("numSimulations") != null) {
			Configuration.numSimulations = Integer.parseInt((String) argMap.get("numSim"));
		} else {
			Configuration.log.log(Level.WARNING, "Default value for numSim = 1");
			Configuration.numSimulations = 1;
		}

		if (argMap.get("numIterations") != null) {
			Configuration.numIterations = Integer.parseInt((String) argMap.get("numIterations"));
		} else {
			Configuration.log.log(Level.WARNING, "Default value for numIterations = 20");
			Configuration.numIterations = 20;
		}

		if (argMap.get("numAgents") != null) {
			Configuration.numAgents = Integer.parseInt((String) argMap.get("numAgents"));

		}

		if (argMap.get("numPlans") != null) {
			Configuration.numPlans = Integer.parseInt((String) argMap.get("numPlans"));
		}

		if (argMap.get("planDim") != null) {
			Configuration.planDim = Integer.parseInt((String) argMap.get("planDim"));
		}

		if (argMap.get("dataset") != null) {
			String dataset = (String) argMap.get("dataset");
			Configuration.dataset = dataset;
			Configuration.selectedDataset = new DatasetDescriptor(dataset, Configuration.planDim,
					Configuration.numAgents, Configuration.numPlans);

			String datasetPath = Configuration.selectedDataset.getPath();
			AtomicInteger agentCounter = new AtomicInteger();
			AtomicInteger maxPlans = new AtomicInteger();
			AtomicInteger maxPlanDims = new AtomicInteger();

			Helper.walkPaths(datasetPath).forEach(p -> {
				File file = new File(p);
				int c_agent = Helper.clearInt(file.getName().replaceAll("agent_", "").replaceAll("\\.plans", ""));

				AtomicInteger cMaxPlans = new AtomicInteger(1);

				Helper.readFile(p).forEach(f -> {
					int c_dims = f.split(",").length;
					maxPlanDims.set(maxPlans.get() > c_dims ? maxPlans.get() : c_dims);
					cMaxPlans.incrementAndGet();
				});

				maxPlans.set(maxPlans.get() > cMaxPlans.get() ? maxPlans.get() : cMaxPlans.get());

				if (c_agent != agentCounter.get()) {
					Configuration.log.log(Level.WARNING,
							"There is a chance your agent files are not named or indexed appropriately, please check them to avoid possible exceptions.");
				}
				agentCounter.incrementAndGet();
			});

			if (Configuration.numAgents > agentCounter.get() + 1) {
				Configuration.log.log(Level.WARNING,
						"You requested to load more agents than the dataset has. Using maximum available agents: "
								+ (agentCounter.get() + 1));
			}
			
			if (Configuration.numPlans > maxPlans.get()) {
				Configuration.log.log(Level.WARNING,
						"You requested to load more plans per agent than the dataset has. Using maximum available plans: "
								+ maxPlans.get());
			}
			
			if (Configuration.planDim > agentCounter.get() + 1) {
				Configuration.log.log(Level.WARNING,
						"You requested to load more plan elements than the dataset has. Using maximum available plane elements per vector: "
								+ maxPlanDims.get());
			}
		}

		if (argMap.get("alpha") != null) {
			config.alpha = Double.parseDouble((String) argMap.get("alpha"));
		} else {
			Configuration.log.log(Level.WARNING, "Default value for alpha = 0");
			config.alpha = 0;
		}

		if (argMap.get("beta") != null) {
			config.beta = Double.parseDouble((String) argMap.get("beta"));
		} else {
			Configuration.log.log(Level.WARNING, "Default value for beta = 0");
			config.beta = 0;
		}

		if (argMap.get("numChildren") != null) {
			Configuration.numChildren = Integer.parseInt((String) argMap.get("numChildren"));
		} else {
			Configuration.log.log(Level.WARNING, "Default value for numChildren = 2");
			Configuration.numChildren = 2;
		}

		if (argMap.get("shuffleFile") != null) {
			Configuration.permutationFile = (String) argMap.get("shuffleFile");
			Configuration.mapping = config.readMapping.apply(config);
		} else {
			Configuration.mapping = config.generateDefaultMapping.apply(config);
			Configuration.log.log(Level.WARNING, "Default agent mapping according to incremental index is applied.");
		}

		if (argMap.get("shuffle") != null) {
			Configuration.permutationID = Integer.parseInt((String) argMap.get("shuffle"));
			Configuration.mapping = config.generateShuffledMapping.apply(config);
		}

		if (argMap.get("strategy").equals("periodically")) {
			config.reorganizationStrategy = ReorganizationStrategyType.PERIODICALLY;
			config.reorganizationPeriod = Integer.parseInt((String) argMap.get("periodically.reorganizationPeriod"));
		} else if (argMap.get("strategy").equals("convergence")) {
			config.reorganizationStrategy = ReorganizationStrategyType.ON_CONVERGENCE;
			config.memorizationOffset = Integer.parseInt((String) argMap.get("convergence.memorizationOffset"));

		} else if (argMap.get("strategy").equals("globalCostReduction")) {
			config.reorganizationStrategy = ReorganizationStrategyType.GLOBAL_COST_REDUCTION;
			config.convergenceTolerance = Double.parseDouble((String) argMap.get("globalCost.reductionThreshold"));
		} else {
			config.reorganizationStrategy = ReorganizationStrategyType.NEVER;
			Configuration.log.log(Level.WARNING, "Default reorganizaiton strategy of no-reorganization is applied.");
		}

		if (argMap.get("reorganizationSeed") != null) {
			config.reorganizationSeed = Long.parseLong((String) argMap.get("reorganizationSeed"));
		} else {
			Configuration.log.log(Level.WARNING, "Default reorganizationSeed = 0 is applied.");
			config.reorganizationSeed = 0;
		}
		if (argMap.get("goalSignalType") != null) {
			try {
				int type = Integer.parseInt((String) argMap.get("goalSignalType"));
				switch (type) {
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
			} catch (NumberFormatException e) {
				Configuration.goalSignalFilename = (String) argMap.get("goalSignalType");
				Configuration.goalSignalSupplier = GoalSignalsCollection.fromFile;
			}
		}

		if (argMap.get("scaling") != null) {
			String func = (String) argMap.get("scaling");
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
		if (argMap.get("globalCostFunction") != null) {
			String func = (String) argMap.get("globalCostFunction");
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
		} else {
			Configuration.globalCostFunc = new VarCostFunction();
			Configuration.log.log(Level.WARNING, "Default globalCostFunction = VAR is applied.");
		}

		if (argMap.get("localCostFunction") != null) {
			String func = (String) argMap.get("localCostFunction");
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
		} else {
			Configuration.localCostFunc = new PlanDiscomfortFunction();
			Configuration.log.log(Level.WARNING, "Default localCostFunc=COST is applied.");
		}

		if (argMap.get("logLevel") != null) {
			String level = (String) argMap.get("logLevel");
			switch (level) {
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
		} else {
			Configuration.loggingLevel = Level.SEVERE;
			Configuration.log.log(Level.SEVERE, "Default logLevel = SEVERE is applied.");
		}
	}

}
