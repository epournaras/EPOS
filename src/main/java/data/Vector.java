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
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import config.Configuration;

/**
 *
 * @author Peter P. & Jovan N.
 */
public class Vector implements DataType<Vector> {

    private double[] values;

    /**
     * create a new vector with given dimensionality
     *
     * @param numDimensions number of dimensions
     */
    public Vector(int numDimensions) {
        values = new double[numDimensions];
    }
    
    public Vector(double[] values) {
        this.values = values;
    }
    
    @Override
    public Vector getValue() {
        return this;
    }

    /**
     * 
     * @param idx - position in vector, user must ensure for idx < numDimensions
     * @param value - value to be set on position idx
     */
    public void setValue(int idx, double value) {
        values[idx] = value;
    }

    /**
     * 
     * @param idx - position from which to read value, user must ensure that idx < numDimensions
     * @return double value stored at position idx in vector
     */
    public double getValue(int idx) {
        return values[idx];
    }

    public int getNumDimensions() {
        return values.length;
    }

    /**
     * 
     * @return sum of all values in vector
     */
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

    /**
     * Calculates dot product of this Vector and other Vector: (this)^T * other (we assume Vector is column-vector)
     * @param other - Vector object, user must ensure that length of 'other' vector equals to length of this vector: this.getNumDimensions()
     * @return
     */
    public double dot(Vector other) {
        double dot = 0;
        for (int i = 0; i < values.length; i++) {
            dot += values[i] * other.values[i];
        }
        return dot;
    }

    /**
     * Calculates information entropy based on formula: -1 * SUM{ p(x_i) * log(p(x_i) | i = 0, ..., this.getNumDimensions()-1 }
     * x_i refers to i-th value in vector of length this.getNumDimensions(), log() refers to natural (e-based) logarithm
     * @return information entropy value
     */
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
     * Calculates standard deviation based on formula: Math.sqrt(this.variance())
     * @return the standard deviation of the vector
     */
    public double std() {
        return Math.sqrt(variance());
    }

    /**
     * Calculates variance amongst values in vector: SUM{ (x_i - this.avg())^2 | i = 0, ..., this.getNumDimensions()-1 } / (this.getNumDimensions() - 1)
     * @return double value representing variance
     */
    public double variance() {
        double average = this.avg();
        double sumSquare = 0.0;
        for (double val : values) {
            sumSquare += Math.pow((val - average), 2.0);
        }
        return sumSquare / values.length;
    }

    /**
     * Calculates relative standard deviation based on formula: this.std() / |this.average()|
     * However, it recalculates standard deviation itself, but reuses this.avg().
     * Modifying this.std() will not influence this method, but modifying this.avg() will!
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

    /**
     * Finds maximum value in the vector
     * @return double value representing first maximal value found in the vector starting from position 0.
     */
    public double max() {
        double maximum = Double.MIN_VALUE;
        for (double val : values) {
            if (val > maximum) {
                maximum = val;
            }
        }
        return maximum;
    }

    /**
     * Finds minimum value in the vector
     * @return double value representing first minimal value found in the vector starting from position 0.
     */
    public double min() {
        double minimum = Double.MAX_VALUE;
        for (double val : values) {
            if (val < minimum) {
                minimum = val;
            }
        }
        return minimum;
    }

    /**
     * Calculates Euclidean 2-norm using formula: Math.sqrt(this.normSqr()). In other words: sqrt((this)^T * this)
     * @return
     */
    public double norm() {
        return Math.sqrt(normSqr());
    }

    /**
     * Calculates p-norm using formula: ( SUM{ (x_i) ^ p | i = 0, ..., this.getNumDimensions()-1 } )^(1/p)
     * @param p - user must ensure that p >= 1, p is real number
     * @return
     */
    public double norm(double p) {
        double norm = 0;
        for (double val : values) {
            norm += Math.pow(Math.abs(val), p);
        }
        return Math.pow(norm, p);		// WARNING! ACHTUNG! HERE IT SHOULD BE 1/p, not p
    }

    /**
     * Calculates squared Euclidean 2-norm, or in other words, dot product with itself: (this)^T * this
     * @return
     */
    public double normSqr() {
        double sum = 0.0;
        for (double state : values) {
            sum += state * state;
        }
        return sum;
    }

    /**
     * Computes correlation coefficient of 2 vectors. FORMULA UKNOWN!
     * @param other the second vector used for the computation of the
     * correlation coefficient. User must ensure that other.getNumDimensions() == this.getNumDimensions()
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

    /**
     * Computes root mean squared error using formula: SQRT( 1/N * SUM{ (x[i] - y[i])^2 | i = 0, ..., this.getNumDimensions()-1 } )
     * @param other - Vector object, user must ensure that other.getNumDimensions() == this.getNumDimensions()
     * @return double value representing root mean squared error
     */
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
     * Sets every indice in vector values to fixed value
     * @param value - fixed value to be set to every element
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

    /**
     * In-place addition of 'value' to every element of vector 'this'
     * @param value - value to be added
     */
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

