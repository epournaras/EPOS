# EPOS

## Installation and setup

1. Download the EPOS source code from the latest release **0.0.4**.

2. Make sure that a version of Oracle Java 8 is installed. You can download it from [here](http://www.oracle.com/technetwork/java/javase/downloads/index.html).

3. Downloaded files should include: `datasets/`, `conf/`, and `src/`.

## Run I-EPOS

1. Choose dataset in `datasets/`. Default is gaussian with 10 agents' plans file and the goal-signal target. Each plan has the same size, consisting of plan cost (before ":") and plan values (after ":"). The goal-signal has the same dimensions as the plan dimensions. You can download other dataset from [here](https://figshare.com/articles/dataset/Agent-based_Planning_Portfolio/7806548). 

2. Config parameters in `conf/epos.properties` based on the dataset you choose, including the number of agents, plans, iterations, simulations, children and the plan dimension. Fixed configs constain `conf/log4j.properties`, `conf/measurement.conf`, and `conf/protopeer.conf`.

3. Run from command line. Navigate to the project directory and execute:

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

For more information of EPOS, you can read the EPOS-Documentation from [here](https://github.com/epournaras/EPOS-Documentation).

## Reference
Pournaras, Evangelos, Peter Pilgerstorfer, and Thomas Asikis. "Decentralized collective learning for self-managed sharing economies." ACM Transactions on Autonomous and Adaptive Systems (TAAS) 13.2 (2018): 1-33.
