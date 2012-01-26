package cz.cesnet.shongo.measurement.launcher;

import org.apache.commons.cli.*;

import java.util.HashMap;
import java.util.Map;

public class LauncherApplication {
    
    public static final int REMOTE_PORT = 9000;
    public static final String RUN_JXTA = "./jxta";
    public static final String RUN_JADE = "./jade";

    public static void main(String[] args) throws Exception {
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
                .create("p");
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

        // Get script name for running platfrom
        String runPlatform = RUN_JXTA;
        if ( commandLine.hasOption("platform") ) {
            String platformValue = commandLine.getOptionValue("platform");
            if ( platformValue.equals("jxta") )
                runPlatform = RUN_JXTA;
            else if ( platformValue.equals("jade") )
                runPlatform = RUN_JADE;
            else {
                System.out.printf("Unknown platform '%s'!", platformValue);
                System.exit(-1);
            }
        }

        // Append extension to script name
        if ( commandLine.hasOption("extension") ) {
            String extensionValue = commandLine.getOptionValue("extension");
            if ( extensionValue.equals("sh") )
                runPlatform = runPlatform + ".sh";
            else if ( extensionValue.equals("bat") )
                runPlatform = runPlatform + ".bat";
            else {
                System.out.printf("Unknown extension '%s'!", extensionValue);
                System.exit(-1);
            }
        } else {
            // Default extension is .sh
            runPlatform = runPlatform + ".sh";
        }

        // Print help
        if ( commandLine.hasOption("help") || commandLine.getOptions().length == 0 ) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("launcher", options);
            System.exit(0);
        }

        // Create remote
        if ( commandLine.hasOption("remote") ) {
            int port = REMOTE_PORT;
            if ( commandLine.getOptionValue("remote") != null )
                port = Integer.parseInt(commandLine.getOptionValue("remote"));
            RemoteLauncher.launchRemote(port);
        }

        // Get variables
        Map<String, String> variables = new HashMap<String, String>();
        String[] defines = commandLine.getOptionValues("define");
        if ( defines != null ) {
            for ( String defineValue : defines ) {
                String[] parts = defineValue.split("=");
                if ( parts.length < 2 )
                    continue;
                variables.put(parts[0], parts[1]);
                System.out.println("Define " + parts[0] + " as " + parts[1]);
            }
        }
        // Predefined variables
        variables.put("run-platform", runPlatform);

        // Launch file
        if ( commandLine.hasOption("launch") ) {
            String launchFile = commandLine.getOptionValue("launch");
            FileLauncher.launchFile(launchFile, variables);
        }
    }

}
