package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.controller.Component;
import cz.cesnet.shongo.controller.ControllerAgent;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.ResourceManager;
import jade.core.AID;

import java.util.*;

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
        ResourceManager resourceManager = new ResourceManager(getEntityManager());

        List<DeviceResource> deviceResourceList = resourceManager.listManagedDevices();
        Map<String, DeviceResource> deviceResourceMap = new HashMap<String, DeviceResource>();
        for (DeviceResource deviceResource : deviceResourceList) {
            String agentName = ((cz.cesnet.shongo.controller.resource.ManagedMode) deviceResource.getMode())
                    .getConnectorAgentName();
            deviceResourceMap.put(agentName, deviceResource);
        }

        List<Connector> connectorList = new ArrayList<Connector>();
        for (AID aid : controllerAgent.listConnectorAgents()) {
            String agentName = aid.getLocalName();

            Connector connector = new Connector();
            connector.setName(agentName);
            connector.setStatus(Connector.Status.AVAILABLE);

            DeviceResource deviceResource = deviceResourceMap.get(agentName);
            if (deviceResource != null) {
                connector.setResourceIdentifier(domain.formatIdentifier(deviceResource.getId()));
                deviceResourceMap.remove(agentName);
            }

            connectorList.add(connector);
        }

        for (Map.Entry<String, DeviceResource> entry : deviceResourceMap.entrySet()) {
            Connector connector = new Connector();
            connector.setName(entry.getKey());
            connector.setResourceIdentifier(domain.formatIdentifier(entry.getValue().getId()));
            connector.setStatus(Connector.Status.NOT_AVAILABLE);
            connectorList.add(connector);
        }

        return connectorList;
    }
}
