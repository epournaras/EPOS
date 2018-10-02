package func.goalsignals;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Locale;
import java.util.Scanner;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import config.Configuration;
import data.Vector;

/**
 * This class is a collection of static methods that generate goal signal
 * 
 * @author jovan
 *
 */
public class GoalSignalsCollection {
	
	/**
	 * Sine function with amplitude 100 and offset 0
	 * 
	 */	
	public static Supplier<Vector> sine_a100_o0 = () -> {
		Vector newvector = new Vector(Configuration.planDim);
		IntStream.range(0, Configuration.planDim).
			forEach(i -> newvector.setValue(i,0 + 100*Math.sin(i * Math.PI / 45)));
		return newvector;
	};
	
	
	/**
	 * Sine function with amplitude 100 and offset 500
	 */
	public static Supplier<Vector> sine_a100_o500 = () -> {		
		Vector newvector = new Vector(Configuration.planDim);
		IntStream.range(0, Configuration.planDim).
			forEach(i -> newvector.setValue(i,500 + 100*Math.sin(i * Math.PI / 45)));
		return newvector;
	};
	
	/**
	 * Sine function with amplitude 1000 and offset 200
	 */
	public static Supplier<Vector> sine_a200_o1000 = () -> {	
		Vector newvector = new Vector(Configuration.planDim);
		IntStream.range(0, Configuration.planDim).
			forEach(i -> newvector.setValue(i,1000 + 200*Math.sin(i * Math.PI / 45)));
		return newvector;		
	};
	
	/**
	 * Single impulse function with amplitude 100.
	 * Left quarter is 0, right quarter is 0, middle half is equal to amplitude value
	 */
	public static Supplier<Vector> singleImpulse_a100 = () -> {
		Vector newvector = new Vector(Configuration.planDim);
		int quarter = (int)Configuration.planDim/4;		
		IntStream.range(0, quarter).
			forEach(i -> newvector.setValue(i, 0));		
		IntStream.range(quarter, Configuration.planDim-quarter).
			forEach(i -> newvector.setValue(i, 100));
		IntStream.range(Configuration.planDim-quarter, Configuration.planDim).
			forEach(i -> newvector.setValue(i, 0));
		return newvector;
	};
	
	/**
	 * Single impulse function with amplitude 500.
	 * Left quarter is 0, right quarter is 0, middle half is equal to amplitude value
	 */
	public static Supplier<Vector> singleImpulse_a500 = () -> {
		Vector newvector = new Vector(Configuration.planDim);
		int quarter = (int)Configuration.planDim/4;		
		IntStream.range(0, quarter).
			forEach(i -> newvector.setValue(i, 0));		
		IntStream.range(quarter, Configuration.planDim-quarter).
			forEach(i -> newvector.setValue(i, 500));
		IntStream.range(Configuration.planDim-quarter, Configuration.planDim).
			forEach(i -> newvector.setValue(i, 0));
		return newvector;
	};
	
	/**
	 * Single impulse function with amplitude 1000.
	 * Left quarter is 0, right quarter is 0, middle half is equal to amplitude value
	 */
	public static Supplier<Vector> singleImpulse_a1000 = () -> {
		Vector newvector = new Vector(Configuration.planDim);
		int quarter = (int)Configuration.planDim/4;		
		IntStream.range(0, quarter).
			forEach(i -> newvector.setValue(i, 0));		
		IntStream.range(quarter, Configuration.planDim-quarter).
			forEach(i -> newvector.setValue(i, 1000));
		IntStream.range(Configuration.planDim-quarter, Configuration.planDim).
			forEach(i -> newvector.setValue(i, 0));
		return newvector;
	};
	
	
	/**
	 * Single impulse function with amplitude 100.
	 * Left quarter is 50, right quarter is 50, middle half is equal to amplitude value
	 */
	public static Supplier<Vector> singleImpulse_a100_b50 = () -> {
		Vector newvector = new Vector(Configuration.planDim);
		int quarter = (int)Configuration.planDim/4;		
		IntStream.range(0, quarter).
			forEach(i -> newvector.setValue(i, 50));		
		IntStream.range(quarter, Configuration.planDim-quarter).
			forEach(i -> newvector.setValue(i, 100));
		IntStream.range(Configuration.planDim-quarter, Configuration.planDim).
			forEach(i -> newvector.setValue(i, 50));
		return newvector;
	};
	
