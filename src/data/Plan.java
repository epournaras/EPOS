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
package data;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A plan represents a vector with certain properties
 *
 * @author Peter
 */
public class Plan<V extends DataType<V>> implements HasValue<V>, Serializable, Cloneable {

    private V value;
    private int index;
    private double score;

    public Plan(V value) {
        this.value = value;
    }

    public V getValue() {
        return value;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double discomfort) {
        this.score = discomfort;
    }

    public Plan<V> cloneThis() {
        Plan<V> clone = null;
        try {
            clone = (Plan<V>) clone();
            clone.value = value.cloneThis();
        } catch (CloneNotSupportedException ex) {
            Logger.getLogger(Plan.class.getName()).log(Level.SEVERE, null, ex);
        }
        return clone;
    }

    /**
     * ADDED BY @author jovan :
     * score is NaN, and it is up to local cost function to
     * decide how to handle this value.
     * 
     * @return
     */
    public Plan<V> cloneNew() {
        Plan<V> clone = null;
        try {
            clone = (Plan<V>) clone();
            clone.value = value.cloneNew();
            clone.score = Double.NaN;
            clone.index = 0;
            return clone;
        } catch (CloneNotSupportedException ex) {
            Logger.getLogger(Plan.class.getName()).log(Level.SEVERE, null, ex);
        }
        return clone;
    }
}
