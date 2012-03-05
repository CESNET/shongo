package cz.cesnet.shongo.measurement.jade;

import jade.content.lang.sl.SLCodec;
import jade.content.onto.basic.Action;
import jade.content.onto.basic.Result;
import jade.core.AID;
import jade.core.ContainerID;
import jade.domain.FIPANames;
import jade.domain.JADEAgentManagement.JADEManagementOntology;
import jade.domain.JADEAgentManagement.QueryAgentsOnLocation;
import jade.lang.acl.ACLMessage;
import jade.proto.AchieveREInitiator;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * This behaviour requests the AMS a list of agents running in a given container.
 * Copied from the JadeGatewayExample.
 */
class AgentsRetrieverBehaviour extends AchieveREInitiator {

    private ContainerID targetContainer;

    private List<AID> agents = null;
    private Condition agentsRetrieved = null;
    private Lock agentsRetrievedLock;

    public AgentsRetrieverBehaviour(ContainerID container) {
        super(null, null);
        targetContainer = container;
    }

    public AgentsRetrieverBehaviour(ContainerID container, Condition agentsRetrieved, Lock agentsRetrievedLock) {
        super(null, null);
        targetContainer = container;
        this.agentsRetrieved = agentsRetrieved;
        this.agentsRetrievedLock = agentsRetrievedLock;
    }

    public List<AID> getAgents() {
        return agents;
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

        QueryAgentsOnLocation qaol = new QueryAgentsOnLocation();
        qaol.setLocation(targetContainer);
        Action actExpr = new Action(myAgent.getAMS(), qaol);
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
            jade.util.leap.List agentsList = (jade.util.leap.List) result.getValue();
            if (agentsList != null) {
                List<AID> resList = new ArrayList<AID>();
                for (int i = 0; i < agentsList.size(); ++i) {
                    resList.add((AID) agentsList.get(i));
                }

                if (agentsRetrieved != null) {
                    agentsRetrievedLock.lock();
                    agents = resList;
                    agentsRetrieved.signalAll();
                    agentsRetrievedLock.unlock();
                }
                else {
                    agents = resList;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
