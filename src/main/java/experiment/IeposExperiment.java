/*
 * Copyright (C) 2016 Peter
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
package experiment;

import agent.Agent;
import agent.IeposAgent;
import agent.logging.AgentLoggingProvider;
import data.Plan;
import data.Vector;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import protopeer.Experiment;
import protopeer.Peer;
import protopeer.PeerFactory;
import protopeer.SimulatedExperiment;
import protopeer.util.quantities.Time;
import util.TreeArchitecture;

/**
 *
 * @author Peter
 */
@Deprecated
public class IeposExperiment extends SimulatedExperiment {
    public static void runSimulation(int numChildren, int numIterations, int numAgents, Function<Integer, Agent> createAgent) {
        SimulatedExperiment experiment = new SimulatedExperiment() {};
        TreeArchitecture architecture = new TreeArchitecture();
       
        SimulatedExperiment.initEnvironment();
        experiment.init();

        PeerFactory peerFactory = new PeerFactory() {

            @Override
            public Peer createPeer(int peerIndex, Experiment e) {
                Agent newAgent = createAgent.apply(peerIndex);
                Peer newPeer = new Peer(peerIndex);

                architecture.addPeerlets(newPeer, newAgent, peerIndex, numAgents);

                return newPeer;
            }
        };
        experiment.initPeers(0, numAgents, peerFactory);
        experiment.startPeers(0, numAgents);

        experiment.runSimulation(Time.inSeconds(3 + numIterations));
    }
}
