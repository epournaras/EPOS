package config;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

import config.ConfigurationReader;
//import protocols.GenerationScheme;
//import protocols.SelectionScheme;
//import topics.Topic;
//import topics.TopicSingleValue;
//import bloomfilter.CHashFactory;
//
//import communication.AggregationStrategy;
//
//import consistency.BloomFilterParams;
//import consistency.BloomFilterType;
//import diasguimsg.SensorDescription;
import dsutil.protopeer.services.aggregation.AggregationType;
public class LiveConfiguration {

    /*
    Farzam
     */

    public String reportPath = "reports/numOfAggregators/";
    public String scenarioPath = "scenarios/numOfAggregators/";
    public String paramPath = "params/numOfAggregators/";
    public String logPath = "peersLog/numOfAggregators/";
    public String topologyPath = "topology/";
    public String topologySimulationPath = topologyPath + "simulation/";
    public String topologyLivePath = topologyPath + "live/";
    public String topicsPath = "topics/";

    public String name = "Experiment";

    public int runDuration = 40;
    public int N = 20;

    // Peer Sampling Service
    public int c = 50;	 																						//length of the view (descriptor table)
    public int H = 1; 																							// healing parameter
    public int S = 24;		 																					//swap parameter
//    public ViewPropagationPolicy viewPropagationPolicy = ViewPropagationPolicy.PUSHPULL;
//    public PeerSelectionPolicy peerSelectionPolicy = PeerSelectionPolicy.RAND;
    public int Tpss = 250;			                                                                // T the period that the active state is triggered (milliseconds)
    public int A = 1000; 																			// increment of age of descriptors
    public int Tbootpss = 10000; 																	// bootstrapping initial period
    public int Tbootfixedtopology = 7000;															// For live experiments with fixed topology only!

    // DIAS Service Parameterization
    public int Tdias = 1000; 																		// the dissemination period - every Tdias miliseconds disseminator sends a request
    public int Tsampling = 250; 																	// the sampling period from Peer Sampling Service - every Tsampling miliseconds peers gossip
    public int sampleSize = 37; 																	// number of neighbors that DIAS takes from pss
    public int numOfSessions = 30; 																	// the number of sessions that can run periodically in DIAS
    public int unexploitedSize = 37;
    public int outdatedSize = 37;
    public int exploitedSize = 37;
//    public AggregationStrategy.Strategy strategy = AggregationStrategy.Strategy.EXPLOITATION;

    // -- Bloom Filter settings --
//    public BloomFilterType amsType = BloomFilterType.COUNTING;

    // new parameters are:
    // 1. fp, the target false positive probability
    // 2. n, the number of elements
    // added edward 2018-08-15

    // --- ams ---
//    public int amsHashType = CHashFactory.DOUBLE_HASH;
    public int 		ams_n = 3000;	// number of elements
    public double 	ams_fp = 1e-11;	// false positive prob

    // --- dma ---
//    public int dmaHashType = CHashFactory.DOUBLE_HASH;
    public int 		dma_n = 3000;	// number of elements
    public double 	dma_fp = 1e-11;	// false positive prob

    // --- amd ---
//    public int amdHashType = CHashFactory.DOUBLE_HASH;
    public int 		amd_n = 3000;	// number of elements
    public double 	amd_fp = 1e-11;	// false positive prob

    // --- sma ---
//    public int smaHashType = CHashFactory.DOUBLE_HASH;
    public int 		sma_n = 3000;	// number of elements
    public double 	sma_fp = 1e-11;	// false positive prob

//    public Map<BloomFilterParams, Object> bfParams = new HashMap<BloomFilterParams, Object>();

    // DIAS Application Parameterization
    public AggregationType type = AggregationType.ARITHMETIC;//AggregationType.ARITHMETIC_LIST; //AggregationType.ARITHMETIC;
    public int Tboot = 15000; 																		// a bootstrapping period before requesting an aggregation
    public int delay_low = 2;
    public int delay_high = 10;
    public boolean scenarioFound = false;

