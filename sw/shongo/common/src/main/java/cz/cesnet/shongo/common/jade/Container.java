package cz.cesnet.shongo.common.jade;

import cz.cesnet.shongo.common.jade.command.Command;
import cz.cesnet.shongo.common.util.Logging;
import cz.cesnet.shongo.common.util.ThreadHelper;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

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
    private ContainerController containerController;

    /**
     * Container agents.
     */
    private Map<String, Object> agents = new HashMap<String, Object>();

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
        profile.setParameter(Profile.FILE_DIR, "data/jade/");
        // Create directory if not exits
        new java.io.File("data/jade").mkdir();

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
                exception.printStackTrace();
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
                stopAgent(agentName);
            }

            // Stop platform
            try {
                if (profile.isMain()) {
                    containerController.getPlatformController().kill();
                }
                else {
                    containerController.kill();
                }
                containerController = null;
            }
            catch (Exception exception) {
                logger.error("Failed to kill container.", exception);
            }
        }

        // Kill JADE threads
        logger.debug("Killing all JADE threads...");
        ThreadHelper.killThreadGroup("JADE");
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
        if (agent instanceof Class) {
            Class agentClass = (Class) agent;
            try {
                agentController = containerController.createNewAgent(agentName, agentClass.getCanonicalName(), null);
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
            try {
                Agent agentInstance = (Agent) agent;
                if (agentInstance.getState() == Agent.AP_DELETED) {
                    logger.error("Can't start agent that was deleted [{} of type {}]!", agentName,
                            agentInstance.getClass().getName());
                    return false;
                }
                agentController = containerController.acceptNewAgent(agentName, agentInstance);
            }
            catch (StaleProxyException exception) {
                logger.error("Failed to accept or start agent.", exception);
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
        if (isAgentStarted(agentName) == false) {
            return;
        }
        AgentController agentController = agentControllers.get(agentName);
        if (agentController != null) {
            try {
                agentController.kill();
                agentControllers.remove(agentName);
            }
            catch (StaleProxyException exception) {
                logger.error("Failed to kill agent.", exception);
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
        return containerController != null && containerController.isJoined();
    }

    /**
     * Add agent to container by it's class. The agent will be started when the container
     * will start and stopped when the container stops.
     *
     * @param agentName
     * @param agentClass
     */
    public void addAgent(String agentName, Class agentClass)
    {
        agents.put(agentName, agentClass);

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
     */
    public void addAgent(String agentName, Agent agent)
    {
        agents.put(agentName, agent);

        if (isStarted()) {
            startAgent(agentName);
        }
    }

    /**
     * Remove agent.
     *
     * @param agentName
     */
    public void removeAgent(String agentName)
    {
        stopAgent(agentName);
        agents.remove(agentName);
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
        if (agents.containsKey(agentName) == false) {
            return false;
        }
        AgentController agentController = agentControllers.get(agentName);
        try {
            agentController.getState();
        }
        catch (Exception exception) {
            return false;
        }
        return true;
    }

    /**
     * Perform command on local agent.
     *
     * @param command
     */
    public void performCommand(String agentName, Command command)
    {
        if (isStarted() == false) {
            logger.error("Cannot perform command when the container is not started.");
            return;
        }
        AgentController agentController = agentControllers.get(agentName);
        if (agentController == null) {
            return;
        }
        try {
            agentController.putO2AObject(command, AgentController.SYNC);
        }
        catch (StaleProxyException exception) {
            logger.error("Failed to put command object to agent queue.", exception);
        }
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
            String containerStatus = "Not-Started";
            if (containerController == null) {
                profile.getParameter(Profile.CONTAINER_NAME, "");
            }
            else {
                containerName = containerController.getContainerName();
                if (containerController.isJoined()) {
                    if (profile.isMain()) {
                        containerStatus = "Started:Main";
                    }
                    else {
                        containerStatus = "Started:Slave";
                    }
                }
            }
            System.out.printf("Container: [%s]:\n", containerName);
            System.out.printf("Status:    [%s]\n", containerStatus);

            // Print agents status
            if (agentControllers.size() > 0) {
                System.out.println();
                System.out.printf("Agents:\n");
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
        addAgent("rma", jade.tools.rma.rma.class);
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
}
