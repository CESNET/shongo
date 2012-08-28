package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.controller.Component;
import cz.cesnet.shongo.controller.Configuration;
import cz.cesnet.shongo.controller.ControllerAgent;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.ResourceManager;
import jade.core.AID;

import javax.persistence.EntityManagerFactory;
import java.util.*;

/**
 * Room service implementation.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class CommonServiceImpl extends Component
        implements CommonService, Component.EntityManagerFactoryAware, Component.DomainAware,
                   Component.ControllerAgentAware
{
    /**
     * @see javax.persistence.EntityManagerFactory
     */
    private EntityManagerFactory entityManagerFactory;

    /**
     * @see cz.cesnet.shongo.controller.Domain
     */
    private cz.cesnet.shongo.controller.Domain domain;

    /**
     * @see ControllerAgent
     */
    private ControllerAgent controllerAgent;

    @Override
    public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory)
    {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public void setDomain(cz.cesnet.shongo.controller.Domain domain)
    {
        this.domain = domain;
    }

    @Override
    public void setControllerAgent(ControllerAgent controllerAgent)
    {
        this.controllerAgent = controllerAgent;
    }

    @Override
    public void init(Configuration configuration)
    {
        checkDependency(entityManagerFactory, EntityManagerFactory.class);
        checkDependency(domain, cz.cesnet.shongo.controller.Domain.class);
        checkDependency(controllerAgent, ControllerAgent.class);
        super.init(configuration);
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
        ResourceManager resourceManager = new ResourceManager(entityManagerFactory.createEntityManager());

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
