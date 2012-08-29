package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.controller.Component;
import cz.cesnet.shongo.controller.Configuration;
import cz.cesnet.shongo.controller.ControllerAgent;
import cz.cesnet.shongo.fault.FaultException;

/**
 * Resource service implementation.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ResourceControlServiceImpl extends Component
        implements ResourceControlService, Component.ControllerAgentAware
{
    ControllerAgent controllerAgent;

    @Override
    public void setControllerAgent(ControllerAgent controllerAgent)
    {
        this.controllerAgent = controllerAgent;
    }

    @Override
    public void init(Configuration configuration)
    {
        checkDependency(controllerAgent, ControllerAgent.class);
        super.init(configuration);
    }

    @Override
    public String getServiceName()
    {
        return "ResourceControl";
    }

    @Override
    public String dial(SecurityToken token, String resourceIdentifier, String target) throws FaultException
    {
        // TODO: use controllerAgent to invoke some connector API

        return "Dialing...OK";
    }
}
