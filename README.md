# EPOS

## Installation and setup

1. Download the EPOS source code from the **latest release**.

2. Make sure that a version of Oracle Java 8 is installed. You can download it from [here](http://www.oracle.com/technetwork/java/javase/downloads/index.html).

3. Downloaded files should include: `datasets/`, `conf/`, and `src/`.

## Run I-EPOS

1. Choose dataset in `datasets/`. Default is gaussian with 10 agents' plans file and the goal-signal target. Each plan has the same size, consisting of plan cost (before ":") and plan values (after ":"). The goal-signal has the same dimensions as the plan dimensions. You can download other dataset from [here](https://figshare.com/articles/dataset/Agent-based_Planning_Portfolio/7806548). 

2. Config parameters in `conf/epos.properties` based on the dataset you choose, including the number of agents, plans, iterations, simulations, children and the plan dimension. Fixed configs constain `conf/log4j.properties`, `conf/measurement.conf`, and `conf/protopeer.conf`.

3. Config the constraints. There are three types in the prop `constraint`.
- `SOFT`: soft constraints, agents select plan following the global cost function
- `HARD_PLANS`: hard constraints for the agents' plans, should input a file `gaussian/hard_constraints_plans.csv`, which needs two rows (length=planDim), one denotes the values of hard constraints for plans, the other denotes the decision for plans. Set 4 rows when have two bounds (upper and lower)
- `HARD_COSTS`: hard constraints for the agents' cost, should input a file `gaussian/hard_constraints_costs.csv`, which needs the values of hard constraint and the decisions for local cost (1st column), global cost (2nd column) and global complex cost (3rd column).
You should make the decisions (**0**: ignore constraints, **1**: no larger than constraints, **2**: no less than constraints, **3**: equal to constraints).

4. Run from command line. Navigate to the project directory and execute:

```
cd EPOS
mvn clean install –U #or where the project pom.xml is
```
Once it successfully finishes, the project can be opened via your IDE of choice as a **Maven project**.

## Results output

After running, results are shown in `output/` directory. The output constains:
- Log output from the loggers in ".csv" format.
- A copy of the applied configuration for sanity checks named "used_conf.txt"
- Visualization of the tree & the plan selections

The results of hard constraints show in the file named `output/hard-constraint-violation.csv`, from which you can find the violation as iteration increases ("1" is violated, "0" is not violated).

For more information of EPOS, you can read the EPOS-Documentation from [here](https://github.com/epournaras/EPOS-Documentation).
