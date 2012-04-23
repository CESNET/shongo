package cz.cesnet.shongo.common.jade;

import jade.core.behaviours.CyclicBehaviour;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Behaviour listening for incoming messages.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class ReceiverBehaviour extends CyclicBehaviour
{
    private static Logger logger = LoggerFactory.getLogger(ReceiverBehaviour.class);

    @Override
    public void action()
    {
        ACLMessage msg = myAgent.receive();
        if (msg == null) {
            block();
            return;
        }

        if (msg.getSender().getLocalName().equals(FIPANames.DEFAULT_DF)) {
            myAgent.putBack(msg);
            return;
        }

        logger.info("{} received from {}: {}\n\n",
                new Object[]{myAgent.getAID().getName(), msg.getSender().getName(), msg.toString()});

        onReceiveMessage(msg);
    }

    private void onReceiveMessage(ACLMessage msg)
    {
        /*ACLMessage reply = msg.createReply();
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
        myAgent.send(reply);*/
    }

}
