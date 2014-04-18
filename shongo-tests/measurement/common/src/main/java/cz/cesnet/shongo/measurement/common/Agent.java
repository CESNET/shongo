package cz.cesnet.shongo.measurement.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import org.apache.log4j.Logger;

/**
 * Abstract agent implementation, all concrete agents (like JadeAgent
 * or JxtaAgent) should extends this class and implement abstract methods:
 * -start()
 * -stop()
 * -sendMessageImpl()
 *
 * @author Martin Srom
 */
public abstract class Agent
{
    /**
     * Logger.
     * Not to be used directly (because of printing agent prefix.
     * Call one of the log*() methods from global context.
     */
    static private Logger logger = Logger.getLogger(Agent.class);

    /** Started message */
    static public final String MESSAGE_STARTED = "[AGENT:STARTED]";
    /** Startup failed message */
    static public final String MESSAGE_STARTUP_FAILED = "[AGENT:STARTUP:FAILED]";
    /** Restarted message */
    static public final String MESSAGE_RESTARTED = "[AGENT:RESTARTED]";
    /** Stopped message */
    static public final String MESSAGE_STOPPED = "[AGENT:STOPPED]";

    /** Agent index */
    private String id;

    /** Agent name */
    private String name;
    
    /** Prefix to append before any console output */
    private String consolePrefix = "";

    /** Stream to read input from */
    private InputStream inputStream = System.in;

    /** Type enumeration */
    public enum Type
    {
        Default,
        Receiver,
        Sender,
    };

    /** Agent implementation */
    private AgentImplementation agentImplementation;

    /**
     * Create agent.
     *
     * @param id  Agent id
     * @param name  Agent name
     */
    public Agent(String id, String name)
    {
        this.id = id;
        this.name = name;
        this.setType(Type.Default);
    }

    /**
     * Get agent index
     *
     * @return index
     */
    public String getId()
    {
        return id;
    }

    /**
     * Get agent name.
     *
     * @return name
     */
    public String getName()
    {
        return name;
    }

    /**
     * Logs an info message.
     *
     * @param message
     */
    public void logInfo(String message) {
        logger.info(consolePrefix + message);
    }

    /**
     * Logs a warning.
     *
     * @param message
     */
    public void logWarning(String message) {
        logger.warn(consolePrefix + message);
    }

    public void logWarning(String message, Throwable t) {
        logger.warn(consolePrefix + message, t);
    }

    /**
     * Logs an error message.
     *
     * @param message
     */
    public void logError(String message) {
        logger.error(consolePrefix + message);
    }

    public void logError(String message, Throwable t) {
        logger.error(consolePrefix + message, t);
    }


    /**
     * Set agent type.
     *
     * SENDER agent is meant to be sending message and
     * measuring durations for answers.
     *
     * RECEIVER agent should answer to every received message.
     *
     * @param type
     */
    public void setType(Type type)
    {
        switch (type) {
            case Default:
                this.agentImplementation = new AgentImplementation();
                break;
            case Sender:
                this.agentImplementation = new SenderAgentImplementation();
                break;
            case Receiver:
                this.agentImplementation = new ReceiverAgentImplementation();
                break;
            default:
                throw new IllegalArgumentException("Unknown agent type");
        }
    }

    /**
     * This method should implement starting agent logic.
     * It should register some listener for agent name and
     * when some messege is delivered to agent it should call
     * onReceiveMessage method.
     */
    protected abstract boolean startImpl();

    /**
     * This method should implement stopping agent logic.
     */
    protected abstract void stopImpl();

    /**
     * This method should implement sending message logic.
     *
     * @param receiverName
     * @param message
     */
    protected abstract void sendMessageImpl(String receiverName, String message);

    /**
     * This method should implement listing all agents in the platform.
     * Override this "not implemented" method for implementations where it is supported.
     */
    protected void listAgentsImpl() {
        logError("Listing agents not supported on this implementation");
    }

