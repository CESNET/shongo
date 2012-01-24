package cz.cesnet.shongo.measurement.jxta;

import cz.cesnet.shongo.measurement.common.CommandParser;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * Agent implementation for JXTA
 *
 * @author Martin Srom
 */
public class Agent extends Peer {

    /**
     * Constructor
     *
     * @param name
     */
    public Agent(String name) {
        super(name);
    }

    /**
     * Run agent
     */
    public void run() {
        start();
        onRun();
        stop();
        System.exit(0);
    }

    /**
     * On run agent
     */
    public void onRun() {
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        String command = "";
        while ( true ) {
            try {
                command = input.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }

            List<String> arguments = CommandParser.parse(command);
            command = arguments.get(0);
            arguments.remove(0);
            if ( processCommand(command, arguments) == true )
                break;
        }
    }

    /**
     * Process command
     *
     * @param command
     * @param arguments
     * @return true for quit otherwise false
     */
    private boolean processCommand(String command, List<String> arguments) {
        if ( command.equals("send") && arguments.size() >= 2 ) {
            String agent = arguments.get(0);
            String message = arguments.get(1);
            if ( agent.equals("*") )
                sendBroadcastMessage(message);
            else
                sendMessage(agent, message);
        }
        else if ( command.equals("quit") ) {
            System.out.println("Quiting...");
            return true;
        } else {
            System.out.printf("Unknown command '%s'\n", command);
        }
        return false;
    }

    /**
     * Run agent
     *
     * @param agentName
     * @param agentClass
     */
    public static void runAgent(String agentName, Class agentClass) {
        System.out.println("Running agent '" + agentName +  "' as '" + agentClass.getSimpleName() + "'...");
        Agent agent = null;
        try {
            Class[] types = {String.class};
            agent = (Agent)agentClass.getDeclaredConstructor(types).newInstance(agentName);
        } catch (InvocationTargetException e) {
        } catch (NoSuchMethodException e) {
        } catch (InstantiationException e) {
        } catch (IllegalAccessException e) {
        }
        assert(agent != null);
        agent.run();
    }

    /**
     * Run agent as process
     *
     * @param args
     */
    public static void main(String[] args) {
        try {
            String agentName = args[0];
            Class agentClass = Class.forName(args[1]);
            runAgent(agentName, agentClass);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

}
