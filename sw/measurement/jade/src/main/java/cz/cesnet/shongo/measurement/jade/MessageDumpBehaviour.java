package cz.cesnet.shongo.measurement.jade;

import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class MessageDumpBehaviour extends CyclicBehaviour {

    public MessageDumpBehaviour(jade.core.Agent a) {
        super(a);
    }

    @Override
    public void action() {
        ACLMessage msg = myAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.INFORM));
        if (msg == null) {
            block();
            return;
        }

        System.out.printf("%s: Received message from %s: %s\n", myAgent.getLocalName(), msg.getSender().getLocalName(), msg.getContent());
    }
}