	/**
	 * Single impulse function with amplitude 500.
	 * Left quarter is 250, right quarter is 250, middle half is equal to amplitude value
	 */
	public static Supplier<Vector> singleImpulse_a500_b250 = () -> {
		Vector newvector = new Vector(Configuration.planDim);
		int quarter = (int)Configuration.planDim/4;		
		IntStream.range(0, quarter).
			forEach(i -> newvector.setValue(i, 250));		
		IntStream.range(quarter, Configuration.planDim-quarter).
			forEach(i -> newvector.setValue(i, 500));
		IntStream.range(Configuration.planDim-quarter, Configuration.planDim).
			forEach(i -> newvector.setValue(i, 250));
		return newvector;
	};
	
	/**
	 * Single impulse function with amplitude 1000.
	 * Left quarter is 500, right quarter is 500, middle half is equal to amplitude value
	 */
	public static Supplier<Vector> singleImpulse_a1000_b500 = () -> {
		Vector newvector = new Vector(Configuration.planDim);
		int quarter = (int)Configuration.planDim/4;		
		IntStream.range(0, quarter).
			forEach(i -> newvector.setValue(i, 500));		
		IntStream.range(quarter, Configuration.planDim-quarter).
			forEach(i -> newvector.setValue(i, 1000));
		IntStream.range(Configuration.planDim-quarter, Configuration.planDim).
			forEach(i -> newvector.setValue(i, 500));
		return newvector;
	};
	
	/**
	 * Single impulse function with amplitude 2000.
	 * Left quarter is 1000, right quarter is 1000, middle half is equal to amplitude value
	 */
	public static Supplier<Vector> singleImpulse_a2000_b1000 = () -> {
		Vector newvector = new Vector(Configuration.planDim);
		int quarter = (int)Configuration.planDim/4;		
		IntStream.range(0, quarter).
			forEach(i -> newvector.setValue(i, 1000));		
		IntStream.range(quarter, Configuration.planDim-quarter).
			forEach(i -> newvector.setValue(i, 2000));
		IntStream.range(Configuration.planDim-quarter, Configuration.planDim).
			forEach(i -> newvector.setValue(i, 1000));
		return newvector;
	};
	
	
	/**
	 * Monotonous linear step-like decrease from 400 to 50
	 */
	public static Supplier<Vector> monotonousDecrease_400_50 = () -> {
		Vector newvector = new Vector(Configuration.planDim);
		int eight = (int)Configuration.planDim/8;	
		int batch_size = 50;
		
		for(int i = 1; i <= 8; i++) {
			final int iter = i;
			if(i == 8) {
				IntStream.range((i-1)*eight, Configuration.planDim).
					forEach(j -> newvector.setValue(j, (8-iter+1)*batch_size));
			} else {
				IntStream.range((i-1)*eight, i*eight).
					forEach(j -> newvector.setValue(j, (8-iter+1)*batch_size));
			}			
		}
		return newvector;
	};
	
	/**
	 * Monotonous linear step-like decrease from 400 to 50
	 */
	public static Supplier<Vector> monotonousDecrease_800_100 = () -> {
		Vector newvector = new Vector(Configuration.planDim);
		int eight = (int)Configuration.planDim/8;	
		int batch_size = 100;
		
		for(int i = 1; i <= 8; i++) {
			final int iter = i;
			if(i == 8) {
				IntStream.range((i-1)*eight, Configuration.planDim).
					forEach(j -> newvector.setValue(j, (8-iter+1)*batch_size));
			} else {
				IntStream.range((i-1)*eight, i*eight).
					forEach(j -> newvector.setValue(j, (8-iter+1)*batch_size));
			}			
		}
		return newvector;
	};
	
