/*
 * Copyright (C) 2015 Evangelos Pournaras
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package agents;

import dsutil.generic.state.ArithmeticListState;
import dsutil.generic.state.ArithmeticState;
import dsutil.generic.state.State;
import dsutil.protopeer.FingerDescriptor;
import dsutil.protopeer.services.topology.trees.TreeApplicationInterface;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.TreeMap;
import messages.EPOSBroadcast;
import messages.EPOSRequest;
import messages.EPOSResponse;
import org.joda.time.DateTime;
import protopeer.BasePeerlet;
import protopeer.Finger;
import protopeer.Peer;
import protopeer.measurement.MeasurementFileDumper;
import protopeer.measurement.MeasurementLog;
import protopeer.measurement.MeasurementLoggerListener;
import protopeer.network.Message;
import protopeer.time.Timer;
import protopeer.time.TimerListener;
import protopeer.util.quantities.Time;

/**
 *
 * @author Evangelos
 */
public class EPOSAgent extends BasePeerlet implements TreeApplicationInterface {
    private String experimentID;
    private String plansLocation;
    private String planConfigurations;
    private String treeStamp;
    private String agentMeterID;
    private DateTime aggregationPhase;
    private List<DateTime> coordinationPhases;
    private int coordinationPhaseIndex;
    private String plansFormat;
    private DateTime historicAggregationPhase;
    private MeasurementFileDumper measurementDumper;
    public static enum Measurements{
        COORDINATION_PHASE,
        PLAN_SIZE,
        SELECTED_PLAN_VALUE,
        DISCOMFORT,
        ROBUSTNESS,
        GLOBAL_PLAN
    }
    
    public static enum TopologicalState{
        ROOT,
        LEAF,
        IN_TREE,
        DISCONNECTED
    }
    public static enum FitnessFunction{
        MINIMIZING_PRODUCT_SUM,
        MINIMIZING_DEVIATIONS,
        MINIMIZING_RELATIVE_DEVIATIONS,
        REVERSING_DEVIATIONS,
        MINIMIZING_CORRELATION,
        MAXIMIZING_LOAD_FACTOR,
        MINIMIZING_DEMAND,
        MAXIMIZING_DEMAND,
        MAXIMIZING_ENTROPY,
        MINIMIZING_ERROR,
        MAXIMIZING_CORRELATION,
        RANDOM_SELECTION,
        MINIMIZE_DISCOMFORT,
        MATCHING_UPPER_BOUND_1,
        MATCHING_UPPER_BOUND_2,
    }
    public static enum EnergyPlanType{
        POSSIBLE_PLAN,
        COMBINATIONAL_PLAN,
        AGGREGATE_PLAN,
        GLOBAL_PLAN,
    }
    public static enum EnergyPlanInformation{
        TYPE,
        COORDINATION_PHASE,
        DISCOMFORT,
        AGENT_METER_ID,
        CONFIGURATION
    }
    public static enum HistoricEnergyPlans{
        SELECTED_PLAN,
        AGGREGATE_PLAN,
        GLOBAL_PLAN
    }
    private double robustness;
    private int energyPlanSize;
    private int historySize;
    private FingerDescriptor myAgentDescriptor;
    private Finger parent=null;
    private List<Finger> children=new ArrayList<Finger>();
    private TopologicalState topologicalState;
    private FitnessFunction fitnessFunction;
    private ArithmeticListState patternEnergyPlan;
    private List<ArithmeticListState> possiblePlans;
    private ArithmeticListState selectedPlan;
    private ArithmeticListState aggregatePlan;
    private ArithmeticListState globalPlan;
    private ArithmeticListState historicSelectedPlan;
    private ArithmeticListState historicAggregatePlan;
    private ArithmeticListState historicGlobalPlan;
    private List<ArithmeticListState> combinationalPlans;
    private Map<ArithmeticListState, Map<Finger,ArithmeticListState>> combinationalPlansMap; 
    private ArithmeticListState selectedCombinationalPlan;
    private TreeMap<DateTime,Map<HistoricEnergyPlans,ArithmeticListState>> historicEnergyPlans;
    private Map<Finger,EPOSRequest> messageBuffer;
    
    public EPOSAgent(String experimentID, String plansLocation, String planConfigurations, String treeStamp, String agentMeterID, String plansFormat, FitnessFunction fitnessFunction, int planSize, DateTime aggregationPhase, DateTime historicAggregationPhase, ArithmeticListState patternEnergyPlan, int historySize){
        this.experimentID=experimentID;
        this.plansLocation=plansLocation;
        this.planConfigurations=planConfigurations;
        this.treeStamp=treeStamp;
        this.agentMeterID=agentMeterID;
        this.aggregationPhase=aggregationPhase;
        this.plansFormat=plansFormat;
        this.fitnessFunction=fitnessFunction;
        this.energyPlanSize=planSize;
        this.historicAggregationPhase=historicAggregationPhase;
        this.patternEnergyPlan=patternEnergyPlan;
        this.historySize=historySize;
        this.coordinationPhases=new ArrayList<DateTime>();
        this.coordinationPhaseIndex=0;
        this.possiblePlans=new ArrayList<ArithmeticListState>();
        this.combinationalPlans=new ArrayList<ArithmeticListState>();
        this.combinationalPlansMap=new HashMap<ArithmeticListState, Map<Finger,ArithmeticListState>>();
        this.historicEnergyPlans=new TreeMap<DateTime,Map<HistoricEnergyPlans,ArithmeticListState>>();
        this.messageBuffer=new HashMap<Finger,EPOSRequest>();
        this.topologicalState=TopologicalState.DISCONNECTED;
    }
    
    /**
    * Intitializes the load management agent by creating the finger descriptor.
    *
    * @param peer the local peer
    */
    @Override
    public void init(Peer peer){
        super.init(peer);
        this.myAgentDescriptor=new FingerDescriptor(getPeer().getFinger());
        this.loadCoordinationPhases();
    }

    /**
    * Starts the load management agent by scheduling the epoch measurements and 
    * defining its network state
    */
    @Override
    public void start(){
        this.runBootstrap();
        scheduleMeasurements();
    }

    /**
    * Stops the load management agent
    */
    @Override
    public void stop(){
        
    }
    
    /**
     * The scheduling of the active state. Computes the output load and sends the
     * load to the network. It is executed periodically. 
     */
    private void runBootstrap(){
        Timer loadAgentTimer= getPeer().getClock().createNewTimer();
        loadAgentTimer.addTimerListener(new TimerListener(){
            public void timerExpired(Timer timer){
                runActiveState();
            }
        });
        loadAgentTimer.schedule(Time.inMilliseconds(2000));
    }
    
    boolean hasBroadcast=false;
    /**
     * The scheduling of the active state. Computes the output load and sends the
     * load to the network. It is executed periodically. 
     */
    private void runActiveState(){
        Timer loadAgentTimer= getPeer().getClock().createNewTimer();
        loadAgentTimer.addTimerListener(new TimerListener(){
            public void timerExpired(Timer timer){
//                System.out.println(coordinationPhaseIndex);
                if(coordinationPhaseIndex<coordinationPhases.size()){
                    clearCoordinationPhase();
                    if(topologicalState==TopologicalState.LEAF){
                        plan();
                        informParent();
                    }
                    runActiveState();
                }
            }
        });
        loadAgentTimer.schedule(Time.inMilliseconds(1000));
    }
    
    private void clearCoordinationPhase(){
        if(this.historicEnergyPlans.size()>this.historySize){
            this.historicEnergyPlans.remove(this.historicEnergyPlans.firstKey());
        }
        aggregationPhase=coordinationPhases.get(coordinationPhaseIndex);
        if(coordinationPhaseIndex!=0){
            historicAggregationPhase=coordinationPhases.get(coordinationPhaseIndex-1);
        }
        coordinationPhaseIndex++;
        this.robustness=0.0;
        this.possiblePlans.clear();
        this.selectedPlan=this.setupEnergyPlan(EnergyPlanType.POSSIBLE_PLAN, aggregationPhase, energyPlanSize);
        this.aggregatePlan=this.setupEnergyPlan(EnergyPlanType.AGGREGATE_PLAN, aggregationPhase, energyPlanSize);
        this.globalPlan=this.setupEnergyPlan(EnergyPlanType.GLOBAL_PLAN, aggregationPhase, energyPlanSize);
        this.historicSelectedPlan=this.setupEnergyPlan(EnergyPlanType.POSSIBLE_PLAN, aggregationPhase, energyPlanSize);
        this.historicAggregatePlan=this.setupEnergyPlan(EnergyPlanType.AGGREGATE_PLAN, aggregationPhase, energyPlanSize);
        this.historicGlobalPlan=this.setupEnergyPlan(EnergyPlanType.GLOBAL_PLAN, aggregationPhase, energyPlanSize);
        this.combinationalPlans.clear();
        this.combinationalPlansMap.clear();
        this.selectedCombinationalPlan=this.setupEnergyPlan(EnergyPlanType.COMBINATIONAL_PLAN, aggregationPhase, energyPlanSize);
        this.messageBuffer.clear();
    }
    
    private void loadCoordinationPhases(){
        File agentDirectory = new File(this.plansLocation+"/"+this.planConfigurations+"/"+this.agentMeterID);
        File[] dates = agentDirectory.listFiles(new FileFilter() {  
            public boolean accept(File pathname) { 
                if(pathname.isHidden()){
                    return false;
                }
                return pathname.isFile();  
            }  
        });
        for(int i=0;i<dates.length;i++){
            StringTokenizer dateTokenizer=new StringTokenizer(dates[i].getName(), ".");
            this.coordinationPhases.add(DateTime.parse(dateTokenizer.nextToken()));
        }
    }
    
