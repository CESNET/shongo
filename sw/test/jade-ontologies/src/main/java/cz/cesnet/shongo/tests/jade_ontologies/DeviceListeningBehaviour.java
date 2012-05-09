package cz.cesnet.shongo.tests.jade_ontologies;

import cz.cesnet.shongo.tests.jade_ontologies.ontology.*;
import jade.content.Concept;
import jade.content.ContentElement;
import jade.content.ContentManager;
import jade.content.lang.Codec;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.content.onto.basic.Done;
import jade.content.onto.basic.Result;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.util.leap.ArrayList;
import jade.util.leap.List;

import java.util.Random;

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
        reply.setPerformative(ACLMessage.INFORM);
        boolean successful = true;

        try {
            ContentManager cm = myAgent.getContentManager();
            Action act = (Action) cm.extractContent(msg);
            Concept concept = act.getAction();
            // the Done predicate should be returned by default, if the command does not return a value
            ContentElement ret = new Done(act);

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
            else if (concept instanceof GetDeviceStatus) {
                try {
                    DeviceStatus ds = new DeviceStatus();
                    ds.setCpuLoad(42.3);
                    ds.setMemoryUsage(123456);

                    // failure simulation - a failure occurs with some probability
                    if (new Random().nextInt(3) == 0) {
                        throw new IllegalStateException("device not ready");
                    }
                    ret = new Result(act, ds);
                }
                catch (IllegalStateException ex) {
                    ret = new DeviceError(ex.getMessage());
                    reply.setPerformative(ACLMessage.FAILURE);
                }
            }
            else if (concept instanceof ListRoomUsers) {
                List cel = new ArrayList();

                RoomUser one = new RoomUser();
                one.setUserId("Azurit");
                one.setRoomId("konf");
                UserIdentity oneId = new UserIdentity();
                oneId.setId("azurit");
                one.setUserIdentity(oneId);
                one.setMicrophoneLevel(45);
                cel.add(one);

                RoomUser two = new RoomUser();
                two.setUserId("Shongololo");
                two.setRoomId("konf");
                UserIdentity twoId = new UserIdentity();
                oneId.setId("shongololo");
                two.setUserIdentity(twoId);
                two.setMicrophoneLevel(57);
                cel.add(two);

                ret = new Result(act, cel);
            }
            else {
                reply.setPerformative(ACLMessage.UNKNOWN);
                successful = false;
            }

            if (successful) {
                // set a reply
                cm.fillContent(reply, ret);
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
