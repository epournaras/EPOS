package config;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.reflections.Reflections;

import com.google.common.collect.Sets;

import agent.ModifiableIeposAgent;
import agent.dataset.Dataset;
import agent.dataset.DatasetDescriptor;
import agent.dataset.DatasetShuffler;
import agent.dataset.FileVectorDataset;
import agent.dataset.GaussianDataset;
import agent.logging.AgentLogger;
import agent.logging.GlobalComplexCostLogger;
import agent.logging.GlobalCostLogger;
import agent.logging.GlobalResponseVectorLogger;
import agent.logging.LocalCostMultiObjectiveLogger;
import agent.logging.LoggingProvider;
import agent.logging.PlanFrequencyLogger;
import agent.logging.PositionLogger;
import agent.logging.ReorganizationLogger;
import agent.logging.SelectedPlanLogger;
import agent.logging.TerminationLogger;
import agent.logging.UnfairnessLogger;
import agent.logging.VisualizerLogger;
import agent.logging.WeightsLogger;
import agent.planselection.PlanSelectionOptimizationFunction;
import agent.planselection.PlanSelectionOptimizationFunctionCollection;
import data.Vector;
import dsutil.generic.RankPriority;
import dsutil.protopeer.services.topology.trees.DescriptorType;
import dsutil.protopeer.services.topology.trees.TreeType;
import func.DifferentiableCostFunction;
import func.HasGoal;
import func.IndexCostFunction;
import func.PlanCostFunction;
import func.PlanDiscomfortFunction;
import func.VarCostFunction;
import javassist.Modifier;
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

	public static double numberOfWeights = 0;
	public static String[] weights;
	public static double lambda = 0;