    public void plan(){
        try {
            File file = new File(this.plansLocation+"/"+this.planConfigurations+"/"+this.agentMeterID+"/"+this.aggregationPhase.toString("yyyy-MM-dd")+this.plansFormat);
            Scanner scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
               String line=scanner.nextLine();
               StringTokenizer planDiscomfortTokenizer = new StringTokenizer(line, ":");
               String discomfortToken=planDiscomfortTokenizer.nextToken();
               double discomfort=1.0-Double.parseDouble(discomfortToken);
               String planToken=planDiscomfortTokenizer.nextToken();
               StringTokenizer planTokenizer=new StringTokenizer(planToken, ",");
               ArithmeticListState possiblePlan=new ArithmeticListState(new ArrayList<ArithmeticState>());
               possiblePlan.addProperty(EnergyPlanInformation.DISCOMFORT, discomfort);
               possiblePlan.addProperty(EnergyPlanInformation.COORDINATION_PHASE, this.aggregationPhase);
               while(planTokenizer.hasMoreTokens()){
                   String value=planTokenizer.nextToken();
                   try{
                       double consumption=Double.parseDouble(value);
                       ArithmeticState consumptionState=new ArithmeticState(consumption);
                       possiblePlan.addArithmeticState(consumptionState);
                   }
                   catch(NumberFormatException ex){
                       System.out.println(file.toString());
                       System.out.println(value);
                       ex.printStackTrace();
                   }
               }
               this.possiblePlans.add(possiblePlan);
            }
            scanner.close();
        } 
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
    
    public void informParent(){
        EPOSRequest request=new EPOSRequest();
        request.child=getPeer().getFinger();
        request.possiblePlans=this.possiblePlans;
        request.aggregatePlan=this.aggregatePlan;
        if(this.historicEnergyPlans.size()!=0){
            Map<HistoricEnergyPlans,ArithmeticListState> historicPlans=this.historicEnergyPlans.get(this.historicAggregationPhase);
            request.aggregateHistoryPlan=historicPlans.get(HistoricEnergyPlans.AGGREGATE_PLAN);
        }
        else{
            request.aggregateHistoryPlan=null;
        }
        this.getPeer().sendMessage(this.parent.getNetworkAddress(), request);
    }
    
    public void preProcessing(){
        int complexity=1;
        ArrayList<Integer> inputPossiblePlanIndices=new ArrayList<Integer>();
        ArrayList<Integer> numberOfInputPossiblePlans=new ArrayList<Integer>();
        for(Finger child:children){
            this.aggregatePlan=sumPlans(aggregatePlan, this.messageBuffer.get(child).aggregatePlan);
            inputPossiblePlanIndices.add(0);
            numberOfInputPossiblePlans.add(this.messageBuffer.get(child).possiblePlans.size());
            complexity*=this.messageBuffer.get(child).possiblePlans.size();
        }
        for(int i=0;i<complexity;i++){
            ArithmeticListState combinationalPlan=this.setupEnergyPlan(EnergyPlanType.COMBINATIONAL_PLAN, aggregationPhase, energyPlanSize);
            HashMap<Finger,ArithmeticListState> inputPossiblePlans=new HashMap<Finger,ArithmeticListState>();
            for(int c=0;c<this.children.size();c++){
                Finger child=this.children.get(c);
                List<ArithmeticListState> possiblePlans=this.messageBuffer.get(child).possiblePlans;
                ArithmeticListState inputPossiblePlan=possiblePlans.get(inputPossiblePlanIndices.get(c));
                combinationalPlan=this.sumPlans(combinationalPlan, inputPossiblePlan);
                double discomfort=((Double)combinationalPlan.getProperty(EnergyPlanInformation.DISCOMFORT)).doubleValue()+((Double)inputPossiblePlan.getProperty(EnergyPlanInformation.DISCOMFORT)).doubleValue();
                combinationalPlan.addProperty(EnergyPlanInformation.DISCOMFORT, discomfort);
                inputPossiblePlans.put(child, inputPossiblePlan);
            }
            this.combinationalPlans.add(combinationalPlan);
            this.combinationalPlansMap.put(combinationalPlan, inputPossiblePlans);
            int lastInputPossiblePlanIndex=inputPossiblePlanIndices.size()-1;
            inputPossiblePlanIndices.set(lastInputPossiblePlanIndex, inputPossiblePlanIndices.get(lastInputPossiblePlanIndex)+1);
            for(int j=lastInputPossiblePlanIndex;j>0;j--){
                if(inputPossiblePlanIndices.get(j)>=numberOfInputPossiblePlans.get(j)){
                    inputPossiblePlanIndices.set(j, 0);
                    inputPossiblePlanIndices.set(j-1, inputPossiblePlanIndices.get(j-1)+1);
                }
            }
        }
    }
    
    public void select(){
        switch(this.fitnessFunction){
            case MINIMIZING_PRODUCT_SUM:
                this.minimizeProductSum();
                break;
            case MINIMIZING_DEVIATIONS:
                this.minimizeDeviations();
                break;
            case MINIMIZING_RELATIVE_DEVIATIONS:
                this.minimizeRelativeDeviations();
                break;
            case REVERSING_DEVIATIONS:
                if(this.coordinationPhaseIndex==1){
                    this.randomSelection();
                }
                else{
                    this.reverseDeviations();
                }
                break;
            case MINIMIZING_CORRELATION:
                if(this.coordinationPhaseIndex==1){
                    this.randomSelection();
                }
                else{
                    this.minimizeCorrelation();
                }
                break;
            case MINIMIZING_ERROR:
                this.matchRootMeanSquaredErrors();
                break;
            case MAXIMIZING_CORRELATION:
                this.maximizeCorrelation();
                break;
            case MINIMIZING_DEMAND:
                this.minimizeDemand();
                break;
            case MAXIMIZING_DEMAND:
                this.maximizeDemand();
                break;
            case MAXIMIZING_ENTROPY:
                this.maximizeEntropy();
                break;
            case MAXIMIZING_LOAD_FACTOR:
                this.maximizeLoadFactor();
                break;
            case RANDOM_SELECTION:
                this.randomSelection();
                break;
            case MINIMIZE_DISCOMFORT:
                this.minimizeDiscomfort();
                break;
            case MATCHING_UPPER_BOUND_1:
                this.matchUpperBound1();
                break;
            case MATCHING_UPPER_BOUND_2:
                this.matchUpperBound2();
                break;
            default:
                // Something wrong!
        }
    }
    
    public void update(){
        this.aggregatePlan=this.sumPlans(this.aggregatePlan, this.selectedCombinationalPlan);
        this.historicAggregatePlan=this.sumPlans(this.historicAggregatePlan, aggregatePlan);
    }
    
    public void informChildren(){
        for(Finger child:children){
            ArithmeticListState selectedPlan=this.combinationalPlansMap.get(this.selectedCombinationalPlan).get(child);
            EPOSResponse response=new EPOSResponse();
            response.selectedPlan=selectedPlan;
            getPeer().sendMessage(child.getNetworkAddress(), response);
        }
    }
    
    public void broadcast(){
        EPOSBroadcast broadcast=new EPOSBroadcast();
        broadcast.coordinationPhase=(DateTime)this.globalPlan.getProperty(EnergyPlanInformation.COORDINATION_PHASE);
        broadcast.globalPlan=this.globalPlan;
        for(Finger child:children){
            getPeer().sendMessage(child.getNetworkAddress(), broadcast);
        }
    }
    
    private void randomSelection(){
        this.selectedCombinationalPlan=combinationalPlans.get((int)(Math.random()*combinationalPlans.size()));
    }
    
    private void randomSelectionRoot(){
        ArithmeticListState selectedPossiblePlan=this.possiblePlans.get((int)(Math.random()*this.possiblePlans.size()));
        this.selectedPlan=sumPlans(this.selectedPlan, selectedPossiblePlan);
        this.selectedPlan.addProperty(EnergyPlanInformation.DISCOMFORT, ((Double)selectedPossiblePlan.getProperty(EnergyPlanInformation.DISCOMFORT)).doubleValue());
        this.historicSelectedPlan=this.sumPlans(this.historicSelectedPlan, selectedPossiblePlan);
    }
    
    private void minimizeDemand(){
        double minDemand=Double.MAX_VALUE;
        for(ArithmeticListState combinationalPlan:combinationalPlans){
            double demand=this.getAverage(combinationalPlan.getArithmeticStates());
            if(demand<minDemand){
                minDemand=demand;
                this.selectedCombinationalPlan=combinationalPlan;
            }
        }
    }
    
    private void minimizeDemandRoot(){
        ArithmeticListState rootSelectedPlan=null;
        double minDemand=Double.MAX_VALUE;
        for(ArithmeticListState possiblePlan:possiblePlans){
            double demand=this.getAverage(possiblePlan.getArithmeticStates());
            if(demand<minDemand){
                minDemand=demand;
                rootSelectedPlan=possiblePlan;
            }
        }
        this.selectedPlan=this.sumPlans(this.selectedPlan, rootSelectedPlan);
        this.selectedPlan.addProperty(EnergyPlanInformation.DISCOMFORT, ((Double)rootSelectedPlan.getProperty(EnergyPlanInformation.DISCOMFORT)).doubleValue());
        this.historicSelectedPlan=this.sumPlans(this.historicSelectedPlan, rootSelectedPlan);
    }
    
    private void maximizeDemand(){
        double maxDemand=Double.MIN_VALUE;
        for(ArithmeticListState combinationalPlan:combinationalPlans){
            double demand=this.getAverage(combinationalPlan.getArithmeticStates());
            if(demand>maxDemand){
                maxDemand=demand;
                this.selectedCombinationalPlan=combinationalPlan;
            }
        }
    }
    
