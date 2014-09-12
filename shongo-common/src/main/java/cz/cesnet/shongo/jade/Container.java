package cz.cesnet.shongo.jade;

import cz.cesnet.shongo.JadeReportSet;
import cz.cesnet.shongo.api.jade.Command;
import cz.cesnet.shongo.util.Logging;
import cz.cesnet.shongo.util.ThreadHelper;
import jade.content.ContentElement;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.basic.Action;
import jade.content.onto.basic.Result;
import jade.core.*;
import jade.core.Runtime;
import jade.domain.FIPANames;
import jade.domain.JADEAgentManagement.JADEManagementOntology;
import jade.domain.JADEAgentManagement.QueryPlatformLocationsAction;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;
import jade.wrapper.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Represents a container in JADE middle-ware.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Container
{
    private static Logger logger = LoggerFactory.getLogger(Container.class);

    /**
     * Configuration for container in JADE middle-ware.
     */
    private Profile profile;

    /**
     * Controller for container in JADE middle-ware.
     */
    private AgentContainer containerController;

    /**
     * Container agents.
     */
    private Map<String, Object> agents = new HashMap<String, Object>();

    /**
     * Arguments agents.
     */
    private Map<String, Object[]> agentsArguments = new HashMap<String, Object[]>();

    /**
     * Container agents controllers.
     */
    private Map<String, AgentController> agentControllers = new HashMap<String, AgentController>();

    /**
     * Construct JADE container.
     *
     * @param profile
     */
    private Container(Profile profile)
    {
        this.profile = profile;
    }

    /**
     * Create a container in JADE middle-ware.
     *
     * @param profile
     * @return jade container
     */
    private static Container createContainer(Profile profile)
    {
        // Setup container profile
        profile.setParameter(Profile.FILE_DIR, ".jade/");
        // Create directory if not exits
        new java.io.File(".jade").mkdir();

        return new Container(profile);
    }

    /**
     * Create a main container in JADE middle-ware. Each main container must run on a local host and port,
     * must have assigned a platform id.
     *
     * @param localHost
     * @param localPort
     * @param platformId
     * @return jade container
     */
    public static Container createMainContainer(String localHost, int localPort, String platformId)
    {
        jade.core.Profile profile = new jade.core.ProfileImpl();
        profile.setParameter(Profile.MAIN, "true");
        profile.setParameter(Profile.LOCAL_HOST, localHost);
        profile.setParameter(Profile.LOCAL_PORT, Integer.toString(localPort));
        profile.setParameter(Profile.PLATFORM_ID, platformId);
        return createContainer(profile);
    }

    /**
     * Create an agent (slave) container in JADE middle-ware. Each agent container must run on a local host
     * and port, must connect to main container on a remote host and port.
     *
     * @param mainHost
     * @param mainPort
     * @param localHost
     * @param localPort
     * @return jade container
     */
    public static Container createContainer(String mainHost, int mainPort, String localHost, int localPort)
    {
        jade.core.Profile profile = new jade.core.ProfileImpl();
        profile.setParameter(Profile.MAIN, "false");
        profile.setParameter(Profile.MAIN_HOST, mainHost);
        profile.setParameter(Profile.MAIN_PORT, Integer.toString(mainPort));
        profile.setParameter(Profile.LOCAL_HOST, localHost);
        profile.setParameter(Profile.LOCAL_PORT, Integer.toString(localPort));
        return createContainer(profile);
    }

    /**
     * Start JADE container.
     *
     * @return true if start succeeded,
     *         false otherwise
     */
    public boolean start()
    {
        if (isStarted()) {
            return true;
        }

        // Setup JADE runtime
        Runtime runtime = Runtime.instance();

        // Disable System.out, because JADE prints an unwanted messages
        Logging.disableSystemOut();
        Logging.disableSystemErr();

        if (containerController != null) {
            try {
                containerController.kill();
            }
            catch (Exception exception) {
            }
            containerController = null;
        }

        // Clone profile
        Properties properties = (Properties) ((ProfileImpl) this.profile).getProperties().clone();
        Profile profile = new ProfileImpl();
        for (java.util.Map.Entry<Object, Object> entry : properties.entrySet()) {
            String name = (String) entry.getKey();
            String value = (String) entry.getValue();
            if (name.equals(Profile.MTPS) || name.equals(Profile.SERVICES)) {
                continue;
            }
            profile.setParameter(name, value);
        }
        this.profile = profile;
        this.profile.setParameter(Profile.SERVICES, "jade.core.faultRecovery.FaultRecoveryService;");

        // Create main or agent container base on Profile.MAIN parameter
        boolean result = true;
        if (profile.isMain()) {
            containerController = runtime.createMainContainer(profile);
            if (containerController == null) {
                logger.error("Failed to start the JADE main container.");
                result = false;
            }
        }
        else {
            containerController = runtime.createAgentContainer(profile);
            if (containerController == null) {
                String url = profile.getParameter(Profile.MAIN_HOST, "");
                url += ":" + profile.getParameter(Profile.MAIN_PORT, "");
                logger.error("Failed to start the JADE container. Is the main container {} running?", url);
                result = false;
            }
        }

        // Enable System.out back
        Logging.enableSystemOut();
        Logging.enableSystemErr();

        if (result == false) {
            return false;
        }

        // Start agents
        for (String agentName : agents.keySet()) {
            if (startAgent(agentName) == false) {
                try {
                    containerController.kill();
                }
                catch (Exception exception) {
                }
                containerController = null;
                return false;
            }
        }

        return true;
    }

    /**
     * Stop JADE container.
     */
    public void stop()
    {
        if (isStarted()) {
            // Stop agents
            for (String agentName : agents.keySet()) {
                logger.debug("Stopping agent '{}'...", agentName);
                stopAgent(agentName);
                logger.debug("Agent stopped '{}'.", agentName);
            }

            // Stop platform
            try {
                logger.debug("Killing container ...");
                if (profile.isMain()) {
                    containerController.getPlatformController().kill();
                }
                else {
                    containerController.kill();
                }
                logger.debug("Container killed.");
                containerController = null;
            }
            catch (Exception exception) {
                logger.error("Failed to kill container.", exception);
            }
        }
    }

    /**
     * Start agent in container.
     *
     * @param agentName
     * @return true if agent start succeeded,
     *         false otherwise
     */
    private boolean startAgent(String agentName)
    {
        // Check if agent controller is started and if so skip the startup
        AgentController agentController = agentControllers.get(agentName);
        if (agentController != null) {
            try {
                // NOTE: tests the agent connection state (if disconnected, an exception is thrown)
                agentController.getState().toString();
                return true;
            }
            catch (StaleProxyException exception) {
                try {
                    agentController.kill();
                }
                catch (StaleProxyException e1) {
                }
                // Remove agent and it will be restarted
                agentControllers.remove(agentName);
            }
            agentController = null;
        }

        // Start agent
        Object agent = agents.get(agentName);
        Object[] arguments = agentsArguments.get(agentName);
        if (agent instanceof Class) {
            Class agentClass = (Class) agent;
            try {
                agentController = containerController.createNewAgent(
                        agentName, agentClass.getCanonicalName(), arguments);
            }
            catch (StaleProxyException exception) {
                logger.error("Failed to create agent.", exception);
                return false;
            }
            try {
                agentController.start();
            }
            catch (Exception exception) {
                logger.error("Failed to start agent.", exception);
                return false;
            }
        }
        else if (agent instanceof Agent) {
            Agent agentInstance = (Agent) agent;
            if (agentInstance.getState() == Agent.AP_DELETED) {
                logger.error("Can't start agent that was deleted [{} of type {}]!", agentName,
                        agentInstance.getClass().getName());
                return false;
            }
            int count = 10;
            while (count > 0) {
                try {
                    agentInstance.setArguments(arguments);
                    agentController = containerController.acceptNewAgent(agentName, agentInstance);
                    break;
                }
                catch (StaleProxyException exception) {
                    if (exception.getMessage().contains("Name-clash")) {
                        logger.warn("Agent '{}' already exists in platform, trying again...", agentName);
                        try {
                            Thread.sleep(100);
                        }
                        catch (InterruptedException interruptedException) {
                            logger.error("Interrupted", interruptedException);
                        }
                        continue;
                    }
                    logger.error("Failed to accept or start agent.", exception);
                    return false;
                }
            }
            if (agentController == null) {
                return false;
            }
            try {
                agentController.start();
            }
            catch (Exception exception) {
                logger.error("Failed to start agent.", exception);
                return false;
            }
        }
        else {
            throw new RuntimeException("Unknown agent type " + agent.getClass().getCanonicalName() + "!");
        }

        agentControllers.put(agentName, agentController);

        return true;
    }

    /**
     * Stop agent in container.
     *
     * @param agentName
     */
    private void stopAgent(String agentName)
    {
        if (!agents.containsKey(agentName)) {
            return;
        }
        AgentController agentController = agentControllers.get(agentName);
        if (agentController != null) {
            agentControllers.remove(agentName);
            try {
                agentController.kill();
            }
            catch (StaleProxyException exception) {
                logger.error("Failed to stop agent.", exception);
            }
        }
    }

    /**
     * Is container started?
     *
     * @return true if the container is started,
     *         false otherwise
     */
    public boolean isStarted()
    {
        if (containerController == null || !containerController.isJoined()) {
            return false;
        }
        return true;
    }

    /**
     * @return collection of agent names
     */
    public Collection<String> getAgentNames()
    {
        return agents.keySet();
    }

    /**
     * Add agent to container by it's class. The agent will be started when the container
     * will start and stopped when the container stops.
     *
     * @param agentName
     * @param agentClass
     */
    public void addAgent(String agentName, Class agentClass, Object[] arguments)
    {
        logger.debug("Adding agent '{}'...", agentName);
        agents.put(agentName, agentClass);
        if (arguments != null) {
            agentsArguments.put(agentName, arguments);
        }

        if (isStarted()) {
            startAgent(agentName);
        }
    }

    /**
     * Add agent instance to container. The agent will be started when the container
     * will start and stopped when the container stops.
     *
     * @param agentName
     * @param agent
     * @return given {@code agent}
     */
    public Agent addAgent(String agentName, Agent agent, Object[] arguments)
    {
        logger.debug("Adding agent '{}'...", agentName);
        agents.put(agentName, agent);
        if (arguments != null) {
            agentsArguments.put(agentName, arguments);
        }

        if (isStarted()) {
            startAgent(agentName);
        }
        return agent;
    }

    /**
     * Remove agent.
     *
     * @param agentName
     */
    public void removeAgent(String agentName)
    {
        logger.debug("Removing agent '{}'...", agentName);
        stopAgent(agentName);
        agents.remove(agentName);
        agentsArguments.remove(agentName);
    }

    /**
     * @param agentName
     * @return true when the agent exists, false otherwise
     */
    public boolean hasAgent(String agentName)
    {
        AgentController agentController = agentControllers.get(agentName);
        try {
            State state = agentController.getState();
            return true;
        }
        catch (Exception exception) {
            return false;
        }
    }

    /**
     * Checks whether container contains agent with given name.
     *
     * @param agentName
     * @return true if container contains agent,
     *         false otherwise
     */
    public boolean isAgentStarted(String agentName)
    {
        if (!agents.containsKey(agentName)) {
            return false;
        }
        Object agent = agents.get(agentName);
        if (agent instanceof Agent) {
            Agent agentInstance = (Agent) agent;
            if (!agentInstance.isStarted()) {
                return false;
            }
        }
        AgentController agentController = agentControllers.get(agentName);
        try {
            State state = agentController.getState();
        }
        catch (Exception exception) {
            return false;
        }
        return true;
    }

    /**
     * Wait for all JADE agents to start.
     */
    public void waitForJadeAgentsToStart()
    {
        int count = 100;
        boolean started;
        do {
            started = true;
            try {
                Thread.sleep(50);
                count--;
                if (count == 0) {
                    String agentNames = "";
                    for (String agentName : getAgentNames()) {
                        if (!isAgentStarted(agentName)) {
                            if (!agentNames.isEmpty()) {
                                agentNames += ", ";
                            }
                            agentNames += agentName;
                        }
                    }
                    logger.error("Cannot start [" + agentNames + "]!");
                    throw new RuntimeException("Cannot start [" + agentNames + "]!");
                }
            }
            catch (InterruptedException exception) {
            }
            for (String agentName : getAgentNames()) {
                if (!isAgentStarted(agentName)) {
                    started = false;
                }
            }
        }
        while (!started);
    }

    /**
     * Perform {@link LocalCommand} on agent with given {@code agentName}.
     *
     * @param agentName    of agent on which the given {@code command} should be performed
     * @param localCommand command to be performed
     */
    public void performAgentLocalCommand(String agentName, LocalCommand localCommand)
    {
        if (!isStarted()) {
            return;
        }
        AgentController agentController = agentControllers.get(agentName);
        if (agentController == null) {
            return;
        }
        try {
            // NOTE: must be ASYNC, otherwise, the connector thread be deadlock, waiting for itself
            agentController.putO2AObject(localCommand, AgentController.ASYNC);
        }
        catch (StaleProxyException exception) {
            logger.error("Failed to put command object to agent queue.", exception);
        }
    }

    /**
     * Send {@link cz.cesnet.shongo.api.jade.Command} to target receiver agent and wait for the result (blocking).
     *
     * @param receiverAgentName target receiver agent name
     * @param command           to be send
     * @return {@link SendLocalCommand} from which the result or failure can be retrieved
     */
    public SendLocalCommand sendAgentCommand(String senderAgentName, String receiverAgentName, Command command)
    {
        SendLocalCommand sendLocalCommand = new SendLocalCommand(receiverAgentName, command);
        if (!isStarted()) {
            sendLocalCommand.setFailed(new JadeReportSet.AgentNotStartedReport(senderAgentName));
            return sendLocalCommand;
        }
        AgentController senderAgentController = agentControllers.get(senderAgentName);
        if (senderAgentController == null) {
            sendLocalCommand.setFailed(new JadeReportSet.AgentNotStartedReport(senderAgentName));
            return sendLocalCommand;
        }
        performAgentLocalCommand(senderAgentName, sendLocalCommand);
        sendLocalCommand.waitForProcessed(null);
        return sendLocalCommand;
    }

    /**
     * Print status information.
     */
    public void printStatus()
    {
        try {
            System.out.println();

            // Print controller status
            String containerName = "Unnamed";
            String containerType = (profile.isMain() ? "Main" : "Slave");
            String containerStatus = "Not-Started";
            String containerAddress = profile.getParameter(Profile.LOCAL_HOST, "") + ":"
                    + profile.getParameter(Profile.LOCAL_PORT, "");
            if (containerController == null) {
                profile.getParameter(Profile.CONTAINER_NAME, "");
            }
            else {
                containerName = containerController.getContainerName();
                if (containerController.isJoined()) {
                    containerStatus = "Started";
                }
            }
            System.out.printf("Container: [%s]\n", containerName);
            System.out.printf("Type:      [%s]\n", containerType);
            System.out.printf("Status:    [%s]\n", containerStatus);
            System.out.printf("Address:   [%s]\n", containerAddress);

            // Show main container address
            if (profile.isMain() == false) {
                System.out.printf("Main:      [%s:%s]\n", profile.getParameter(Profile.MAIN_HOST, ""),
                        profile.getParameter(Profile.MAIN_PORT, ""));
            }
            // Show all slave containers in platform
            else {
                List<ContainerID> containerList = listContainers();
                if (containerList.size() > 1) {
                    System.out.println();
                    System.out.printf("List of slave containers:\n");

                    int index = 0;
                    for (ContainerID container : containerList) {
                        if (container.getMain()) {
                            continue;
                        }
                        System.out.printf("%2d) [%s] at [%s]\n", index + 1, container.getName(),
                                container.getAddress());
                        index++;
                    }
                }
            }

            // Print agents status
            if (agentControllers.size() > 0) {
                System.out.println();
                System.out.printf("List of agents:\n");

                int index = 0;
                for (Map.Entry<String, AgentController> entry : agentControllers.entrySet()) {
                    AgentController agentController = entry.getValue();
                    String agentName = entry.getKey();
                    String agentStatus = "Not-Started";
                    try {
                        agentName = agentController.getName();
                        agentStatus = agentController.getState().getName();
                    }
                    catch (StaleProxyException exception) {
                    }
                    System.out.printf("%2d) Name:   [%s]\n", index + 1, agentName);
                    System.out.printf("    Status: [%s]\n", agentStatus);
                    index++;
                }
            }

            System.out.println();
        }
        catch (Exception exception) {
            logger.error("Failed to get container status.", exception);
        }
    }

    /**
     * Show JADE Management GUI.
     */
    public void addManagementGui()
    {
        addAgent("rma", jade.tools.rma.rma.class, null);
    }

    /**
     * Has JADE management GUI?
     *
     * @return boolean
     */

    public boolean hasManagementGui()
    {
        return isAgentStarted("rma");
    }

    /**
     * Hide JADE Management GUI.
     */
    public void removeManagementGui()
    {
        removeAgent("rma");
    }

    /**
     * List containers in platform.
     *
     * @return containers
     */
    private List<ContainerID> listContainers()
    {
        try {
            Ontology ontology = JADEManagementOntology.getInstance();

            // Create agent that will perform listing
            jade.core.Agent agent = new jade.core.Agent();
            agent.getContentManager().registerLanguage(new SLCodec());
            agent.getContentManager().registerOntology(ontology);
            AgentController agentController = containerController.acceptNewAgent("listContainers", agent);
            agentController.start();

            // Send Request to AMS
            QueryPlatformLocationsAction query = new QueryPlatformLocationsAction();
            Action action = new Action(agent.getAID(), query);
            ACLMessage message = new ACLMessage(ACLMessage.REQUEST);
            message.addReceiver(agent.getAMS());
            message.setLanguage(FIPANames.ContentLanguage.FIPA_SL);
            message.setOntology(ontology.getName());
            agent.getContentManager().fillContent(message, action);
            agent.send(message);

            // Receive response
            ACLMessage receivedMessage = agent.blockingReceive(MessageTemplate.MatchSender(agent.getAMS()));
            ContentElement content = agent.getContentManager().extractContent(receivedMessage);

            // Build list of containers
            List<ContainerID> containers = new ArrayList<ContainerID>();
            Result result = (Result) content;
            jade.util.leap.List listOfPlatforms = (jade.util.leap.List) result.getValue();
            Iterator iter = listOfPlatforms.iterator();
            while (iter.hasNext()) {
                ContainerID next = (ContainerID) iter.next();
                containers.add(next);
            }

            // Kill agent
            agentController.kill();

            return containers;
        }
        catch (Exception exception) {
            logger.error("Failed to list containers", exception);
        }
        return new ArrayList<ContainerID>();
    }

    /**
     * Kill JADE threads
     */
    public static void killAllJadeThreads()
    {
        logger.info("Killing all JADE threads...");
        for (Thread thread : ThreadHelper.listThreadGroup("JADE")) {
            logger.info("Active JADE Thread {}", thread.getName());
        }
        ThreadHelper.killThreadGroup("JADE");
    }
}
