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

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Peter
 */
public class Vector implements Value<Vector> {

    private double[] values;

    /**
     * create a new vector with given dimensionality
     *
     * @param numDimensions number of dimensions
     */
    public Vector(int numDimensions) {
        values = new double[numDimensions];
    }
    
    @Override
    public Vector getValue() {
        return this;
    }

    public void setValue(int idx, double value) {
        values[idx] = value;
    }

    public double getValue(int idx) {
        return values[idx];
    }

    public int getNumDimensions() {
        return values.length;
    }

    public double sum() {
        double sum = 0.0;
        for (double val : values) {
            sum += val;
        }
        return sum;
    }

    public double avg() {
        return sum() / values.length;
    }

    public double dot(Vector other) {
        double dot = 0;
        for (int i = 0; i < values.length; i++) {
            dot += values[i] * other.values[i];
        }
        return dot;
    }

    public double entropy() {
        double sum = sum();
        double entropy = 0.0;
        if (sum == 0) {
            return 0;
        }
        for (double val : values) {
            double p = val / sum;
            if (p == 0.0) {
                entropy += 0.0;
            } else {
                entropy += p * Math.log(p);
            }
        }
        return -entropy;
    }

    /**
     * @return the standard deviation of the vector
     */
    public double std() {
        return Math.sqrt(variance());
    }

    public double variance() {
        double average = avg();
        double sumSquare = 0.0;
        for (double val : values) {
            sumSquare += Math.pow((val - average), 2.0);
        }
        return sumSquare / (values.length - 1);
    }

    /**
     * @return the relative standard deviation of the vector
     */
    public double relativeStd() {
        double average = avg();
        double sumSquare = 0.0;
        for (double val : values) {
            sumSquare += Math.pow((val - average), 2.0);
        }
        double variance = sumSquare / (values.length - 1);
        double stDev = Math.sqrt(variance);
        if (stDev == 0) {
            return 0;
        }
        return stDev / Math.abs(average);
    }

    public double max() {
        double maximum = Double.MIN_VALUE;
        for (double val : values) {
            if (val > maximum) {
                maximum = val;
            }
        }
        return maximum;
    }

    public double min() {
        double minimum = Double.MAX_VALUE;
        for (double val : values) {
            if (val < minimum) {
                minimum = val;
            }
        }
        return minimum;
    }

    public double norm() {
        return Math.sqrt(normSqr());
    }

    public double norm(double p) {
        double norm = 0;
        for (double val : values) {
            norm += Math.pow(Math.abs(val), p);
        }
        return Math.pow(norm, p);
    }

    public double normSqr() {
        double sum = 0.0;
        for (double state : values) {
            sum += state * state;
        }
        return sum;
    }

    /**
     * Computes the correlation coefficient of two energy plans
     *
     * @param other the second vector used for the computation of the
     * correlation coefficient
     * @return the correlation coefficient
     */
    public double correlationCoefficient(Vector other) {
        double[] vectorX = values;
        double[] vectorY = other.values;
        double result;
        double sum_sq_x = 0;
        double sum_sq_y = 0;
        double sum_coproduct = 0;
        double mean_x = vectorX[0];
        double mean_y = vectorY[0];
        for (int i = 2; i <= vectorX.length; i += 1) {
            double sweep = Double.valueOf(i - 1) / i;
            double delta_x = vectorX[i - 1] - mean_x;
            double delta_y = vectorY[i - 1] - mean_y;
            sum_sq_x += delta_x * delta_x * sweep;
            sum_sq_y += delta_y * delta_y * sweep;
            sum_coproduct += delta_x * delta_y * sweep;
            mean_x += delta_x / i;
            mean_y += delta_y / i;
        }
        double pop_sd_x = (double) Math.sqrt(sum_sq_x / vectorX.length);
        double pop_sd_y = (double) Math.sqrt(sum_sq_y / vectorY.length);
        double cov_x_y = sum_coproduct / vectorX.length;
        result = cov_x_y / (pop_sd_x * pop_sd_y);
        return result;
    }