    /**
     * In-place subtraction 'value' from every element of internal vector 'this'
     * @param value - value to be subtracted
     */
    public void subtract(double value) {
        for (int i = 0; i < values.length; i++) {
            values[i] -= value;
        }
    }

    /**
     * In-place element-wise multiplication of vector 'this' and vector 'other'
     * @param other
     */
    public void multiply(Vector other) {
        for (int i = 0; i < values.length; i++) {
            values[i] *= other.values[i];
        }
    }

    /**
     * In-place multiplication of every element of 'vector' by 'factor' 
     * @param factor
     */
    public void multiply(double factor) {
        for (int i = 0; i < values.length; i++) {
            values[i] *= factor;
        }
    }

    /**
     * In-place computation of the power for each dimension
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

    /**
     * Computes element-wise mean of vectors from the list by using this.add(vectors) and then this.multiply(1/vectors.size())
     * @param vectors - user must ensure that all vectors in the list are of equal size
     * @return double array of values representing mean vector
     */
    public static double[] meanVector(List<Vector> vectors) {
        Vector avg = new Vector(vectors.get(0).getNumDimensions());
        for (Vector p : vectors) {
            avg.add(p);
        }
        avg.multiply(1.0 / vectors.size());

        return avg.values;
    }
    
    /**
     * Computes element-wise mean of vectors from the list by using this.add(vectors) and then this.multiply(1/vectors.size())
     * @param vectors - user must ensure that all vectors in the list are of equal size
     * @return vector representing mean vector
     */
    public static Vector meanVector1(List<Vector> vectors) {
        Vector avg = new Vector(vectors.get(0).getNumDimensions());
        for (Vector p : vectors) {
            avg.add(p);
        }
        avg.multiply(1.0 / vectors.size());

        return avg;
    }
    
    /**
     * Computes residual sum of squares via function:
     * 		RSS = SUM{ (v[i] - u[i])^2 | i = 0, ..., v.numDimensions() }
     * Note that user must ensure that <code>v</code> and <code>u</code>
     * are of the same dimensionality.
     * @param v vector 1
     * @param u vector 2
     * @return
     */
    public static double residualSumOfSquares(Vector v, Vector u) {
    	Vector other = v.cloneThis();
    	other.subtract(u);
    	other.pow(2);
    	return other.sum();
    }

    /**
     * Computes covariance matrix (symmetrical, dimensions: d x d) of given plans
     * @param plans - list of plans, user must ensure that they are of equal size d
     * @return double array matrix d x d representing covariance matrix
     */
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
    /**
     * 2 Vectors are equal if:
     *  - 'obj' is pointer to this
     *  - 'obj' is not null and points to object of Vector type that has equal sequence of equal values as 'this'
     */
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
        if (values.length > 0) {
            out.append(values[0]);
        }
        for (int i = 1; i < values.length; i++) {
            out.append(',');
            out.append(values[i]);
        }
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
    
    //////////////////////////////////////////////////////////////////////////////////////////////////
    /// BY JOVAN:
    
    public Complex[] convert2complex() {
    	Complex[] complex = new Complex[this.values.length];
    	IntStream.range(0, this.values.length).forEach(i -> {
    		Complex c = new Complex(this.values[i]);
    		complex[i] = c;
    	});
    	return complex;
    }    
    