    public int Taggr = 100 * 1000; 																	// the period of aggregation request
    public int k = 5; 																				// the number of possible states
    public double minValueDomain = 0;
    public double maxValueDomain = 1;
    // SYNCHRONUS SETTINGS: Pt = 1.0 and Ps = 1.0
    public double Pt = 1.0;																			// 0.4;
    public double Ps = 1.0;																			// 0.7;

    public int t = 200000; 																			// the period of evaluation for changing a selected state
//    public GenerationScheme genScheme = GenerationScheme.BETA;
//    public SelectionScheme selScheme = SelectionScheme.CYCLICAL;

    public int numOfAggregatorsON = 15;
    public int numOfDisseminatorsON = 90;
    public int topKbufferSize = 100;
    public int numOfStatesInList = 1;
//    public Topic topic = TopicSingleValue.TEMPERATURE;

    public int myIndex = 0;
    public boolean isCarrier = false;
    public int myPort = 0;
    public String myIP = "127.0.0.1";

    // Protopeer Bootstrap server
    public String bootstrapIP = "127.0.0.1";
    public int bootstrapPort = 12000;

    // device gateway, so that devices can obtain IP address of peers
    public String deviceGatewayIP = "127.0.0.1";
    public int deviceGatewayPort = 3427;

    // Persistence Daemon server, for logging to Postgres
    public String persistenceDaemonIP = "127.0.0.1";
    public int persistenceDaemonPort = 6433;

    // Logging options, specifies what is logged to Postgres
    public boolean persistenceActive = true;
    public boolean persistMessages = true;
    public boolean eventLogging = false;
    public boolean vizPersistence = false;

    public boolean persistAggregationEvent = false;

    public boolean persistPSSSamples = false;



    public boolean rawLog = false;
    public int rawLogLevel= 1;

    //
    public boolean enablePSSlogging = false;
    public boolean enableDIASmsgsLogging = false; // true; mod edward | 2018-04-03
    public int skipTransitions = 0;
    public boolean enableJsons = false;

    // DIAS connection with the gateway
    // add edward | 2018-05-22
    public boolean				hasSensorDescription = false;

    public String				sensorName = null,
            sensorType = null;

    public int					diasNetworkId = 0;

    private ArrayList<Integer> intermedier = null;

    // other peerlets, added by edward gaere
    public int 	NDFlushRate = 1;	//100;

    public void setIntermedier(ArrayList<Integer> intermedier) {
        this.intermedier = intermedier;
        triggerPrintingUsers();
    }

    public int getIndex(int i) {
        if(this.intermedier != null) {
            return this.intermedier.get(i);
        }
        return i;
    }

    private void triggerPrintingUsers() {
        StringBuilder sb = new StringBuilder();
        for(Integer user : intermedier) {
            sb.append(user + System.lineSeparator());
        }

        PrintWriter out = null;
        try {
            out = new PrintWriter(new BufferedWriter(new FileWriter("usedUsers.txt", false)));
            out.print(sb.toString());

        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            if(out != null) {
                out.flush();
                out.close();
            }
        }
    }

//    public Map<BloomFilterParams, Object> collectBloomFilterParams()
//    {
//        bfParams.put(BloomFilterParams.AMS_TYPE, amsType);
//        bfParams.put(BloomFilterParams.AMS_HASH_TYPE, amsHashType);
//        bfParams.put(BloomFilterParams.AMS_N, ams_n);
//        bfParams.put(BloomFilterParams.AMS_FP, ams_fp);
//
//        bfParams.put(BloomFilterParams.AMD_HASH_TYPE, amdHashType);
//        bfParams.put(BloomFilterParams.AMD_N, amd_n);
//        bfParams.put(BloomFilterParams.AMD_FP, amd_fp);
//
//        bfParams.put(BloomFilterParams.DMA_HASH_TYPE, dmaHashType);
//        bfParams.put(BloomFilterParams.DMA_N, dma_n);
//        bfParams.put(BloomFilterParams.DMA_FP, dma_fp);
//
//        bfParams.put(BloomFilterParams.SMA_HASH_TYPE, smaHashType);
//        bfParams.put(BloomFilterParams.SMA_N, sma_n);
//        bfParams.put(BloomFilterParams.SMA_FP, sma_fp);
//
//        return bfParams;
//    }