	/**
	 * Monotonous linear step-like decrease from 400 to 50
	 */
	public static Supplier<Vector> monotonousDecrease_1600_200 = () -> {
		Vector newvector = new Vector(Configuration.planDim);
		int eight = (int)Configuration.planDim/8;	
		int batch_size = 200;
		
		for(int i = 1; i <= 8; i++) {
			final int iter = i;
			if(i == 8) {
				IntStream.range((i-1)*eight, Configuration.planDim).
					forEach(j -> newvector.setValue(j, (8-iter+1)*batch_size));
			} else {
				IntStream.range((i-1)*eight, i*eight).
					forEach(j -> newvector.setValue(j, (8-iter+1)*batch_size));
			}			
		}
		return newvector;
	};
	
	
	/**
	 * Monotonously increasing function from -10 to 10
	 */
	public static Supplier<Vector> monotonousIncrease_m10_10 = () -> {
		Vector newvector = new Vector(Configuration.planDim);
		int half = (int)Configuration.planDim/2;		
		IntStream.range(0, half).
			forEach(i -> newvector.setValue(i, -10));		
		IntStream.range(half, Configuration.planDim).
			forEach(i -> newvector.setValue(i, 10));
		return newvector;
	};
	
	/**
	 * Monotonously increasing function from -100 to 100
	 */
	public static Supplier<Vector> monotonousIncrease_m100_100 = () -> {
		Vector newvector = new Vector(Configuration.planDim);
		int half = (int)Configuration.planDim/2;		
		IntStream.range(0, half).
			forEach(i -> newvector.setValue(i, -100));		
		IntStream.range(half, Configuration.planDim).
			forEach(i -> newvector.setValue(i, 100));
		return newvector;
	};
	
	/**
	 * Monotonously increasing function from 150 to 250
	 */
	public static Supplier<Vector> monotonousIncrease_150_250 = () -> {
		Vector newvector = new Vector(Configuration.planDim);
		int half = (int)Configuration.planDim/2;		
		IntStream.range(0, half).
			forEach(i -> newvector.setValue(i, 150));		
		IntStream.range(half, Configuration.planDim).
			forEach(i -> newvector.setValue(i, 250));
		return newvector;
	};
	
	/**
	 * Monotonously increasing function from 1300 to 1500
	 */
	public static Supplier<Vector> monotonousIncrease_1300_1500 = () -> {
		Vector newvector = new Vector(Configuration.planDim);
		int half = (int)Configuration.planDim/2;		
		IntStream.range(0, half).
			forEach(i -> newvector.setValue(i, 1300));		
		IntStream.range(half, Configuration.planDim).
			forEach(i -> newvector.setValue(i, 1500));
		return newvector;
	};
	
	/**
	 * Monotonously increasing function from 8 to 10
	 */
	public static Supplier<Vector> monotonousIncrease_8_10 = () -> {
		Vector newvector = new Vector(Configuration.planDim);
		int half = (int)Configuration.planDim/2;		
		IntStream.range(0, half).
			forEach(i -> newvector.setValue(i, 8));		
		IntStream.range(half, Configuration.planDim).
			forEach(i -> newvector.setValue(i, 10));
		return newvector;
	};
	
	/**
	 * 2 single impulses, one twice as big as the other one.
	 */
	public static Supplier<Vector> camelImpulse_50_100 = () -> {
		Vector newvector = new Vector(Configuration.planDim);
		
		int five = (int)Configuration.planDim/5;
		
		IntStream.range(0, five).
			forEach(i -> newvector.setValue(i, 0));
		IntStream.range(five, 2*five).
			forEach(i -> newvector.setValue(i, 50));
		IntStream.range(2*five, 3*five).
			forEach(i -> newvector.setValue(i, 0));
		IntStream.range(3*five, 4*five).
			forEach(i -> newvector.setValue(i, 100));
		IntStream.range(5*five, Configuration.planDim).
			forEach(i -> newvector.setValue(i, 0));
		
		return newvector;
	};
	
