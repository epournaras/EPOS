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

import func.CostFunction;

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
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import agent.Agent;
//import agent.logging.LocalCostLogger.Token; //TODO ask Jovan Use local token implementation
//import agent.logging.LocalCostMultiObjectiveLogger.Token;

import protopeer.measurement.Aggregate;
import protopeer.measurement.MeasurementLog;
import data.DataType;

/**
 * Determines when the algorithm terminates. The termination is defined at the
 * last iteration that changes the global cost of the result.
 *
 * @author Peter P. & Jovan N.
 */
public class TerminationLogger<V extends DataType<V>> extends AgentLogger<Agent<V>> {

    private CostFunction<V> 		globalCostFunc;
    private int 					index;
    private double 					prevGlobalCost;				// previous global cost
    
    private String 					filepath;					// path to file in which result should be written
    private long 					id = System.currentTimeMillis();

    /**
     * Creates a TerminationLogger that detects termination based on the global
     * cost function.
     */
    public TerminationLogger() { }
    
    /**
     * Creates a <code>TerminationLogger</code> that dumps result in
     * file <code>filepath</code>
     * @param filepath	path to file
     */
    public TerminationLogger(String filepath) {
    	this(filepath, null);
    }

    /**
     * Creates a TerminationLogger that detects termination based on the
     * provided cost function.
     *
     * @param globalCostFunc
     */
    public TerminationLogger(CostFunction<V> globalCostFunc) {
    	this(null, globalCostFunc);
    }
    
    public TerminationLogger(String filepath, CostFunction<V> globalCostFunc) {
    	this.globalCostFunc = globalCostFunc;
    	this.filepath = filepath;
    }

    @Override
    public void init(Agent<V> agent) {
        if (globalCostFunc == null) {
            globalCostFunc = agent.getGlobalCostFunction();
        }
    }

    @Override
    public void log(MeasurementLog log, int epoch, Agent<V> agent) {    	
        if (agent.isRepresentative()) {
        	
        	V glresp = agent.getGlobalResponse();        	
        	
            if (agent.isIterationAfterReorganization()) {
                this.index = agent.getIteration() + 1;
                this.prevGlobalCost = this.globalCostFunc.calcCost(glresp);
            } else {
            	double globalCost = this.globalCostFunc.calcCost(glresp);
        		
                /*
                 * Note that this expects globalCost to monotonically decrease
                 */                
                if (globalCost < this.prevGlobalCost) {
                    index = agent.getIteration() + 1;
                    if(glresp != null) {
                    	this.prevGlobalCost = globalCost;
                    } else {
                    }
                }
            }

            // only log when agent finishes iterating
            if (agent.getIteration() == agent.getNumIterations() - 1) {
            	Token token = new Token(this.index, this.run);            
                log.log(epoch, TerminationLogger.class.getName(), token, 1.0);
            }
        }
    }

    @Override
	public void print(MeasurementLog log) {
		String outcome = this.internalFetching(log);
    	
        if (this.filepath == null) {
            System.out.print(outcome);
        } else {
            try (PrintWriter out = new PrintWriter(new BufferedWriter(new java.io.FileWriter(this.filepath, true)))) {   
                out.append(outcome);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(GlobalCostLogger.class.getName()).log(Level.SEVERE, null, ex);
            } catch(IOException e) {
            	Logger.getLogger(GlobalCostLogger.class.getName()).log(Level.SEVERE, null, e);
            }
        }
	}
    
    private String internalFetching(MeasurementLog log) {
    	
    	///////////////////////////////////////////////////////////////////////////////////////
    	// PER RUN, PER ITERATION
    	
    	TreeSet<Object> allTokens = new TreeSet<Object>();
		allTokens.addAll(log.getTagsOfType(Token.class));
		
		ArrayList<Token> sortedTokens = new ArrayList<>();
		allTokens.forEach(o -> {
			Token t = (Token) o;
			sortedTokens.add(t);
		});

		Collections.sort(sortedTokens);
		
		
		///////////////////////////////////////////////////////////////////////////////////////
		// FORMTATTING        
		
		StringBuilder sb = new StringBuilder();
		sb.append("Run")
		  .append("," + "Terminal Iteration");
		
		sb.append(System.lineSeparator());
		
		for(int i = 0; i < sortedTokens.size(); i++) {
			sb.append(i)
			  .append("," + sortedTokens.get(i).terminalIteration)
			  .append(System.lineSeparator());
		}
		
		return sb.toString();
    }
    
    private void printOut(int epoch, Agent agent, String message) {
    	System.out.println("epoch: "		+	epoch	+ 
    					   ", id: " 		+ this.id 	+ 
    					   ", Node " 		+ agent.getPeer().getIndexNumber()	+
    					   ", iteration: "	+ agent.getIteration()	+
    					   message);
    }
    
    private class Token implements Comparable<Token> {
		
		public int terminalIteration;
		public int run;
		
		public Token(int terminalIteration, int run) {
			this.terminalIteration = terminalIteration;
			this.run = run;
		}
		
		@Override
        public int hashCode() {
            int hash = 3;
            hash = 53 * hash + this.run;
            hash = 53 * hash + this.terminalIteration;
            return hash;
        }

		@Override
		public int compareTo(TerminationLogger<V>.Token other) {
			
			if		(this.run > other.run)					return 1;
			else if (this.run < other.run)					return -1;
			
			return  0;
		}		
	}
}
