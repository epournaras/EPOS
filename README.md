# EPOS-Logging-ZMQ
## This is the repository for the live version of I-EPOS.
To have a running version, you need to set up few modules first:

################  
1) Epos Database \
################  
You need to set up the epos database first. A lot of the commands are tuned for postgresdb. \
The commands (definitions) can be found in the `~/sql/definitions/create.database.sql` 

################  
2) Persistance Daemon \
################  
After the database is set up, you need to have the db daemon running and listening. \
to launch the daemon: \
./start.daemon.sh deployments/localhost \
make sure the ports are open, the user in the database has superuser permissions \
The deployement setting can be found in `~/deployements/localhost/conf/daemon.conf` 

################  
3) EPOS Live Config \
################  
The configurations can be found in the `~/conf/epos.properties` \
There are many configurations and dymamic settings in the `epos.properties` \
Very important in the EPOS Live configuration are the following parameters: \
sleepSecondBetweenRuns=10 ## after each simulation is over, system waits for 10 seconds \
randomiseUsers = true ## after each run, the mapping between users <-> agents (EV plans) is changed \
planChange=true ## users changing plans at the end of each run \
newPlanProb = 9 ## probability of a users changing plans at the end of each run (9 means 10%, look up the formula in `communication.User.java`) \
weightChange=true ## users changing weights at the end of each run \
newWeightProb = 9 ## probability of a users changing the weights for the cost function at the end of each run (9 means 10%, look up the formula in `communication.User.java`) \
userChange=true ## allowing users join / leave the system \
joinLeaveRate = 9 ## the probability of having a users join/leave event at the end of each run \
userChangeProb = 9 ## the max number of users allowed to join/leave given the above event (9 means maximum 0.1th number of users can join/leave) \
maxNumPeers=180 ## given the above event, the max number of users \
minNumPeers = 120 ## given the above event, the min number of users 

################  
4) Building EPOS Live \
################  
-- if you're not planning to change anything in the code, skip to step 5 -- \
The first step is to learn about the core I-EPOS functionalities: \
http://epos-net.org/software_doc/documentation/index.html \
After this, you can go to the code for the new I-EPOS live functionalities. Most of the new classes are in the `communication` package. To see the different objects and the algorithms, have a look at the sequence diagram: `~/Live I-EPOS Architecture.png` 

################  
5) Running I-EPOS Live \
################  
After having your configuration set-up, you can now run the I-EPOS live. \
Before that, make sure no other application is using these three ports locally: \
- default EPOS Requester port: 54321 \
- default GateWay port: 12345 \
- default User port: 15545 \
- default Persistance Daemon port: 6433 \
- default Bootstrap node port: 12000 


Also note that if another instance of EPOS Live is running with bootstrap port 12000 and has 100 starting nodes, the ports between 12000 - 12099 are also busy. \
After checking the above, you can start EPOS Live, run EPOSRequester, either in IDE of by `Java -jar EPOSRequester.java` \
This will run the Users application, and the GateWay in a screen session. You can access each by running `screen -r -S Users` or `screen -r -S GateWay`, respectively. \
After the initial setup is done via the User and GateWay application, the EPOS nodes are executed. you can access each by running `screen -r -S peer[id]` (e.g., `screen -r -S peer0` for the bootstrap peer). \
The epos will run for the designated maxNumRuns * maxNumSimulations (in `~/conf/epos.properties`) and record everthing in the database. \
If you want to stop it prematurely, you can run `~/killall.sh` 

################  
6) Some more help \
################  
In the `~/useful commands.txt` files, there are some useful commands for dealing with DB and EPOS Live.