    final private boolean start()
    {
        long startTime = System.nanoTime();

        // Start agent by agent implementation
        if ( startImpl() == false ) {
            System.out.println(consolePrefix + MESSAGE_STARTUP_FAILED);
            return false;
        }

        double duration = (double)(System.nanoTime() - startTime) / 1000000.0;
        System.out.printf(consolePrefix + MESSAGE_STARTED + "[in %.2f ms]\n", duration);

        return true;
    }

    final private void stop()
    {
        long startTime = System.nanoTime();

        // Stop agent by agent implementation
        stopImpl();

        double duration = (double)(System.nanoTime() - startTime) / 1000000.0;
        System.out.printf(consolePrefix + MESSAGE_STOPPED + "[in %.2f ms]\n", duration);

    }

    /**
     * Run agent.
     */
    public void run()
    {
        start();
        onRun();
        stop();
    }

    /**
     * Exit agent
     */
    protected void exit()
    {
    }

    /**
     * On send message event is called every time the message should be sent.
     *
     * @param receiverName
     * @param message
     */
    public void onSendMessage(String receiverName, String message)
    {
        this.agentImplementation.onSendMessage(this, receiverName, message);

        // Send message by agent implementation
        sendMessageImpl(receiverName, message);
    }

    /**
     * On receive mesage is called by concrete agent implementation when
     * a message is delivered to them.
     *
     * @param senderName
     * @param message
     */
    public void onReceiveMessage(String senderName, String message)
    {
        this.agentImplementation.onReceiveMessage(this, senderName, message);
    }

