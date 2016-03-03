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
import dsutil.generic.RankPriority;
import dsutil.generic.state.ArithmeticListState;
import dsutil.generic.state.ArithmeticState;
import dsutil.protopeer.services.topology.trees.DescriptorType;
import dsutil.protopeer.services.topology.trees.TreeProvider;
import dsutil.protopeer.services.topology.trees.TreeType;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import org.joda.time.DateTime;
import protopeer.Experiment;
import protopeer.Peer;
import protopeer.PeerFactory;
import protopeer.SimulatedExperiment;
import protopeer.servers.bootstrap.SimplePeerIdentifierGenerator;
import protopeer.util.quantities.Time;
import tree.centralized.client.TreeClient;
import tree.centralized.server.TreeServer;

/**
 *
 * @author Evangelos
 */
public class TestEPOS extends SimulatedExperiment{
    
    private final static String expSeqNum="01";
    private static String experimentID="Experiment "+expSeqNum+"/";
    
    //Simulation Parameters
    private final static int runDuration=25;
    private final static int N=27;
    
    // Tree building
    private static final RankPriority priority=RankPriority.HIGH_RANK;
    private static final DescriptorType descriptor=DescriptorType.RANK;
    private static final TreeType type=TreeType.SORTED_HtL;
    private final static int[] v=new int[]{4};
    // EPOS Agent
    private static int treeInstances=10;
    private static String plansLocation="input-data";
    private static String planConfigurations="20131009_205144-H-E-CAISO-K3";
    static File dir = new File(plansLocation+"/"+planConfigurations);  
    private static String treeStamp="2BR"; //1. average k-ary tree, 2. Balanced or random k-ary tree, 3. random positioning or nodes 
    private static File[] agentMeterIDs = dir.listFiles(new FileFilter() {  
        public boolean accept(File pathname) {  
            return pathname.isDirectory();  
        }  
    });
    private static DateTime aggregationPhase=DateTime.parse("2010-01-09");
    private static String plansFormat=".plans";
    private static EPOSAgent.FitnessFunction fitnessFunction=EPOSAgent.FitnessFunction.MAXIMIZING_ENTROPY;
    private static int planSize=24;
    private static DateTime historicAggregationPhase=DateTime.parse("2010-01-09");
    private static ArithmeticListState patternEnergyPlan;
    private static int historySize=5;
    
    public static void main(String[] args) {
        for(int i=0;i<treeInstances;i++){
            treeStamp="3BR"+i;
            System.out.println("Experiment "+expSeqNum+"\n");
            Experiment.initEnvironment();
            final TestEPOS test = new TestEPOS();
            test.init();
            experimentID="Experiment "+i+"/";
            final File folder = new File("peersLog/"+experimentID);
            clearExperimentFile(folder);
            folder.mkdir();
            patternEnergyPlan=getPatternPlan(planSize);
            PeerFactory peerFactory=new PeerFactory() {
                public Peer createPeer(int peerIndex, Experiment experiment) {
                    Peer newPeer = new Peer(peerIndex);
                    if (peerIndex == 0) {
                       newPeer.addPeerlet(new TreeServer(N, priority, descriptor, type));
                    }
                    newPeer.addPeerlet(new TreeClient(Experiment.getSingleton().getAddressToBindTo(0), new SimplePeerIdentifierGenerator(), Math.random(), v[(int)(Math.random()*v.length)]));
                    newPeer.addPeerlet(new TreeProvider());
                    newPeer.addPeerlet(new EPOSAgent(experimentID, plansLocation, planConfigurations, treeStamp, agentMeterIDs[peerIndex].getName(), plansFormat, fitnessFunction, 24, aggregationPhase, historicAggregationPhase, patternEnergyPlan, historySize)); 

                    return newPeer;
                }
            };
            test.initPeers(0,N,peerFactory);
            test.startPeers(0,N);
            //run the simulation
            test.runSimulation(Time.inSeconds(runDuration));
        }
        
    }
    
    public final static ArithmeticListState getPatternPlan(int planSize){
        ArithmeticListState patternEnergyPlan=new ArithmeticListState(new ArrayList());
        for(int i=0;i<planSize;i++){
            if(i>=2 && i<=9){
                patternEnergyPlan.addArithmeticState(new ArithmeticState(0.8));
            }
            else{
                patternEnergyPlan.addArithmeticState(new ArithmeticState(0.2));
            }
            
        }
        return patternEnergyPlan;
    }
    
    public final static void clearExperimentFile(File experiment){
        File[] files = experiment.listFiles();
        if(files!=null) { //some JVMs return null for empty dirs
            for(File f: files) {
                if(f.isDirectory()) {
                    clearExperimentFile(f);
                } else {
                    f.delete();
                }
            }
        }
        experiment.delete();
    }
}
