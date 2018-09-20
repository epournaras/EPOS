package config;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import agent.dataset.Dataset;
import agent.dataset.DatasetDescriptor;
import agent.dataset.DatasetShuffler;
import agent.dataset.FileVectorDataset;
import agent.dataset.GaussianDataset;
import agent.multiobjectiveutils.ControllerCollection;
import agent.multiobjectiveutils.LocalCostController;
import agent.multiobjectiveutils.UnfairnessController;
import agent.planselection.PlanSelectionOptimizationFunction;
import agent.planselection.PlanSelectionOptimizationFunctionCollection;
import data.Vector;
import func.ConstrainedVarianceCostFunction;
import func.ConstraintCostFunction;
import func.CrossCorrelationCostFunction;
import func.DifferentiableCostFunction;
import func.DiscomfortPlanCostFunction;
import func.IndexCostFunction;
import func.PeriodicCostFunction;
import func.PlanCostFunction;
import func.PreferencePlanCostFunction;
import func.RMSECostFunction;
import func.RSSCostFunction;
import func.VarCostFunction;
import func.goalsignals.GoalSignalsCollection;
import treestructure.reorganizationstrategies.ReorganizationStrategy.ReorganizationStrategyType;


/**
 * Class that keeps all global parameters for IEPOS run.
 * 
 * @author Jovan N.
 */
public class Configuration {
	
	/*
	 * -dataset energy -numSim 4 -iterations 50 -lambda 7 -numAgents 888 -numPlans 33 -planDim 12 -nodeDegree 5
	 * -permOffset 11 -permID 22 -enableNEVERstrategy
	 * -enablePERIODICALLYstrategy 44
	 * -enableCONVERGENCEstrategy 55
	 * -logLevel FINEST
	 * -readInitialStructure 3
	 * -reorganizationSeed 81
	 */
	
	public static DatasetDescriptor[]	datasets 			= 	null;
	public static DatasetDescriptor 	selectedDataset		=	null;
	public static String 				dataset				=	null; //selectedDataset.getDatasetName();
	public static int					numDimensions		=	-1;   //selectedDataset.getDimensionality();
	public static int					numAgents			=	-1;   //selectedDataset.getTotalNumAgentsAvailable(); //1000;
	public static int					numPlans			=	-1;   //selectedDataset.getNumPlansAvailable(); //16;
	public static Map<Integer, Integer> mapping				=	null;
	
	public static int					numSimulations		=	3;
	public static int					numIterations		=	40;	
	public double						lambda				=	0;		
	public static int					numChildren			=	2;	
	
	public double						alpha				=	0;
	public double						beta				=	0;
	
	public static int					permutationID		=	0;
	public static String 				permutationFile		=	null;
	
	public static DifferentiableCostFunction<Vector>	globalCostFunc				=	new VarCostFunction(); //new ConstraintCostFunction(); //new ConstrainedVarianceCostFunction(); //new SimilarityCostFunction(); // new VarCostFunction();
	public static PlanCostFunction					localCostFunc				= 	new IndexCostFunction(); //new PreferencePlanCostFunction();
	public static Supplier<Vector> 					goalSignalSupplier			=	null; //GoalSignalsCollection.constant_signal; //GoalSignalsCollection.fromOnelinerFile; // GoalSignalsCollection.lowerBound; //GoalSignalsCollection.gaussian_mixture_impulse;
	public static UnaryOperator<Vector> 			normalizer					=	null; //Vector.no_normalization; //Vector.standard_normalization;
	public static PlanSelectionOptimizationFunction	planOptimizationFunction	=	PlanSelectionOptimizationFunctionCollection.complexFunction1;

	////////////////////////////////////////////////////////////////////////////////////////////////////
	// SEEDS:
	public long			reorganizationSeed			=	0;
	public long			permutationSeed 			=	0;
	public long			simulationSeed				=	0;
	public Random		simulationRNG				=	new Random(this.simulationSeed);
	public static long	reorganizationOffsetSeed	=	0; // not used anywhere for now I think
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	// REGARDING REORGANIZATION:
	public int							reorganizationPeriod	=	3;
	public int							reorganizationOffset	=	2;
	public ReorganizationStrategyType 	reorganizationType		=	ReorganizationStrategyType.PERIODICALLY;
	public double						convergenceTolerance	=	0.1;
	
	///////////////////////////////////////////////////////////////////////////////////////////////////
	// LOGGING INSTRUMENTATION:
	public static		Level		loggingLevel					=	Level.SEVERE;
	public static final String		logDirectory					=	null; //"outputLogs";
	public static final String		outputDirectory					=	"output";
	public static final String		pathDelimiter					=	"/";
	
