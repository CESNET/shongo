package cz.cesnet.shongo.measurement.jade;

import cz.cesnet.shongo.measurement.common.Agent;
import cz.cesnet.shongo.measurement.common.Application;
import cz.cesnet.shongo.measurement.common.StreamConnector;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.tools.rma.rma;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.ControllerException;
import jade.wrapper.StaleProxyException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import java.io.IOException;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class JadeApplication extends Application {

    /**
     * Use GUI for the platform?
     */
    private boolean useGui = false;
    
    
    public static void main(String[] args) {
        Application.runApplication(args, new JadeApplication());
    }


    @Override
    protected void onInitOptions(Options options) {
        Option gui = OptionBuilder.withLongOpt("gui")
                .withDescription("Start GUI at the main container")
                .create("g");
        options.addOption(gui);
    }

    @Override
    protected String[] onProcessCommandLine(CommandLine commandLine) {
        useGui = commandLine.hasOption("gui");


        return new String[0];
    }

    @Override
    protected void onRun() {
        // FIXME: domain / common container
        // create a container connecting to a domain

        // create a new domain
        String host = "127.0.0.1"; // host name to listen at
        int port = 1099; // the default Jade port
        Profile mainProfile = new ProfileImpl(host, port, null);
        AgentContainer main = jade.core.Runtime.instance().createMainContainer(mainProfile);

        if (useGui) {
            try {
                AgentController rma = main.createNewAgent("rma", "jade.tools.rma.rma", new Object[0]);
                rma.start();
            } catch (StaleProxyException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }

        // upon exit, shut down the Jade threads (otherwise, the program would hang waiting for other threads)
        jade.core.Runtime.instance().setCloseVM(true);
    }

    public JadeApplication() {
        super("jade");
    }

    @Override
    public Class getAgentClass() {
        return JadeAgent.class;
    }
}
