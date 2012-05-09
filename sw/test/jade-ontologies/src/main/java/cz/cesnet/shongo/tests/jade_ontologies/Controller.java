package cz.cesnet.shongo.tests.jade_ontologies;

import cz.cesnet.shongo.tests.jade_ontologies.commands.Command;
import cz.cesnet.shongo.tests.jade_ontologies.commands.SendCommand;
import cz.cesnet.shongo.tests.jade_ontologies.ontology.*;
import jade.content.Concept;
import jade.content.lang.sl.SLCodec;
import jade.core.AID;
import jade.core.Agent;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.lang.acl.ACLMessage;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.ControllerException;
import jade.wrapper.StaleProxyException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

/**
 * A controller prototype communicating with device agents.
 * <p/>
 * An example of how the controller may send action requests to device agents.
 * The controller offers a command-line interface.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class Controller
{
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
            System.out.println("Usage: Controller [<controller-address>[:<port>]]");
            return;
        }

        if (args.length == 1) {
            controllerAddress = new Address(args[0], JadeUtils.DEFAULT_ADDRESS.getPort());
        }
        else {
            controllerAddress = JadeUtils.DEFAULT_ADDRESS;
        }

        init();

        System.out.println("Controller prototype demonstrating ontologies usage.");
        printCommands();
        System.out.println();

        System.out.println("Listening for device agents...");

        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        String command;
        while (true) {
            try {
                command = input.readLine();
                if (command == null) {
                    break;
                }
                else {
                    processCommand(command);
                }
            }
            catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private static void printCommands()
    {
        System.out.println("You can instruct a device agent to mute/unmute, set microphone level, get the device status, or list users using command:");
        System.out.println("  <agent-name> mute");
        System.out.println("  <agent-name> unmute");
        System.out.println("  <agent-name> miclevel <0-100>");
        System.out.println("  <agent-name> getdevicestatus");
        System.out.println("  <agent-name> listusers");
    }

    private static void processCommand(String commandStr)
    {
        System.out.println("Issuing command: '" + commandStr + "'");

        // parse the command
        Command command;
        String[] words = commandStr.split("\\s");

        // currently, just the send command is supported
        AID recipient = new AID(words[0], AID.ISLOCALNAME);
        int performative = ACLMessage.REQUEST;
        Concept content = parseCommandConcept(Arrays.copyOfRange(words, 1, words.length));
        if (content == null) {
            printCommands();
            return;
        }

        command = new SendCommand(recipient, performative, content);
        try {
            agentController.putO2AObject(command, AgentController.SYNC);
            System.out.println("Command OK");
        }
        catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }

    private static Concept parseCommandConcept(String[] words)
    {
        if (words.length == 0) {
            return null;
        }

        String command = words[0];
        if (command.equals("mute")) {
            return new Mute();
        }
        else if (command.equals("unmute")) {
            return new Unmute();
        }
        else if (command.equals("miclevel")) {
            if (words.length != 2) {
                return null; // level is required
            }
            int level = Integer.parseInt(words[1]);
            if (level < 0 || level > 100) {
                return null; // not in range
            }
            return new SetMicrophoneLevel(level);
        }
        else if (command.equals("getdevicestatus")) {
            return new GetDeviceStatus();
        }
        else if (command.equals("listusers")) {
            UserIdentity user = new UserIdentity();
            user.setId("shongololo");

            SecurityToken token = new SecurityToken();
            token.setUser(user);

            ListRoomUsers lru = new ListRoomUsers();
            lru.setRoomId("konf");
            lru.setToken(token);
            return new ListRoomUsers();
        }

        return null; // bad command
    }


    private static void init()
    {
        Profile profile = new ProfileImpl(); // profile for the platform controller
        container = JadeUtils.containerFactory(profile, controllerAddress);

        // init GUI
        try {
            AgentController rma = container.createNewAgent("rma", "jade.tools.rma.rma", new Object[0]);
            rma.start();
        }
        catch (StaleProxyException e) {
            e.printStackTrace();
        }

        try {
            Agent agent = new JadeControllerAgent();
            agentController = container.acceptNewAgent("Controller-" + container.getContainerName(), agent);
            agentController.start();
            System.err.printf("Controller agent %s started\n", agent.getLocalName());
        }
        catch (ControllerException e) {
            e.printStackTrace();
        }

        JadeUtils.treatShutDown();
    }


    private static class JadeControllerAgent extends Agent
    {
        @Override
        protected void setup()
        {
            System.err.println("Starting controller agent " + getName());

            super.setup();

            // register content language and the ontology
            getContentManager().registerLanguage(new SLCodec());
            getContentManager().registerOntology(ShongoOntology.getInstance());

            setEnabledO2ACommunication(true, 0);

            addBehaviour(new ControllerCommandBehaviour());
            addBehaviour(new PrintingBehaviour());
        }
    }

}