	public static final String		globalCostFilename				=	"global-cost.csv";
	public static final String		localCostFilename				=	"local-cost.csv";
	public static final String		terminationFilename				=	"termination.csv";
	public static final String		selectedPlanFilename			=	"selected-plans.csv";
	public static final String		numReorganizationsFilename		=	"num-reorganizations.csv";
	public static final String		globalResponseFilename			=	"global-response.csv";
	public static final String		fairnessFilename				=	"fairness-distribution.csv";
	public static final String		distributionFilename			=	"indexes-histogram.csv";
	public static final String		initialStructureBaseFilename	=	"metric_permutations/"; //"datasets/initial-full-tree-";
	public static final String		unfairnessFilename				=	"unfairness.csv";
	public static final String		repetetiveVectorFilename		=	"repetetive-vector.csv";
	public static final String		repetetiveFairnessFilename		=	"repetetive-fairness.csv";
	public static final String		initialStructureSuffix			=	"-all-permutations-";
	public static final String 		globalComplexCostFilename		=	"global-complex-cost.csv";
	public static final String 		globalWeightsFilename			=	"weights-alpha-beta.csv";
	public static  		String		initialSortingOrder				=	"ASC";
	public static       String		goalSignalFilename				=	"TIS-GENERATION-FAILURE.txt";
	
	/**
	 * Default mapping is 0->0, 1->1, 2->2, ...
	 */
	public Function<Configuration, Map<Integer, Integer>> generateDefaultMapping = config -> {
		return DatasetShuffler.getDefaultMapping(config);
	};
	
	/**
	 * Shuffles the initial mapping (default or the one read from a file) <code>permutationID</code> times
	 */
	public Function<Configuration, Map<Integer, Integer>> generateShuffledMapping = config -> {
		return DatasetShuffler.getMappingByShuffling(config);
	};
	
	/**
	 *  Returns vertex ID -> agent Plans ID
	 *  Shuffles the list of agents once using random generator initialized with <code>permutationSeed</code> seed.
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
		return new GaussianDataset(numPlans, numDimensions, 0, 1, random);		
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
		if(path != null) {
			File dir = new File(path);
			if(!dir.exists()) {
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
		return Configuration.outputDirectory	+	Configuration.pathDelimiter +
			   Configuration.selectedPlanFilename;
	}
	
	public static String getReorganizationPath() {
		return Configuration.outputDirectory	+	Configuration.pathDelimiter		+
			   Configuration.numReorganizationsFilename;
	}
	
	public static String getFairnessPath() {
		return Configuration.outputDirectory	+	Configuration.pathDelimiter	+ Configuration.fairnessFilename;
	}
	
	public static String getDistributionPath() {
		return Configuration.outputDirectory	+	Configuration.pathDelimiter	+ Configuration.distributionFilename;
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
		if(Configuration.selectedDataset.getDatasetGroup().equalsIgnoreCase("energy2")) {
			return "datasets/energy2/" + "incentive-signals/" + Configuration.goalSignalFilename;
		} else {
//			StringTokenizer st = new StringTokenizer(Configuration.selectedDataset.getDatasetName(), "\\");
//			st.nextToken();
//			String filename = st.nextToken();
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
		sb.append("numDimensions = ").append(Configuration.numDimensions).append(System.lineSeparator());		
		sb.append("numIterations = ").append(Configuration.numIterations).append(System.lineSeparator());
		sb.append("numChildren = ").append(Configuration.numChildren).append(System.lineSeparator());
		sb.append("--------------").append(System.lineSeparator());
		sb.append("lambda = ").append(this.lambda).append(System.lineSeparator());
		sb.append("alpha = ").append(this.alpha).append(System.lineSeparator());
		sb.append("beta = ").append(this.beta).append(System.lineSeparator());
		sb.append("global cost function = ").append(Configuration.globalCostFunc.toString()).append(System.lineSeparator());
		sb.append("local cost function = ").append(Configuration.localCostFunc.toString()).append(System.lineSeparator());
		sb.append("goal signal supplier = ").append(Configuration.goalSignalSupplier == null ? "null" : Configuration.goalSignalSupplier.toString()).append(System.lineSeparator());
		sb.append("--------------").append(System.lineSeparator());
		sb.append("permutationID = ").append(Configuration.permutationID).append(System.lineSeparator());
		sb.append("reorganizationSeed = ").append(this.reorganizationSeed).append(System.lineSeparator());
		sb.append("permutationSeed = ").append(this.permutationSeed).append(System.lineSeparator());		
		sb.append("permutationFile = ").append(Configuration.permutationFile).append(System.lineSeparator());
		sb.append("reorganizationPeriod = ").append(this.reorganizationPeriod).append(System.lineSeparator());
		sb.append("reorganizationOffset = ").append(this.reorganizationOffset).append(System.lineSeparator());
		sb.append("reorganizationType = ").append(this.reorganizationType).append(System.lineSeparator());
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

}
