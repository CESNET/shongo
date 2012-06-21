package cz.cesnet.shongo.controller.impl;

import cz.cesnet.shongo.common.api.AbsoluteDateTime;
import cz.cesnet.shongo.common.api.SecurityToken;
import cz.cesnet.shongo.controller.api.Resource;
import cz.cesnet.shongo.controller.api.ResourceService;
import cz.cesnet.shongo.controller.api.ResourceSummary;

import java.util.Map;

/**
 * Resource service implementation.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ResourceServiceImpl implements ResourceService
{
    @Override
    public String getServiceName()
    {
        return "Resource";
    }

    @Override
    public String createResource(SecurityToken token, String domain, Map attributes)
    {
        throw new RuntimeException("TODO: Implement ResourceServiceImpl.createResource");
    }

    @Override
    public void modifyResource(SecurityToken token, String resourceId, Map attributes)
    {
        throw new RuntimeException("TODO: Implement ResourceServiceImpl.modifyResource");
    }

    @Override
    public void deleteResource(SecurityToken token, String resourceId)
    {
        throw new RuntimeException("TODO: Implement ResourceServiceImpl.deleteResource");
    }

    @Override
    public Resource getResource(SecurityToken token, String resourceId)
    {
        throw new RuntimeException("TODO: Implement ResourceServiceImpl.getResource");
    }

    @Override
    public ResourceSummary[] listResources(SecurityToken token, Map filter)
    {
        // TODO: resource identifier should be computed only here
        throw new RuntimeException("TODO: Implement ResourceServiceImpl.listResources");
    }

    @Override
    public boolean isResourceActive(SecurityToken token, String resourceId, AbsoluteDateTime dateTime)
    {
        throw new RuntimeException("TODO: Implement ResourceServiceImpl.isResourceActive");
    }
}
