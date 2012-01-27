package cz.cesnet.shongo.measurement.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import org.apache.log4j.Logger;

/**
 * Agent implementation for JXTA
 *
 * @author Martin Srom
 */
public abstract class Agent {

    /** Logger */
    static protected Logger logger = Logger.getLogger(Agent.class);

    /** Agent index */
    private String id;

    /** Agent name */
    private String name;

    /** Type enumeration */
    public enum Type {
        Default,
        Receiver,
        Sender
    };

    /** Agent implementation */
    private AgentImplementation agentImplementation;

    /**
     * Constructor
     *
     * @param id  Agent id
     * @param name  Agent name
     */
    public Agent(String id, String name) {
        this.id = id;
        this.name = name;
        this.setType(Type.Default);
    }

    /**
     * Get agent index
     *
     * @return index
     */
    public String getId() {
        return id;
    }

    /**
     * Set agent type
     *
     * @param type
     */
    public void setType(Type type) {
        if ( type == Type.Receiver ) {
            this.agentImplementation = new ReceiverAgentImplementation();
        }
        else if ( type == Type.Receiver ) {
            this.agentImplementation = new ReceiverAgentImplementation();
        }
        else {
            this.agentImplementation = new AgentImplementation();
        }
    }

    /**
     * Start agent
     */
    public abstract void start();

    /**
     * Stop agent
     */
    public abstract void stop();

    /**
     * Run agent
     */
    public void run() {
        start();
        onRun();
        stop();
    }

    /**
     * On send message event
     *
     * @param receiverName
     * @param message
     */
    public void onSendMessage(String receiverName, String message) {
        this.agentImplementation.onSendMessage(this, receiverName, message);
    }

    /**
     * On receive mesage
     *
     * @param senderName
     * @param message
     */
    public void onReceiveMessage(String senderName, String message) {
        this.agentImplementation.onReceiveMessage(this, senderName, message);
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
                if ( command == null )
                    break;
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
            agent = agent.replaceAll("\\{agent-id\\}", getId());
            String message = arguments.get(1);
            onSendMessage(agent, message);
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
     * @param agentId
     * @param agentName
     * @param agentClass
     */
    public static void runAgent(String agentId, String agentName, String agentType, Class agentClass) {
        System.out.println("Running agent '" + agentName +  "' as '" + agentClass.getSimpleName() + "'...");
        Agent agent = null;
        try {
            Class[] types = {String.class, String.class};
            agent = (Agent)agentClass.getDeclaredConstructor(types).newInstance(agentId, agentName);
        } catch (InvocationTargetException e) {
        } catch (NoSuchMethodException e) {
        } catch (InstantiationException e) {
        } catch (IllegalAccessException e) {
        }
        assert(agent != null);

        // Set agent type
        if ( agentType.equals("sender") )
            agent.setType(Type.Sender);
        else if ( agentType.equals("receiver") )
            agent.setType(Type.Receiver);
        else
            agent.setType(Type.Default);

        agent.run();
    }

    /**
     * Run agent as process
     *
     * @param args
     */
    public static void main(String[] args) {
        try {
            String agentId = args[0];
            String agentName = args[1];
            String agentType = args[2];
            Class agentClass = Class.forName(args[3]);
            runAgent(agentId, agentName, agentType, agentClass);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Agent implementation
     */
    private static class AgentImplementation {
        protected void onSendMessage(Agent agent, String receiverName, String message) {
            if ( receiverName.equals("*") )
                receiverName = "all";
            logger.info(String.format("Sending message to %s: %s", receiverName, message));
        }

        protected void onReceiveMessage(Agent agent, String senderName, String message) {
            logger.info(String.format("Received message from %s: %s", senderName, message));
        }
    }

    /**
     * Agent implementation for Sender
     */
    private static class SenderAgentImplementation extends AgentImplementation {

        private HashMap<String, Long> timerMap = new HashMap<String, Long>();

        @Override
        protected void onSendMessage(Agent agent, String receiverName, String message) {
            super.onSendMessage(agent, receiverName, message);
            timerMap.put(ReceiverAgentImplementation.getMessageAnswer(message), System.nanoTime());
        }

        @Override
        protected void onReceiveMessage(Agent agent, String senderName, String message) {
            String durationFormatted = "";
            if ( timerMap.containsKey(message) ) {
                double duration = (double)(System.nanoTime() - timerMap.get(message)) / 1000000.0;
                durationFormatted = String.format(" (in %f ms)", duration);
            }
            logger.info(String.format("Received message from %s: %s%s", senderName, message, durationFormatted));
        }
    }

    /**
     * Agent implementation for Receiver
     */
    private static class ReceiverAgentImplementation extends AgentImplementation {

        public static String getMessageAnswer(String message) {
            return "answer to [" + message + "]";
        }

        @Override
        protected void onReceiveMessage(Agent agent, String senderName, String message) {
            super.onReceiveMessage(agent, senderName, message);
            agent.onSendMessage(senderName, getMessageAnswer(message));
        }
    }

}
