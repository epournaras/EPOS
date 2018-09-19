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
import func.SimilarityCostFunction;
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
	
	// the size of this array is 62
	public static final String[] tags = 
					{"average_stdev", "min_stdev", "max_stdev", "max_value", "min_value",
					 // CORRELATIONS:
	                 // -- pearson:
	                "average_correlation_pearson", "max_avg_correlation_pearson",
	                "min_avg_correlation_pearson", "avg_max_correlation_pearson",
	                "avg_min_correlation_pearson", "min_min_correlation_pearson",
	                "max_max_correlation_pearson",
	                // -- kendall:
	                "average_correlation_kendall", "max_avg_correlation_kendall",
	                "min_avg_correlation_kendall", "avg_max_correlation_kendall",
	                "avg_min_correlation_kendall", "min_min_correlation_kendall",
	                "max_max_correlation_kendall",
	                // -- spearman:
	                "average_correlation_spearman", "max_avg_correlation_spearman",
	                "min_avg_correlation_spearman", "avg_max_correlation_spearman",
	                "avg_min_correlation_spearman", "min_min_correlation_spearman",
	                "max_max_correlation_spearman",
	                // DISCRETE COSINE TRANSFORMS:
	                // -- dct 1
	                "average_dct1_coeff",
	                "average_max_dct1_coeff", "average_min_dct1_coeff",
	                "max_dct1_coeff", "min_dct1_coeff",
	                // -- dct 2
	                "average_dct2_coeff",
	                "average_max_dct2_coeff", "average_min_dct2_coeff",
	                "max_dct2_coeff", "min_dct2_coeff",
	                // -- dct 3
	                "average_dct3_coeff",
	                "average_max_dct3_coeff", "average_min_dct3_coeff",
	                "max_dct3_coeff", "min_dct3_coeff",
	                // DISCRETE SINE TRANSFORMS:
	                // -- dst 1
	                "average_dst1_coeff",
	                "average_max_dst1_coeff", "average_min_dst1_coeff",
	                "max_dst1_coeff", "min_dst1_coeff",
	                // -- dst 2
	                "average_dst2_coeff",
	                "average_max_dst2_coeff", "average_min_dst2_coeff",
	                "max_dst2_coeff", "min_dst2_coeff",
	                // -- dst 3
	                "average_dst3_coeff",
	                "average_max_dst3_coeff", "average_min_dst3_coeff",
	                "max_dst3_coeff", "min_dst3_coeff",
	                // DISCRETE FOURIER TRANSFORMS:
	                "sum_of_0_dft_coeff", "max_of_0_dft_coeff", "avg_stdev_dft_coeff",
	                "sum_all_dft_coeff", "max_non0_dft_coeff", "sum_non0_dft_coeff"
        };
	
	public static final String[] 		percentileTags 		= 	{"10%", "25%", "50%", "75%", "90%"};
	
//	public static final double[] 		parameters 			= 	{0, 0.1, 0.25, 0.5, 0.75, 1};
	
