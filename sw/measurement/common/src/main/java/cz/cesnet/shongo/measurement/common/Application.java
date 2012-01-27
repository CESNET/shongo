package cz.cesnet.shongo.measurement.common;

import org.apache.commons.cli.*;

import java.io.IOException;

public abstract class Application {

    public abstract Class getAgentClass();

    public static void runApplication(String[] args, Application application) {
        Option help = new Option("h", "help", false, "Print this usage information");
        Option agent = OptionBuilder.withLongOpt("agent")
                .withArgName("name")
                .hasArg()
                .withDescription("Run agent")
                .create("a");
        Option agentCount = OptionBuilder.withLongOpt("count")
                .withArgName("count")
                .hasArg()
                .withDescription("Number of agents")
                .create("c");
        Option agentType = OptionBuilder.withLongOpt("type")
                .withArgName("type")
                .hasArg()
                .withDescription("Type of agents")
                .create("t");

        // Create options
        Options options = new Options();
        options.addOption(help);
        options.addOption(agent);
        options.addOption(agentCount);
        options.addOption(agentType);

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
            formatter.printHelp("jxta", options);
            System.exit(0);
        }

        // Create agent
        if ( commandLine.hasOption("agent") ) {
            String agentName = commandLine.getOptionValue("agent");
            Class agentClass = application.getAgentClass();

            // Get agent type
            String type = "default";
            if ( commandLine.hasOption("type") ) {
                type = commandLine.getOptionValue("type");
            }

            // Get agent number
            int number = 1;
            StringBuilder numberFormat = new StringBuilder();
            if ( commandLine.hasOption("count") ) {
                number = Integer.parseInt(commandLine.getOptionValue("count"));
                int places = (int)Math.round(Math.log10(number) + 1.0);
                for ( int index = 0; index < places; index++ )
                    numberFormat.append("0");
            }

            if ( number == 1 ) {
                Agent.runAgent("", agentName, type, agentClass);
            } else {
                for ( int index = 0; index < number; index++ ) {
                    String agentNumber = new java.text.DecimalFormat(numberFormat.toString()).format(index + 1);
                    final String[] arguments = {agentNumber, agentName + agentNumber, type, agentClass.getName()};
                    cz.cesnet.shongo.measurement.common.Application.runProcess(agentName + agentNumber, Agent.class, arguments);
                }
            }
        }
    }

    /**
     * Input stream connector
     */
    static StreamConnector streamConnectorInput = new StreamConnector(System.in);

    /**
     * Run new process from some class that has implemented main() function.
     * All output and errors from process will be printed to standard/error output.
     *
     * @param name      Process name
     * @param mainClass Process main class
     * @param arguments Process arguments
     * @return void
     */
    public static Process runProcess(final String name, final Class mainClass, final String[] arguments) {

        // Run process
        Process process = null;
        try {
            String classPath = "-Djava.class.path=" + System.getProperty("java.class.path");
            StringBuilder command = new StringBuilder();
            command.append("java ");
            command.append(classPath);
            command.append(" ");
            command.append(mainClass.getName());
            if ( arguments != null ) {
                for ( String argument : arguments ) {
                    command.append(" ");
                    command.append(argument);
                }
            }
            process = Runtime.getRuntime().exec(command.toString());
        } catch (IOException e) {
        }

        // Print standard output
        StreamConnector streamConnectorOutput = new StreamConnector(process.getInputStream(), System.out, name);
        streamConnectorOutput.start();

        // Print error output
        StreamConnector streamConnectorError = new StreamConnector(process.getErrorStream(), System.err, name);
        streamConnectorError.start();

        // Print standard output
        streamConnectorInput.addOutput(process.getOutputStream());
        streamConnectorInput.start();

        return process;
    }

}