    private void maximizeDemandRoot(){
        ArithmeticListState rootSelectedPlan=null;
        double maxDemand=Double.MIN_VALUE;
        for(ArithmeticListState possiblePlan:possiblePlans){
            double demand=this.getAverage(possiblePlan.getArithmeticStates());
            if(demand>maxDemand){
                maxDemand=demand;
                rootSelectedPlan=possiblePlan;
            }
        }
        this.selectedPlan=this.sumPlans(this.selectedPlan, rootSelectedPlan);
        this.selectedPlan.addProperty(EnergyPlanInformation.DISCOMFORT, ((Double)rootSelectedPlan.getProperty(EnergyPlanInformation.DISCOMFORT)).doubleValue());
        this.historicSelectedPlan=this.sumPlans(this.historicSelectedPlan, rootSelectedPlan);
    }
    
    private void maximizeEntropy(){
        double maxEntropy=-Double.MAX_VALUE;
        for(ArithmeticListState combinationalPlan:combinationalPlans){
            ArithmeticListState testAggregatePlan=this.setupEnergyPlan(EnergyPlanType.AGGREGATE_PLAN, aggregationPhase, energyPlanSize);
            testAggregatePlan=this.sumPlans(testAggregatePlan, aggregatePlan);
            testAggregatePlan=this.sumPlans(testAggregatePlan, combinationalPlan);
            double entropy=this.getEntropy(testAggregatePlan.getArithmeticStates());
            if(entropy>maxEntropy){
                maxEntropy=entropy;
                this.selectedCombinationalPlan=combinationalPlan;
            }
        }
    }
    
    private void maximizeEntropyRoot(){
        ArithmeticListState rootSelectedPlanMD=null;
        double maxEntropy=-Double.MAX_VALUE;
        for(ArithmeticListState possiblePlan:possiblePlans){
            ArithmeticListState testAggregatePlan=this.setupEnergyPlan(EnergyPlanType.AGGREGATE_PLAN, aggregationPhase, energyPlanSize);
            testAggregatePlan=this.sumPlans(testAggregatePlan, aggregatePlan);
            testAggregatePlan=this.sumPlans(testAggregatePlan, possiblePlan);
            double entropy=this.getEntropy(testAggregatePlan.getArithmeticStates());
            if(entropy>maxEntropy){
                maxEntropy=entropy;
                rootSelectedPlanMD=possiblePlan;
            }
        }
        this.selectedPlan=this.sumPlans(this.selectedPlan, rootSelectedPlanMD);
        this.selectedPlan.addProperty(EnergyPlanInformation.DISCOMFORT, ((Double)rootSelectedPlanMD.getProperty(EnergyPlanInformation.DISCOMFORT)).doubleValue());
        this.historicSelectedPlan=this.sumPlans(this.historicSelectedPlan, rootSelectedPlanMD);
    }
    
    private void minimizeDeviations(){
        double minStandardDeviation=Double.MAX_VALUE;
        for(ArithmeticListState combinationalPlan:combinationalPlans){
            ArithmeticListState testAggregatePlan=this.setupEnergyPlan(EnergyPlanType.AGGREGATE_PLAN, aggregationPhase, energyPlanSize);
            testAggregatePlan=this.sumPlans(testAggregatePlan, aggregatePlan);
            testAggregatePlan=this.sumPlans(testAggregatePlan, combinationalPlan);
            double standardDeviation=this.getStandardDeviation(testAggregatePlan.getArithmeticStates());
            if(standardDeviation<minStandardDeviation){
                minStandardDeviation=standardDeviation;
                this.selectedCombinationalPlan=combinationalPlan;
            }
        }
    }
    
    private void minimizeDeviationsRoot(){
        ArithmeticListState rootSelectedPlanMD=null;
        double minStandardDeviationMD=Double.MAX_VALUE;
        for(ArithmeticListState possiblePlan:possiblePlans){
            ArithmeticListState testAggregatePlan=this.setupEnergyPlan(EnergyPlanType.AGGREGATE_PLAN, aggregationPhase, energyPlanSize);
            testAggregatePlan=this.sumPlans(testAggregatePlan, aggregatePlan);
            testAggregatePlan=this.sumPlans(testAggregatePlan, possiblePlan);
            double standardDeviation=this.getStandardDeviation(testAggregatePlan.getArithmeticStates());
            if(standardDeviation<minStandardDeviationMD){
                minStandardDeviationMD=standardDeviation;
                rootSelectedPlanMD=possiblePlan;
            }
        }
        this.selectedPlan=this.sumPlans(this.selectedPlan, rootSelectedPlanMD);
        this.selectedPlan.addProperty(EnergyPlanInformation.DISCOMFORT, ((Double)rootSelectedPlanMD.getProperty(EnergyPlanInformation.DISCOMFORT)).doubleValue());
        this.historicSelectedPlan=this.sumPlans(this.historicSelectedPlan, rootSelectedPlanMD);
    }
    
    private void minimizeRelativeDeviations(){
        double minRelativeStandardDeviation=Double.MAX_VALUE;
        for(ArithmeticListState combinationalPlan:combinationalPlans){
            ArithmeticListState testAggregatePlan=this.setupEnergyPlan(EnergyPlanType.AGGREGATE_PLAN, aggregationPhase, energyPlanSize);
            testAggregatePlan=this.sumPlans(testAggregatePlan, aggregatePlan);
            testAggregatePlan=this.sumPlans(testAggregatePlan, combinationalPlan);
            double relativeStandardDeviation=this.getRelativeStandardDeviation(testAggregatePlan.getArithmeticStates());
            if(relativeStandardDeviation<minRelativeStandardDeviation){
                minRelativeStandardDeviation=relativeStandardDeviation;
                this.selectedCombinationalPlan=combinationalPlan;
            }
        }
    }
    
    private void minimizeRelativeDeviationsRoot(){
        ArithmeticListState rootSelectedPlanMRD=null;
        double minRelativeStandardDeviationMRD=Double.MAX_VALUE;
        for(ArithmeticListState possiblePlan:possiblePlans){
            ArithmeticListState testAggregatePlan=this.setupEnergyPlan(EnergyPlanType.AGGREGATE_PLAN, aggregationPhase, energyPlanSize);
            testAggregatePlan=this.sumPlans(testAggregatePlan, aggregatePlan);
            testAggregatePlan=this.sumPlans(testAggregatePlan, possiblePlan);
            double standardDeviation=this.getRelativeStandardDeviation(testAggregatePlan.getArithmeticStates());
            if(standardDeviation<minRelativeStandardDeviationMRD){
                minRelativeStandardDeviationMRD=standardDeviation;
                rootSelectedPlanMRD=possiblePlan;
            }
        }
        this.selectedPlan=this.sumPlans(this.selectedPlan, rootSelectedPlanMRD);
        this.selectedPlan.addProperty(EnergyPlanInformation.DISCOMFORT, ((Double)rootSelectedPlanMRD.getProperty(EnergyPlanInformation.DISCOMFORT)).doubleValue());
        this.historicSelectedPlan=this.sumPlans(this.historicSelectedPlan, rootSelectedPlanMRD);
    }
    
    private void minimizeCorrelation(){
        double minCorrelation=1.0;
        for(ArithmeticListState combinationalPlan:combinationalPlans){
            ArithmeticListState testAggregatePlan=this.setupEnergyPlan(EnergyPlanType.AGGREGATE_PLAN, aggregationPhase, energyPlanSize);
            testAggregatePlan=this.sumPlans(testAggregatePlan, aggregatePlan);
            testAggregatePlan=this.sumPlans(testAggregatePlan, combinationalPlan);
            ArithmeticListState historicAggregatePlan=this.historicEnergyPlans.get(this.historicAggregationPhase).get(HistoricEnergyPlans.AGGREGATE_PLAN);
            double correlation=this.getCorrelationCoefficient(testAggregatePlan.getArithmeticStates(), historicAggregatePlan.getArithmeticStates());
            if(correlation<minCorrelation){
                minCorrelation=correlation;
                this.selectedCombinationalPlan=combinationalPlan;
            }
        }
    }
    
    private void minimizeCorrelationRoot(){
        ArithmeticListState rootSelectedPlanMC=null;
        if(this.coordinationPhaseIndex==1){
            ArithmeticListState selectedPossiblePlan=this.possiblePlans.get((int)(Math.random()*this.possiblePlans.size()));
            this.selectedPlan=sumPlans(this.selectedPlan, selectedPossiblePlan);
            this.selectedPlan.addProperty(EnergyPlanInformation.DISCOMFORT, ((Double)selectedPossiblePlan.getProperty(EnergyPlanInformation.DISCOMFORT)).doubleValue());
            this.historicSelectedPlan=this.sumPlans(this.historicSelectedPlan, selectedPossiblePlan);
        }
        else{
            double minCorrelationMC=Double.MAX_VALUE;
            for(ArithmeticListState possiblePlan:possiblePlans){
                ArithmeticListState testAggregatePlan=this.setupEnergyPlan(EnergyPlanType.AGGREGATE_PLAN, aggregationPhase, energyPlanSize);
                testAggregatePlan=this.sumPlans(testAggregatePlan, aggregatePlan);
                testAggregatePlan=this.sumPlans(testAggregatePlan, possiblePlan);
                ArithmeticListState historicAggregatePlan=this.historicEnergyPlans.get(this.historicAggregationPhase).get(HistoricEnergyPlans.AGGREGATE_PLAN);
                double correlation=this.getCorrelationCoefficient(testAggregatePlan.getArithmeticStates(), historicAggregatePlan.getArithmeticStates());
                if(correlation<minCorrelationMC){
                    minCorrelationMC=correlation;
                    rootSelectedPlanMC=possiblePlan;
                }
            }
            this.selectedPlan=this.sumPlans(this.selectedPlan, rootSelectedPlanMC);
            this.selectedPlan.addProperty(EnergyPlanInformation.DISCOMFORT, ((Double)rootSelectedPlanMC.getProperty(EnergyPlanInformation.DISCOMFORT)).doubleValue());
            this.historicSelectedPlan=this.sumPlans(this.historicSelectedPlan, rootSelectedPlanMC);
        }
    }
    
