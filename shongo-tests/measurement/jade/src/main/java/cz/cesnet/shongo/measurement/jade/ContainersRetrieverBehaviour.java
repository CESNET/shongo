package cz.cesnet.shongo.measurement.jade;

import jade.content.lang.sl.SLCodec;
import jade.content.onto.basic.Action;
import jade.content.onto.basic.Result;
import jade.core.ContainerID;
import jade.domain.FIPANames;
import jade.domain.JADEAgentManagement.JADEManagementOntology;
import jade.domain.JADEAgentManagement.QueryPlatformLocationsAction;
import jade.lang.acl.ACLMessage;
import jade.proto.AchieveREInitiator;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * This behaviour requests the AMS a list of containers in the platform.
 *
 * @author Ondřej Bouda
 */
public class ContainersRetrieverBehaviour extends AchieveREInitiator {

    private List<ContainerID> containers = null;
    private Condition containersRetrieved = null;
    private Lock containersRetrievedLock;

    public ContainersRetrieverBehaviour() {
        super(null, null);
    }

    public ContainersRetrieverBehaviour(Condition containersRetrieved, Lock containersRetrievedLock) {
        super(null, null);
        this.containersRetrieved = containersRetrieved;
        this.containersRetrievedLock = containersRetrievedLock;
    }

    public List<ContainerID> getContainers() {
        return containers;
    }

    @Override
    public void onStart() {
        super.onStart();

        // Be sure the JADEManagementOntology and the Codec for the SL language are
        // registered in the Gateway Agent
        myAgent.getContentManager().registerLanguage(new SLCodec());
        myAgent.getContentManager().registerOntology(JADEManagementOntology.getInstance());
    }

    @Override
    @SuppressWarnings("CallToThreadDumpStack")
    protected Vector prepareRequests(ACLMessage initialMsg) {
        Vector v = null;

        // Prepare the request to be sent to the AMS
        ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
        request.addReceiver(myAgent.getAMS());
        request.setOntology(JADEManagementOntology.getInstance().getName());
        request.setLanguage(FIPANames.ContentLanguage.FIPA_SL);

        QueryPlatformLocationsAction qpla = new QueryPlatformLocationsAction();
        Action actExpr = new Action(myAgent.getAMS(), qpla);
        try {
            myAgent.getContentManager().fillContent(request, actExpr);
            v = new Vector(1);
            v.add(request);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return v;
    }

    @Override
    @SuppressWarnings("CallToThreadDumpStack")
    protected void handleInform(ACLMessage inform) {
        try {
            // Get the result from the AMS, parse it and store the list of agents
            Result result = (Result) myAgent.getContentManager().extractContent(inform);
            jade.util.leap.List containersList = (jade.util.leap.List) result.getValue();
            if (containersList != null) {
                List<ContainerID> resList = new ArrayList<ContainerID>();
                for (int i = 0; i < containersList.size(); ++i) {
                    resList.add((ContainerID) containersList.get(i));
                }

                if (containersRetrieved != null) {
                    containersRetrievedLock.lock();
                    containers = resList;
                    containersRetrieved.signalAll();
                    containersRetrievedLock.unlock();
                }
                else {
                    containers = resList;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