//	public double alpha = 0;
//	public double beta = 0;
	
	//Behaviour
		public static String agentsBehavioursPath = null;
		public static String behaviours = null;
	///////////////////////////////////

	public static int permutationID = 0;
	public static String permutationFile = null;

	public static DifferentiableCostFunction<Vector> globalCostFunc = new VarCostFunction();
	public static PlanCostFunction localCostFunc = new IndexCostFunction();
	public static Supplier<Vector> goalSignalSupplier = null;
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
	public static String logDirectory = null; // "outputLogs";
	public static String outputDirectory = null;
	public static final String pathDelimiter = File.separator;

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
	// Amal
	public static final String behavioursFilename = "behaviours.csv";
	public static final String agentsMappingOrder = "agents-mapping-order.csv";
	
	public static Set<AgentLogger> loggers = new HashSet<>();
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
		// created empty and then populated from file
	}

	private static void makeDirectory(String path) {
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
	//Amal
	public static String getAgentsMappingOderPath() {
		return Configuration.outputDirectory + Configuration.pathDelimiter + Configuration.agentsMappingOrder;
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


	public void printConfiguration() {
		StringBuilder sb = new StringBuilder();
		sb.append("CONFIGURATION:").append(System.lineSeparator());
		sb.append("dataset = ").append(Configuration.dataset).append(System.lineSeparator());
		sb.append("output = ").append(Configuration.outputDirectory).append(System.lineSeparator());
		sb.append("==============").append(System.lineSeparator());
		sb.append("numSimulations = ").append(Configuration.numSimulations).append(System.lineSeparator());
		sb.append("dataset = ").append(Configuration.dataset).append(System.lineSeparator());
		sb.append("numAgents = ").append(Configuration.numAgents).append(System.lineSeparator());
		sb.append("numPlans = ").append(Configuration.numPlans).append(System.lineSeparator());
		sb.append("planDim = ").append(Configuration.planDim).append(System.lineSeparator());
		sb.append("numIterations = ").append(Configuration.numIterations).append(System.lineSeparator());
		sb.append("numChildren = ").append(Configuration.numChildren).append(System.lineSeparator());
		sb.append("--------------").append(System.lineSeparator());
		sb.append("alpha = ").append(this.weights[0]).append(System.lineSeparator());
		sb.append("beta = ").append(this.weights[1]).append(System.lineSeparator());
		sb.append("global cost function = ").append(Configuration.globalCostFunc.toString())
				.append(System.lineSeparator());
		sb.append("local cost function = ").append(Configuration.localCostFunc.toString())
				.append(System.lineSeparator());
		sb.append("goal signal = ").append(
				Configuration.goalSignalSupplier == null ? "null" : Configuration.goalSignalSupplier.get().toString())
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

		String selectedLoggers = this.loggers.stream().map(l -> l.getClass().getSimpleName())
				.reduce((s1, s2) -> s1 + ", " + s2).orElse("None");
		sb.append("Selected Loggers: " + selectedLoggers).append(System.lineSeparator());
		System.out.println(sb.toString());

		try (PrintWriter out = new PrintWriter(new BufferedWriter(
				new java.io.FileWriter(this.outputDirectory + File.separator + "used_conf.txt", true)))) {
			out.append(sb.toString());
		} catch (FileNotFoundException ex) {
			Logger.getLogger(GlobalCostLogger.class.getName()).log(Level.SEVERE, null, ex);
		} catch (IOException e) {
			Logger.getLogger(GlobalCostLogger.class.getName()).log(Level.SEVERE, null, e);
		}
	}

	private static void propertyCleanUp(Properties argMap) {
		argMap.entrySet().forEach(e -> {
			Object k = e.getKey();
			Object v = ((String) e.getValue()).replaceAll("\\s", "").replaceAll("\\\"|\\'", "");
			argMap.put(k, v);
		});
	}

	public static void setUpEposBasicParams(Properties argMap, Configuration config) {

		if (argMap.get("numSimulations") != null) {
			Configuration.numSimulations = Helper.clearInt(((String) argMap.get("numSimulations")));
		} else {
			Configuration.log.log(Level.WARNING, "Default value for numSim = 1");
			Configuration.numSimulations = 1;
		}

		if (argMap.get("numIterations") != null) {
			Configuration.numIterations = Helper.clearInt((String) argMap.get("numIterations"));
		} else {
			Configuration.log.log(Level.WARNING, "Default value for numIterations = 20");
			Configuration.numIterations = 20;
		}

		if (argMap.get("numAgents") != null) {
			Configuration.numAgents = Helper.clearInt((String) argMap.get("numAgents"));

		}

		if (argMap.get("numPlans") != null) {
			Configuration.numPlans = Helper.clearInt((String) argMap.get("numPlans"));
		}

		if (argMap.get("planDim") != null) {
			Configuration.planDim = Helper.clearInt((String) argMap.get("planDim"));
		}

		if (argMap.get("numChildren") != null) {
			Configuration.numChildren = Helper.clearInt((String) argMap.get("numChildren"));
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
			Configuration.permutationID = Helper.clearInt((String) argMap.get("shuffle"));
			Configuration.mapping = config.generateShuffledMapping.apply(config);
		}
		if (argMap.get("agentsBehavioursPath") != null ) {
			Configuration.agentsBehavioursPath = (String) argMap.get("agentsBehavioursPath");
			log.log(Level.INFO, "A valid behaviour path is found, attempting to laod");
	} else {
		log.log(Level.WARNING, "No valid path provided for a behaviour!");	
	}	
		
		if (argMap.get("behaviours")!= null) {
			Configuration.behaviours = (String) argMap.get("behaviours");
		}
	}

	public static Configuration fromFile(String path) {

		Configuration config = new Configuration();

		Properties argMap = new Properties();
		try (InputStream input = new FileInputStream(new File(path))) {
			argMap.load(input);
		} catch (IOException e1) {
			Configuration.log.log(Level.SEVERE, e1.getMessage());
			throw new IllegalStateException(e1);
		}

		propertyCleanUp(argMap);
		setUpEposBasicParams(argMap, config);
		prepareDataset(argMap);
		prepareReorganization(argMap, config);
		prepareCostFunctions(argMap, config);
		prepareLoggers(argMap, config);

		return config;
	}

	public static boolean checkMethodExistence(Class cl, String methodName) {
		try {
			cl.getDeclaredMethod(methodName);
			return true;
		} catch (NoSuchMethodException e) {
			return false;
		}
	}
	
	private static void prepareLoggers(Properties argMap, Configuration config) {
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

		String[] possLoggers = { "logger.GlobalCostLogger", "logger.LocalCostMultiObjectiveLogger",
				"logger.TerminationLogger", "logger.SelectedPlanLogger", "logger.GlobalResponseVectorLogger",
				"logger.PlanFrequencyLogger", "logger.UnfairnessLogger", "logger.GlobalComplexCostLogger",
				"logger.WeightsLogger", "logger.ReorganizationLogger", "logger.VisualizerLogger", "logger.PositionLogger" };

		Set<String> selectedLoggers = Arrays.stream(possLoggers)
				.filter(key -> argMap.containsKey(key) && argMap.getProperty(key).equals("true"))
				.collect(Collectors.toSet());

		Configuration.loggers = initializeLoggers(selectedLoggers);
	}

	public static void prepareDataset(Properties argMap) {
		if (argMap.get("dataset") != null) {
			String dataset = (String) argMap.get("dataset");
			Configuration.dataset = dataset;
			Configuration.selectedDataset = new DatasetDescriptor(dataset, Configuration.planDim,
					Configuration.numAgents, Configuration.numPlans);
			Configuration.outputDirectory = System.getProperty("user.dir") + File.separator + "output" + File.separator
					+ dataset + "_" + System.currentTimeMillis() / 1000;
			// Configuration.outputDirectory = "output";

			Configuration.logDirectory = Configuration.outputDirectory;
			makeDirectory(Configuration.logDirectory);
			makeDirectory(Configuration.outputDirectory);

			String datasetPath = Configuration.selectedDataset.getPath();
			AtomicInteger maxPlans = new AtomicInteger();
			AtomicInteger maxPlanDims = new AtomicInteger();

			System.out.println(datasetPath + " " + Files.notExists(Paths.get(datasetPath)));

			Set<Integer> requested = IntStream.range(0, Configuration.numAgents).boxed().collect(Collectors.toSet());
			Set<Integer> found = new HashSet<>();

			Helper.walkPaths(datasetPath).filter(p -> new File(p).getName().matches("agent_\\d++.plans")).sorted()
					.forEach(p -> {
						File file = new File(p);
						int c_agent = Helper
								.clearInt(file.getName().replaceAll("agent_", "").replaceAll("\\.plans", ""));

						AtomicInteger cMaxPlans = new AtomicInteger(0);

						Helper.readFile(p).forEach(f -> {
							int c_dims = f.split(",").length;
							maxPlanDims.set(maxPlans.get() > c_dims ? maxPlans.get() : c_dims);
							cMaxPlans.getAndIncrement();
						});

						maxPlans.set(maxPlans.get() > cMaxPlans.get() ? maxPlans.get() : cMaxPlans.get());
						found.add(c_agent);

					});

			int totalFound = found.size();
			int difference = Sets.difference(requested, found).size();

			if (difference > 0) {
				Configuration.log.log(Level.WARNING,
						"You requested to load more agents than the dataset has. Using maximum available agents: "
								+ found.size()
								+ ". There is a chance your agent files are not named or indexed appropriately, "
								+ "please check them to avoid possible exceptions. Possible causes, files not named in the \"agent_{index}.plans format, missing"
								+ "indeces or wrong path.\"");
				Configuration.numAgents = totalFound;
			}

			if (Configuration.numPlans > maxPlans.get()) {
				Configuration.log.log(Level.WARNING,
						"You requested to load more plans per agent than the dataset has. Using maximum available plans: "
								+ maxPlans.get());
				Configuration.numPlans = maxPlans.get();
			}

			if (Configuration.planDim > maxPlanDims.get() + 1) {
				Configuration.log.log(Level.WARNING,
						"You requested to load more plan elements than the dataset has. Using maximum available plane elements per vector: "
								+ maxPlanDims.get());
			}
		}
	}

	public static void prepareCostFunctions(Properties argMap, Configuration config) {

		Reflections reflections = new Reflections("func");

		weights = new String[Integer.parseInt(argMap.get("numberOfWeights").toString())];

		if (argMap.get("weightsString") != null) {
			String[] inputWeight = argMap.get("weightsString").toString().split(",");
			for (int i = 0;i<inputWeight.length;i++){
				weights[i] = inputWeight[i];
			}

		}

		Set<Class<? extends PlanCostFunction>> allClasses = reflections.getSubTypesOf(PlanCostFunction.class);

		Map<String, PlanCostFunction> costFunctions = allClasses.stream()
				.filter(cl -> checkMethodExistence(cl, "getLabel") && !Modifier.isAbstract(cl.getModifiers()))
				.map(f -> {
					try {
						return (PlanCostFunction) f.newInstance();
					} catch (InstantiationException | IllegalAccessException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						return null;
					}
				}).collect(Collectors.toMap(c -> c.getLabel(), c -> c));

		if (argMap.get("globalCostFunction") != null) {
			PlanCostFunction global = costFunctions.get(argMap.get("globalCostFunction"));

			if (!(global instanceof DifferentiableCostFunction)) {
				log.log(Level.SEVERE, "Non differentiable global cost is provided! Cannot proceed. "
						+ global.getClass().getSimpleName());
				throw new IllegalStateException("Non differentiable global cost is provided! Cannot proceed. "
						+ global.getClass().getSimpleName());
			}

			Configuration.globalCostFunc = (DifferentiableCostFunction<Vector>) global;
			log.log(Level.INFO, "Global cost function successfully assigned: " + global.getClass().getSimpleName());

		} else {
			Configuration.globalCostFunc = new VarCostFunction();
			Configuration.log.log(Level.WARNING, "Default globalCostFunction = VAR is applied.");
		}

		if (argMap.get("localCostFunction") != null) {
			Configuration.localCostFunc = costFunctions.get(argMap.get("localCostFunction"));
		} else {
			Configuration.localCostFunc = new PlanDiscomfortFunction();
			Configuration.log.log(Level.WARNING, "Default localCostFunc=COST is applied.");
		}

		prepareGoalSignal(argMap);

		if (Configuration.globalCostFunc instanceof HasGoal) {
			((HasGoal) Configuration.globalCostFunc).populateGoalSignal();
		}

		if (Configuration.localCostFunc instanceof HasGoal) {
			((HasGoal) Configuration.globalCostFunc).populateGoalSignal();
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

	}
	
	public static void prepareReorganization(Properties argMap, Configuration config) {
		if (argMap.get("strategy").equals("periodically")) {
			config.reorganizationStrategy = ReorganizationStrategyType.PERIODICALLY;
			config.reorganizationPeriod = Helper.clearInt((String) argMap.get("periodically.reorganizationPeriod"));
		} else if (argMap.get("strategy").equals("convergence")) {
			config.reorganizationStrategy = ReorganizationStrategyType.ON_CONVERGENCE;
			config.memorizationOffset = Helper.clearInt((String) argMap.get("convergence.memorizationOffset"));

		} else if (argMap.get("strategy").equals("globalCostReduction")) {
			config.reorganizationStrategy = ReorganizationStrategyType.GLOBAL_COST_REDUCTION;
			config.convergenceTolerance = Double
					.parseDouble(Helper.clearNumericString((String) argMap.get("globalCost.reductionThreshold")));
		} else {
			config.reorganizationStrategy = ReorganizationStrategyType.NEVER;
			Configuration.log.log(Level.WARNING, "Default reorganizaiton strategy of no-reorganization is applied.");
		}

		if (argMap.get("reorganizationSeed") != null) {
			config.reorganizationSeed = Long
					.parseLong(Helper.clearNumericString((String) argMap.get("reorganizationSeed")));
		} else {
			Configuration.log.log(Level.WARNING, "Default reorganizationSeed = 0 is applied.");
			config.reorganizationSeed = 0;
		}
	}

	public static boolean prepareGoalSignal(Properties argMap) {

		boolean forLocal = Configuration.localCostFunc instanceof HasGoal;
		boolean forGlobal = Configuration.globalCostFunc instanceof HasGoal;

		if (!forLocal && !forGlobal) {
			log.log(Level.INFO,
					"Neither local or global function demand a goal signal, therefore it will not be loaded.");
			return false;
		}

		StringBuilder strb = new StringBuilder();
		String dependedFuncitons = forLocal && forGlobal ? "local and global cost functions"
				: forLocal ? "local cost function" : "global cost function";
		strb.append("A goal signal will be used for ").append(dependedFuncitons).append(".");
		log.log(Level.INFO, strb.toString());

		log.log(Level.INFO, "Loading Goal signal");
		// Default goal signal is a zero vector of planDim
		double[] baseSignal = new double[Configuration.planDim];

		if (argMap.get("goalSignalPath") != null && Files.notExists(Paths.get((String) argMap.get("goalSignalPath")))
				&& Files.isRegularFile(Paths.get((String) argMap.get("goalSignalPath")))) {
			log.log(Level.INFO, "A valid goal singnal path is found, attempting to laod");
			baseSignal = Arrays
					.stream(Helper.readFile(argMap.getProperty("goalSignalPath")).findFirst().orElse("0").split(","))
					.mapToDouble(s -> Double.parseDouble(Helper.clearNumericString(s))).toArray();
			if (baseSignal.length == 1 && Configuration.planDim > 1 && baseSignal[0] == 0) {
				log.log(Level.WARNING, "Probably no valid signal format is found, default zero signal is used");
				baseSignal = new double[0];
			}

		} else {
			log.log(Level.WARNING, "No valid path provided for a goal signal!");
			String targetFilePath = Helper.walkPaths(Configuration.selectedDataset.getPath())
					.filter(f -> f.endsWith(".target")).findFirst().orElse(null);
			File targetFile = targetFilePath != null ? new File(targetFilePath) : null;

			System.out.println(targetFilePath);

			if (targetFile.exists() && targetFile.isFile()) {
				log.log(Level.INFO, "Loading goal signal from dataset .target file of datast File");
				String signalString = Helper.readFile(targetFile.getPath()).findFirst().orElse("0");
				baseSignal = Arrays.stream(signalString.split(","))
						.mapToDouble(s -> Double.parseDouble(Helper.clearNumericString(s))).limit(Configuration.planDim)
						.toArray();
			} else {
				log.log(Level.WARNING,
						"No goal signal is found in dataset folder, the default zero valued vector is used as goal signal.");
			}
		}

		// TODO enclosed scope warning solution by copying. Introduces a small memory
		// reduduncy. Wrapper object can solve this
		final double[] finalizedBaseSignal = Arrays.copyOf(baseSignal, baseSignal.length);

		if (finalizedBaseSignal.length == Configuration.planDim) {
			log.log(Level.INFO, "Loaded goal signal is of equal dimensions as planDim, using as is for goal signal.");
			Configuration.goalSignalSupplier = () -> new Vector(finalizedBaseSignal);

		} else if (finalizedBaseSignal.length > Configuration.planDim) {
			log.log(Level.INFO,
					"Loaded goal signal is of greater dimensions as planDim, using a cropped version from signal start to planDim as goal signal.");
			Configuration.goalSignalSupplier = () -> new Vector(
					Arrays.stream(finalizedBaseSignal).limit(Configuration.planDim).toArray());
		} else {
			log.log(Level.INFO,
					"Loaded goal signal is of lesser dimensions as planDim, using a repetitive padding of the loaded signal to match plan dim.");
			Configuration.goalSignalSupplier = () -> new Vector(IntStream.range(0, finalizedBaseSignal.length)
					.mapToDouble(i -> finalizedBaseSignal[i % finalizedBaseSignal.length]).toArray());
		}

		log.log(Level.INFO, "loaded goal signal: " + Arrays.toString(baseSignal) + "\n final goal signal: "
				+ Arrays.toString(Configuration.goalSignalSupplier.get().getValues()));
		return true;
	}

	public static Set<AgentLogger> initializeLoggers(Set<String> selectedLoggers) {
		LoggingProvider<ModifiableIeposAgent<Vector>> loggingProvider = new LoggingProvider<ModifiableIeposAgent<Vector>>();
		GlobalCostLogger<Vector> GCLogger = new GlobalCostLogger<Vector>(Configuration.getGlobalCostPath());
		LocalCostMultiObjectiveLogger<Vector> LCLogger = new LocalCostMultiObjectiveLogger<Vector>(
				Configuration.getLocalCostPath());
		TerminationLogger<Vector> TLogger = new TerminationLogger<Vector>(Configuration.getTerminationPath());
		SelectedPlanLogger<Vector> SPLogger = new SelectedPlanLogger<Vector>(Configuration.getSelectedPlansPath(),
				Configuration.numAgents);
		GlobalResponseVectorLogger<Vector> GRVLogger = new GlobalResponseVectorLogger<Vector>(
				Configuration.getGlobalResponsePath());
		PlanFrequencyLogger<Vector> DstLogger = new PlanFrequencyLogger<Vector>(Configuration.getDistributionPath());
		UnfairnessLogger<Vector> ULogger = new UnfairnessLogger<Vector>(Configuration.getUnfairnessPath());
		GlobalComplexCostLogger<Vector> GCXLogger = new GlobalComplexCostLogger<Vector>(
				Configuration.getGlobalComplexCostPath());
		WeightsLogger<Vector> WLogger = new WeightsLogger<Vector>(Configuration.getWeightsPath());
		ReorganizationLogger<Vector> RLogger = new ReorganizationLogger<Vector>(Configuration.getReorganizationPath());
		VisualizerLogger<Vector> VLogger = new VisualizerLogger<Vector>();
		PositionLogger<Vector> PLogger = new PositionLogger<Vector>(Configuration.getAgentsMappingOderPath(),Configuration.numAgents);
		

		GCLogger.setRun(Configuration.permutationID);
		LCLogger.setRun(Configuration.permutationID);
		TLogger.setRun(Configuration.permutationID);
		SPLogger.setRun(Configuration.permutationID);
		GRVLogger.setRun(Configuration.permutationID);
		DstLogger.setRun(Configuration.permutationID);
		ULogger.setRun(Configuration.permutationID);
		GCXLogger.setRun(Configuration.permutationID);
		WLogger.setRun(Configuration.permutationID);
		RLogger.setRun(Configuration.permutationID);
		VLogger.setRun(Configuration.permutationID);
		PLogger.setRun(Configuration.permutationID);

		Map<String, AgentLogger> result = Arrays
				.stream(new AgentLogger[] { GCLogger, LCLogger, TLogger, SPLogger, GRVLogger, DstLogger, ULogger,
						GCXLogger, WLogger, RLogger, VLogger, PLogger })
				.collect(Collectors.toMap(a -> a.getClass().getSimpleName(), a -> a));

		Set<AgentLogger> res = new HashSet<>();
		for (String selLoggers : selectedLoggers) {
			String className = selLoggers.replaceAll("logger\\.", "");
			if (result.containsKey(className)) {
				res.add(result.get(className));
			}
		}

		// TODO select loggers based on conf
		return res;
	}

}