    private void maximizeLoadFactor(){
        double maxLoadFactor=Double.MIN_VALUE;
        for(ArithmeticListState combinationalPlan:combinationalPlans){
            ArithmeticListState testAggregatePlan=this.setupEnergyPlan(EnergyPlanType.AGGREGATE_PLAN, aggregationPhase, energyPlanSize);
            testAggregatePlan=this.sumPlans(testAggregatePlan, aggregatePlan);
            testAggregatePlan=this.sumPlans(testAggregatePlan, combinationalPlan);
            double loadFactor=this.getAverage(testAggregatePlan.getArithmeticStates())/this.getMaximum(testAggregatePlan.getArithmeticStates());
            if(loadFactor>maxLoadFactor){
                maxLoadFactor=loadFactor;
                this.selectedCombinationalPlan=combinationalPlan;
            }
        }
    }
    
    private void maximizeLoadFactorRoot(){
        ArithmeticListState rootSelectedPlan=null;
        double maxLoadFactor=Double.MIN_VALUE;
        for(ArithmeticListState possiblePlan:possiblePlans){
            ArithmeticListState testAggregatePlan=this.setupEnergyPlan(EnergyPlanType.AGGREGATE_PLAN, aggregationPhase, energyPlanSize);
            testAggregatePlan=this.sumPlans(testAggregatePlan, aggregatePlan);
            testAggregatePlan=this.sumPlans(testAggregatePlan, possiblePlan);
            double loadFactor=this.getAverage(testAggregatePlan.getArithmeticStates())/this.getMaximum(testAggregatePlan.getArithmeticStates());
            if(loadFactor>maxLoadFactor){
                maxLoadFactor=loadFactor;
                rootSelectedPlan=possiblePlan;
            }
        }
        this.selectedPlan=this.sumPlans(this.selectedPlan, rootSelectedPlan);
        this.selectedPlan.addProperty(EnergyPlanInformation.DISCOMFORT, ((Double)rootSelectedPlan.getProperty(EnergyPlanInformation.DISCOMFORT)).doubleValue());
        this.historicSelectedPlan=this.sumPlans(this.historicSelectedPlan, rootSelectedPlan);
        
        
    }
    
    private void reverseDeviations(){
        double minStandardDeviation=Double.MAX_VALUE;
        for(ArithmeticListState combinationalPlan:combinationalPlans){
            ArithmeticListState testAggregatePlan=this.setupEnergyPlan(EnergyPlanType.AGGREGATE_PLAN, aggregationPhase, energyPlanSize);
            testAggregatePlan=this.sumPlans(testAggregatePlan, this.historicEnergyPlans.get(this.historicAggregationPhase).get(HistoricEnergyPlans.GLOBAL_PLAN));
            testAggregatePlan=this.substractPlans(testAggregatePlan, this.historicEnergyPlans.get(this.historicAggregationPhase).get(HistoricEnergyPlans.AGGREGATE_PLAN));
            testAggregatePlan=this.substractPlans(testAggregatePlan, this.historicEnergyPlans.get(this.historicAggregationPhase).get(HistoricEnergyPlans.SELECTED_PLAN)); 
            testAggregatePlan=this.sumPlans(testAggregatePlan, this.aggregatePlan);
            testAggregatePlan=this.sumPlans(testAggregatePlan, combinationalPlan);
            double standardDeviation=this.getStandardDeviation(testAggregatePlan.getArithmeticStates());
            if(standardDeviation<minStandardDeviation){
                minStandardDeviation=standardDeviation;
                this.selectedCombinationalPlan=combinationalPlan;
            }
        }
    }
    
    private void reverseDeviationsRoot(){
        ArithmeticListState rootSelectedPlanRD=null;
        if(this.coordinationPhaseIndex==1){
            ArithmeticListState selectedPossiblePlan=this.possiblePlans.get((int)(Math.random()*this.possiblePlans.size()));
            this.selectedPlan=sumPlans(this.selectedPlan, selectedPossiblePlan);
            this.selectedPlan.addProperty(EnergyPlanInformation.DISCOMFORT, ((Double)selectedPossiblePlan.getProperty(EnergyPlanInformation.DISCOMFORT)).doubleValue());
            this.historicSelectedPlan=this.sumPlans(this.historicSelectedPlan, selectedPossiblePlan);
        }
        else{
            double minStandardDeviationRD=Double.MAX_VALUE;
            for(ArithmeticListState possiblePlan:possiblePlans){
                ArithmeticListState testAggregatePlan=this.setupEnergyPlan(EnergyPlanType.AGGREGATE_PLAN, aggregationPhase, energyPlanSize);
                testAggregatePlan=this.sumPlans(testAggregatePlan, this.historicEnergyPlans.get(this.historicAggregationPhase).get(HistoricEnergyPlans.GLOBAL_PLAN));
                testAggregatePlan=this.substractPlans(testAggregatePlan, this.historicEnergyPlans.get(this.historicAggregationPhase).get(HistoricEnergyPlans.AGGREGATE_PLAN));
                testAggregatePlan=this.substractPlans(testAggregatePlan, this.historicEnergyPlans.get(this.historicAggregationPhase).get(HistoricEnergyPlans.SELECTED_PLAN)); 
                testAggregatePlan=this.sumPlans(testAggregatePlan, this.aggregatePlan);
                testAggregatePlan=this.sumPlans(testAggregatePlan, possiblePlan);
                double standardDeviation=this.getStandardDeviation(testAggregatePlan.getArithmeticStates());
                if(standardDeviation<minStandardDeviationRD){
                    minStandardDeviationRD=standardDeviation;
                    rootSelectedPlanRD=possiblePlan;
                }
            }
            this.selectedPlan=this.sumPlans(this.selectedPlan, rootSelectedPlanRD);
            this.selectedPlan.addProperty(EnergyPlanInformation.DISCOMFORT, ((Double)rootSelectedPlanRD.getProperty(EnergyPlanInformation.DISCOMFORT)).doubleValue());
            this.historicSelectedPlan=this.sumPlans(this.historicSelectedPlan, rootSelectedPlanRD);
        }
    }
    
    private void maximizeCorrelation(){
        double maxCorrelationCoefficient=-1.0;
        for(ArithmeticListState combinationalPlan:combinationalPlans){
            ArithmeticListState testAggregatePlan=this.setupEnergyPlan(EnergyPlanType.AGGREGATE_PLAN, aggregationPhase, energyPlanSize);
            testAggregatePlan=this.sumPlans(testAggregatePlan, aggregatePlan);
            testAggregatePlan=this.sumPlans(testAggregatePlan, combinationalPlan);
            double multiplicationFactor=this.getAverage(testAggregatePlan.getArithmeticStates())/this.getAverage(this.patternEnergyPlan.getArithmeticStates());
            ArithmeticListState normalizedPatternPlan=this.setupEnergyPlan(EnergyPlanType.GLOBAL_PLAN, aggregationPhase, energyPlanSize);
            normalizedPatternPlan=this.sumPlans(normalizedPatternPlan, patternEnergyPlan);
            normalizedPatternPlan=this.multiplyPlan(normalizedPatternPlan, multiplicationFactor);
            double correlationCoefficient=this.getCorrelationCoefficient(normalizedPatternPlan.getArithmeticStates(), testAggregatePlan.getArithmeticStates());
            if(correlationCoefficient>maxCorrelationCoefficient){
                maxCorrelationCoefficient=correlationCoefficient;
                this.selectedCombinationalPlan=combinationalPlan;
            }
        }
    }
    
    private void maximizeCorrelationRoot(){
        ArithmeticListState rootSelectedPlanMatchDev=null;
        double maxCorrelationCoefficient=-1.0;
        for(ArithmeticListState possiblePlan:possiblePlans){
            ArithmeticListState testAggregatePlan=this.setupEnergyPlan(EnergyPlanType.AGGREGATE_PLAN, aggregationPhase, energyPlanSize);
            testAggregatePlan=this.sumPlans(testAggregatePlan, aggregatePlan);
            testAggregatePlan=this.sumPlans(testAggregatePlan, possiblePlan);
            double multiplicationFactor=this.getAverage(testAggregatePlan.getArithmeticStates())/this.getAverage(this.patternEnergyPlan.getArithmeticStates());
            ArithmeticListState normalizedPatternPlan=this.setupEnergyPlan(EnergyPlanType.GLOBAL_PLAN, aggregationPhase, energyPlanSize);
            normalizedPatternPlan=this.sumPlans(normalizedPatternPlan, patternEnergyPlan);
            normalizedPatternPlan=this.multiplyPlan(normalizedPatternPlan, multiplicationFactor);
            double correlationCoefficient=this.getCorrelationCoefficient(normalizedPatternPlan.getArithmeticStates(), testAggregatePlan.getArithmeticStates());
            if(correlationCoefficient>maxCorrelationCoefficient){
                maxCorrelationCoefficient=correlationCoefficient;
                rootSelectedPlanMatchDev=possiblePlan;
            }
            
        }
        this.selectedPlan=this.sumPlans(this.selectedPlan, rootSelectedPlanMatchDev);
        this.selectedPlan.addProperty(EnergyPlanInformation.DISCOMFORT, ((Double)rootSelectedPlanMatchDev.getProperty(EnergyPlanInformation.DISCOMFORT)).doubleValue());
        this.historicSelectedPlan=this.sumPlans(this.historicSelectedPlan, rootSelectedPlanMatchDev);
    }
    
