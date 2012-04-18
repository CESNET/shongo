package cz.cesnet.shongo.tests.jade_ontologies;

import cz.cesnet.shongo.tests.jade_ontologies.ontology.ShongoOntology;
import jade.content.lang.sl.SLCodec;
import jade.core.Agent;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

/**
 * A device agent prototype communicating with a controller.
 * <p/>
 * An example demonstrating usage of ontologies.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class DeviceAgent
{
    /**
     * The agent name. Should be unique within the platform.
     */
    private static String name = "dev";

    /**
     * Address of the controller to connect to.
     */
    private static Address controllerAddress;

    /**
     * The container holding all agents of this instance.
     */
    private static ContainerController container;

    /**
     * Controller of the agent.
     */
    private static AgentController agentController;

    public static void main(String[] args)
    {
        if (args.length > 1) {
            System.out.println("Usage: DeviceAgent [<controller-address>[:<port>]]");
            return;
        }

        if (args.length == 1) {
            controllerAddress = new Address(args[0], JadeUtils.DEFAULT_ADDRESS.getPort());
        }
        else {
            controllerAddress = JadeUtils.DEFAULT_ADDRESS;
        }

        init();

        // so far, the device agent does nothing else than responding to requests of the domain controller
        // to shut down the agent, issue container.kill()
    }

    private static void init()
    {
        Profile profile = new ProfileImpl(controllerAddress.getHost(), controllerAddress.getPort(), null, false);
        Address localAddress = JadeUtils.DEFAULT_ADDRESS; // local address to run at
        container = JadeUtils.containerFactory(profile, localAddress);

        // start the device agent
        JadeDeviceAgent agent = new JadeDeviceAgent();
        try {
            agentController = container.acceptNewAgent(name, agent);
            agentController.start();
        }
        catch (StaleProxyException e) {
            e.printStackTrace();
            System.exit(1);
        }

        JadeUtils.treatShutDown();
    }


    private static class JadeDeviceAgent extends Agent//JadeListeningAgent
    {
        public JadeDeviceAgent()
        {
            //super(new DeviceListeningBehaviour());
        }

        @Override
        protected void setup()
        {
            System.err.println("Starting agent " + getName());

            super.setup();

            // register content language and the ontology
            getContentManager().registerLanguage(new SLCodec());
            getContentManager().registerOntology(ShongoOntology.getInstance());

            addBehaviour(new DeviceListeningBehaviour());
        }
    }

}
