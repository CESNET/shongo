package cz.cesnet.shongo.tests.jade_ontologies;

import cz.cesnet.shongo.tests.jade_ontologies.ontology.Mute;
import cz.cesnet.shongo.tests.jade_ontologies.ontology.SetMicrophoneLevel;
import cz.cesnet.shongo.tests.jade_ontologies.ontology.Unmute;
import jade.content.Concept;
import jade.content.ContentManager;
import jade.content.lang.Codec;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.content.onto.basic.Done;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

/**
 * A behaviour used by devices to response to controller requests.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class DeviceListeningBehaviour extends CyclicBehaviour
{
    @Override
    public void action() {
        ACLMessage msg = myAgent.receive();
        if (msg != null) {
            onReceiveMessage(msg);
        }
        else {
            block();
        }
    }

    private void onReceiveMessage(ACLMessage msg)
    {
        System.err.printf("Received a message from %s: %s\n\n", msg.getSender().getLocalName(), msg.toString());

        ACLMessage reply = msg.createReply();
        boolean successful = true;

        try {
            ContentManager cm = myAgent.getContentManager();
            Action act = (Action) cm.extractContent(msg);
            Concept concept = act.getAction();

            if (concept instanceof Mute) {
                System.out.println("Muting the device");
            }
            else if (concept instanceof Unmute) {
                System.out.println("Unmuting the device");
            }
            else if (concept instanceof SetMicrophoneLevel) {
                int level = ((SetMicrophoneLevel) concept).getLevel();
                if (level >= 0 && level <= 100) {
                    System.out.println("Setting microphone level to " + level);
                }
                else {
                    reply.setPerformative(ACLMessage.REFUSE);
                    successful = false;
                }
            }

            if (successful) {
                // inform the action was done
                Done d = new Done();
                d.setAction(act);
                cm.fillContent(reply, d);
                reply.setPerformative(ACLMessage.INFORM);
            }
        }
        catch (OntologyException ex) {
            ex.printStackTrace();
            reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
        }
        catch (Codec.CodecException ex) {
            ex.printStackTrace();
            reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
        }
        myAgent.send(reply);
    }


}
