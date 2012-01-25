package cz.cesnet.shongo.measurement.jade;

import cz.cesnet.shongo.measurement.common.CommandParser;
import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

/**
 * Behaviour of agents listening for commands.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class CommandListenerBehaviour extends OneShotBehaviour {

    public CommandListenerBehaviour(jade.core.Agent agent) {
        super(agent);
    }

    @Override
    public void action() {
        String cmdLine;
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

        try {
            System.out.printf("%s: waiting for a command\n", myAgent.getLocalName());
            while ((cmdLine = in.readLine()) != null) {
                List<String> tokens = CommandParser.parse(cmdLine);
                String command = tokens.get(0);
                tokens.remove(0);
                boolean quit = processCommand(command, tokens);
                if (quit) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Processes a command.
     * @param command
     * @param arguments
     * @return TRUE if the agent should quit, FALSE if not
     */
    protected boolean processCommand(String command, List<String> arguments) {
        if ("send".equals(command) && arguments.size() >= 2) {
            String recipient = arguments.get(0);
            String message = arguments.get(1);
            // TODO: broadcast messages
            sendMessage(recipient, message);
        }
        else if ("quit".equals(command)) {
            System.out.println("Quiting...");
            myAgent.doDelete();
            return true;
        }
        else {
            System.out.printf("Unknown command '%s'\n", command);
        }
        return false;
    }
    
    
    private void sendMessage(String recipient, String message) {
        System.out.printf("%s: Sending message to %s: %s\n", myAgent.getLocalName(), recipient, message);
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(new AID(recipient, AID.ISLOCALNAME)); // FIXME: just local names so far
        msg.setContent(message);
        myAgent.send(msg);
    }
}
