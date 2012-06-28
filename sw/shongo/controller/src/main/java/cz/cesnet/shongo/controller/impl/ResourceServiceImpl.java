package cz.cesnet.shongo.controller.impl;

import cz.cesnet.shongo.controller.api.API;
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
    public String createResource(API.SecurityToken token, String domain, Map attributes)
    {
        throw new RuntimeException("TODO: Implement ResourceServiceImpl.createResource");
    }

    @Override
    public void modifyResource(API.SecurityToken token, String resourceId, Map attributes)
    {
        throw new RuntimeException("TODO: Implement ResourceServiceImpl.modifyResource");
    }

    @Override
    public void deleteResource(API.SecurityToken token, String resourceId)
    {
        throw new RuntimeException("TODO: Implement ResourceServiceImpl.deleteResource");
    }

    @Override
    public Resource getResource(API.SecurityToken token, String resourceId)
    {
        throw new RuntimeException("TODO: Implement ResourceServiceImpl.getResource");
    }

    @Override
    public ResourceSummary[] listResources(API.SecurityToken token, Map filter)
    {
        // TODO: resource identifier should be computed only here
        throw new RuntimeException("TODO: Implement ResourceServiceImpl.listResources");
    }

    @Override
    public boolean isResourceActive(API.SecurityToken token, String resourceId, API.AbsoluteDateTime dateTime)
    {
        throw new RuntimeException("TODO: Implement ResourceServiceImpl.isResourceActive");
    }
}
