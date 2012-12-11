package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.Authorization;
import cz.cesnet.shongo.controller.Cache;
import cz.cesnet.shongo.controller.Component;
import cz.cesnet.shongo.controller.Configuration;
import cz.cesnet.shongo.controller.cache.AvailableRoom;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.ResourceManager;
import cz.cesnet.shongo.controller.resource.RoomProviderCapability;
import cz.cesnet.shongo.controller.util.DatabaseFilter;
import cz.cesnet.shongo.fault.EntityNotFoundException;
import cz.cesnet.shongo.fault.FaultException;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.*;

/**
 * Resource service implementation.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ResourceServiceImpl extends Component
        implements ResourceService, Component.EntityManagerFactoryAware, Component.DomainAware,
                   Component.AuthorizationAware
{
    /**
     * @see Cache
     */
    private Cache cache;

    /**
     * @see javax.persistence.EntityManagerFactory
     */
    private EntityManagerFactory entityManagerFactory;

    /**
     * @see cz.cesnet.shongo.controller.Domain
     */
    private cz.cesnet.shongo.controller.Domain domain;

    /**
     * @see cz.cesnet.shongo.controller.Authorization
     */
    private Authorization authorization;

    /**
     * @param cache sets the {@link #cache}
     */
    public void setCache(Cache cache)
    {
        this.cache = cache;
    }

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
    public void setAuthorization(Authorization authorization)
    {
        this.authorization = authorization;
    }

    @Override
    public void init(Configuration configuration)
    {
        checkDependency(cache, Cache.class);
        checkDependency(entityManagerFactory, EntityManagerFactory.class);
        checkDependency(domain, cz.cesnet.shongo.controller.Domain.class);
        checkDependency(authorization, Authorization.class);
        super.init(configuration);
    }

    @Override
    public String getServiceName()
    {
        return "Resource";
    }

    @Override
    public String createResource(SecurityToken token, Resource resource) throws FaultException
    {
        authorization.validate(token);

        resource.setupNewEntity();

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();

        cz.cesnet.shongo.controller.resource.Resource resourceImpl;
        try {
            // Create resource from API
            resourceImpl = cz.cesnet.shongo.controller.resource.Resource.createFromApi(resource, entityManager, domain);
            resourceImpl.setUserId(authorization.getUserId(token));

            // Save it
            ResourceManager resourceManager = new ResourceManager(entityManager);
            resourceManager.create(resourceImpl);

            entityManager.getTransaction().commit();

            // Add resource to the cache
            if (cache != null) {
                cache.addResource(resourceImpl, entityManager);
            }
        }
        catch (Exception exception) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            if (exception instanceof FaultException) {
                throw (FaultException) exception;
            }
            else {
                throw new FaultException(exception);
            }
        }
        finally {
            entityManager.close();
        }

        // Return resource shongo-id
        return domain.formatId(resourceImpl.getId());
    }

    @Override
    public void modifyResource(SecurityToken token, Resource resource) throws FaultException
    {
        authorization.validate(token);

        Long resourceId = domain.parseId(resource.getId());

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();

        try {
            ResourceManager resourceManager = new ResourceManager(entityManager);

            // Get reservation request
            cz.cesnet.shongo.controller.resource.Resource resourceImpl = resourceManager.get(resourceId);

            // Synchronize from API
            resourceImpl.fromApi(resource, entityManager, domain);

            resourceManager.update(resourceImpl);

            entityManager.getTransaction().commit();

            // Update resource in the cache
            if (cache != null) {
                cache.updateResource(resourceImpl, entityManager);
            }
        }
        catch (Exception exception) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            if (exception instanceof FaultException) {
                throw (FaultException) exception;
            }
            else {
                throw new FaultException(exception);
            }
        }
        finally {
            entityManager.close();
        }
    }

    @Override
    public void deleteResource(SecurityToken token, String resourceId) throws FaultException
    {
        authorization.validate(token);

        Long id = domain.parseId(resourceId);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();

        try {
            ResourceManager resourceManager = new ResourceManager(entityManager);

            // Get the resource
            cz.cesnet.shongo.controller.resource.Resource resourceImpl = resourceManager.get(id);

            // Delete the resource
            resourceManager.delete(resourceImpl);

            // Remove resource from the cache
            if (cache != null) {
                cache.removeResource(resourceImpl);
            }

            entityManager.getTransaction().commit();
        }
        catch (Exception exception) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            if (exception instanceof FaultException) {
                throw (FaultException) exception;
            }
            else {
                throw new FaultException(exception);
            }
        }
        finally {
            entityManager.close();
        }
    }

    @Override
    public Collection<ResourceSummary> listResources(SecurityToken token, Map<String, Object> filter)
    {
        authorization.validate(token);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ResourceManager resourceManager = new ResourceManager(entityManager);

        String userId = DatabaseFilter.getUserIdFromFilter(filter, authorization.getUserId(token));
        List<cz.cesnet.shongo.controller.resource.Resource> list = resourceManager.list(userId);

        List<ResourceSummary> summaryList = new ArrayList<ResourceSummary>();
        for (cz.cesnet.shongo.controller.resource.Resource resource : list) {
            ResourceSummary summary = new ResourceSummary();
            summary.setId(domain.formatId(resource.getId()));
            summary.setUserId(resource.getUserId());
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
                summary.setParentResourceId(domain.formatId(parentResource.getId()));
            }
            summaryList.add(summary);
        }

        entityManager.close();

        return summaryList;
    }

    @Override
    public Resource getResource(SecurityToken token, String resourceId) throws EntityNotFoundException
    {
        authorization.validate(token);

        Long id = domain.parseId(resourceId);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ResourceManager resourceManager = new ResourceManager(entityManager);

        cz.cesnet.shongo.controller.resource.Resource resourceImpl = resourceManager.get(id);
        Resource resourceApi = resourceImpl.toApi(entityManager, domain);

        entityManager.close();

        return resourceApi;
    }

    @Override
    public ResourceAllocation getResourceAllocation(SecurityToken token, String resourceId, Interval interval)
            throws EntityNotFoundException
    {
        authorization.validate(token);

        Long id = domain.parseId(resourceId);
        if (interval == null) {
            interval = cache.getWorkingInterval();
            if (interval == null) {
                interval = new Interval(DateTime.now(), Period.days(31));
            }
        }

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ResourceManager resourceManager = new ResourceManager(entityManager);

        cz.cesnet.shongo.controller.resource.Resource resourceImpl = resourceManager.get(id);
        RoomProviderCapability roomProviderCapability = resourceImpl.getCapability(RoomProviderCapability.class);

        // Setup resource allocation
        ResourceAllocation resourceAllocation = null;
        if (resourceImpl instanceof DeviceResource && roomProviderCapability != null) {
            AvailableRoom availableRoom = cache.getResourceCache().getAvailableRoom(
                    (cz.cesnet.shongo.controller.resource.DeviceResource) resourceImpl, interval, null);
            RoomProviderResourceAllocation allocation = new RoomProviderResourceAllocation();
            allocation.setMaximumLicenseCount(availableRoom.getMaximumLicenseCount());
            allocation.setAvailableLicenseCount(availableRoom.getAvailableLicenseCount());
            resourceAllocation = allocation;
        }
        else {
            resourceAllocation = new ResourceAllocation();
        }
        resourceAllocation.setId(domain.formatId(id));
        resourceAllocation.setName(resourceImpl.getName());
        resourceAllocation.setInterval(interval);

        // Fill resource allocations
        Collection<cz.cesnet.shongo.controller.reservation.ResourceReservation> resourceReservations =
                resourceManager.listResourceReservationsInInterval(id, interval);
        for (cz.cesnet.shongo.controller.reservation.ResourceReservation resourceReservation : resourceReservations) {
            resourceAllocation.addReservation(resourceReservation.toApi(domain));
        }

        // Fill alias allocations
        List<cz.cesnet.shongo.controller.resource.AliasProviderCapability> aliasProviders =
                resourceImpl.getCapabilities(cz.cesnet.shongo.controller.resource.AliasProviderCapability.class);
        for (cz.cesnet.shongo.controller.resource.AliasProviderCapability aliasProvider : aliasProviders) {
            List<cz.cesnet.shongo.controller.reservation.AliasReservation> aliasReservations =
                    resourceManager.listAliasReservationsInInterval(aliasProvider.getId(), interval);
            for (cz.cesnet.shongo.controller.reservation.AliasReservation aliasReservation : aliasReservations) {
                resourceAllocation.addReservation(aliasReservation.toApi(domain));
            }
        }

        entityManager.close();

        return resourceAllocation;
    }
}
