#!/bin/bash

dataset_name="gaussian" 				# "energy", "bicycle"
num_simulations=1 						# any integer > 0
num_iterations=40						# any integer > 0
num_agents=1000							# any integer > 0
num_plans=16							# any integer > 0
num_children=2							# any integer > 0
plan_dim=100 							# exact dimensionality from the dataset
alpha=0									# double from [0, 1], alpha + beta <= 1
beta=0									# double from [0, 1], alpha + beta <= 1
shuffle=0								# any integer > 0
reorganization_seed=0
reorganization_period=3					# any integer > 0
memorization_offset=5					# any integer > 0
global_cost_reduction_threshold=0.5		# double from [0, 1]
shuffle_file="permutation.csv" 			# path to a file containing permutation of indices
goal_signal=1 							# integer from [1, 19] or filepath
global_cost_func="XCORR" 				# "VAR", "RSS", "RMSE"
scaling="MIN-MAX"		 				# "STD", "UNIT-LENGTH"
local_cost_func="INDEX"  				# "COST", "PREF"
log_level="SEVERE"

arguments="NOTHING"
jar_name="IEPOS-new.jar"


make_command() {
	arguments="-dataset "${dataset_name}" -planDim "${plan_dim}

	# all options below are optional. If not set, default values will be used

	arguments=${arguments}" -numIterations "${num_iterations}
	arguments=${arguments}" -numSim "${num_simulations}
	arguments=${arguments}" -numAgents "${num_agents}
	arguments=${arguments}" -numPlans "${num_plans}
	arguments=${arguments}" -numChildren "${num_children}

	arguments=${arguments}" -setGlobalCostFunc "${global_cost_func}
	arguments=${arguments}" -setLocalCostFunc "${local_cost_func}
	arguments=${arguments}" -setScaling "${scaling}
	arguments=${arguments}" -goalSignalType "${goal_signal}
	arguments=${arguments}" -alpha "${alpha}
	arguments=${arguments}" -beta "${beta}

	arguments=${arguments}" -shuffle "${shuffle}
	#arguments=${arguments}" -shuffleFile "${shuffle_file}
	arguments=${arguments}" -reorganizationSeed "${reorganization_seed}

	################################################################################################
	# CHOOSE ONLY ONE OF THE OPTIONS HERE:

	arguments=${arguments}" -enableNEVERstrategy"
	#arguments=${arguments}" -enablePERIODICALLYstrategy "${reorganization_period}
	#arguments=${arguments}" -enableCONVERGENCEstrategy "${memorization_offset}
	#arguments=${arguments}" -enableGLOBALCOSTREDUCTIONstrategy "${global_cost_reduction_threshold}

	################################################################################################

	arguments=${arguments}" -logLevel "${log_level}
}

run_jar() {
	java -jar $jar_name ${arguments}
}

make_command
run_jar