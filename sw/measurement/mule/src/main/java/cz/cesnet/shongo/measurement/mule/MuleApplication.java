package cz.cesnet.shongo.measurement.mule;

import cz.cesnet.shongo.measurement.common.Application;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

/**
 * FUSE application
 */
public class MuleApplication extends Application
{
    /**
     * Create FUSE application
     */
    public MuleApplication()
    {
        super("mule");
    }

    /**
     * Main FUSE application method
     *
     * @param args
     */
    public static void main(String[] args)
    {
        Application.runApplication(args, new MuleApplication());
    }

    /**
     * Get agent class for FUSE application
     *
     * @return class
     */
    @Override
    public Class getAgentClass()
    {
        return MuleAgent.class;
    }

    /**
     * Add custom options to command-line parser
     *
     * @param options
     */
    @Override
    protected void onInitOptions(Options options)
    {
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
        return new String[]{};
    }

    @Override
    protected void onRun() {
    }

    @Override
    protected void onExit() {
        super.onExit();
    }
}
