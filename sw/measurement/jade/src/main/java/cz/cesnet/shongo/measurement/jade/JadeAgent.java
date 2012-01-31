package cz.cesnet.shongo.measurement.jade;

import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.ThreadedBehaviourFactory;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.ControllerException;
import jade.wrapper.StaleProxyException;
import jade.wrapper.gateway.JadeGateway;
import org.apache.log4j.Logger;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class JadeAgent extends cz.cesnet.shongo.measurement.common.Agent {
    
    private JadeAgentImpl agent;

    /**
     * The concrete Jade agent class implementing all the stuff.
     * We use composition, as jade agents should inherit after jade.core.Agent.
     */
    private class JadeAgentImpl extends jade.core.Agent {

        protected Logger logger = Logger.getLogger(JadeAgentImpl.class);

        Thread listeningThread;

        @Override
        protected void setup() {
            logger.info("Started agent " + getName());

            // add behaviour for listening to messages
            CyclicBehaviour listeningBehaviour = new CyclicBehaviour() {
                @Override
                public void action() {
                    ACLMessage msg = myAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.INFORM));
                    if (msg == null) {
                        block();
                        return;
                    }

                    onReceiveMessage(msg.getSender().toString(), msg.getContent());
                }
            };

            // create a separate thread for listening, as the main thread responds to user commands
            ThreadedBehaviourFactory tbf = new ThreadedBehaviourFactory();

            addBehaviour(tbf.wrap(listeningBehaviour));

            // keep the listening thread to be able to interrupt it on exit
            listeningThread = tbf.getThread(listeningBehaviour);
        }


        @Override
        public void doDelete() {
            super.doDelete();
            listeningThread.interrupt();
        }
    }
    
    
    public JadeAgent(String id, String name) {
        super(id, name);
        agent = new JadeAgentImpl();
    }


    @Override
    protected boolean startImpl() {
        try {
            JadeGateway.execute(new OneShotBehaviour() {
                @Override
                public void action() {
                    AgentContainer containerController = myAgent.getContainerController();
                    try {
                        AgentController agentController = containerController.acceptNewAgent(getName(), agent);
                        agentController.start();
                    } catch (StaleProxyException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (ControllerException e) {
            e.printStackTrace();
            return false;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    @Override
    protected void stopImpl() {
        agent.doDelete();
        System.exit(0);
    }

    @Override
    protected void sendMessageImpl(String receiverName, String message) {
        if (receiverName.equals("*")) {
            throw new NotImplementedException();
        }
        else {
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            AID receiver = new AID(receiverName, AID.ISLOCALNAME); // FIXME: just local names so far
            msg.addReceiver(receiver);
            msg.setContent(message);
            agent.send(msg);
        }
    }

}
