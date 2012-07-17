package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.controller.Component;
import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.request.*;
import cz.cesnet.shongo.controller.resource.ResourceManager;
import org.joda.time.Interval;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Resource service implementation.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ResourceServiceImpl extends Component implements ResourceService
{
    /**
     * @see Domain
     */
    private Domain domain;

    /**
     * Constructor.
     */
    public ResourceServiceImpl()
    {
    }

    /**
     * Constructor.
     *
     * @param domain sets the {@link #domain}
     */
    public ResourceServiceImpl(Domain domain)
    {
        setDomain(domain);
    }

    /**
     * @param domain sets the {@link #domain}
     */
    public void setDomain(Domain domain)
    {
        this.domain = domain;
    }

    @Override
    public void init()
    {
        super.init();
        if (domain == null) {
            throw new IllegalStateException(getClass().getName() + " doesn't have the domain set!");
        }
    }


    @Override
    public String getServiceName()
    {
        return "Resource";
    }

    @Override
    public String createResource(SecurityToken token, Resource resource)
    {
        throw new RuntimeException("TODO: Implement ResourceServiceImpl.createResource");
    }

    @Override
    public void modifyResource(SecurityToken token, Resource resource)
    {
        throw new RuntimeException("TODO: Implement ResourceServiceImpl.modifyResource");
    }

    @Override
    public void deleteResource(SecurityToken token, String resourceId)
    {
        throw new RuntimeException("TODO: Implement ResourceServiceImpl.deleteResource");
    }

    @Override
    public ResourceSummary[] listResources(SecurityToken token)
    {
        EntityManager entityManager = getEntityManager();
        ResourceManager resourceManager = new ResourceManager(entityManager);

        List<cz.cesnet.shongo.controller.resource.Resource> list = resourceManager.list();
        List<ResourceSummary> summaryList = new ArrayList<ResourceSummary>();
        for (cz.cesnet.shongo.controller.resource.Resource resource : list) {
            ResourceSummary summary = new ResourceSummary();
            summary.setIdentifier(domain.formatIdentifier(resource.getId()));
            summary.setName(resource.getName());
            summaryList.add(summary);
        }

        entityManager.close();

        return summaryList.toArray(new ResourceSummary[summaryList.size()]);
    }

    @Override
    public Resource getResource(SecurityToken token, String resourceId)
    {
        throw new RuntimeException("TODO: Implement ResourceServiceImpl.getResource");
    }
}
