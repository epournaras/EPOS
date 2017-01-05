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

import data.Vector;

/**
 * The cost according to this cost function is the maximal value of the given
 * vector. This cost function can be used for peak reduction, for example.
 *
 * @author Peter
 */
public class MaxCostFunction extends CostFunction<Vector> {

    @Override
    public double calcCost(Vector vector) {
        return vector.max();
    }

    @Override
    public String toString() {
        return "max";
    }
}
