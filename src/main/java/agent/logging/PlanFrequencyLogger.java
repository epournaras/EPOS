/*
 * Copyright (C) 2016 Evangelos Pournaras
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
package agent.logging;

import agent.Agent;
import config.Configuration;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import protopeer.measurement.Aggregate;
import protopeer.measurement.MeasurementLog;
import data.DataType;

/**
 * Writes the histogram of selected plan indices in the last iteration to
 * std-out
 *
 * @author Peter P. & Jovan N.
 */
public class PlanFrequencyLogger<V extends DataType<V>> extends AgentLogger<Agent<V>> {
	
	private String				filename = null;
	
	public PlanFrequencyLogger(String filename) {
		this.filename = filename;
	}

    @Override
    public void init(Agent<V> agent) {
    }

    @Override
    public void log(MeasurementLog log, int epoch, Agent<V> agent) {
        if (agent.getIteration() == agent.getNumIterations() - 1) {
            int idx = agent.getPossiblePlans().indexOf(agent.getSelectedPlan());
            Token token = new Token(idx, agent.getSelectedPlan().getScore(), agent.getPeer().getIndexNumber(), this.run);
            log.log(epoch, PlanFrequencyLogger.class.getName(), token, 1.0);
        }
    }

    @Override
    public void print(MeasurementLog log) {
    	String outcome = this.internalFetching(log);
    	
        if (this.filename == null) {
            System.out.print(outcome);
        } else {
            try (PrintWriter out = new PrintWriter(new BufferedWriter(new java.io.FileWriter(this.filename, false)))) {   
                out.append(outcome);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(GlobalCostLogger.class.getName()).log(Level.SEVERE, null, ex);
            } catch(IOException e) {
            	Logger.getLogger(GlobalCostLogger.class.getName()).log(Level.SEVERE, null, e);
            }
        }
    }
    
    private String internalFetching(MeasurementLog log) {
    	
    	TreeSet<Object> allTokens = new TreeSet<Object>();
		allTokens.addAll(log.getTagsOfType(Token.class));
		Iterator<Object> iter = allTokens.iterator();
		
//		System.out.println("Size is: " + allTokens.size());
		
		HashMap<Integer, HashMap<Integer, ArrayList<Token>>> perRun = 
				new HashMap<Integer, HashMap<Integer, ArrayList<Token>>>();    // <run, <idx, list>>
		
		while(iter.hasNext()) {
			Token token = (Token) iter.next();			
			if(!perRun.containsKey(token.run)) {
				perRun.put(token.run, new HashMap<Integer, ArrayList<Token>>());
			}
			if(!perRun.get(token.run).containsKey(token.idx)) {
				perRun.get(token.run).put(token.idx, new ArrayList<Token>());
			}
			ArrayList<Token> thelist = perRun.get(token.run).get(token.idx);
			thelist.add(token);
		}
		
		ArrayList<Integer> sortedRuns = new ArrayList<>(perRun.keySet());
		Collections.sort(sortedRuns);
		
		StringBuilder sb = new StringBuilder();
		sb.append("Plan ID")
		  .append("," + "Plan Scores");
		
		for(int j = 0; j < sortedRuns.size(); j++) {
			sb.append("," + "Run-" + sortedRuns.get(j));
		}
		
		sb.append(System.lineSeparator());
		
//		for(Integer key1: perRun.keySet()) {
//			for(Integer plan: perRun.get(key1).keySet()) {
//				System.out.println("Run = " + key1 + ", plan id = " + plan + ", size = " + perRun.get(key1).get(plan).size());
//			}
//		}
		
		for(int p = 0; p < Configuration.numPlans; p++) {
			int planID = p;
			double score = perRun.get(0).containsKey(p) ? perRun.get(0).get(p).get(0).score : Double.NaN;
			sb.append(planID)
			  .append(",")
			  .append(score);
			
			for(int i = 0; i < sortedRuns.size(); i++) {
				sb.append("," + (perRun.get(i).containsKey(p) ? perRun.get(i).get(p).size() : 0));
			}
			
			sb.append(System.lineSeparator());
		}
		
		return sb.toString();
    }

    private class Token implements Comparable<Token>  {

        public int idx;						// represents id of selected plan
        public int agentId;
        public double score;
        public int run;

        public Token(int idx, double score, int agentId, int run) {
            this.idx = idx;
            this.agentId = agentId;
            this.score = score;
            this.run = run;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 53 * hash + this.run;
            hash = 53 * hash + this.idx;
            hash = 53 * hash + this.agentId;
            return hash;
        }

		@Override
		public int compareTo(PlanFrequencyLogger<V>.Token other) {
			
			if		(this.run > other.run)				return 1;
			else if (this.run < other.run)				return -1;
			
			if		(this.agentId > other.agentId)		return 1;
			else if (this.agentId < other.agentId)		return -1;
			
			if		(this.idx > other.idx)				return 1;
			else if (this.idx < other.idx)				return -1;
			
			return  0;
		}
    }
}