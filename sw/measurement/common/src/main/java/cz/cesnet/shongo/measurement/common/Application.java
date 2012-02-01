package cz.cesnet.shongo.measurement.common;

import org.apache.commons.cli.*;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.LinkedList;
import java.util.List;

/**
 * Common implementation of measuring application.
 *
 * Every concrete application (Jxta, Jade, ...) should extend it
 * and implement getAgentClass() method. Every applicaton should
 * then call runApplication method which parses common parameters
 * and runs that application.
 *
 * @author Martin Srom
 */
public abstract class Application
{
    /**
     * Application name
     */
    private String name;

    /**
     * Create application
     *
     * @param name
     */
    public Application(String name)
    {
        this.name = name;
    }

    /**
     * Get application name
     *
     * @return name
     */
    public String getName()
    {
        return name;
    }
    
    /**
     * This method should implement retrived of concrete agent class
     *
     * @return agent class
     */
    public abstract Class getAgentClass();

    /**
     * This event is called when initializing application command-line
     * options. Extending application can override it to specify
     * it's own command-line options
     *
     * @param options
     */
    protected void onInitOptions(Options options)
    {
    }

    /**
     * This event is called when application is processing it's options.
     * When extending application specified custom option by onInitOptions
     * it can process them by overriding this method.
     *
     * This method should return array of strings that will be passed to agent
     * in Agent.onProcessArguments() method.
     *
     * @param commandLine
     * @return arguments for agent
     */
    protected String[] onProcessCommandLine(CommandLine commandLine)
    {
        return new String[0];
    }

    /**
     * This event is called right after processing the command line. Thus, custom application
     * initialization dependent on arguments may be implemented by overriding this method.
     */
    protected void onRun()
    {
    }

    /**
     * This event is called right before exiting the application, after all child processes have exited.
     */
    protected void onExit()
    {
    }

    /**
     * Parse application parameters and run it.
     *
     * @param args
     * @param application
     */
    public static void runApplication(String[] args, Application application)
    {
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

        // Setup application custom options
        application.onInitOptions(options);

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
            formatter.printHelp(application.getName(), options);
            System.exit(0);
        }

        // Process command line by application
        String [] applicationArguments = application.onProcessCommandLine(commandLine);
        application.onRun();
        
        List<Process> processesToWaitFor = new LinkedList<Process>();

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

            // TODO: differentiate running multiple agents within the application and as standalone processes
            if ( number == 1 ) {
                Agent.runAgent("", agentName, type, agentClass, applicationArguments);
            } else {
                for ( int index = 0; index < number; index++ ) {
                    String agentNumber = new java.text.DecimalFormat(numberFormat.toString()).format(index + 1);
                    String[] arguments = {agentNumber, agentName + agentNumber, type, agentClass.getName()};
                    arguments = mergeArrays(arguments, applicationArguments);
                    Process agentProcess = runProcess(agentName + agentNumber, Agent.class, arguments);
                    processesToWaitFor.add(agentProcess);
                }
            }
        }
        
        for (Process p : processesToWaitFor) {
            try {
                p.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        application.onExit();
    }

    /**
     * Merge arrays
     *
     * @param arrays
     * @return
     */
    public static <T> T[] mergeArrays(T[]... arrays)
    {
        // Determine required size of new array
        int count = 0;
        for (T[] array : arrays) {
            count += array.length;
        }

        // create new array of required class
        T[] mergedArray = (T[]) Array.newInstance(arrays[0][0].getClass(), count);

        // Merge each array into new array
        int start = 0;
        for ( T[] array : arrays ) {
            System.arraycopy(array, 0,
                    mergedArray, start, array.length);
            start += array.length;
        }
        return (T[]) mergedArray;
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
     * @return Process the process created
     */
    public static Process runProcess(final String name, final Class mainClass, final String[] arguments)
    {
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
