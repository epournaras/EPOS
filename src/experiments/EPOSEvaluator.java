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
package experiments;

import agents.EPOSAgent;
import agents.EPOSAgent.EnergyPlanInformation;
import agents.EPOSAgent.EnergyPlanType;
import dsutil.generic.state.ArithmeticListState;
import dsutil.generic.state.ArithmeticState;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Iterator;
import java.util.Set;
import org.joda.time.DateTime;
import protopeer.measurement.LogReplayer;
import protopeer.measurement.MeasurementLog;
import protopeer.measurement.MeasurementLoggerListener;

/**
 *
 * @author Evangelos
 */
public class EPOSEvaluator {

    private final static String expSeqNum="0";
    private final static String expID="Experiment "+expSeqNum+"/";
    
    private final static int minLoad=0;
    private final static int maxLoad=1000;

    private LogReplayer replayer;
    private final String coma=",";


    public EPOSEvaluator(String logsDir, int minLoad, int maxLoad){
        this.replayer=new LogReplayer();
        this.loadLogs(logsDir, minLoad, maxLoad);
        this.replayResults();
    }

    public static void main(String args[]){
//        EPOSEvaluator replayer=new EPOSEvaluator("peersLog/"+expID, minLoad, maxLoad);
        for(int i=0;i<1;i++){
            EPOSEvaluator replayer=new EPOSEvaluator("peersLog/Experiment "+i+"/", minLoad, maxLoad);
        }
        
    }

    public void loadLogs(String directory, int minLoad, int maxLoad){
        try{
            File folder = new File(directory);
            File[] listOfFiles = folder.listFiles();
            for (int i = 0; i < listOfFiles.length; i++) {
                if (listOfFiles[i].isFile()&&!listOfFiles[i].isHidden()) {
                    MeasurementLog loadedLog=replayer.loadLogFromFile(directory+listOfFiles[i].getName());
                    MeasurementLog replayedLog=this.getMemorySupportedLog(loadedLog, minLoad, maxLoad);
                    replayer.mergeLog(replayedLog);
                }
                else
                    if (listOfFiles[i].isDirectory()) {
                        //do sth else
                    }
            }
        }
        catch(IOException io){

        }
        catch(ClassNotFoundException ex){

        }
    }

    public void replayResults(){
//        this.printGlobalMetricsTags();
//        this.calculatePeerResults(replayer.getCompleteLog());
        this.printLocalMetricsTags();
        replayer.replayTo(new MeasurementLoggerListener(){
            public void measurementEpochEnded(MeasurementLog log, int epochNumber){
                calculateEpochResults(log, epochNumber);
            }
        });
    }

    private void calculatePeerResults(MeasurementLog globalLog){
        
    }

    private void calculateEpochResults(MeasurementLog log, int epochNumber){
        double epochNum=epochNumber;
        Set plans=log.getTagsOfExactType(epochNumber, ArithmeticListState.class);
        String hourValues="";
        String coordinationPhase="";
        if(plans.size()>0){
            Iterator it=plans.iterator();
            while(it.hasNext()){
                ArithmeticListState plan=(ArithmeticListState)it.next();
                if((EnergyPlanType)plan.getProperty(EPOSAgent.EnergyPlanInformation.TYPE)==EnergyPlanType.GLOBAL_PLAN){
                    for(ArithmeticState hourValue:plan.getArithmeticStates()){
                        hourValues=hourValues+this.roundDecimals(hourValue.getValue(),2)+",";
                    }
                    coordinationPhase=((DateTime)plan.getProperty(EPOSAgent.EnergyPlanInformation.COORDINATION_PHASE)).toString("yyyy-MM-dd");
                }
                if((EnergyPlanType)plan.getProperty(EPOSAgent.EnergyPlanInformation.TYPE)==EnergyPlanType.POSSIBLE_PLAN){
                    String planConfigurations=(String)plan.getProperty(EnergyPlanInformation.CONFIGURATION);
                    File outputDirectory=new File("output-data"+"/"+planConfigurations);
                    if(!outputDirectory.exists()){
                        outputDirectory.mkdir();
                    }
                    String agentMeterID=(String)plan.getProperty(EPOSAgent.EnergyPlanInformation.AGENT_METER_ID);
                    File agentMeterIDDirectory=new File("output-data"+"/"+planConfigurations+"/"+agentMeterID);
                    if(!agentMeterIDDirectory.exists()){
                        agentMeterIDDirectory.mkdir();
                    }
                    try{
                        String coordinationPhaseNameFile=((DateTime)plan.getProperty(EPOSAgent.EnergyPlanInformation.COORDINATION_PHASE)).toString("yyyy-MM-dd");
                        File selectedPlan=new File("output-data"+"/"+planConfigurations+"/"+agentMeterID+"/"+coordinationPhaseNameFile+".plans");
                        selectedPlan.createNewFile();
                        BufferedWriter output = new BufferedWriter(new FileWriter(selectedPlan));
                        int planSize=plan.getNumberOfStates();
                        String selectedPlanLine=((Double)plan.getProperty(EnergyPlanInformation.DISCOMFORT)).doubleValue()+":";
                        for(int i=0;i<planSize;i++){
                            selectedPlanLine=selectedPlanLine+roundDecimals(plan.getArithmeticState(i).getValue(),2);
                            if(i!=planSize-1){
                                selectedPlanLine=selectedPlanLine+",";
                            }
                        }
                        output.write(selectedPlanLine);
                        output.close();
                    } 
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
//        else{
//            coordinationPhase="bootstrapping";
//            hourValues="bootstrapping";
//        }        
        double planSize=this.roundDecimals(log.getAggregateByEpochNumber(epochNumber, EPOSAgent.Measurements.PLAN_SIZE).getSum(),2);
        double robustness=this.roundDecimals(log.getAggregateByEpochNumber(epochNumber, EPOSAgent.Measurements.ROBUSTNESS).getSum(),2);
        double discomfort=this.roundDecimals(log.getAggregateByEpochNumber(epochNumber, EPOSAgent.Measurements.DISCOMFORT).getSum(),2);
        double selectedPlan=this.roundDecimals(log.getAggregateByEpochNumber(epochNumber, EPOSAgent.Measurements.SELECTED_PLAN_VALUE).getSum(),2);
        System.out.println(
                epochNum+coma+
                coordinationPhase+coma+
                planSize+coma+
                hourValues+
                robustness+coma+
                discomfort
                );
    }
    
    private MeasurementLog getMemorySupportedLog(MeasurementLog log, int minLoad, int maxLoad){
        return log.getSubLog(minLoad, maxLoad);
    }

    public void printGlobalMetricsTags(){
       System.out.println("*** RESULTS PER PEER ***\n");
    }

    public void printLocalMetricsTags(){
        String hourValueTags="";
        for(int i=1;i<=24;i++){
            hourValueTags=hourValueTags+"Hour "+i+",";
        }
        System.out.println("*** RESULTS PER EPOCH ***\n");
        System.out.println(
                "Number of Epoch,"
                +"Coordination Phase,"
                +"planSize,"
                +hourValueTags
                +"Robustness,"
                +"Discomfort"
                );
    }

    public double roundDecimals(double decimal, int decimalPlace) {
        BigDecimal bd = new BigDecimal(decimal);
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_UP);
        return bd.doubleValue();
    }
}
