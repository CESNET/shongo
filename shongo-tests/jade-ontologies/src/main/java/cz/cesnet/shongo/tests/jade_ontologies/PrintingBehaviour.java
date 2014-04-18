package cz.cesnet.shongo.tests.jade_ontologies;

import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class PrintingBehaviour extends CyclicBehaviour
{
    @Override
    public void action() {
        ACLMessage msg = myAgent.receive();
        if (msg != null) {
            System.err.printf("\n%s received from %s: %s\n\n", myAgent.getLocalName(), msg.getSender().getLocalName(), msg.toString());
        }
        else {
            block();
        }
    }

}
