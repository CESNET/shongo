package cz.cesnet.shongo.measurement.jade;

import cz.cesnet.shongo.measurement.common.Address;
import jade.core.*;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.ThreadedBehaviourFactory;
import jade.core.messaging.TopicManagementHelper;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.ControllerException;
import jade.wrapper.StaleProxyException;

import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class JadeAgent extends cz.cesnet.shongo.measurement.common.Agent {
    
    /** Address the container of this agent listens at (when run in a separate JVM). */
    private Address localAddress;

    /** Address of the platform to join (when run in a separate JVM). */
    private Address joinAddress;

    private JadeAgentImpl agent;

    /**
     * Container in which this agent is retained.
     */
    private ContainerController container;

    /**
     * Whether to kill the container when the agent is stopped.
     */
    private boolean killContainerOnStop;

    /**
     * Controller of the agent.
     */
    private AgentController controller;


    /**
     * The concrete Jade agent class implementing all the stuff.
     * We use composition, as jade agents should inherit after jade.core.Agent.
     */
    private class JadeAgentImpl extends jade.core.Agent {

        private Thread listeningThread;
        private Behaviour threadedListeningBehaviour;

        /**
         * Topic used for broadcast (platform-wide) messages.
         * The topic ID used for broadcasting, or <code>null</code> if the broadcasting is not available.
         */
        public AID topicAll = null;

        @Override
        protected void setup() {
            logInfo("Started agent " + getName());

            // add behaviour for listening to messages
            Behaviour listeningBehaviour = new CyclicBehaviour() {
                @Override
                public void action() {
                    ACLMessage msg = myAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.INFORM));
                    if (msg == null) {
                        block();
                        return;
                    }

                    onReceiveMessage(msg.getSender().getLocalName(), msg.getContent()); // FIXME: just local names so far
                }
            };

            // create a separate thread for listening, as the main thread responds to user commands
            ThreadedBehaviourFactory tbf = new ThreadedBehaviourFactory();
            threadedListeningBehaviour = tbf.wrap(listeningBehaviour);
            addBehaviour(threadedListeningBehaviour);

            // keep the listening thread to be able to interrupt it on exit
            listeningThread = tbf.getThread(listeningBehaviour);

            // register the topic management service
            try {
                TopicManagementHelper topicHelper = (TopicManagementHelper) agent.getHelper(TopicManagementHelper.SERVICE_NAME);
                topicAll = topicHelper.createTopic("ALL");
                topicHelper.register(topicAll);
            }
            catch (ServiceException e) {
                logWarning("The TopicManagement service is not available, broadcast messaging will not be available");
            }
        }

        public void suspendListening() {
            removeBehaviour(threadedListeningBehaviour);
        }

        public void resumeListening() {
            addBehaviour(threadedListeningBehaviour);
        }

        @Override
        public void doDelete() {
            super.doDelete();
            listeningThread.interrupt();
        }

        public void sendMessage(String receiverName, String message) {
            AID receiver;
            if (receiverName.equals("*")) {
                // NOTE: broadcast messaging implemented as a topic-based communication on topic "ALL"
                if (topicAll == null) {
                    logError("Error sending a broadcast message: broadcast messaging is not available");
                    return;
                }
                receiver = topicAll;
            } else {
                receiver = new AID(receiverName, AID.ISLOCALNAME); // FIXME: just local names so far
            }

            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.addReceiver(receiver);
            msg.setContent(message);
            send(msg);
        }
    }


    /**
     *
     * @param id
     * @param name
     */
    public JadeAgent(String id, String name) {
        super(id, name);
        agent = new JadeAgentImpl();
    }


    /**
     * The Jade agent requires two arguments:
     * - localhost address (optionally with port, defaults to <code>JadeApplication.DEFAULT_ADDRESS.getPort()</code>)
     * - address of the main container to connect to (optionally with port, defaults to
     *   <code>JadeApplication.DEFAULT_ADDRESS.getPort()</code>)
     *
     * @param arguments
     */
    @Override
    protected void onProcessArguments(String[] arguments) {
        if (arguments.length != 2) {
            throw new IllegalArgumentException("The JadeAgent requires localhost and main container addresses");
        }
        
        localAddress = new Address(arguments[0], JadeApplication.DEFAULT_ADDRESS.getPort());
        joinAddress = new Address(arguments[1], JadeApplication.DEFAULT_ADDRESS.getPort());
    }

    @Override
    protected boolean startImpl() {
        container = JadeApplication.getDefaultContainer();
        if (container == null) {
            // no JadeApplication has been run - start our own container
            Profile profile = new ProfileImpl(joinAddress.getHost(), joinAddress.getPort(), null, false);
            container = JadeApplication.containerFactory(JadeApplication.Mode.Container, profile, localAddress);
            killContainerOnStop = true;
        }
        else {
            killContainerOnStop = false;
        }

        try {
            controller = container.acceptNewAgent(getName(), agent);
            controller.start();
        } catch (ControllerException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    @Override
    protected void stopImpl() {
        try {
            controller.kill();
            if (killContainerOnStop) {
                container.kill();
            }
        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    @Override
    protected void sendMessageImpl(String receiverName, String message) {
        agent.sendMessage(receiverName, message);
    }

    @Override
    protected void listAgentsImpl() {
        // to get the list of containers, we add a behaviour which queries the AMS, and wait until the results are available
        Lock lock = new ReentrantLock();
        Condition listRetrieved = lock.newCondition();

        ContainersRetrieverBehaviour containerRetriever = new ContainersRetrieverBehaviour(listRetrieved, lock);

        agent.suspendListening();
        agent.addBehaviour(containerRetriever);
        lock.lock();
        try {
            while (containerRetriever.getContainers() == null) {
                listRetrieved.await(); // wait until the behaviour has filled in the results
            }
        }
        catch (InterruptedException e) {
            logError("Interrupted when waiting for the containers list", e);
            return;
        }
        finally {
            lock.unlock();
            agent.removeBehaviour(containerRetriever);
            agent.resumeListening();
        }

        List<ContainerID> containers = containerRetriever.getContainers();
        if (containers == null) {
            logError("Error retrieving list of containers");
            return;
        }

        logInfo("Containers:");
        for (ContainerID cid : containers) {
            logInfo(String.format("- %s (%s:%s)", cid.getName(), cid.getAddress(), cid.getPort()));
        }

        logInfo("Agents:");
        agent.suspendListening();
        try {
            for (ContainerID containerID : containers) {
                AgentsRetrieverBehaviour agentRetriever = new AgentsRetrieverBehaviour(containerID, listRetrieved, lock);
                agent.addBehaviour(agentRetriever);
                lock.lock();
                try {
                    while (agentRetriever.getAgents() == null) {
                        listRetrieved.await(); // wait until the behaviour has filled in the results
                    }
                }
                catch (InterruptedException e) {
                    logError("Interrupted when waiting for the containers list", e);
                    return;
                }
                finally {
                    lock.unlock();
                    agent.removeBehaviour(agentRetriever);
                }

                List<AID> agents = agentRetriever.getAgents();
                if (agents == null) {
                    logError("Error retrieving list of agent for container " + containerID.getName());
                    continue;
                }

                logInfo(String.format("  %s:", containerID.getName()));
                for (AID agent : agents) {
                    logInfo(String.format("  - %s", agent.getLocalName()));
                }
            }
        }
        finally {
            agent.resumeListening();
        }
    }
}
