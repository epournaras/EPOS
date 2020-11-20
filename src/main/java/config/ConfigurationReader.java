package config;

import org.apache.commons.cli.*;

import java.io.FileReader;
import java.util.Properties;

public class ConfigurationReader {
	
	public static Properties initializeConfig(String[]args) {
		Properties prop = readConfigurationFile(args);
		CommandLine line = parseCommandLine(args);
		
		for(Option o:line.getOptions()) {
			if(o.getValue() != null) {
				prop.put(o.getOpt(), o.getValue());
			} else {
				prop.put(o.getOpt(), true);
			}			
		}
		return prop;
	}
	
	private static Properties readConfigurationFile(String[] args) {
		String configfile = "conf/dias.conf";
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-p"))
				configfile = args[i + 1];
		}
		Properties prop = new Properties();
		try {
			prop.load(new FileReader(configfile));
		} catch (Exception e) {
			System.out.println("Could not read configuraton file, using internal defaults: " + e.getMessage());
		}
		
		System.out.println( "properties read from " + configfile + " : " + prop );

		return prop;
	}

	private static CommandLine parseCommandLine(String[] args) {
		Options opts = new Options();
		opts.addOption("h","show helptext");
		
		opts.addOption("N", true, "total number of participating nodes");
		opts.addOption("runDuration", true, "run duration in number of epochs");
		
		opts.addOption("c", true, "view length");
		opts.addOption("H", true, "healing");
		opts.addOption("S", true, "swap");
		
		opts.addOption("Tpss", true, "PSS comm period");
		opts.addOption("Tdias", true, "DIAS comm period");
		opts.addOption("Tsampling", true, "DIAS-PNS sampling period");
		opts.addOption("Tboot", true, "DIAS bootstrapping period");
		opts.addOption("Tbootpss", true, "PSS bootstrapping period");
		opts.addOption("Tbootfixedtopology", true, "Fake bootstrapping period for live experiments with fixed topology");
		
		opts.addOption("sampleSize", true, "DIAS neighbors list length");
		opts.addOption("numOfSessions",true,"number of sessions");
		
		opts.addOption("name", true, "Experiment name");
		opts.addOption("myIP", true, "own iP");
		opts.addOption("myPort", true, "own Port");
		opts.addOption("bootstrapIP", true, "zero iP");
		opts.addOption("bootstrapPort", true, "zero Port");
		opts.addOption("myIndex", true, "index");
		
		opts.addOption("skipTransitions", true, "allows every <specified> transition");
		opts.addOption("enablePSSlogging", false, "enables logging PSS info");
		opts.addOption("enableDIASmsgsLogging", false, "enables PUSH, PULL, LEAVE and RETURN messages logging");
		opts.addOption("topKbufferSize", true, "Min/Max buffer size for top-K algorithm");
		opts.addOption("enableJsons", false, "Enable dumping jSons for visualization purposes");
		
		CommandLineParser clp = new DefaultParser();
		try {
			CommandLine line = clp.parse(opts, args);
			if(line.hasOption("h")) {
				new HelpFormatter().printHelp( "java LiveRun [OPTIONS]", opts );
				System.exit(1);
			}
			return line;
		} catch (Exception e) {
			System.err.println("Could not parse command line: " + e.getMessage());
			System.exit(1);
		}
		return null;
	}
}
