package cz.cesnet.shongo.measurement.launcher;

import org.apache.commons.cli.*;

public class LauncherApplication {
    
    public static final int REMOTE_PORT = 9000;

    public static void main(String[] args) throws Exception
    {
        Option help = new Option("h", "help", false, "Print this usage information");
        Option remote = OptionBuilder.withLongOpt("remote")
                .withArgName("port")
                .hasOptionalArg()
                .withDescription("Launch remote")
                .create("r");
        Option launch = OptionBuilder.withLongOpt("launch")
                .withArgName("file")
                .hasArg()
                .withDescription("Launch from file that contains commands")
                .create("l");
        Option platform = OptionBuilder.withLongOpt("platform")
                .withArgName("jxta|jade")
                .hasArg()
                .withDescription("Set launching platform")
                .create("p");
        Option extension = OptionBuilder.withLongOpt("extension")
                .withArgName("sh|bat")
                .hasArg()
                .withDescription("Set extension for platform scripts")
                .create("e");
        Option define = OptionBuilder.withLongOpt("define")
                .withArgName("name=value")
                .withValueSeparator(';')
                .hasArgs()
                .withDescription("Define constant")
                .create("D");

        // Create options
        Options options = new Options();
        options.addOption(help);
        options.addOption(remote);
        options.addOption(launch);
        options.addOption(platform);
        options.addOption(extension);
        options.addOption(define);

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
        if ( commandLine.hasOption("help") || commandLine.getOptions().length == 0
                || (commandLine.getOptions().length == 1 && commandLine.hasOption("extension") ) )  {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("launcher", options);
            System.exit(0);
        }

        // Get script name for running platfrom
        String platformType = "jxta";
        if ( commandLine.hasOption("platform") ) {
            platformType = commandLine.getOptionValue("platform");
        }

        // Append extension to script name
        String extensionValue = "sh";
        if ( commandLine.hasOption("extension") ) {
            extensionValue = commandLine.getOptionValue("extension");
        }

        // Create remote
        if ( commandLine.hasOption("remote") ) {
            int port = REMOTE_PORT;
            if ( commandLine.getOptionValue("remote") != null )
                port = Integer.parseInt(commandLine.getOptionValue("remote"));
            RemoteLauncher.launchRemote(port);
        }

        // Get variables
        Evaluator evaluator = new Evaluator();
        evaluator.setVariable("platform", platformType);
        evaluator.setVariable("extension", extensionValue);
        String[] defines = commandLine.getOptionValues("define");
        if ( defines != null ) {
            for ( String defineValue : defines ) {
                String[] parts = defineValue.split("=");
                if ( parts.length < 2 )
                    continue;
                evaluator.setVariable(parts[0], parts[1]);
            }
        }

        // Launch file
        if ( commandLine.hasOption("launch") ) {
            String launchFile = commandLine.getOptionValue("launch");
            FileLauncher.launchFile(launchFile, evaluator);
        }
    }

}
