# EPOS

## Installation and setup

1. Download the project repository from the latest **release zip**.

2. Make sure that a version of Oracle Java 8 is installed. You can download it from [here](http://www.oracle.com/technetwork/java/javase/downloads/index.html).

3. This should be sufficient to execute I-EPOS.

## Run I-EPOS

1. Config parameters in `conf/epos.properties`, including the dataset, the number of agents, plans, plan dimension, and etc.

2. Choose dataset. Default is gaussian with 10 agents. You can download other dataset from [here](https://figshare.com/articles/dataset/Agent-based_Planning_Portfolio/7806548).

3. Run from command line. Navigate to the project directory and execute:

```
java -jar IEPOS-Tutorial.jar
```

## Results output

After running, results are output in `output` directory. You can also set the types of output in `epos.properties`.

