package cz.cesnet.shongo.measurement.launcher;

import org.apache.commons.cli.*;

public class LauncherApplication {
    
    public static final String RUN_JXTA = "./jxta.sh";
    public static final String RUN_JADE = "./jade.sh";

    public static void main(String[] args) throws Exception {
        Option help = new Option("h", "help", false, "Print this usage information");
        Option remote = OptionBuilder.withLongOpt("remote")
                .withArgName("port")
                .hasArg()
                .withDescription("Launch remote")
                .create("r");
        Option launch = OptionBuilder.withLongOpt("launch")
                .withArgName("file")
                .hasArg()
                .withDescription("Launch from file that contains commands")
                .create("l");
        Option platform = OptionBuilder.withLongOpt("platform")
                .withArgName("[jxta|jade]")
                .hasArg()
                .withDescription("Set launching platform")
                .create("p");

        // Create options
        Options options = new Options();
        options.addOption(help);
        options.addOption(remote);
        options.addOption(launch);
        options.addOption(platform);

        // Parse command line
        CommandLine commandLine = null;
        try {
            CommandLineParser parser = new PosixParser();
            commandLine = parser.parse(options, args);
        } catch ( ParseException e ) {
            System.out.println("Error: " + e.getMessage());
            return;
        }
        
        String runPlatform = RUN_JXTA;
        if ( commandLine.hasOption("platform") ) {
            String platformValue = commandLine.getOptionValue("platform");
            if ( platformValue.equals("jxta") )
                runPlatform = RUN_JXTA;
            else if ( platformValue.equals("jade") )
                runPlatform = RUN_JADE;
            else {
                System.out.printf("Unknown platform '%s'!");
                System.exit(-1);
            }
        }

        // Print help
        if ( commandLine.hasOption("help") || commandLine.getOptions().length == 0 ) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("launcher", options);
            System.exit(0);
        }

        // Create remote
        if ( commandLine.hasOption("remote") ) {
            int port = Integer.parseInt(commandLine.getOptionValue("remote"));
            RemoteLauncher.launchRemote(port);
        }

        // Launch file
        if ( commandLine.hasOption("launch") ) {
            String launchFile = commandLine.getOptionValue("launch");
            FileLauncher.launchFile(launchFile, runPlatform);
        }
    }

}
