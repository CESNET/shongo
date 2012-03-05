package cz.cesnet.shongo.measurement.common;

import cz.cesnet.shongo.measurement.common.ActiveMq;
import cz.cesnet.shongo.measurement.common.Application;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.jmx.BrokerView;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import javax.management.ObjectName;

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

    /** ActiveMQ Server info thread */
    private Thread activeMqServerInfoThread;

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
            activeMqServer = ActiveMq.createServer(activeMqUrl);
            if ( activeMqServer == null ) {
                return false;
            }

            try {
                final BrokerView view = activeMqServer.getAdminView();
                activeMqServerInfoThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while ( true ) {
                            try {
                                Thread.sleep(10000);
                            } catch (InterruptedException e) {}

                            // Print queues
                            System.out.print("[ACTIVEMQ:QUEUES]");
                            ObjectName[] queues = view.getQueues();
                            for ( ObjectName queue : queues ) {
                                String name = queue.getKeyProperty("Destination");
                                System.out.print(" " + name);
                            }
                            System.out.println();
                        }
                    }
                });
                activeMqServerInfoThread.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return super.onRun();
    }

    @Override
    protected void onExit() {
        if ( activeMqServerInfoThread != null ) {
            activeMqServerInfoThread.stop();
        }
        if ( activeMqServer != null ) {
            try {
                logger.info("Stopping ActiveMQ Server at [" + activeMqUrl.split(",")[0] +"]");
                activeMqServer.stop();
                activeMqServer.waitUntilStopped();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        super.onExit();
    }
}