    /**
     * Computes Forward Fourier Transformation of given signal in time domain.
     * Note that it uses UNITARY transformation which preserves value of inner product 
     * before and after transformation.
     * @param other time signal
     * @return array of Complex values representing Fourier Transformation
     */
    public static Complex[] forwardFourierTransform(Vector other) {
    	FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.UNITARY);    	
    	// TODO does transform change given array???
    	int newlength = other.getNumDimensions() <= 128 ? 128 : 256;
    	double[] newarray = new double[newlength];
    	for(int i = 0; i < newlength; i++) {
    		if(i < other.getNumDimensions()) {
    			newarray[i] = other.getValue(i);
    		} else {
    			newarray[i] = 0;
    		}
    	}
    	Complex[] result = fft.transform(newarray, TransformType.FORWARD);
    	return result;
    }
    
    /**
     * Computes inverse Fourier Transformation of given signal in frequency domain.
     * Note that it uses UNITARY transformation which preserves values of inner product
     * before and after transformation.
     * @param complex signal in frequency domain
     * @return array of Complex values representing signal in time domain
     */
    public static Complex[] inverseFourierTransform(Complex[] complex) {
    	FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.UNITARY);    	
    	
    	int newlength = complex.length <= 128 ? 128 : 256;
    	Complex[] newarray = new Complex[newlength];
    	for(int i = 0; i < newlength; i++) {
    		if(i < complex.length) {
    			newarray[i] = complex[i];
    		} else {
    			newarray[i] = new Complex(0);
    		}
    	}    	
    	
    	Complex[] result = fft.transform(newarray, TransformType.INVERSE);
    	return result;
    }
    
    /**
     * For now, it just takes the real part of every complex number.
     * Note that our time signals are given only in real domcomplexain. 
     * Applying Fourier transform on real time signal, and then applying 
     * inverse Fourier transform on this signal in frequency domain should preserve
     * the original signal.
     * @param complex signal in time domain represented as array of complex numbers
     * @return Vector representing only real parts of given complex numbers.
     */
    public static Vector convertWreal(Complex[] complex) {
    	Vector v = new Vector(Configuration.planDim);
    	for(int i = 0; i < v.getNumDimensions(); i++) {
    		v.setValue(i, complex[i].getReal());
    	}
    	return v;
    }
    
    /**
     * For now, it just takes the modulus of every complex number.
     * Note that our time signals are given only in real domain. 
     * Applying Fourier transform on real time signal, and then applying 
     * inverse Fourier transform on this signal in frequency domain should preserve
     * the original signal.
     * If complex number x = a + b*j, then modulus = sqrt(a^2 + b^2), where j = sqrt(-1)
     * @param complex signal in time domain represented as array of complex numbers
     * @return Vector representing only modulus of given complex numbers.
     */
    public static Vector convertWabs(Complex[] complex) {
    	Vector v = new Vector(Configuration.planDim);
    	for(int i = 0; i < v.getNumDimensions(); i++) {
    		v.setValue(i, complex[i].abs());
    	}
    	return v;
    }
    
    /**
     * Cross-correlation of 2 signals in time domain x(t) and y(t) translates to the following 
     * in frequency domain:
     *                       F[x(t) * y(t)] = ~X Y
     * where F[] is Fourier transform, * is cross-correlation operator and ~X is complex conjugate of X.
     * In other words, Fourier transform  of cross-correlation of 2 signals represents product of complex cojugate
     * Fourier transform of one signal and Fourier transform of the other signal.
     * 
     * Note that this method conjugates the first signal!.
     * 
     * @param X Fourier transform of first signal
     * @param y Fourier transform of second signal
     * @return Cross correlation of 2 signals in frequency domain
     */
    public static Complex[] crossCorrelationInFrequencyDomain(Complex[] X, Complex[] Y) {
    	if(X.length != Y.length) {
    		System.err.println("Signals in frequency domain are not of the same length!");
    	}
    	Complex[] result = new Complex[X.length];
    	for(int i = 0; i < X.length; i++) {
    		result[i] = X[i].conjugate().multiply(Y[i]);
    	}    	
    	return result;
    }
    
    /**
     * Applies Student's t-statistics for "standardization" of data.
     * It converts value x_i to (x_i - x_mean)/x_std
     * where x_mean is the mean of the vector and x_std is standard deviation of the vector
     * 
     * For numerical stability, small constant 1e-10 is added to the denominator in 
     * case that deviation is 0.
     * 
     * As a result all values have zero mean and unit variance.
     * @return normalized vector
     */    
    public static UnaryOperator<Vector> standard_normalization = (Vector v) -> {
    	Vector normalized = new Vector(v.getNumDimensions());
    	double mean = v.avg();
    	double std = v.std();
    	for(int i = 0; i < v.values.length; i++) {
    		normalized.setValue(i, (v.values[i]-mean)/(std + 1e-10));
    	}
    	return normalized;
    };
    
    /**
     * Applies min-max scaling known also as "feature scaling".
     * The formula is (x_i - x_min)/(x_max - x_min)
     * where x_min is the minimal and x_max the maximal value in the signal.
     * 
     * As a result all values are in range [0,1]
     * @return normalized vector
     */
    public static UnaryOperator<Vector> min_max_normalization = (Vector v) -> {
    	Vector normalized = new Vector(v.getNumDimensions());
    	double min = v.min();
    	double max = v.max();
    	for(int i = 0; i < v.values.length; i++) {
    		normalized.setValue(i, (v.values[i]-min)/(max-min));
    	}
    	return normalized;
    };
    
    /**
     * Applies unit length normalization which scales the signal
     * to have length of 1 (in Euclidean space).
     * 
     * Implements function x_i / ||x||
     * where ||x|| is L-2 norm.
     * @return
     */
    public static UnaryOperator<Vector> unit_length_normalization = (Vector v) -> {
    	Vector normalized = new Vector(v.getNumDimensions());
    	double norm = Math.sqrt(v.normSqr());
    	for(int i = 0; i < v.values.length; i++) {
    		normalized.setValue(i, v.values[i]/(norm + 1e-10));
    	}
    	return normalized;
    };
    
    /**
     * Creates a new vector with exactly the same values as provided vector.
     * Can be used in cases 
     */
    public static UnaryOperator<Vector> no_normalization = (Vector v) -> {
    	Vector normalized = new Vector(v.getNumDimensions());
    	for(int i = 0; i < v.values.length; i++) {
    		normalized.setValue(i, v.values[i]);
    	}
    	return normalized;
    };

	public double[] getValues() {
		return values;
	}
    
    
}
