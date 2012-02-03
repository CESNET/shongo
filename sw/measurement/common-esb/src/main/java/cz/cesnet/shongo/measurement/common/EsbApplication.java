package cz.cesnet.shongo.measurement.common;

import cz.cesnet.shongo.measurement.common.ActiveMq;
import cz.cesnet.shongo.measurement.common.Application;
import org.apache.activemq.broker.BrokerService;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

/**
 * FUSE application
 */
public abstract class EsbApplication extends Application
{
    /** ActiveMQ broker url */
    private String activeMqUrl = ActiveMq.BROKER_DEFAULT_URL;

    /** Flag if application should not create ActiveMQ server but only connect to existing */
    private boolean activeMqJoin = false;

    /** ActiveMQ Server instance */
    private BrokerService activeMqServer;

    /**
     * Create FUSE application
     */
    public EsbApplication(String name)
    {
        super(name);
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
                .withDescription("Set ActiveMQ address (default " + ActiveMq.BROKER_DEFAULT_URL + ")")
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
        if ( commandLine.hasOption("activemq") ) {
            activeMqUrl = commandLine.getOptionValue("activemq");
        }
        if ( commandLine.hasOption("join") ) {
            activeMqJoin = true;
        }
        String[] arguments = {activeMqUrl};
        return arguments;
    }

    @Override
    protected boolean onRun() {
        if ( activeMqJoin == false ) {
            logger.info("Starting ActiveMQ Server at [" + activeMqUrl +"]");
            activeMqServer = ActiveMq.createServer(activeMqUrl);
            if ( activeMqServer == null ) {
                logger.info("ActiveMQ Server failed to start at [" + activeMqUrl +"]");
                return false;
            }
        }
        return super.onRun();
    }

    @Override
    protected void onExit() {
        if ( activeMqServer != null ) {
            try {
                logger.info("Stopping ActiveMQ Server at [" + activeMqUrl +"]");
                activeMqServer.stop();
                activeMqServer.waitUntilStopped();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        super.onExit();
    }
}