    private void minimizeProductSum(){
        double minProductSum=Double.MAX_VALUE;
        for(ArithmeticListState combinationalPlan:combinationalPlans){
            ArithmeticListState testAggregatePlan=this.setupEnergyPlan(EnergyPlanType.AGGREGATE_PLAN, aggregationPhase, energyPlanSize);
            testAggregatePlan=this.sumPlans(testAggregatePlan, aggregatePlan);
            testAggregatePlan=this.sumPlans(testAggregatePlan, combinationalPlan);
            ArithmeticListState normalizedPatternPlan=this.setupEnergyPlan(EnergyPlanType.GLOBAL_PLAN, aggregationPhase, energyPlanSize);
            normalizedPatternPlan=this.sumPlans(normalizedPatternPlan, patternEnergyPlan);
            testAggregatePlan=this.multiplyPlans(testAggregatePlan, normalizedPatternPlan);
            double productSum=this.getSum(testAggregatePlan.getArithmeticStates());
            if(productSum<minProductSum){
                minProductSum=productSum;
                this.selectedCombinationalPlan=combinationalPlan;
            }
        }
    }
    
    private void minimizeProductSumRoot(){
        ArithmeticListState rootSelectedPlanMinProdSum=null;
        double minProductSum=Double.MAX_VALUE;
        for(ArithmeticListState possiblePlan:possiblePlans){
            ArithmeticListState testAggregatePlan=this.setupEnergyPlan(EnergyPlanType.AGGREGATE_PLAN, aggregationPhase, energyPlanSize);
            testAggregatePlan=this.sumPlans(testAggregatePlan, aggregatePlan);
            testAggregatePlan=this.sumPlans(testAggregatePlan, possiblePlan);
            ArithmeticListState normalizedPatternPlan=this.setupEnergyPlan(EnergyPlanType.GLOBAL_PLAN, aggregationPhase, energyPlanSize);
            normalizedPatternPlan=this.sumPlans(normalizedPatternPlan, patternEnergyPlan);
            testAggregatePlan=this.multiplyPlans(testAggregatePlan, normalizedPatternPlan);
            double productSum=this.getSum(testAggregatePlan.getArithmeticStates());
            if(productSum<minProductSum){
                minProductSum=productSum;
                rootSelectedPlanMinProdSum=possiblePlan;
            }
        }
    }
    
    private void matchRootMeanSquaredErrors(){
        double minRootMeanSquaredError=Double.MAX_VALUE;
        for(ArithmeticListState combinationalPlan:combinationalPlans){
            ArithmeticListState testAggregatePlan=this.setupEnergyPlan(EnergyPlanType.AGGREGATE_PLAN, aggregationPhase, energyPlanSize);
            testAggregatePlan=this.sumPlans(testAggregatePlan, aggregatePlan);
            testAggregatePlan=this.sumPlans(testAggregatePlan, combinationalPlan);
            double multiplicationFactor=this.getAverage(testAggregatePlan.getArithmeticStates());///this.getAverage(this.patternEnergyPlan.getArithmeticStates());
            ArithmeticListState normalizedPatternPlan=this.setupEnergyPlan(EnergyPlanType.GLOBAL_PLAN, aggregationPhase, energyPlanSize);
            normalizedPatternPlan=this.sumPlans(normalizedPatternPlan, patternEnergyPlan);
            normalizedPatternPlan=this.multiplyPlan(normalizedPatternPlan, multiplicationFactor);
            double rootMeanSquaredError=this.getRootMeanSquareError(normalizedPatternPlan.getArithmeticStates(), testAggregatePlan.getArithmeticStates());
            if(rootMeanSquaredError<minRootMeanSquaredError){
                minRootMeanSquaredError=rootMeanSquaredError;
                this.selectedCombinationalPlan=combinationalPlan;
            }
        }
    }
    
    private void matchRootMeanSquaredErrorsRoot(){
        ArithmeticListState rootSelectedPlanMatchDev=null;
        double minRootMeanSquaredError=Double.MAX_VALUE;
        for(ArithmeticListState possiblePlan:possiblePlans){
            ArithmeticListState testAggregatePlan=this.setupEnergyPlan(EnergyPlanType.AGGREGATE_PLAN, aggregationPhase, energyPlanSize);
            testAggregatePlan=this.sumPlans(testAggregatePlan, aggregatePlan);
            testAggregatePlan=this.sumPlans(testAggregatePlan, possiblePlan);
            double multiplicationFactor=this.getAverage(testAggregatePlan.getArithmeticStates());///this.getAverage(this.patternEnergyPlan.getArithmeticStates());
            ArithmeticListState normalizedPatternPlan=this.setupEnergyPlan(EnergyPlanType.GLOBAL_PLAN, aggregationPhase, energyPlanSize);
            normalizedPatternPlan=this.sumPlans(normalizedPatternPlan, patternEnergyPlan);
            normalizedPatternPlan=this.multiplyPlan(normalizedPatternPlan, multiplicationFactor);
            double rootMeanSquaredError=this.getRootMeanSquareError(normalizedPatternPlan.getArithmeticStates(), testAggregatePlan.getArithmeticStates());
            if(rootMeanSquaredError<minRootMeanSquaredError){
                minRootMeanSquaredError=rootMeanSquaredError;
                rootSelectedPlanMatchDev=possiblePlan;
            }
            
        }
        this.selectedPlan=this.sumPlans(this.selectedPlan, rootSelectedPlanMatchDev);
        this.selectedPlan.addProperty(EnergyPlanInformation.DISCOMFORT, ((Double)rootSelectedPlanMatchDev.getProperty(EnergyPlanInformation.DISCOMFORT)).doubleValue());
        this.historicSelectedPlan=this.sumPlans(this.historicSelectedPlan, rootSelectedPlanMatchDev);
    }
    
    private void matchUpperBound1(){
        double minRootMeanSquaredError=Double.MAX_VALUE;
        for(ArithmeticListState combinationalPlan:combinationalPlans){
            ArithmeticListState testAggregatePlan=this.setupEnergyPlan(EnergyPlanType.AGGREGATE_PLAN, aggregationPhase, energyPlanSize);
            testAggregatePlan=this.sumPlans(testAggregatePlan, aggregatePlan);
            testAggregatePlan=this.sumPlans(testAggregatePlan, combinationalPlan);
            ArithmeticListState normalizedPatternPlan=this.setupEnergyPlan(EnergyPlanType.GLOBAL_PLAN, aggregationPhase, energyPlanSize);
            normalizedPatternPlan=this.sumPlans(normalizedPatternPlan, patternEnergyPlan);
            this.reversePlan(normalizedPatternPlan);
            double avgTestAggregatePlan=this.getAverage(testAggregatePlan.getArithmeticStates());
            double avgNormalizedPatternPlan=this.getAverage(normalizedPatternPlan.getArithmeticStates());
            this.multiplyPlan(normalizedPatternPlan, avgTestAggregatePlan/avgNormalizedPatternPlan);
            double rootMeanSquaredError=this.getRootMeanSquareError(normalizedPatternPlan.getArithmeticStates(), testAggregatePlan.getArithmeticStates());
            if(rootMeanSquaredError<minRootMeanSquaredError){
                minRootMeanSquaredError=rootMeanSquaredError;
                this.selectedCombinationalPlan=combinationalPlan;
            }
        }
    }
    
    private void matchUpperBound1Root(){
        ArithmeticListState rootSelectedPlanMatchDev=null;
        double minRootMeanSquaredError=Double.MAX_VALUE;
        for(ArithmeticListState possiblePlan:possiblePlans){
            ArithmeticListState testAggregatePlan=this.setupEnergyPlan(EnergyPlanType.AGGREGATE_PLAN, aggregationPhase, energyPlanSize);
            testAggregatePlan=this.sumPlans(testAggregatePlan, aggregatePlan);
            testAggregatePlan=this.sumPlans(testAggregatePlan, possiblePlan);
            ArithmeticListState normalizedPatternPlan=this.setupEnergyPlan(EnergyPlanType.GLOBAL_PLAN, aggregationPhase, energyPlanSize);
            normalizedPatternPlan=this.sumPlans(normalizedPatternPlan, patternEnergyPlan);
            this.reversePlan(normalizedPatternPlan);
            double avgTestAggregatePlan=this.getAverage(testAggregatePlan.getArithmeticStates());
            double avgNormalizedPatternPlan=this.getAverage(normalizedPatternPlan.getArithmeticStates());
            this.multiplyPlan(normalizedPatternPlan, avgTestAggregatePlan/avgNormalizedPatternPlan);
            double rootMeanSquaredError=this.getRootMeanSquareError(normalizedPatternPlan.getArithmeticStates(), testAggregatePlan.getArithmeticStates());
            if(rootMeanSquaredError<minRootMeanSquaredError){
                minRootMeanSquaredError=rootMeanSquaredError;
                rootSelectedPlanMatchDev=possiblePlan;
            }
            
        }
        this.selectedPlan=this.sumPlans(this.selectedPlan, rootSelectedPlanMatchDev);
        this.selectedPlan.addProperty(EnergyPlanInformation.DISCOMFORT, ((Double)rootSelectedPlanMatchDev.getProperty(EnergyPlanInformation.DISCOMFORT)).doubleValue());
        this.historicSelectedPlan=this.sumPlans(this.historicSelectedPlan, rootSelectedPlanMatchDev);
    }
    
