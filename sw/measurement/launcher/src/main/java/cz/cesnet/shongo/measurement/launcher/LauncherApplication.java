package cz.cesnet.shongo.measurement.launcher;

import org.apache.commons.cli.*;

public class LauncherApplication {

    public static void main(String[] args) throws Exception {
        Option help = new Option("h", "help", false, "Print this usage information");
        Option remote = OptionBuilder.withLongOpt("remote")
                .withDescription("Launch remote")
                .create("a");
        Option launch = OptionBuilder.withLongOpt("launch")
                .withArgName("file")
                .hasArg()
                .withDescription("Launch from file that contains commands")
                .create("c");

        // Create options
        Options options = new Options();
        options.addOption(help);
        options.addOption(remote);
        options.addOption(launch);

        // Parse command line
        CommandLine commandLine = null;
        try {
            CommandLineParser parser = new PosixParser();
            commandLine = parser.parse(options, args);
        } catch ( ParseException e ) {
            System.out.println("Error: " + e.getMessage());
            return;
        }

        // Print help
        if ( commandLine.hasOption("help") || commandLine.getOptions().length == 0 ) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("launcher", options);
            System.exit(0);
        }

        // Create agent
        if ( commandLine.hasOption("remote") ) {
            throw new Exception("TODO: Implement launching remote");
        }

        if ( commandLine.hasOption("launch") ) {
            String launchFile = commandLine.getOptionValue("launch");
            FileLauncher.launchFile(launchFile);
        }
    }

}
