package cz.cesnet.shongo.controller.api;

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

    /*@Override
    public Resource getResource(Types.SecurityToken token, String resourceId)
    {
        throw new RuntimeException("TODO: Implement ResourceServiceImpl.getResource");
    }

    @Override
    public ResourceSummary[] listResources(Types.SecurityToken token, Map filter)
    {
        // TODO: resource identifier should be computed only here
        throw new RuntimeException("TODO: Implement ResourceServiceImpl.listResources");
    }

    @Override
    public boolean isResourceActive(Types.SecurityToken token, String resourceId, Types.AbsoluteDateTime dateTime)
    {
        throw new RuntimeException("TODO: Implement ResourceServiceImpl.isResourceActive");
    }*/
}