    private void matchUpperBound2(){
        double minRootMeanSquaredError=Double.MAX_VALUE;
        for(ArithmeticListState combinationalPlan:combinationalPlans){
            ArithmeticListState testAggregatePlan=this.setupEnergyPlan(EnergyPlanType.AGGREGATE_PLAN, aggregationPhase, energyPlanSize);
            testAggregatePlan=this.sumPlans(testAggregatePlan, aggregatePlan);
            testAggregatePlan=this.sumPlans(testAggregatePlan, combinationalPlan);
            ArithmeticListState normalizedPatternPlan=this.setupEnergyPlan(EnergyPlanType.GLOBAL_PLAN, aggregationPhase, energyPlanSize);
            normalizedPatternPlan=this.sumPlans(normalizedPatternPlan, patternEnergyPlan);
            this.reversePlan(normalizedPatternPlan);
            double avgNormalizedPatternPlan=this.getAverage(normalizedPatternPlan.getArithmeticStates());
            double avgTestAggregatePlan=this.getAverage(testAggregatePlan.getArithmeticStates());
            double stdevNormalizedPatternPlan=this.getStandardDeviation(normalizedPatternPlan.getArithmeticStates());
            double stdevTestAggregatePlan=this.getStandardDeviation(testAggregatePlan.getArithmeticStates());
            this.substractPlan(normalizedPatternPlan, avgNormalizedPatternPlan);
            this.multiplyPlan(normalizedPatternPlan, stdevTestAggregatePlan/stdevNormalizedPatternPlan);
            this.sumPlan(normalizedPatternPlan, avgTestAggregatePlan);
            double rootMeanSquaredError=this.getRootMeanSquareError(normalizedPatternPlan.getArithmeticStates(), testAggregatePlan.getArithmeticStates());
            if(rootMeanSquaredError<minRootMeanSquaredError){
                minRootMeanSquaredError=rootMeanSquaredError;
                this.selectedCombinationalPlan=combinationalPlan;
            }
        }
    }
    
    private void matchUpperBound2Root(){
        ArithmeticListState rootSelectedPlanMatchDev=null;
        double minRootMeanSquaredError=Double.MAX_VALUE;
        for(ArithmeticListState possiblePlan:possiblePlans){
            ArithmeticListState testAggregatePlan=this.setupEnergyPlan(EnergyPlanType.AGGREGATE_PLAN, aggregationPhase, energyPlanSize);
            testAggregatePlan=this.sumPlans(testAggregatePlan, aggregatePlan);
            testAggregatePlan=this.sumPlans(testAggregatePlan, possiblePlan);
            ArithmeticListState normalizedPatternPlan=this.setupEnergyPlan(EnergyPlanType.GLOBAL_PLAN, aggregationPhase, energyPlanSize);
            normalizedPatternPlan=this.sumPlans(normalizedPatternPlan, patternEnergyPlan);
            this.reversePlan(normalizedPatternPlan);
            double avgTestAggregatePlan=this.getAverage(testAggregatePlan.getArithmeticStates());
            double avgNormalizedPatternPlan=this.getAverage(normalizedPatternPlan.getArithmeticStates());
            double stdevNormalizedPatternPlan=this.getStandardDeviation(normalizedPatternPlan.getArithmeticStates());
            double stdevTestAggregatePlan=this.getStandardDeviation(testAggregatePlan.getArithmeticStates());
            this.substractPlan(normalizedPatternPlan, avgNormalizedPatternPlan);
            this.multiplyPlan(normalizedPatternPlan, stdevTestAggregatePlan/stdevNormalizedPatternPlan);
            this.sumPlan(normalizedPatternPlan, avgTestAggregatePlan);
            double rootMeanSquaredError=this.getRootMeanSquareError(normalizedPatternPlan.getArithmeticStates(), testAggregatePlan.getArithmeticStates());
            if(rootMeanSquaredError<minRootMeanSquaredError){
                minRootMeanSquaredError=rootMeanSquaredError;
                rootSelectedPlanMatchDev=possiblePlan;
            }
            
        }
        this.selectedPlan=this.sumPlans(this.selectedPlan, rootSelectedPlanMatchDev);
        this.selectedPlan.addProperty(EnergyPlanInformation.DISCOMFORT, ((Double)rootSelectedPlanMatchDev.getProperty(EnergyPlanInformation.DISCOMFORT)).doubleValue());
        this.historicSelectedPlan=this.sumPlans(this.historicSelectedPlan, rootSelectedPlanMatchDev);
    }
    
    private void minimizeDiscomfort(){
        double minDiscomfort=Double.MAX_VALUE;
        for(ArithmeticListState combinationalPlan:combinationalPlans){
            double discomfort=((Double)combinationalPlan.getProperty(EnergyPlanInformation.DISCOMFORT)).doubleValue();
            if(discomfort<minDiscomfort){
                minDiscomfort=discomfort;
                this.selectedCombinationalPlan=combinationalPlan;
            }
        }
    }
    
    private void minimizeDiscomfortRoot(){
        ArithmeticListState rootSelectedPlanMinDis=null;
        double minDiscomfort=Double.MAX_VALUE;
        for(ArithmeticListState possiblePlan:possiblePlans){
            double discomfort=((Double)possiblePlan.getProperty(EnergyPlanInformation.DISCOMFORT)).doubleValue();
            if(discomfort<minDiscomfort){
                minDiscomfort=discomfort;
                rootSelectedPlanMinDis=possiblePlan;
            }
        }
        this.selectedPlan=this.sumPlans(this.selectedPlan, rootSelectedPlanMinDis);
        this.selectedPlan.addProperty(EnergyPlanInformation.DISCOMFORT, ((Double)rootSelectedPlanMinDis.getProperty(EnergyPlanInformation.DISCOMFORT)).doubleValue());
        this.historicSelectedPlan=this.sumPlans(this.historicSelectedPlan, rootSelectedPlanMinDis);
    }
    
    public void computeRobustness(){
        switch(this.fitnessFunction){
            case MINIMIZING_PRODUCT_SUM:
                this.robustness=this.getSum(this.globalPlan.getArithmeticStates());
                break;
            case MINIMIZING_DEVIATIONS:
                this.robustness=this.getStandardDeviation(this.globalPlan.getArithmeticStates());
                break;
            case MINIMIZING_RELATIVE_DEVIATIONS:
                this.robustness=this.getRelativeStandardDeviation(this.globalPlan.getArithmeticStates());
                break;
            case REVERSING_DEVIATIONS:
                if(this.coordinationPhaseIndex==1){
                    this.robustness=0.0;
                }
                else{
                    ArithmeticListState historicGlobalPlan=this.historicEnergyPlans.get(this.historicAggregationPhase).get(HistoricEnergyPlans.GLOBAL_PLAN);
                    this.robustness=this.getCorrelationCoefficient(this.globalPlan.getArithmeticStates(), historicGlobalPlan.getArithmeticStates());
                }
                break;
            case MINIMIZING_CORRELATION:
                if(this.coordinationPhaseIndex==1){
                    this.robustness=0.0;
                }
                else{
                    ArithmeticListState historicGlobalPlan=this.historicEnergyPlans.get(this.historicAggregationPhase).get(HistoricEnergyPlans.GLOBAL_PLAN);
                    this.robustness=this.getCorrelationCoefficient(this.globalPlan.getArithmeticStates(), historicGlobalPlan.getArithmeticStates());
                }
                break;
            case MAXIMIZING_LOAD_FACTOR:
                this.robustness=this.getAverage(this.globalPlan.getArithmeticStates())/this.getMaximum(this.globalPlan.getArithmeticStates());
                break;
            case MINIMIZING_ERROR:
                this.robustness=this.getRootMeanSquareError(this.globalPlan.getArithmeticStates(), this.patternEnergyPlan.getArithmeticStates());
                break;
            case MATCHING_UPPER_BOUND_1:
                this.robustness=this.getRootMeanSquareError(this.globalPlan.getArithmeticStates(), this.patternEnergyPlan.getArithmeticStates());
                break;
            case MATCHING_UPPER_BOUND_2:
                this.robustness=this.getRootMeanSquareError(this.globalPlan.getArithmeticStates(), this.patternEnergyPlan.getArithmeticStates());
                break;
            case MAXIMIZING_CORRELATION:
                this.robustness=this.getCorrelationCoefficient(this.globalPlan.getArithmeticStates(), this.patternEnergyPlan.getArithmeticStates());
                break;
            case MAXIMIZING_ENTROPY:
                this.robustness=this.getEntropy(this.globalPlan.getArithmeticStates());
                break;
            case MINIMIZING_DEMAND:
                this.robustness=this.getAverage(this.globalPlan.getArithmeticStates());
                break;
            case MAXIMIZING_DEMAND:
                this.robustness=this.getAverage(this.globalPlan.getArithmeticStates());
                break;
            case RANDOM_SELECTION:
                this.robustness=this.getStandardDeviation(this.globalPlan.getArithmeticStates());
                break;
            case MINIMIZE_DISCOMFORT:
                this.robustness=this.getStandardDeviation(this.globalPlan.getArithmeticStates());
                break;
            default:
                // Something wrong!
        }
    }
    
    /**
     * Computes the standard deviation of a list of arithmetic energyPlan
     * 
     * @param energyPlan the arithemtic energyPlan used for the computation of the 
     * standard deviation
     * @return the standard deviation
    */
    private double getStandardDeviation(List<ArithmeticState> energyPlan){
        double average=this.getAverage(energyPlan);
        double sumSquare=0.0;
        for(ArithmeticState state:energyPlan){
            sumSquare+=Math.pow((state.getValue()-average), 2.0);
        }
        double variance=sumSquare/energyPlan.size();
        double stDev=Math.sqrt(variance);
        return stDev;
    }
    
