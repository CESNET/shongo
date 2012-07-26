package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.controller.Component;
import cz.cesnet.shongo.controller.ControllerAgent;
import jade.core.AID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Room service implementation.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class CommonServiceImpl extends Component.WithDomain implements CommonService, Component.ControllerAgentAware
{
    /**
     * @see ControllerAgent
     */
    private ControllerAgent controllerAgent;

    @Override
    public void setControllerAgent(ControllerAgent controllerAgent)
    {
        this.controllerAgent = controllerAgent;
    }

    @Override
    public String getServiceName()
    {
        return "Common";
    }

    @Override
    public Controller getController()
    {
        Controller controller = new Controller();
        controller.setDomain(domain.toApi());
        return controller;
    }

    @Override
    public Collection<Domain> listDomains(SecurityToken token)
    {
        List<Domain> domainList = new ArrayList<Domain>();
        domainList.add(domain.toApi());
        return domainList;
    }

    @Override
    public Collection<Connector> listConnectors(SecurityToken token)
    {
        List<Connector> connectorList = new ArrayList<Connector>();
        for (AID aid : controllerAgent.listConnectorAgents() ) {
            Connector connector = new Connector();
            connector.setName(aid.getLocalName());
            connector.setStatus(Connector.Status.AVAILABLE);
            connectorList.add(connector);
        }
        return connectorList;
    }
}