    /**
     * On run agent is called when agent is started and its purpose is to implement
     * agent processing.
     */
    public void onRun()
    {
        BufferedReader input = new BufferedReader(new InputStreamReader(inputStream));
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
            if ( arguments.size() == 0 )
                continue;
            command = arguments.get(0);
            arguments.remove(0);
            if ( processCommand(command, arguments) == true )
                break;
        }
    }

    /**
     * Process one agent command that was received from launcher engine or
     * from user.
     *
     * @param command
     * @param arguments
     * @return true for quit otherwise false
     */
    private boolean processCommand(String command, List<String> arguments)
    {
        if ( command.equals("send") && arguments.size() >= 2 ) {
            String agentName = arguments.get(0);
            agentName = agentName.replaceAll("\\{agent-id\\}", getId());
            String message = arguments.get(1);
            onSendMessage(agentName, message);
        }
        else if ( command.equals("send-delayed") && arguments.size() >= 3 ) {
            try {
                Thread.sleep(Integer.parseInt(arguments.get(0)));
            } catch (InterruptedException e) {}
            String agentName = arguments.get(1);
            agentName = agentName.replaceAll("\\{agent-id\\}", getId());
            String message = arguments.get(2);
            onSendMessage(agentName, message);
        }
        else if ( command.equals("restart") ) {
            long startTime = System.nanoTime();
            stop();
            start();
            double duration = (double)(System.nanoTime() - startTime) / 1000000.0;
            System.out.printf(MESSAGE_RESTARTED + "[in %.2f ms]\n", duration);
        }
        else if ( command.equals("quit") ) {
            System.out.println(consolePrefix + "Quiting...");
            return true;
        }
        else if ( command.equals("list") ) {
            onListAgents();
        }
        else {
            System.out.printf("%sUnknown command '%s'\n", consolePrefix, command);
        }
        return false;
    }

    /**
     * Called when the agent is requested to list contents of the platform.
     */
    protected void onListAgents() {
        this.agentImplementation.onListAgents(this);
        listAgentsImpl();
    }

    /**
     * Event called after agent creation to initialize it by some arguments
     *
     * @param arguments
     */
    protected void onProcessArguments(String[] arguments)
    {
    }

    /**
     * Create and run agent from main parameters. Each agent is described by it's unique id,
     * unique name, type (default|sender|receiver) and agent class which implements abstract
     * agent. Agent is created and called method run() on the created instance.
     *
     * @param agentId
     * @param agentName
     * @param agentType
     * @param agentClass
     * @param agentArguments
     * @param inputStream       stream to read input from
     * @param consolePrefix     the prefix to append before any output
     */
    public static void runAgent(String agentId, String agentName, String agentType, Class agentClass, String[] agentArguments, InputStream inputStream, String consolePrefix)
    {
        System.out.println("Running agent '" + agentName +  "' as '" + agentType + "'...");
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

        if (inputStream != null) {
            agent.inputStream = inputStream;
        }
        if (consolePrefix != null) {
            agent.consolePrefix = consolePrefix;
        }

        // Pass arguments
        agent.onProcessArguments(agentArguments);

        // Set agent type
        if ( agentType.equals("sender") )
            agent.setType(Type.Sender);
        else if ( agentType.equals("receiver") )
            agent.setType(Type.Receiver);
        else
            agent.setType(Type.Default);

        agent.run();
        agent.exit();
    }

    /**
     * Main method that is used for creating and running agent in separate thread (JVM)
     *
     * @param args
     */
    public static void main(String[] args)
    {
        try {
            String agentId = args[0];
            String agentName = args[1];
            String agentType = args[2];
            Class agentClass = Class.forName(args[3]);
            String[] agentArguments = new String[args.length - 4];
            System.arraycopy(args, 4, agentArguments, 0, args.length - 4);
            runAgent(agentId, agentName, agentType, agentClass, agentArguments, null, null);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Agent behaviour implementation
     *
     * @author Martin Srom
     */
    private static class AgentImplementation
    {
        /**
         * Event called before message is send
         *
         * @param agent
         * @param receiverName
         * @param message
         */
        protected void onSendMessage(Agent agent, String receiverName, String message)
        {
            if ( receiverName.equals("*") )
                receiverName = "all";
            logger.info(agent.consolePrefix + String.format("Sending message to %s: %s", receiverName, message));
        }

        /**
         * Event called after message is received
         *
         * @param agent
         * @param senderName
         * @param message
         */
        protected void onReceiveMessage(Agent agent, String senderName, String message)
        {
            logger.info(agent.consolePrefix + String.format("Received message from %s: %s", senderName, message));
        }

        /**
         * Event called when the agent is told to list all agents in the platform.
         *
         * @param agent
         */
        public void onListAgents(Agent agent) {
            logger.info(agent.consolePrefix + "Listing agents");
        }
    }

    /**
     * Agent behaviour implementation for Sender type.
     *
     * This agent persist sending time and when it receives the answer it
     * computes duration and prints it.
     *
     * @author Martin Srom
     */
    private static class SenderAgentImplementation extends AgentImplementation
    {
        private HashMap<String, Long> timerMap = new HashMap<String, Long>();

        @Override
        protected void onSendMessage(Agent agent, String receiverName, String message)
        {
            long time = System.nanoTime();
            super.onSendMessage(agent, receiverName, message);
            timerMap.put(ReceiverAgentImplementation.getMessageAnswer(message), time);
        }

        @Override
        protected void onReceiveMessage(Agent agent, String senderName, String message)
        {
            long time = System.nanoTime();
            String durationFormatted = "";
            if ( timerMap.containsKey(message) ) {
                double duration = (double)(time - timerMap.get(message)) / 1000000.0;
                durationFormatted = String.format(" (in %f ms)", duration);
            }
            logger.info(agent.consolePrefix + String.format("Received message from %s: %s%s", senderName, message, durationFormatted));
        }
    }

    /**
     * Agent behaviour implementation for Receiver type.
     *
     * This agent answers to every message.
     *
     * @author Martin Srom
     */
    private static class ReceiverAgentImplementation extends AgentImplementation
    {
        public static String getMessageAnswer(String message)
        {
            return "answer to [" + message + "]";
        }

        @Override
        protected void onReceiveMessage(Agent agent, String senderName, String message)
        {
            super.onReceiveMessage(agent, senderName, message);
            agent.onSendMessage(senderName, getMessageAnswer(message));
        }
    }

}