    /**
     * Computes the standard deviation of a list of arithmetic energyPlan
     * 
     * @param energyPlan the arithemtic energyPlan used for the computation of the 
     * standard deviation
     * @return the standard deviation
    */
    private double getRelativeStandardDeviation(List<ArithmeticState> energyPlan){
        double average=this.getAverage(energyPlan);
        double sumSquare=0.0;
        for(ArithmeticState state:energyPlan){
            sumSquare+=Math.pow((state.getValue()-average), 2.0);
        }
        double variance=sumSquare/energyPlan.size();
        double stDev=Math.sqrt(variance);
        return stDev/average;
    }
    
    /**
     * Computes the average of a list of arithmetic energyPlan
     * 
     * @param energyPlan the arithemtic energyPlan used for the computation of the 
     * average
     * @return the average
    */
    private double getAverage(List<ArithmeticState> energyPlan){
        double average=Double.NaN;
        double sum=0.0;
        for(ArithmeticState state:energyPlan){
            sum+=state.getValue();
        }
        average=sum/energyPlan.size();
        return average;
    }
    
    /**
     * Computes the average of a list of arithmetic energyPlan
     * 
     * @param energyPlan the arithemtic energyPlan used for the computation of the 
     * average
     * @return the average
    */
    private double getSum(List<ArithmeticState> energyPlan){
        double sum=0.0;
        for(ArithmeticState state:energyPlan){
            sum+=state.getValue();
        }
        return sum;
    }
    
    private double getEntropy(List<ArithmeticState> energyPlan){
        double sum=this.getSum(energyPlan);
        double entropy=0.0;
        for(ArithmeticState state:energyPlan){
            double p=state.getValue()/sum;
            if(p==0.0){
                entropy+=0.0;
            }
            else{
                entropy+=p*Math.log(p);
            }
            
        }
        return -entropy;
    }
    
    private double getMaximum(List<ArithmeticState> energyPlan){
        double maximum=Double.MIN_VALUE;
        for(ArithmeticState state:energyPlan){
            if(state.getValue()>maximum){
                maximum=state.getValue();
            }
        }
        return maximum;
    }
    
    private double getMinimum(List<ArithmeticState> energyPlan){
        double minimum=Double.MAX_VALUE;
        for(ArithmeticState state:energyPlan){
            if(state.getValue()<minimum){
                minimum=state.getValue();
            }
        }
        return minimum;
    }
    
    /**
     * Computes the correlation coefficient of two list of arithmetic states
     * 
     * @param energyPlanX the first list of arithemtic states used for the computation
     * of the correlation coefficient
     * @param energyPlanY the second list of arithemtic states used for the computation
     * of the correlation coefficient
     * @return the correlation coefficient
    */
    private double getCorrelationCoefficient(List<ArithmeticState> energyPlanX, List<ArithmeticState> energyPlanY){
//        double corrCoeff=Double.NaN;
//        int n=energyPlanX.size();
//        double x=0.0;
//        double y=0.0;
//        double xy=0.0;
//        double x2=0.0;
//        double y2=0.0;
//        double sqrt01=0.0;
//        double sqrt02=0.0;
//        for(ArithmeticState state01:energyPlanX){
//            x+=state01.getValue();
//        }
//        for(ArithmeticState state02:energyPlanY){
//            y+=state02.getValue();
//        }
//        for(int i=0;i<n;i++){
//            xy+=energyPlanX.get(i).getValue()*energyPlanY.get(i).getValue();
//        }
//        for(ArithmeticState state01:energyPlanX){
//            x2+=Math.pow(state01.getValue(), 2.0);
//        }
//        for(ArithmeticState state02:energyPlanY){
//            y2+=Math.pow(state02.getValue(), 2.0);
//        }
//        sqrt01=Math.sqrt(n*x2-Math.pow(x,2.0));
//        sqrt02=Math.sqrt(n*y2-Math.pow(y,2.0));
//        corrCoeff=(n*xy-x*y)/(sqrt01*sqrt02);
//        return corrCoeff;
        double result = 0;
        double sum_sq_x = 0;
        double sum_sq_y = 0;
        double sum_coproduct = 0;
        double mean_x = energyPlanX.get(0).getValue();
        double mean_y = energyPlanY.get(0).getValue();
        for(int i=2;i<energyPlanX.size()+1;i+=1){
            double sweep =Double.valueOf(i-1)/i;
            double delta_x = energyPlanX.get(i-1).getValue()-mean_x;
            double delta_y = energyPlanY.get(i-1).getValue()-mean_y;
            sum_sq_x += delta_x * delta_x * sweep;
            sum_sq_y += delta_y * delta_y * sweep;
            sum_coproduct += delta_x * delta_y * sweep;
            mean_x += delta_x / i;
            mean_y += delta_y / i;
        }
        double pop_sd_x = (double) Math.sqrt(sum_sq_x/energyPlanX.size());
        double pop_sd_y = (double) Math.sqrt(sum_sq_y/energyPlanY.size());
        double cov_x_y = sum_coproduct / energyPlanX.size();
        result = cov_x_y / (pop_sd_x*pop_sd_y);
        return result;
    }
    
    public double getRootMeanSquareError(List<ArithmeticState> energyPlanX, List<ArithmeticState> energyPlanY){
        double squaredError=0;
        for(int i=0;i<energyPlanX.size();i++){
            squaredError+=Math.pow(energyPlanX.get(i).getValue()-energyPlanY.get(i).getValue(), 2);
        }
        double meanSquaredError=squaredError/energyPlanX.size();
        double rootMeanSquaredError=Math.sqrt(meanSquaredError);
        return rootMeanSquaredError;
    }
    
    public ArithmeticListState setupEnergyPlan(EnergyPlanType type, DateTime aggregationPhase, int energyPlanSize){
        ArithmeticListState energyPlan=new ArithmeticListState(new ArrayList());
        for(int i=0;i<energyPlanSize;i++){
            ArithmeticState consumption=new ArithmeticState();
            consumption.setValue(0.0);
            energyPlan.addArithmeticState(consumption);
        }
        energyPlan.addProperty(EnergyPlanInformation.TYPE, type);
        energyPlan.addProperty(EnergyPlanInformation.COORDINATION_PHASE, aggregationPhase);
        energyPlan.addProperty(EnergyPlanInformation.DISCOMFORT, 0.0);
        energyPlan.addProperty(EnergyPlanInformation.AGENT_METER_ID, agentMeterID);
        energyPlan.addProperty(EnergyPlanInformation.CONFIGURATION, planConfigurations+"-"+treeStamp);
        return energyPlan;
    }
    
    public ArithmeticListState sumPlans(ArithmeticListState aggregateEnergyPlan, ArithmeticListState aggregatedEnergyPlan){
//        try{
        for(int i=0;i<aggregateEnergyPlan.getNumberOfStates();i++){
            double aggregateConsumption=aggregateEnergyPlan.getArithmeticState(i).getValue()+aggregatedEnergyPlan.getArithmeticState(i).getValue();
            aggregateEnergyPlan.setArithmeticState(i, aggregateConsumption);
        }
//        }
//        catch(IndexOutOfBoundsException e){
//            System.out.println("***");
//            System.out.println("Agent: "+this.agentMeterID);
//                System.out.println(aggregateEnergyPlan.getArithmeticStates().size());
//                System.out.println(aggregatedEnergyPlan.getArithmeticStates().size());
//                System.out.println(this.possiblePlans.size());
//        }
        return aggregateEnergyPlan;
    }
    
    public ArithmeticListState substractPlans(ArithmeticListState aggregateEnergyPlan, ArithmeticListState aggregatedEnergyPlan){
        for(int i=0;i<aggregateEnergyPlan.getNumberOfStates();i++){
            double aggregateConsumption=aggregateEnergyPlan.getArithmeticState(i).getValue()-aggregatedEnergyPlan.getArithmeticState(i).getValue();
            aggregateEnergyPlan.setArithmeticState(i, aggregateConsumption);
        }
        return aggregateEnergyPlan;
    }
    
    public ArithmeticListState multiplyPlans(ArithmeticListState aggregateEnergyPlan, ArithmeticListState aggregatedEnergyPlan){
        for(int i=0;i<aggregateEnergyPlan.getNumberOfStates();i++){
            double aggregateConsumption=aggregateEnergyPlan.getArithmeticState(i).getValue()*aggregatedEnergyPlan.getArithmeticState(i).getValue();
            aggregateEnergyPlan.setArithmeticState(i, aggregateConsumption);
        }
        return aggregateEnergyPlan;
    }
    
    public ArithmeticListState multiplyPlan(ArithmeticListState aggregateEnergyPlan, double multiplicationFactor){
        for(int i=0;i<aggregateEnergyPlan.getNumberOfStates();i++){
            double aggregateConsumption=aggregateEnergyPlan.getArithmeticState(i).getValue()*multiplicationFactor;
            aggregateEnergyPlan.setArithmeticState(i, aggregateConsumption);
        }
        return aggregateEnergyPlan;
    }
    
    public ArithmeticListState substractPlan(ArithmeticListState aggregateEnergyPlan, double substractFactor){
        for(int i=0;i<aggregateEnergyPlan.getNumberOfStates();i++){
            double aggregateConsumption=aggregateEnergyPlan.getArithmeticState(i).getValue()-substractFactor;
            aggregateEnergyPlan.setArithmeticState(i, aggregateConsumption);
        }
        return aggregateEnergyPlan;
    }
    
