package cz.cesnet.shongo.measurement.fuse;

import cz.cesnet.shongo.measurement.common.Application;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

/**
 * FUSE application
 */
public class FuseApplication extends Application
{
    /** ActiveMQ default address */
    final static String ACTIVEMQ_ADRESS = "localhost:61616";

    /**
     * Create FUSE application
     */
    public FuseApplication()
    {
        super("fuse");
    }

    /**
     * Main FUSE application method
     *
     * @param args
     */
    public static void main(String[] args)
    {
        Application.runApplication(args, new FuseApplication());
    }

    /**
     * Get agent class for FUSE application
     *
     * @return class
     */
    @Override
    public Class getAgentClass()
    {
        return FuseAgent.class;
    }

    /**
     * Add custom options to command-line parser
     *
     * @param options
     */
    @Override
    protected void onInitOptions(Options options)
    {
        Option activeMq = OptionBuilder.withLongOpt("activemq")
                .withArgName("host")
                .hasArg()
                .withDescription("Set ActiveMQ address (default " + ACTIVEMQ_ADRESS + ")")
                .create("m");
        options.addOption(activeMq);
    }

    /**
     * Process custom options from command-line parser and pass it to agent
     *
     * @param commandLine
     * @return arguments for agent
     */
    @Override
    protected String[] onProcessCommandLine(CommandLine commandLine)
    {
        String activeMq = ACTIVEMQ_ADRESS;
        if ( commandLine.hasOption("activemq") ) {
            activeMq = commandLine.getOptionValue("activemq");
        }
        String[] arguments = {activeMq};
        return arguments;
    }
}
