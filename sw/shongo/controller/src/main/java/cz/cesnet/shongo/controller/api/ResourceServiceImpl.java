package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.Component;
import cz.cesnet.shongo.controller.ResourceDatabase;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.ResourceManager;
import cz.cesnet.shongo.fault.EntityNotFoundException;
import cz.cesnet.shongo.fault.FaultException;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Resource service implementation.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ResourceServiceImpl extends Component.WithDomain implements ResourceService
{
    /**
     * @see ResourceDatabase
     */
    private ResourceDatabase resourceDatabase;

    /**
     * @param resourceDatabase sets the {@link #resourceDatabase}
     */
    public void setResourceDatabase(ResourceDatabase resourceDatabase)
    {
        this.resourceDatabase = resourceDatabase;
    }

    @Override
    public void init()
    {
        super.init();
        if (resourceDatabase == null) {
            throw new IllegalStateException(getClass().getName() + " doesn't have the resource database set!");
        }
    }


    @Override
    public String getServiceName()
    {
        return "Resource";
    }

    @Override
    public String createResource(SecurityToken token, Resource resource) throws FaultException
    {
        resource.setupNewEntity();

        EntityManager entityManager = getEntityManager();
        entityManager.getTransaction().begin();

        // Create reservation request
        cz.cesnet.shongo.controller.resource.DeviceResource resourceImpl =
                new cz.cesnet.shongo.controller.resource.DeviceResource();

        // Synchronize from API
        resourceImpl.fromApi(resource, entityManager, domain);

        // Save it
        ResourceManager resourceManager = new ResourceManager(entityManager);
        resourceManager.create(resourceImpl);

        entityManager.getTransaction().commit();

        // Add resource to resource database
        if (resourceDatabase != null) {
            resourceDatabase.addResource(resourceImpl, entityManager);
        }

        entityManager.close();

        // Return resource identifier
        return domain.formatIdentifier(resourceImpl.getId());
    }

    @Override
    public void modifyResource(SecurityToken token, Resource resource) throws FaultException
    {
        Long resourceId = domain.parseIdentifier(resource.getIdentifier());

        EntityManager entityManager = getEntityManager();
        entityManager.getTransaction().begin();

        ResourceManager resourceManager = new ResourceManager(entityManager);

        // Get reservation request
        cz.cesnet.shongo.controller.resource.Resource resourceImpl = resourceManager.get(resourceId);

        // Synchronize from API
        resourceImpl.fromApi(resource, entityManager, domain);

        resourceManager.update(resourceImpl);

        entityManager.getTransaction().commit();

        // Update resource in resource database
        if (resourceDatabase != null) {
            resourceDatabase.updateResource(resourceImpl, entityManager);
        }

        entityManager.close();
    }

    @Override
    public void deleteResource(SecurityToken token, String resourceIdentifier) throws EntityNotFoundException
    {
        Long resourceId = domain.parseIdentifier(resourceIdentifier);

        EntityManager entityManager = getEntityManager();
        entityManager.getTransaction().begin();

        ResourceManager resourceManager = new ResourceManager(entityManager);

        // Get the resource
        cz.cesnet.shongo.controller.resource.Resource resourceImpl = resourceManager.get(resourceId);

        // Delete the resource
        resourceManager.delete(resourceImpl);

        // Remove resource from resource database
        if (resourceDatabase != null) {
            resourceDatabase.removeResource(resourceImpl, entityManager);
        }

        entityManager.getTransaction().commit();
        entityManager.close();
    }

    @Override
    public Collection<ResourceSummary> listResources(SecurityToken token)
    {
        EntityManager entityManager = getEntityManager();
        ResourceManager resourceManager = new ResourceManager(entityManager);

        List<cz.cesnet.shongo.controller.resource.Resource> list = resourceManager.list();
        List<ResourceSummary> summaryList = new ArrayList<ResourceSummary>();
        for (cz.cesnet.shongo.controller.resource.Resource resource : list) {
            ResourceSummary summary = new ResourceSummary();
            summary.setIdentifier(domain.formatIdentifier(resource.getId()));
            summary.setName(resource.getName());
            if (resource instanceof DeviceResource) {
                StringBuilder stringBuilder = new StringBuilder();
                for (Technology technology : ((DeviceResource) resource).getTechnologies()) {
                    if (stringBuilder.length() > 0) {
                        stringBuilder.append(",");
                    }
                    stringBuilder.append(technology.getCode());
                }
                summary.setTechnologies(stringBuilder.toString());
            }
            cz.cesnet.shongo.controller.resource.Resource parentResource = resource.getParentResource();
            if (parentResource != null) {
                summary.setParentIdentifier(domain.formatIdentifier(parentResource.getId()));
            }
            summaryList.add(summary);
        }

        entityManager.close();

        return summaryList;
    }

    @Override
    public Resource getResource(SecurityToken token, String resourceIdentifier) throws EntityNotFoundException
    {
        Long resourceId = domain.parseIdentifier(resourceIdentifier);

        EntityManager entityManager = getEntityManager();
        ResourceManager resourceManager = new ResourceManager(entityManager);

        cz.cesnet.shongo.controller.resource.Resource resourceImpl = resourceManager.get(resourceId);
        Resource resourceApi = resourceImpl.toApi(entityManager, domain);

        entityManager.close();

        return resourceApi;
    }
}
