package cz.cesnet.shongo.measurement.jade;

import jade.core.AID;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.ServiceException;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.ThreadedBehaviourFactory;
import jade.core.messaging.TopicManagementHelper;
import jade.core.messaging.TopicManagementService;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.ControllerException;
import jade.wrapper.StaleProxyException;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class JadeAgent extends cz.cesnet.shongo.measurement.common.Agent {

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

        Thread listeningThread;

        /**
         * Topic used for broadcast (platform-wide) messages.
         * The topic ID used for broadcasting, or <code>null</code> if the broadcasting is not available.
         */
        public AID topicAll = null;

        @Override
        protected void setup() {
            logInfo("Started agent " + getName());

            // add behaviour for listening to messages
            CyclicBehaviour listeningBehaviour = new CyclicBehaviour() {
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

            addBehaviour(tbf.wrap(listeningBehaviour));

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

        @Override
        public void doDelete() {
            super.doDelete();
            listeningThread.interrupt();
        }
        
        public void sendMessage(String receiverName, String message) {
            AID receiver;
            if (receiverName.equals("*")) {
                // NOTE: broadcast messaging implemented as a topic-based communication on topic ALL
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
     * @throws ServiceException on topic service error (needed for broadcast messaging)
     */
    public JadeAgent(String id, String name) {
        super(id, name);
        agent = new JadeAgentImpl();
    }


    @Override
    protected boolean startImpl() {
        container = JadeApplication.getDefaultContainer();
        if (container == null) {
            // no JadeApplication has been run - start our own container
            Profile profile = new ProfileImpl(false);
            JadeApplication.addService(profile, TopicManagementService.class);
            // FIXME: where to connect taken from parameter
//            profile.setParameter(Profile.MAIN_HOST, joinHost);
//            profile.setParameter(Profile.MAIN_PORT, Integer.toString(joinPort));
            container = jade.core.Runtime.instance().createAgentContainer(profile);
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

}
