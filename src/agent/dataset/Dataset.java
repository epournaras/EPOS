/*
 * Copyright (C) 2016 Peter Pilgerstorfer
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
package agent.dataset;

import data.Plan;
import java.util.List;
import data.DataType;

/**
 * This class represents a dataset.
 *
 * @author Peter Pilgerstorfer
 * @param <V> the type of data that is used by the dataset
 */
public interface Dataset<V extends DataType<V>> {

    /**
     * Returns the plans for the specified agent.
     *
     * @param agentId the id of the specified agent; the first agent has id 0,
     * the second agent id 1 and so on
     * @return the plans for the specified agent
     */
    public List<Plan<V>> getPlans(int agentId);
}
