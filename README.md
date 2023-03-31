# EPOS

## Installation and setup

1. Download the EPOS source code from the **latest release**.

2. Make sure that a version of Oracle Java 8 is installed. You can download it from [here](http://www.oracle.com/technetwork/java/javase/downloads/index.html).

3. Downloaded files should include: `datasets`, `conf`, and `src`.

## Run I-EPOS

1. Choose dataset in `conf/epos.properties`. Default is gaussian with 10 agents. You can download other dataset from [here](https://figshare.com/articles/dataset/Agent-based_Planning_Portfolio/7806548). Config parameters based on the dataset you choose, including the number of agents, plans, iterations, simulations and plan dimension. 

2. Config the constraints. There are three types in the prop `constraint`.
- `SOFT`: soft constraints, agents select plan following the global cost function
- `HARD_PLANS`: hard constraints for the agents' plans, should input a file `gaussian/hard_constraints_plans.csv`, which needs two rows (length=planDim), one denotes the values of hard constraints for plans, the other denotes the decision for plans. Set 4 rows when have two bounds (upper and lower)
- `HARD_COSTS`: hard constraints for the agents' cost, should input a file `gaussian/hard_constraints_costs.csv`, which needs the values of hard constraint and the decisions for local cost (1st column), global cost (2nd column) and global complex cost (3rd column).
You should make the decisions (**0**: ignore constraints, **1**: no larger than constraints, **2**: no less than constraints, **3**: equal to constraints).

3. Run from command line. Navigate to the project directory and execute:

```
cd EPOS
mvn clean install –U #or where the project pom.xml is
```
Once it successfully finishes, the project can be opened via your IDE of choice as a **Maven project**.

## Results output

After running, results are shown in `output` directory. You can also set the types of outputs in `epos.properties`.

The results of hard constraints show in the file named "hard-constraint-violation.csv", from which you can find the violation as iteration increases ("1" is violated, "0" is not violated).

For more information of EPOS, you can read the EPOS-Documentation from [here](https://github.com/epournaras/EPOS-Documentation).
