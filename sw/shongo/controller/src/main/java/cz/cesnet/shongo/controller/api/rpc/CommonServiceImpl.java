package cz.cesnet.shongo.controller.api.rpc;

import cz.cesnet.shongo.api.jade.PingCommand;
import cz.cesnet.shongo.controller.Component;
import cz.cesnet.shongo.controller.Configuration;
import cz.cesnet.shongo.controller.ControllerAgent;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.ResourceManager;
import cz.cesnet.shongo.jade.SendLocalCommand;
import jade.core.AID;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.*;

/**
 * Room service implementation.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class CommonServiceImpl extends AbstractServiceImpl
        implements CommonService, Component.EntityManagerFactoryAware,
                   Component.ControllerAgentAware, Component.AuthorizationAware
{
    /**
     * @see javax.persistence.EntityManagerFactory
     */
    private EntityManagerFactory entityManagerFactory;

    /**
     * @see ControllerAgent
     */
    private ControllerAgent controllerAgent;

    /**
     * @see Authorization
     */
    private Authorization authorization;

    @Override
    public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory)
    {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public void setControllerAgent(ControllerAgent controllerAgent)
    {
        this.controllerAgent = controllerAgent;
    }

    @Override
    public void setAuthorization(Authorization authorization)
    {
        this.authorization = authorization;
    }

    @Override
    public void init(Configuration configuration)
    {
        checkDependency(entityManagerFactory, EntityManagerFactory.class);
        checkDependency(controllerAgent, ControllerAgent.class);
        checkDependency(authorization, Authorization.class);
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
        controller.setDomain(cz.cesnet.shongo.controller.Domain.getLocalDomain().toApi());
        return controller;
    }

    @Override
    public Collection<Domain> listDomains(SecurityToken token)
    {
        authorization.validate(token);

        List<Domain> domainList = new ArrayList<Domain>();
        domainList.add(cz.cesnet.shongo.controller.Domain.getLocalDomain().toApi());
        return domainList;
    }

    @Override
    public Collection<Connector> listConnectors(SecurityToken token)
    {
        authorization.validate(token);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ResourceManager resourceManager = new ResourceManager(entityManager);

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

            SendLocalCommand sendLocalCommand = new SendLocalCommand(agentName, new PingCommand());
            controllerAgent.performLocalCommand(sendLocalCommand);
            sendLocalCommand.waitForProcessed(null);
            if (sendLocalCommand.getState().equals(SendLocalCommand.State.SUCCESSFUL)) {
                connector.setStatus(Status.AVAILABLE);
            }
            else {
                connector.setStatus(Status.NOT_AVAILABLE);
            }

            DeviceResource deviceResource = deviceResourceMap.get(agentName);
            if (deviceResource != null) {
                connector.setResourceId(EntityIdentifier.formatId(deviceResource));
                deviceResourceMap.remove(agentName);
            }

            connectorList.add(connector);
        }

        for (Map.Entry<String, DeviceResource> entry : deviceResourceMap.entrySet()) {
            Connector connector = new Connector();
            connector.setName(entry.getKey());
            connector.setResourceId(EntityIdentifier.formatId(entry.getValue()));
            connector.setStatus(Status.NOT_AVAILABLE);
            connectorList.add(connector);
        }

        entityManager.close();

        return connectorList;
    }
}
