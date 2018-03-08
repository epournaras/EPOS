Here is a quick description on how to use the code


### Requirements
Python Version: 3.6 or higher

Required packages (pip install):
numpy
pandas

Expected built-ins (should be there, in case there are not, pip install):
operator
re
json
multiprocessing

### Usage

python brute_force.py -i "input_folder" -o "output_folder"

A runnig example:

python brute_force.py -i energy_example/ -o output/

Output:
Input folder is " energy_example/
Output folder is " output/
Loading agent data from folder: energy_example/
dataset shape: (5, 10, 144)
plans per user: [10 10 10 10 10]
total combinations: 100000
combinations for threads: [(0, 12500), (12500, 25000), (25000, 37500), (37500, 50000), (50000, 62500), (62500, 75000), (75000, 87500), (87500, 100000)]
0.0884862575517
0.0905709460572
0.0792574029551
0.0837316088933
0.0910217122356
0.0766414778851
0.0820505022987
0.0832949197079
total execution time: 1.4151489734649658


It breaks the range of plan combinations per agent to threads. Each float after, is the optimal solution per thread.
The input_folder should contain plan files, ending with suffix ".plans". The output folder should be empty, to avoid errors since the program may append to existing files. The input plan files should be structured like:
preference_value::timestep_1,timestep_2....
where each line is a plan, the first value is lamda or the agent prefernce, and the rest of comma seperated values are plan values per timestep

The output file structure contains the plan combinations executed in this thread. Each line is a tested plan selection for all agents and the evaluated variance of this selection. The format of each line is:
plan_index_agent_1, plan_index_agent_2 ....::variance_value
