package cz.cesnet.shongo.common.jade;

import cz.cesnet.shongo.common.util.Logging;
import jade.core.Agent;
import jade.core.Profile;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a container in JADE middle-ware.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Container
{
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
        // Setup JADE runtime
        Runtime runtime = Runtime.instance();
        runtime.setCloseVM(true);

        // Disable System.out, because JADE prints an unwanted messages
        Logging.disableSystemOut();

        // Create main or agent container base on Profile.MAIN parameter
        if (profile.isMain()) {
            containerController = runtime.createMainContainer(profile);
        }
        else {
            containerController = runtime.createAgentContainer(profile);
        }

        // Enable System.out back
        Logging.enableSystemOut();

        // Start agents
        for (String agentName : agents.keySet()) {
            startAgent(agentName);
        }

        return true;
    }

    /**
     * Stop JADE container
     */
    public void stop()
    {
        if (containerController == null || containerController.isJoined() == false) {
            return;
        }

        // Stop agents
        for (String agentName : agents.keySet()) {
            stopAgent(agentName);
        }

        try {
            containerController.kill();
        }
        catch (StaleProxyException e) {
            e.printStackTrace();
        }
        containerController = null;
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
        Object agent = agents.get(agentName);
        if (agent instanceof Class) {
            Class agentClass = (Class) agent;
            try {
                AgentController agentController = containerController.createNewAgent(agentName,
                        agentClass.getCanonicalName(), null);
                agentController.start();
                agentControllers.put(agentName, agentController);
                return true;
            }
            catch (StaleProxyException e) {
                e.printStackTrace();
            }
        }
        else if (agent instanceof Agent) {
            try {
                Agent agentInstance = (Agent) agent;
                AgentController agentController = containerController.acceptNewAgent(agentName, agentInstance);
                agentController.start();
                agentControllers.put(agentName, agentController);
                return true;
            }
            catch (StaleProxyException e) {
                e.printStackTrace();
            }
        }
        else {
            throw new RuntimeException("Unknown agent type " + agent.getClass().getCanonicalName() + "!");
        }
        return false;
    }

    /**
     * Stop agent in container.
     *
     * @param agentName
     */
    private void stopAgent(String agentName)
    {
        AgentController agentController = agentControllers.get(agentName);
        if (agentController != null) {
            try {
                agentController.kill();
                agentControllers.remove(agentName);
            }
            catch (StaleProxyException e) {
                e.printStackTrace();
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
}