	/**
	 * 2 single gaussian impulses
	 */
	public static Supplier<Vector> gaussian_mixture_impulse = () -> {
		Vector newvector = new Vector(Configuration.planDim);
		double mean_1 = 30;
		double mean_2 = 75;
		double std_1 = 10;
		double std_2 = 15;
		IntStream.range(0, Configuration.planDim).forEach(i -> {
			double value_1 = 20*Math.pow(Math.sqrt(2*Math.PI*Math.sqrt(std_1)), -1) * 
					         Math.exp(-1*Math.pow(i - mean_1, 2)/(2*Math.pow(std_1, 2)));
			double value_2 = 30*Math.pow(Math.sqrt(2*Math.PI*Math.sqrt(std_2)), -1) * 
			         Math.exp(-1*Math.pow(i - mean_2, 2)/(2*Math.pow(std_2, 2)));
			newvector.setValue(i, value_1 + value_2);
		});
		return newvector;
	};
	
	/**
	 * constant to a reasonable value
	 */
	public static Supplier<Vector> constant_signal = () -> {
		Vector newvector = new Vector(Configuration.planDim);
		double constant = 5;
		IntStream.range(0, Configuration.planDim).forEach(i -> {
			newvector.setValue(i, constant);
		});
		return newvector;
	};
	
	/**
	 * This goal signal will be applied in frequency domain.
	 */
	public static Supplier<Vector> frequencyGoal = () -> {
		Vector newvector = new Vector(Configuration.planDim);
		IntStream.range(0, Configuration.planDim).forEach(i -> {
			if(i == 3) {
				newvector.setValue(i, 10);
			} else if (i == 2) {
				newvector.setValue(i, 5);
			} else {
				newvector.setValue(i, 0);
			}
		});
		return newvector;
	};
	
	public static Supplier<Vector> upperBound = () -> {
		Vector newvector = new Vector(Configuration.planDim);
		IntStream.range(0, Configuration.planDim).forEach(i -> {
			newvector.setValue(i, 10e6);
		});
		return newvector;
	};
	
	public static Supplier<Vector> lowerBound = () -> {
		Vector newvector = new Vector(Configuration.planDim);
		IntStream.range(0, Configuration.planDim).forEach(i -> {
			newvector.setValue(i, -1);
		});
		return newvector;
	};
	
	/**
	 * Reads goal signal from a file. 
	 */
	public static Supplier<Vector> fromFile = () -> {
		Vector newvector = new Vector(Configuration.planDim);
		File file = new File(Configuration.getGoalSignalPath());
		try (Scanner scanner = new Scanner(file)) {
			scanner.useLocale(Locale.US);
			for (int i = 0; scanner.hasNextLine(); i++) {
                String line = scanner.nextLine();
                double val = Double.parseDouble(line);
                newvector.setValue(i, val);
            }
		} catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch(Exception e) {
        	e.printStackTrace();
        }
		double mean = newvector.avg();
		newvector.multiply(-1.0);
		newvector.add(2*mean);
		double newmean = newvector.avg();
		if(mean != newmean) {
			System.err.println("MEANS NOT EQUAL: before = " + mean + ", after: " + newmean);
		}
		return newvector;
	};
	
	/**
	 * Reads goal signal from a file. 
	 */
	public static Supplier<Vector> fromOnelinerFile = () -> {
		Vector newvector = new Vector(Configuration.planDim);
		File file = new File(Configuration.getGoalSignalPath());
		try (Scanner scanner = new Scanner(file)) {
			scanner.useLocale(Locale.US);
			String line = scanner.nextLine();
			String[] splitline = line.split(",");
			for(int i = 0; i < splitline.length; i++) {
				double val = Double.parseDouble(splitline[i]);
				if(i >= Configuration.planDim) continue;
				newvector.setValue(i, val);
			}
		} catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch(Exception e) {
        	e.printStackTrace();
        }
//		double mean = newvector.avg();
//		newvector.multiply(-1.0);
//		newvector.add(2*mean);
//		double newmean = newvector.avg();
//		if(mean != newmean) {
//			System.err.println("MEANS NOT EQUAL: before = " + mean + ", after: " + newmean);
//		}
		return newvector;
	};
	
	

}
