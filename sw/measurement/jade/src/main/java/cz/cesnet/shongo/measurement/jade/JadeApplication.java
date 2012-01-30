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

import java.io.IOException;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class JadeApplication extends Application {

    public static void main(String[] args) throws StaleProxyException {
        Application.runApplication(args, new JadeApplication());
    }

    public JadeApplication() throws StaleProxyException {
        super("jade");
        
        new JadeAgent("", "");

        // FIXME: domain / common container
        // create a container connecting to a domain

        // create a new domain
        int port = 1099; // the default Jade port
        Profile mainProfile = new ProfileImpl(null, port, null);
        AgentContainer main = jade.core.Runtime.instance().createMainContainer(mainProfile);

        // TODO
//        // domain GUI
//        if (commandLine.hasOption("gui")) {
            AgentController rma = main.createNewAgent("rma", "jade.tools.rma.rma", new Object[0]);
            rma.start();
//        }

        // upon exit, shut down the Jade threads (otherwise, the program would hang waiting for other threads)
        jade.core.Runtime.instance().setCloseVM(true);
    }

    @Override
    public Class getAgentClass() {
        return JadeAgent.class;
    }
}