    public void readConfiguration(String[] args) {
        try {
            Properties argMap = ConfigurationReader.initializeConfig(args);

            //System.out.println( "readConfiguration::argMap: " + argMap );
            assert( argMap.containsKey("hasSensorDescription") );

            if (argMap.get("hasSensorDescription") != null)
            {

                final String	s = (String)argMap.get("hasSensorDescription");
                if( s.equals("True") == true)
                {
                    this.hasSensorDescription = true;

                    if( this.hasSensorDescription )
                    {
                        this.sensorName = (String)argMap.get("sensorName");
                        this.sensorType = (String)argMap.get("sensorType");
                        this.diasNetworkId = Integer.valueOf((String)argMap.get("diasNetworkId"));
                    }
                }


            }



            if (argMap.get("name") != null) {
                this.name = (String) argMap.get("name");
            }
            if(argMap.get("N") != null) {
                this.N = Integer.parseInt((String) argMap.get("N"));
            }
            if(argMap.get("runDuration") != null) {
                this.runDuration = Integer.parseInt((String) argMap.get("runDuration"));
            }
            if (argMap.get("myIndex") != null) {
                this.myIndex = Integer.parseInt((String) argMap.get("myIndex"));
            }
            if (argMap.get("myPort") != null) {
                this.myPort = Integer.parseInt((String) argMap.get("myPort"));
            }
            if (argMap.get("c") != null) {
                this.c = Integer.parseInt((String) argMap.get("c"));
            }
            if (argMap.get("S") != null) {
                this.S = Integer.parseInt((String) argMap.get("S"));
            }
            if (argMap.get("H") != null) {
                this.H = Integer.parseInt((String) argMap.get("H"));
            }
            if (argMap.get("viewPropagationPolicy") != null) {
//                this.viewPropagationPolicy = (ViewPropagationPolicy) argMap.get("viewPropagationPolicy");
            }
            if (argMap.get("peerSelectionPolicy") != null) {
//                this.peerSelectionPolicy = (PeerSelectionPolicy) argMap.get("peerSelectionPolicy");
            }
            if (argMap.get("Tpss") != null) {
                this.Tpss = Integer.parseInt((String) argMap.get("Tpss"));
            }
            if (argMap.get("A") != null) {
                this.A = Integer.parseInt((String) argMap.get("A"));
            }
            if (argMap.get("Tbootpss") != null) {
                this.Tbootpss = Integer.parseInt((String) argMap.get("Tbootpss"));
            }
            if (argMap.get("Tbootfixedtopology") != null) {
                this.Tbootfixedtopology = Integer.parseInt((String) argMap.get("Tbootfixedtopology"));
            }
            if (argMap.get("Tdias") != null) {
                this.Tdias = Integer.parseInt((String) argMap.get("Tdias"));
            }
            if (argMap.get("Tsampling") != null) {
                this.Tsampling = Integer.parseInt((String) argMap.get("Tsampling"));
            }
            if (argMap.get("sampleSize") != null) {
                this.sampleSize = Integer.parseInt((String) argMap.get("sampleSize"));
            }
            if (argMap.get("numOfSessions") != null) {
                this.numOfSessions = Integer.parseInt((String) argMap.get("numOfSessions"));
            }
            if (argMap.get("unexploitedSize") != null) {
                this.unexploitedSize = Integer.parseInt((String) argMap.get("unexploitedSize"));
            }
            if (argMap.get("outdatedSize") != null) {
                this.outdatedSize = Integer.parseInt((String) argMap.get("outdatedSize"));
            }
            if (argMap.get("exploitedSize") != null) {
                this.exploitedSize = Integer.parseInt((String) argMap.get("exploitedSize"));
            }
            if (argMap.get("strategy") != null) {
//                this.strategy = (AggregationStrategy.Strategy) argMap.get("strategy");
            }

            // -- ams --
            if (argMap.get("amsType") != null) {
//                this.amsType = (BloomFilterType) argMap.get("amsType");
            }
            if (argMap.get("amsHashType") != null) {
//                this.amsHashType = Integer.parseInt((String) argMap.get("amsHashType"));
            }
            if (argMap.get("ams_n") != null) {
                this.ams_n = Integer.parseInt((String) argMap.get("ams_n"));
            }

            if (argMap.get("ams_fp") != null) {
                this.ams_fp = Double.parseDouble((String) argMap.get("ams_fp"));
            }

            // -- dma --
            if (argMap.get("dmaHashType") != null) {
//                this.dmaHashType = Integer.parseInt((String) argMap.get("dmaHashType"));
            }
            if (argMap.get("dma_n") != null) {
                this.dma_n = Integer.parseInt((String) argMap.get("dma_n"));
            }

            if (argMap.get("dma_fp") != null) {
                this.dma_fp = Double.parseDouble((String) argMap.get("dma_fp"));
            }

            // -- amd --
            if (argMap.get("amdHashType") != null) {
//                this.amdHashType = Integer.parseInt((String) argMap.get("amdHashType"));
            }
            if (argMap.get("amd_n") != null) {
                this.amd_n = Integer.parseInt((String) argMap.get("amd_n"));
            }
            if (argMap.get("amd_fp") != null) {
                this.amd_fp = Double.parseDouble((String) argMap.get("amd_fp"));
            }

            // -- sma --
            if (argMap.get("smaHashType") != null) {
//                this.smaHashType = Integer.parseInt((String) argMap.get("smaHashType"));
            }
            if (argMap.get("sma_n") != null) {
                this.sma_n = Integer.parseInt((String) argMap.get("sma_n"));
            }
            if (argMap.get("sma_fp") != null) {
                this.sma_fp = Double.parseDouble((String) argMap.get("sma_fp"));
            }

            if (argMap.get("type") != null) {
                this.type = (AggregationType) argMap.get("type");
            }
            if (argMap.get("Tboot") != null) {
                this.Tboot = Integer.parseInt((String) argMap.get("Tboot"));
            }
            if (argMap.get("Taggr") != null) {
                this.Taggr = Integer.parseInt((String) argMap.get("Taggr"));
            }
            if (argMap.get("k") != null) {
                this.k = Integer.parseInt((String) argMap.get("k"));
            }
            if (argMap.get("minValueDomain") != null) {
                this.minValueDomain = (Double) argMap.get("minValueDomain");
            }
            if (argMap.get("maxValueDomain") != null) {
                this.maxValueDomain = (Double) argMap.get("maxValueDomain");
            }
            if (argMap.get("Pt") != null) {
                this.Pt = (Double) argMap.get("Pt");
            }
            if (argMap.get("Ps") != null) {
                this.Ps = (Double) argMap.get("Ps");
            }
            if (argMap.get("t") != null) {
                this.t = Integer.parseInt((String) argMap.get("t"));
            }
            if (argMap.get("genScheme") != null) {
//                this.genScheme = (GenerationScheme) argMap.get("genScheme");
            }
            if (argMap.get("selScheme") != null) {
//                this.selScheme = (SelectionScheme) argMap.get("selScheme");
            }
            if (argMap.get("numOfAggregatorsON") != null) {
                this.numOfAggregatorsON = Integer.parseInt((String) argMap.get("numOfAggregatorsON"));
            }
            if (argMap.get("numOfDisseminatorsON") != null) {
                this.numOfDisseminatorsON = Integer.parseInt((String) argMap.get("numOfDisseminatorsON"));
            }
            if (argMap.get("delay_low") != null) {
                this.delay_low = Integer.parseInt((String) argMap.get("delay_low"));
            }
            if (argMap.get("delay_high") != null) {
                this.delay_high = Integer.parseInt((String) argMap.get("delay_high"));
            }
            if (argMap.get("myIP") != null) {
                this.myIP = (String) argMap.get("myIP");
            }

            // Protopeer Bootstrap server
            if (argMap.get("bootstrapIP") != null)
            {
                this.bootstrapIP = (String) argMap.get("bootstrapIP");
                System.out.printf("ConfigurationObject: bootstrapIP overide -> %s\n", bootstrapIP );
            }
            if (argMap.get("bootstrapPort") != null)
            {
                this.bootstrapPort = Integer.parseInt((String) argMap.get("bootstrapPort"));
                System.out.printf("ConfigurationObject: bootstrapPort overide -> %d\n", bootstrapPort );
            }

            // device gateway, so that devices can obtain IP address of peers
            if (argMap.get("deviceGatewayIP") != null) {
                this.deviceGatewayIP = (String) argMap.get("deviceGatewayIP");
            }
            if (argMap.get("deviceGatewayPort") != null) {
                this.deviceGatewayPort = Integer.parseInt((String) argMap.get("deviceGatewayPort"));
            }

            // Persistence Daemon server, for logging to Postgres
            if (argMap.get("persistenceDaemonIP") != null) {
                this.persistenceDaemonIP = (String) argMap.get("persistenceDaemonIP");
            }
            if (argMap.get("persistenceDaemonPort") != null) {
                this.persistenceDaemonPort = Integer.parseInt((String) argMap.get("persistenceDaemonPort"));
            }

            // Logging options, specifies what is logged to Postgres
            if (argMap.get("persistenceActive") != null)
            {
                if( argMap.get("persistenceActive").equals("True") )
                    this.persistenceActive = true;
                else
                    this.persistenceActive = false;
            }

            if (argMap.get("persistMessages") != null)
            {
                if( argMap.get("persistMessages").equals("True") )
                    this.persistMessages = true;
                else
                    this.persistMessages = false;
            }

            if (argMap.get("eventLogging") != null)
            {
                if( argMap.get("eventLogging").equals("True") )
                    this.eventLogging = true;
                else
                    this.eventLogging = false;
            }

            if (argMap.get("vizPersistence") != null)
            {
                if( argMap.get("vizPersistence").equals("True") )
                    this.vizPersistence = true;
                else
                    this.vizPersistence = false;
            }

            if (argMap.get("persistAggregationEvent") != null)
            {
                if( argMap.get("persistAggregationEvent").equals("True") )
                    this.persistAggregationEvent = true;
                else
                    this.persistAggregationEvent = false;
            }

            if (argMap.get("persistPSSSamples") != null)
            {
                if( argMap.get("persistPSSSamples").equals("True") )
                    this.persistPSSSamples = true;
                else
                    this.persistPSSSamples = false;
            }

            if (argMap.get("rawLog") != null)
            {
                if( argMap.get("rawLog").equals("True") )
                    this.rawLog = true;
                else
                    this.rawLog = false;
            }

            if (argMap.get("rawLogLevel") != null)
            {
                this.rawLogLevel = Integer.parseInt((String) argMap.getProperty("rawLogLevel"));
            }


            if (argMap.get("skipTransitions") != null) {
                this.skipTransitions = Integer.parseInt((String) argMap.getProperty("skipTransitions"));
            }
            if (argMap.get("enablePSSlogging") != null) {
                this.enablePSSlogging = (Boolean) argMap.get("enablePSSlogging");
            }
            if (argMap.get("enableDIASmsgsLogging") != null) {
                this.enableDIASmsgsLogging = (Boolean) argMap.get("enableDIASmsgsLogging");
            }
            if(argMap.get("topKbufferSize") != null) {
                this.topKbufferSize = Integer.parseInt((String) argMap.getProperty("topKbufferSize"));
            }
            if(argMap.get("enableJsons") != null) {
                this.enableJsons = (Boolean) argMap.get("enableJsons");
            }



        } catch (Exception e) {
            e.printStackTrace();
        }

        //put back variables to protpopeeer

    }