    public double rootMeanSquareError(Vector other) {
        double[] vectorX = values;
        double[] vectorY = other.values;
        double squaredError = 0;
        for (int i = 0; i < vectorX.length; i++) {
            squaredError += Math.pow(vectorX[i] - vectorY[i], 2);
        }
        double meanSquaredError = squaredError / vectorX.length;
        double rootMeanSquaredError = Math.sqrt(meanSquaredError);
        return rootMeanSquaredError;
    }

    @Override
    public final void set(Vector other) {
        System.arraycopy(other.values, 0, values, 0, values.length);
    }

    @Override
    public void reset() {
        set(0);
    }

    /**
     * all dimensions are set to the specified value
     *
     * @param value
     */
    public void set(double value) {
        Arrays.fill(values, value);
    }

    @Override
    public void add(Vector other) {
        for (int i = 0; i < values.length; i++) {
            values[i] += other.values[i];
        }
    }

    public void add(double value) {
        for (int i = 0; i < values.length; i++) {
            values[i] += value;
        }
    }

    @Override
    public void subtract(Vector other) {
        for (int i = 0; i < values.length; i++) {
            values[i] -= other.values[i];
        }
    }

    public void subtract(double value) {
        for (int i = 0; i < values.length; i++) {
            values[i] -= value;
        }
    }

    /**
     * Multiplies the other plan element wise to this one.
     *
     * @param other
     */
    public void multiply(Vector other) {
        for (int i = 0; i < values.length; i++) {
            values[i] *= other.values[i];
        }
    }

    public void multiply(double factor) {
        for (int i = 0; i < values.length; i++) {
            values[i] *= factor;
        }
    }

    /**
     * Computes the power for each dimension
     *
     * @param x exponent
     */
    public void pow(double x) {
        for (int i = 0; i < values.length; i++) {
            values[i] = Math.pow(values[i], x);
        }
    }

    /**
     * Reverses the vector (mirror around average)
     */
    public void reverse() {
        double average = avg();
        for (int i = 0; i < values.length; i++) {
            values[i] = 2 * average - values[i];
        }
    }

    public static double[] meanVector(List<Vector> vectors) {
        Vector avg = new Vector(vectors.get(0).getNumDimensions());
        for (Vector p : vectors) {
            avg.add(p);
        }
        avg.multiply(1.0 / vectors.size());

        return avg.values;
    }

    public static double[][] covarianceMatrix(List<Vector> plans) {
        int n = plans.size();
        int d = plans.get(0).getNumDimensions();
        double[][] cov = new double[d][d];
        double[] avg = meanVector(plans);

        for (Vector p : plans) {
            for (int i = 0; i < d; i++) {
                for (int j = 0; j < d; j++) {
                    cov[i][j] += (p.values[i] - avg[i]) * (p.values[j] - avg[j]);
                }
            }
        }

        for (int i = 0; i < d; i++) {
            for (int j = 0; j < d; j++) {
                cov[i][j] /= (n - 1);
            }
        }

        return cov;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(values);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Vector)) {
            return false;
        }
        final Vector other = (Vector) obj;
        for (int i = 0; i < values.length; i++) {
            if (values[i] != other.values[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Vector cloneThis() {
        Vector clone = null;
        try {
            clone = (Vector) clone();
            clone.values = Arrays.copyOf(values, values.length);
        } catch (CloneNotSupportedException ex) {
            Logger.getLogger(Vector.class.getName()).log(Level.SEVERE, null, ex);
        }
        return clone;
    }
   
    @Override
    public Vector cloneNew() {
        Vector clone = null;
        try {
            clone = (Vector) clone();
            clone.values = new double[values.length];
            return clone;
        } catch (CloneNotSupportedException ex) {
            Logger.getLogger(Vector.class.getName()).log(Level.SEVERE, null, ex);
        }
        return clone;
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        out.append('[');
        if (values.length > 0) {
            out.append(values[0]);
        }
        for (int i = 1; i < values.length; i++) {
            out.append(',');
            out.append(values[i]);
        }
        out.append(']');
        return out.toString();
    }

    @Override
    public String toString(String format) {
        StringBuilder out = new StringBuilder();
        out.append('[');
        if (values.length > 0) {
            out.append(String.format(Locale.US, format, values[0]));
        }
        for (int i = 1; i < values.length; i++) {
            out.append(',');
            out.append(String.format(Locale.US, format, values[i]));
        }
        out.append(']');
        return out.toString();
    }

}
