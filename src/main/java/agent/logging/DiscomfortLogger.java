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
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import protopeer.measurement.MeasurementLog;
import data.DataType;

/**
 * 
 * Instead of logging index of a plan, we will log it's discomfort score which
 * is actually it's new local cost function.
 * 
 * Instead of histogram, discomfort score of selected plan at convergence is logged
 * for each agent.
 * 
 * @author jovan
 *
 */
public class DiscomfortLogger<V extends DataType<V>> extends AgentLogger<Agent<V>> {
	
	private String 					filepath;
	
	
	/**
     * Outputs the global response to the specified file.
     *
     * @param filename the output file
     */
    public DiscomfortLogger(String filename) {
        this.filepath = filename;
    }

    @Override
    public void init(Agent<V> agent) { }

    @Override
    /**
     * Note that discomfort scores are logged only in last iteration!
     */
    public void log(MeasurementLog log, int epoch, Agent<V> agent) {
        if (agent.getIteration() == agent.getNumIterations() - 1) {
//            int idx = agent.getPossiblePlans().indexOf(agent.getSelectedPlan());
        	double localPlanCost = agent.getLocalCostFunction().calcCost(agent.getSelectedPlan());
            log.log(epoch, new Token(localPlanCost, agent.getPeer().getIndexNumber()), 1);
        }
    }
    
    @Override
	public void print(MeasurementLog log) {
		String outcome = this.extractScores(log);
    	
        if (this.filepath == null) {
            System.out.print(outcome);
        } else {
            try (PrintWriter out = new PrintWriter(new BufferedWriter(new java.io.FileWriter(this.filepath, false)))) {   
                out.append(outcome);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(GlobalCostLogger.class.getName()).log(Level.SEVERE, null, ex);
            } catch(IOException e) {
            	Logger.getLogger(GlobalCostLogger.class.getName()).log(Level.SEVERE, null, e);
            }
        }
	}
    
    private String extractScores(MeasurementLog log) {
    	Set<Object> entries = log.getTagsOfType(Token.class);
		
		Set<Object> sortedEntries = new TreeSet<>((x, y) -> Integer.compare(((DiscomfortLogger.Token) x).agent,
																			((DiscomfortLogger.Token) y).agent));
		sortedEntries.addAll(entries);
    	
		StringBuilder sb = new StringBuilder();
		
		sortedEntries.forEach(obj -> {
			DiscomfortLogger.Token token = (DiscomfortLogger.Token) obj;
			sb.append(token.score);
			if(token.agent < Configuration.numAgents-1) {
				sb.append(",");
			}
		});
		
		return sb.toString();
    }

    private class Token implements Serializable {

    	public double score;
    	public int agent;

        public Token(double score, int agentID) {
            this.score = score;
            this.agent = agentID;
        }        

        @Override
        public int hashCode() {
            double hash = 7;
            hash = 61 * hash + this.score + this.agent;
            return (int) hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Token other = (Token) obj;
            if (this.score != other.score) {
                return false;
            }
            return true;
        }
    }
}