    public void printParameterFile() {
        String delimiter = System.getProperty("line.separator");
        StringBuilder sb = new StringBuilder();


        sb.append("# SensorDescription Parameters" + delimiter);
        sb.append("hasSensorDescription=" + this.hasSensorDescription + delimiter) ;
        sb.append("sensorName=" + this.sensorName + delimiter);
        sb.append("sensorType=" + this.sensorType+ delimiter);
        sb.append("diasNetworkId=" + this.diasNetworkId + delimiter);


        sb.append("# Simulation Parameters" + delimiter);
        sb.append("Peer index=" + myIndex + delimiter);
        sb.append("Runtime [ms]=" + runDuration*1000 + delimiter);
        sb.append("Number of nodes=" + N + delimiter);
        sb.append(delimiter);

        sb.append("# Peer sampling Service Parameters" + delimiter);
        sb.append("View length=" + c + delimiter);
        sb.append("Healing parameter=" + H + delimiter);
        sb.append("Swap parameter=" + S + delimiter);
        String policy = "";
//        if (viewPropagationPolicy.equals(ViewPropagationPolicy.PUSH)) {
//            policy = "PUSH";
//        } else if (viewPropagationPolicy.equals(ViewPropagationPolicy.PUSHPULL)) {
//            policy = "PUSH-PULL";
//        }
//        sb.append("View propagation policy=" + policy + delimiter);
//        sb.append("Peer selection policy=" + peerSelectionPolicy.toString() + delimiter);
        sb.append("PSS Communication period [ms]=" + Tpss + delimiter);
        sb.append("Age increment [ms]=" + A + delimiter);
        sb.append("Bootstrap PSS time [ms]=" + Tbootpss + delimiter);
        sb.append(delimiter);

        sb.append("# DIAS Service Parameterization" + delimiter);
        sb.append("DIAS communication period [ms]=" + Tdias + delimiter);
        sb.append("DIAS sampling period (Tsampling) [ms]=" + Tsampling + delimiter);
        sb.append("Sampling size=" + sampleSize + delimiter);
        sb.append("Number of sessions=" + numOfSessions + delimiter);
        sb.append("Unexploited buffer size=" + unexploitedSize + delimiter);
        sb.append("Outdated buffer size=" + outdatedSize + delimiter);
        sb.append("Exploited buffer size=" + exploitedSize + delimiter);
//        sb.append("Aggregation strategy=" + strategy.toString() + delimiter);
//        sb.append("AMS type=" + amsType.toString() + delimiter);
        String hashType = "";
//        switch (amsHashType) {
//            case 1:
//                hashType = "DEFAULT_HASH";
//                break;
//            case 2:
//                hashType = "DOUBLE_HASH";
//                break;
//            case 3:
//                hashType = "TRIPLE_HASH";
//                break;
//        }
        sb.append("AMS hashing type=" + hashType + delimiter);
        sb.append("AMS target number of elements =" + ams_n + delimiter);
        sb.append("AMS target false positive probability = " + ams_fp + delimiter);

//        switch (dmaHashType) {
//            case 1:
//                hashType = "DEFAULT_HASH";
//                break;
//            case 2:
//                hashType = "DOUBLE_HASH";
//                break;
//            case 3:
//                hashType = "TRIPLE_HASH";
//                break;
//        }
        sb.append("DMA hashing type=" + hashType + delimiter);
        sb.append("DMA target number of elements =" + dma_n + delimiter);
        sb.append("DMA target false positive probability = " + dma_fp + delimiter);


//        switch (amdHashType) {
//            case 1:
//                hashType = "DEFAULT_HASH";
//                break;
//            case 2:
//                hashType = "DOUBLE_HASH";
//                break;
//            case 3:
//                hashType = "TRIPLE_HASH";
//                break;
//        }
        sb.append("AMD hashing type=" + hashType + delimiter);
        sb.append("AMD target number of elements=" + amd_n + delimiter);
        sb.append("AMD target false positive probability = " + amd_fp + delimiter);


//        switch (smaHashType) {
//            case 1:
//                hashType = "DEFAULT_HASH";
//                break;
//            case 2:
//                hashType = "DOUBLE_HASH";
//                break;
//            case 3:
//                hashType = "TRIPLE_HASH";
//                break;
//        }
        sb.append("SMA hashing type=" + hashType + delimiter);
        sb.append("SMA target number of elements=" + sma_n + delimiter);
        sb.append("SMA target false positive probability = " + sma_fp + delimiter);

        sb.append(delimiter);

        sb.append("# DIAS application Parameterization" + delimiter);
        sb.append("Aggregation type=" + type.toString() + delimiter);
        sb.append("Application bootstrap time=" + Tboot + delimiter);
        sb.append("Aggregation period [ms]=" + Taggr + delimiter);
        sb.append("Number of possible states=" + k + delimiter);
        sb.append("Minimal input value=" + minValueDomain + delimiter);
        sb.append("Maximal input value=" + maxValueDomain + delimiter);
        sb.append("Time transition probability=" + Pt + delimiter);
        sb.append("Parameter transition probability=" + Ps + delimiter);
        sb.append("State transitions period [ms]=" + t + delimiter);

//        sb.append("Generation scheme=" + genScheme.toString() + delimiter);
//        sb.append("Selection scheme=" + selScheme.toString() + delimiter);
        sb.append(delimiter);

        // sb.append("Active aggregator number=" + numOfAggregatorsON + delimiter);
        // sb.append("Active disseminator number=" + numOfDisseminatorsON + delimiter);
        sb.append("Minimal delay time [ms]=" + delay_low + delimiter);
        sb.append("Maximal delay time [ms]=" + delay_high + delimiter);
        sb.append("Top K Buffer Size=" + topKbufferSize + delimiter);
        sb.append(delimiter);

        sb.append("# Connectivity" + delimiter);
        sb.append("My IP address=" + myIP + delimiter);
        sb.append("My port number=" + myPort + delimiter);

        sb.append("Bootstrap server IP address=" + bootstrapIP + delimiter);
        sb.append("Bootstrap server port number=" + bootstrapPort + delimiter);

        sb.append("Device gateway IP address=" + deviceGatewayIP + delimiter);
        sb.append("Device gateway port number=" + deviceGatewayPort + delimiter);

        sb.append("Persistence Daemon IP address=" + persistenceDaemonIP + delimiter);
        sb.append("Persistence Daemon port number=" + persistenceDaemonPort + delimiter);

        sb.append(delimiter);
        sb.append("# Logging" + delimiter);
        sb.append("Logging::persistenceActive=" + persistenceActive + delimiter);
        sb.append("Logging::persistMessages=" + persistMessages + delimiter);
        sb.append("Logging::eventLogging=" + eventLogging + delimiter);
        sb.append("Logging::vizPersistence=" + vizPersistence + delimiter);
        sb.append("Logging::persistAggregationEvent=" + persistAggregationEvent + delimiter);
        sb.append("Logging::persistPSSSamples=" + persistPSSSamples + delimiter);
        sb.append("Logging::rawLog=" + rawLog + delimiter);
        sb.append("Logging::rawLogLevel=" + rawLogLevel + delimiter);


        sb.append(delimiter);
        sb.append("# Flags:");
        sb.append("PSS logging=" + enablePSSlogging + delimiter);
        sb.append("Allows every Xth transition=" + skipTransitions + delimiter);

        System.out.println(sb.toString());
        FileWriter out;
        try {
            new File("params/numOfAggregators/").mkdirs();
            out = new FileWriter("params/numOfAggregators/" + name + "_" + myIndex + ".txt", false);
            out.write(sb.toString());
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
