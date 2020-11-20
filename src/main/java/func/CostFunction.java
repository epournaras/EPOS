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
package func;

import data.Plan;
import data.DataType;

/**
 * The general abstraction of a cost function. This function is optimized during the EPOS runtime. 
 * Often the objective is a minimization of a cost. Less often it is the maximizaiton
 * of a gain. In case of maximization of (f), the optimization can be achieved by minimizng an
 * opposite goal, such as (1-f).
 * 
 * @author Peter, Thomas Asikis
 * @param <V> the type of the data this cost function should handle
 */
public interface CostFunction<V extends DataType<V>> extends PlanCostFunction<V> {
	
	/***
	 * Calculate cost is the function that 
	 * @param value, an object that is used as the input of the objective function. 
	 * The result of the objective function is a single digit real number that is 
	 * used in the optimization
	 * @return
	 */
    public abstract double calcCost(V value);
    
    @Override
    public default double calcCost(Plan<V> plan) {
        return calcCost(plan.getValue());
    }
    
}