    public ArithmeticListState sumPlan(ArithmeticListState aggregateEnergyPlan, double summationFactor){
        for(int i=0;i<aggregateEnergyPlan.getNumberOfStates();i++){
            double aggregateConsumption=aggregateEnergyPlan.getArithmeticState(i).getValue()+summationFactor;
            aggregateEnergyPlan.setArithmeticState(i, aggregateConsumption);
        }
        return aggregateEnergyPlan;
    }
    
    public ArithmeticListState reversePlan(ArithmeticListState aggregateEnergyPlan){
        double average=this.getAverage(aggregateEnergyPlan.getArithmeticStates());
        for(int i=0;i<aggregateEnergyPlan.getNumberOfStates();i++){
            double aggregateConsumption=aggregateEnergyPlan.getArithmeticState(i).getValue();
            if(aggregateConsumption>average){
                aggregateConsumption=average-(aggregateConsumption-average);
            }
            else{
                aggregateConsumption=average+(average-aggregateConsumption);
            }
            aggregateEnergyPlan.setArithmeticState(i, aggregateConsumption);
        }
        return aggregateEnergyPlan;
    }
    
    @Override
    public void handleIncomingMessage(Message message){
        if(message instanceof EPOSRequest){
            EPOSRequest request=(EPOSRequest)message;
            State requestState=new State();
            this.messageBuffer.put(request.child, request);
            if(this.children.size()==this.messageBuffer.size()){
                this.preProcessing();
                this.select();
                this.update();
                this.informChildren();
                this.plan();
                if(this.topologicalState==TopologicalState.ROOT){
                    switch(this.fitnessFunction){
                        case MINIMIZING_PRODUCT_SUM:
                            this.minimizeProductSumRoot();
                            break;
                        case MINIMIZING_DEVIATIONS:
                            this.minimizeDeviationsRoot();
                            break;
                        case MINIMIZING_RELATIVE_DEVIATIONS:
                            this.minimizeRelativeDeviationsRoot();
                            break;
                        case REVERSING_DEVIATIONS:
                            this.reverseDeviationsRoot();
                            break;
                        case MINIMIZING_ERROR:
                            this.matchRootMeanSquaredErrorsRoot();
                            break;
                        case MAXIMIZING_CORRELATION:
                            this.maximizeCorrelationRoot();
                            break;
                        case MINIMIZING_DEMAND:
                            this.minimizeDemandRoot();
                            break;
                        case MAXIMIZING_DEMAND:
                            this.maximizeDemandRoot();
                            break;
                        case MAXIMIZING_ENTROPY:
                            this.maximizeEntropyRoot();
                            break;
                        case MAXIMIZING_LOAD_FACTOR:
                            this.maximizeLoadFactorRoot();
                            break;
                        case MINIMIZING_CORRELATION:
                            this.minimizeCorrelationRoot();
                            break;
                        case RANDOM_SELECTION:
                            this.randomSelectionRoot();
                            break;
                        case MINIMIZE_DISCOMFORT:
                            this.minimizeDiscomfortRoot();
                            break;
                        case MATCHING_UPPER_BOUND_1:
                            this.matchUpperBound1Root();
                            break;
                        case MATCHING_UPPER_BOUND_2:
                            this.matchUpperBound2Root();
                            break;
                        default:
                            // Something wrong!
                    }
                    this.aggregatePlan=this.sumPlans(this.aggregatePlan, this.selectedPlan);
                    this.globalPlan=this.sumPlans(this.globalPlan, this.aggregatePlan);
                    this.historicAggregatePlan=this.sumPlans(this.historicAggregatePlan, aggregatePlan);
                    this.historicGlobalPlan=this.sumPlans(this.historicGlobalPlan, globalPlan);
                    HashMap<HistoricEnergyPlans,ArithmeticListState> historicPlans=new HashMap<HistoricEnergyPlans,ArithmeticListState>();
                    historicPlans.put(HistoricEnergyPlans.GLOBAL_PLAN, historicGlobalPlan);
                    historicPlans.put(HistoricEnergyPlans.AGGREGATE_PLAN, historicAggregatePlan);
                    historicPlans.put(HistoricEnergyPlans.SELECTED_PLAN, historicSelectedPlan);
                    this.historicEnergyPlans.put(this.aggregationPhase, historicPlans);
                    this.computeRobustness();
                    System.out.print(globalPlan.getNumberOfStates()+","+aggregationPhase.toString("yyyy-MM-dd")+",");
                    for(ArithmeticState value:globalPlan.getArithmeticStates()){
                        System.out.print(value.getValue()+",");
                    }
                    System.out.print(this.robustness);
                    System.out.println();
                    this.broadcast();
                }
                else{
                    this.informParent();
                }
            }
        }
        if(message instanceof EPOSResponse){
            EPOSResponse response=(EPOSResponse)message;
            this.selectedPlan=sumPlans(this.selectedPlan, response.selectedPlan);
            this.selectedPlan.addProperty(EnergyPlanInformation.DISCOMFORT, ((Double)response.selectedPlan.getProperty(EnergyPlanInformation.DISCOMFORT)).doubleValue());
            this.historicSelectedPlan=this.sumPlans(this.historicSelectedPlan, selectedPlan);
        }
        if(message instanceof EPOSBroadcast){
            EPOSBroadcast broadcast=(EPOSBroadcast)message;
            this.globalPlan=this.sumPlans(this.globalPlan, broadcast.globalPlan);
            this.historicGlobalPlan=this.sumPlans(this.historicGlobalPlan, globalPlan);
            HashMap<HistoricEnergyPlans,ArithmeticListState> historicPlans=new HashMap<HistoricEnergyPlans,ArithmeticListState>();
            historicPlans.put(HistoricEnergyPlans.GLOBAL_PLAN, historicGlobalPlan);
            historicPlans.put(HistoricEnergyPlans.AGGREGATE_PLAN, historicAggregatePlan);
            historicPlans.put(HistoricEnergyPlans.SELECTED_PLAN, historicSelectedPlan);
            this.historicEnergyPlans.put(this.aggregationPhase, historicPlans);
            this.computeRobustness();
            if(this.topologicalState==TopologicalState.LEAF){
//                this.plan();
//                this.informParent();
            }
            else{
                for(Finger child:children){
                    getPeer().sendMessage(child.getNetworkAddress(), broadcast);
                }
            }
        }
    }
    
    public void setParent(Finger parent){
        if(parent!=null){
            this.parent=parent;
        }
        this.computeTopologicalState();
    }

    public void setChildren(List<Finger> children){
        for(Finger child:children){
            if(child!=null){
                this.children.add(child);
            }
        }
        this.computeTopologicalState();
    }

    public void setTreeView(Finger parent, List<Finger> children){
        this.setParent(parent);
        this.setChildren(children);
        this.computeTopologicalState();
    }
    
    private void computeTopologicalState(){
        if(parent==null && children.size()!=0){
            this.topologicalState=TopologicalState.ROOT;
        }
        if(parent!=null && children.size()==0){
            this.topologicalState=TopologicalState.LEAF;
        }
        if(parent!=null && children.size()!=0){
            this.topologicalState=TopologicalState.IN_TREE;
        }
        if(parent==null && children.size()==0){
            this.topologicalState=TopologicalState.DISCONNECTED;
        }
    }
    
    public TopologicalState getTopologicalState(){
        return this.topologicalState;
    }
    
    //****************** MEASUREMENTS ******************
        
    /**
     * Scheduling the measurements for the load management agent
     */
    private void scheduleMeasurements(){
        this.measurementDumper=new MeasurementFileDumper("peersLog/"+this.experimentID+getPeer().getIdentifier().toString());
        getPeer().getMeasurementLogger().addMeasurementLoggerListener(new MeasurementLoggerListener(){
            public void measurementEpochEnded(MeasurementLog log, int epochNumber){
                if(epochNumber==2){
                    if(topologicalState==TopologicalState.ROOT){
                        log.log(epochNumber, globalPlan, 1.0);
                        log.log(epochNumber, Measurements.PLAN_SIZE, energyPlanSize);
                        log.log(epochNumber, Measurements.ROBUSTNESS, robustness);
                    }
                    log.log(epochNumber, selectedPlan, 1.0);
                    log.log(epochNumber, Measurements.DISCOMFORT, ((Double)selectedPlan.getProperty(EnergyPlanInformation.DISCOMFORT)).doubleValue());
//                    log.log(epochNumber, Measurements.SELECTED_PLAN_VALUE, selectedPlan.getArithmeticState(0).getValue());
                writeGraphData(epochNumber);
                }
                measurementDumper.measurementEpochEnded(log, epochNumber);
                log.shrink(epochNumber, epochNumber+1);
            }
        });
    }
    
    /**
     * Problems encountered: 
     * 
     * 1. Root plan cannot be detected in MIN-COST
     * 2. There are nodes in the EDF dataset that have possible plans with more than 144 size. 
     * 
     * @param epochNumber 
     */
    private void writeGraphData(int epochNumber){
        System.out.println(getPeer().getNetworkAddress().toString()+","+
                ((parent!=null)?parent.getNetworkAddress().toString():"-")+","+
                findSelectedPlan());
    }
    
    private boolean isEqual(ArithmeticListState planA, ArithmeticListState planB){
        for(int i=0;i<planA.getArithmeticStates().size();i++){
            if(planA.getArithmeticState(i).getValue()!=planB.getArithmeticState(i).getValue()){
                return false;
            }
        }
        return true;
    }
    
    private int findSelectedPlan(){
        for(int i=0;i<possiblePlans.size();i++){
            if(isEqual(possiblePlans.get(i),selectedPlan)){
                return i;
            }
        }
        return -1;
    }
}