//	public static final double[] 		parameters 			= 	{0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8,
//																 0.9, 0.95, 0.99, 0.999, 0.9999, 0.99999, 1};
	
	public static final double[] 		parameters 			= 	{0, 0.05, 0.1, 0.15, 0.2, 0.25, 0.3, 0.35, 
																 0.4, 0.45, 0.5, 0.55, 0.6, 0.65, 0.7, 0.75, 
																 0.8, 0.85, 0.9, 0.95, 0.99, 0.999, 0.9999, 0.99999, 1};
	
	public static DatasetDescriptor[]	datasets 			= 	null;
	public static DatasetDescriptor 	selectedDataset		=	null;
	public static String 				dataset				=	null; //selectedDataset.getDatasetName();
	public static int					numDimensions		=	-1;   //selectedDataset.getDimensionality();
	public static int					numAgents			=	-1;   //selectedDataset.getTotalNumAgentsAvailable(); //1000;
	public static int					numPlans			=	-1;   //selectedDataset.getNumPlansAvailable(); //16;
	public static boolean 				useNumPlans			=	false; //!!!!!!!!!!!
	
	
	public static int					numSimulations		=	1;
	public static int					numIterations		=	40;	
	public double						lambda				=	0;		
	public static int					numChildren			=	2;	
	
	public double						alpha				=	0;
	public double						beta				=	0;
	
	public double						gamma				=	0;		// equivalent of alpha, just in preliminary responses acceptance
	public double 						delta				=	0;		// equivalent of beta, just in preliminary responses acceptance
	
	public static int					permutationOffset	=	0;
	public static int					permutationID		=	0;
	
	public final DifferentiableCostFunction<Vector>	globalCostFunc				=	new VarCostFunction(); //new ConstraintCostFunction(); //new ConstrainedVarianceCostFunction(); //new SimilarityCostFunction(); // new VarCostFunction();
	public final PlanCostFunction					localCostFunc				= 	new PreferencePlanCostFunction();
	public static Supplier<Vector> 					goalSignalSupplier			=	null; //GoalSignalsCollection.constant_signal; //GoalSignalsCollection.fromOnelinerFile; // GoalSignalsCollection.lowerBound; //GoalSignalsCollection.gaussian_mixture_impulse;
	public static UnaryOperator<Vector> 			normalizer					=	null; //Vector.no_normalization; //Vector.standard_normalization;
	
	public static PlanSelectionOptimizationFunction	planOptimizationFunction	=	PlanSelectionOptimizationFunctionCollection.complexFunction1;
	public static LocalCostController				localCostController			=	null; //ControllerCollection.thresholdLCController; //ControllerCollection.complexOscilatingLCController; //ControllerCollection.simpleOscilatingLCController; //ControllerCollection.simpleDecreasingLCController;
	public static UnfairnessController				unfairnessController		=	null; //ControllerCollection.thresholdUFController; //ControllerCollection.complexOscilatingUFController; //ControllerCollection.simpleOscilatingUFController; //ControllerCollection.simpleDecreasingUFController;
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	// SEEDS:
	public long			reorganizationSeed			=	0;
	public long			permutationSeed 			=	0;
	public long			simulationSeed				=	0;
	public Random		simulationRNG				=	new Random(this.simulationSeed);
	public static long	reorganizationOffsetSeed	=	0; // not used anywhere for now I think
	
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	// DATA SHUFFLING - PERMUTATIONS:
	/**
	 * Some permutations are pre-made based on some metrics (statistics) calculated upon plans.
	 * <code>chosenMetric</code> is received as command line argument, and 
	 * <code>permutationFile</code> stores all permutations.
	 */
	public static String	chosenMetric	=	null;	
	public static String permutationFile 	=	null;
	
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	// REGARDING REORGANIZATION:
	public int							reorganizationPeriod	=	3;
	public int							reorganizationOffset	=	2;
	public ReorganizationStrategyType 	reorganizationType		=	ReorganizationStrategyType.PERIODICALLY;
	public double						convergenceTolerance	=	0.1;
	
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	// REGARDING FAIRNESS:
	public static double				fairnessMin				=	Double.NEGATIVE_INFINITY;
	public static double				fairnessMax				=	Double.POSITIVE_INFINITY;
	
	
	///////////////////////////////////////////////////////////////////////////////////////////////////
	// LOGGING INSTRUMENTATION:
	public static		Level		loggingLevel					=	Level.FINER;
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
	 * Returns vertex ID -> agent Plans ID
	 */
	public Function<Configuration, Map<Integer, Integer>> generateMapping = config -> {
		return DatasetShuffler.getMapping(config);
	};
	
	/**
	 *  Returns vertex ID -> agent Plans ID
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
		sb.append("dataset = ").append(Configuration.dataset).append(System.lineSeparator());
		sb.append("numAgents = ").append(Configuration.numAgents).append(System.lineSeparator());
		sb.append("numPlans = ").append(Configuration.numPlans).append(System.lineSeparator());
		sb.append("numDimensions = ").append(Configuration.numDimensions).append(System.lineSeparator());		
		sb.append("numSimulations = ").append(Configuration.numSimulations).append(System.lineSeparator());
		sb.append("numIterations = ").append(Configuration.numIterations).append(System.lineSeparator());
		sb.append("lambda = ").append(this.lambda).append(System.lineSeparator());
		sb.append("alpha = ").append(this.alpha).append(System.lineSeparator());
		sb.append("beta = ").append(this.beta).append(System.lineSeparator());
		sb.append("gamma = ").append(this.gamma).append(System.lineSeparator());
		sb.append("delta = ").append(this.delta).append(System.lineSeparator());
		sb.append("numChildren = ").append(Configuration.numChildren).append(System.lineSeparator());
		sb.append(System.lineSeparator());
		sb.append("global cost function = ").append(this.globalCostFunc.toString()).append(System.lineSeparator());
		sb.append("local cost function = ").append(this.localCostFunc.toString()).append(System.lineSeparator());
		sb.append("goal signal supplier = ").append(Configuration.goalSignalSupplier == null ? "null" : Configuration.goalSignalSupplier.toString()).append(System.lineSeparator());
		sb.append(System.lineSeparator());
		sb.append("permutationOffset = ").append(Configuration.permutationOffset).append(System.lineSeparator());
		sb.append("permutationID = ").append(Configuration.permutationID).append(System.lineSeparator());
		sb.append("reorganizationSeed = ").append(this.reorganizationSeed).append(System.lineSeparator());
		sb.append("permutationSeed = ").append(this.permutationSeed).append(System.lineSeparator());
		sb.append("chosenMetric = ").append(Configuration.chosenMetric).append(System.lineSeparator());
		sb.append("permutationFile = ").append(Configuration.permutationFile).append(System.lineSeparator());
		sb.append("reorganizationPeriod = ").append(this.reorganizationPeriod).append(System.lineSeparator());
		sb.append("reorganizationOffset = ").append(this.reorganizationOffset).append(System.lineSeparator());
		sb.append("reorganizationType = ").append(this.reorganizationType).append(System.lineSeparator());
		sb.append("convergenceTolerance = ").append(this.convergenceTolerance).append(System.lineSeparator());
		sb.append("loggingLevel = ").append(this.loggingLevel).append(System.lineSeparator());
		
		System.out.println(sb.toString());
	}
	
	public static void populateDatasets() {
		Configuration.datasets = new DatasetDescriptor[45];
		
		Configuration.datasets[0] = new DatasetDescriptor("bicycle", 98, 1000, 23); //new DatasetDescriptor("bicycle", 98, 2300, 23);
		Configuration.datasets[1] = new DatasetDescriptor("energy", 144, 1000, 10); //new DatasetDescriptor("energy", 144, 5600, 10);
		Configuration.datasets[2] = new DatasetDescriptor("gaussian", 100, 1000, 16);
		
		Configuration.datasets[3] = new DatasetDescriptor("energy2", "1-SHUFFLE", 144, 5600, 4);
		Configuration.datasets[4] = new DatasetDescriptor("energy2", "1-SHIFT(10)", 144, 5600, 4);
		Configuration.datasets[5] = new DatasetDescriptor("energy2", "1-SWAP(15)", 144, 5600, 4);
		Configuration.datasets[6] = new DatasetDescriptor("energy2", "1-SHIFT(20)", 144, 5600, 4);
		Configuration.datasets[7] = new DatasetDescriptor("energy2", "1-SWAP(30)", 144, 5600, 4);
		
		Configuration.datasets[8] = new DatasetDescriptor("energy2", "2-SHUFFLE", 144, 2374, 4);
		Configuration.datasets[9] = new DatasetDescriptor("energy2", "2-SHIFT(10)", 144, 2374, 4);
		Configuration.datasets[10] = new DatasetDescriptor("energy2", "2-SWAP(15)", 144, 2374, 4);
		Configuration.datasets[11] = new DatasetDescriptor("energy2", "2-SHIFT(20)", 144, 2374, 4);
		Configuration.datasets[12] = new DatasetDescriptor("energy2", "2-SWAP(30)", 144, 2374, 4);
		
		Configuration.datasets[13] = new DatasetDescriptor("energy2", "3-SHUFFLE", 144, 724, 4);
		Configuration.datasets[14] = new DatasetDescriptor("energy2", "3-SHIFT(10)", 144, 724, 4);
		Configuration.datasets[15] = new DatasetDescriptor("energy2", "3-SWAP(15)", 144, 724, 4);
		Configuration.datasets[16] = new DatasetDescriptor("energy2", "3-SHIFT(20)", 144, 724, 4);
		Configuration.datasets[17] = new DatasetDescriptor("energy2", "3-SWAP(30)", 144, 724, 4);
		
		Configuration.datasets[18] = new DatasetDescriptor("energy2", "4-SHUFFLE", 144, 724, 4);
		Configuration.datasets[19] = new DatasetDescriptor("energy2", "4-SHIFT(10)", 144, 724, 4);
		Configuration.datasets[20] = new DatasetDescriptor("energy2", "4-SWAP(15)", 144, 724, 4);
		Configuration.datasets[21] = new DatasetDescriptor("energy2", "4-SHIFT(20)", 144, 724, 4);
		Configuration.datasets[22] = new DatasetDescriptor("energy2", "4-SWAP(30)", 144, 724, 4);
		
		Configuration.datasets[23] = new DatasetDescriptor("energy2", "5-SHUFFLE", 144, 497, 4);
		Configuration.datasets[24] = new DatasetDescriptor("energy2", "5-SHIFT(10)", 144, 497, 4);
		Configuration.datasets[25] = new DatasetDescriptor("energy2", "5-SWAP(15)", 144, 497, 4);
		Configuration.datasets[26] = new DatasetDescriptor("energy2", "5-SHIFT(20)", 144, 497, 4);
		Configuration.datasets[27] = new DatasetDescriptor("energy2", "5-SWAP(30)", 144, 497, 4);
		
		Configuration.datasets[28] = new DatasetDescriptor("energy2", "6-SHUFFLE", 144, 500, 4);
		Configuration.datasets[29] = new DatasetDescriptor("energy2", "6-SHIFT(10)", 144, 500, 4);
		Configuration.datasets[30] = new DatasetDescriptor("energy2", "6-SWAP(15)", 144, 500, 4);
		Configuration.datasets[31] = new DatasetDescriptor("energy2", "6-SHIFT(20)", 144, 500, 4);
		Configuration.datasets[32] = new DatasetDescriptor("energy2", "6-SWAP(30)", 144, 500, 4);
		
		Configuration.datasets[33] = new DatasetDescriptor("energy2", "7-SHUFFLE", 144, 493, 4);
		Configuration.datasets[34] = new DatasetDescriptor("energy2", "7-SHIFT(10)", 144, 493, 4);
		Configuration.datasets[35] = new DatasetDescriptor("energy2", "7-SWAP(15)", 144, 493, 4);
		Configuration.datasets[36] = new DatasetDescriptor("energy2", "7-SHIFT(20)", 144, 493, 4);
		Configuration.datasets[37] = new DatasetDescriptor("energy2", "7-SWAP(30)", 144, 493, 4);
		
		Configuration.datasets[38] = new DatasetDescriptor("energy2", "8-SHUFFLE", 144, 496, 4);
		Configuration.datasets[39] = new DatasetDescriptor("energy2", "8-SHIFT(10)", 144, 496, 4);
		Configuration.datasets[40] = new DatasetDescriptor("energy2", "8-SWAP(15)", 144, 496, 4);
		Configuration.datasets[41] = new DatasetDescriptor("energy2", "8-SHIFT(20)", 144, 496, 4);
		Configuration.datasets[42] = new DatasetDescriptor("energy2", "8-SWAP(30)", 144, 496, 4);
		
		Configuration.datasets[42] = new DatasetDescriptor("energy2", "PNW-EVENING-SHUFFLE", 144, 989, 4);
		
		Configuration.datasets[43] = new DatasetDescriptor("EPOS-ELECTRIC-VEHICLES", "ev_2779/MON12-TUE12", 1440, 2779, 4);
		Configuration.datasets[44] = new DatasetDescriptor("EPOS-ELECTRIC-VEHICLES", "ev_2779/weekly", 1440, 2779, 4);
		
	}
	
	public static void main(String args[]) {
		Configuration config = new Configuration();
		CommandLineArgumentReader.setConfiguration(config, args);
		config.printConfiguration();
	}

}
