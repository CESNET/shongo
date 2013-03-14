package cz.cesnet.shongo.controller.api.rpc;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.cache.AvailableRoom;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.fault.PersistentEntityNotFoundException;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.ResourceManager;
import cz.cesnet.shongo.controller.resource.RoomProviderCapability;
import cz.cesnet.shongo.controller.scheduler.ReservationTask;
import cz.cesnet.shongo.controller.util.DatabaseFilter;
import cz.cesnet.shongo.fault.EntityToDeleteIsReferencedException;
import cz.cesnet.shongo.fault.FaultException;
import org.hibernate.exception.ConstraintViolationException;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import javax.persistence.RollbackException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Resource service implementation.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ResourceServiceImpl extends Component
        implements ResourceService, Component.EntityManagerFactoryAware,
                   Component.AuthorizationAware
{
    private static Logger logger = LoggerFactory.getLogger(ResourceServiceImpl.class);

    /**
     * @see Cache
     */
    private Cache cache;

    /**
     * @see javax.persistence.EntityManagerFactory
     */
    private EntityManagerFactory entityManagerFactory;

    /**
     * @see cz.cesnet.shongo.controller.Authorization
     */
    private Authorization authorization;

    /**
     * Constructor.
     *
     * @param cache sets the {@link #cache}
     */
    public ResourceServiceImpl(Cache cache)
    {
        this.cache = cache;
    }

    @Override
    public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory)
    {
        this.entityManagerFactory = entityManagerFactory;
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
        String userId = authorization.validate(token);

        resource.setupNewEntity();

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();

        cz.cesnet.shongo.controller.resource.Resource resourceImpl;
        try {
            // Create resource from API
            resourceImpl = cz.cesnet.shongo.controller.resource.Resource.createFromApi(resource, entityManager);
            resourceImpl.setUserId(userId);

            // Save it
            ResourceManager resourceManager = new ResourceManager(entityManager);
            resourceManager.create(resourceImpl);

            entityManager.getTransaction().commit();

            // Add resource to the cache
            if (cache != null) {
                cache.addResource(resourceImpl, entityManager);
            }
        }
        catch (FaultException exception) {
            throw exception;
        }
        catch (Exception exception) {
            throw new FaultException(exception);
        }
        finally {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            entityManager.close();
        }

        // Return resource shongo-id
        return EntityIdentifier.formatId(resourceImpl);
    }

    @Override
    public void modifyResource(SecurityToken token, Resource resource) throws FaultException
    {
        String userId = authorization.validate(token);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ResourceManager resourceManager = new ResourceManager(entityManager);
        String resourceId = resource.getId();
        EntityIdentifier entityId = EntityIdentifier.parse(resourceId, EntityType.RESOURCE);

        try {
            entityManager.getTransaction().begin();

            // Get reservation request
            cz.cesnet.shongo.controller.resource.Resource resourceImpl =
                    resourceManager.get(entityId.getPersistenceId());

            authorization.checkPermission(userId, entityId, Permission.WRITE);

            // Synchronize from API
            resourceImpl.fromApi(resource, entityManager);

            resourceManager.update(resourceImpl);

            entityManager.getTransaction().commit();

            // Update resource in the cache
            if (cache != null) {
                cache.updateResource(resourceImpl, entityManager);
            }
        }
        catch (FaultException exception) {
            throw exception;
        }
        catch (Exception exception) {
            throw new FaultException(exception);
        }
        finally {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            entityManager.close();
        }
    }

    @Override
    public void deleteResource(SecurityToken token, String resourceId) throws FaultException
    {
        String userId = authorization.validate(token);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ResourceManager resourceManager = new ResourceManager(entityManager);
        EntityIdentifier entityId = EntityIdentifier.parse(resourceId, EntityType.RESOURCE);

        try {
            entityManager.getTransaction().begin();

            // Get the resource
            cz.cesnet.shongo.controller.resource.Resource resourceImpl =
                    resourceManager.get(entityId.getPersistenceId());

            authorization.checkPermission(userId, entityId, Permission.WRITE);

            // Delete the resource
            resourceManager.delete(resourceImpl);

            entityManager.getTransaction().commit();

            // Remove resource from the cache
            if (cache != null) {
                cache.removeResource(resourceImpl);
            }
        }
        catch (FaultException exception) {
            throw exception;
        }
        catch (RollbackException exception) {
            if (exception.getCause() != null && exception.getCause() instanceof PersistenceException) {
                PersistenceException cause = (PersistenceException) exception.getCause();
                if (cause.getCause() != null && cause.getCause() instanceof ConstraintViolationException) {
                    logger.warn("Resource '" + resourceId + "' cannot be deleted because is still referenced.",
                            exception);
                    throw new EntityToDeleteIsReferencedException(Resource.class, entityId.getPersistenceId());
                }
            }
        }
        catch (Exception exception) {
            throw new FaultException(exception);
        }
        finally {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            entityManager.close();
        }
    }

    @Override
    public Collection<ResourceSummary> listResources(SecurityToken token, Map<String, Object> filter)
    {
        String userId = authorization.validate(token);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ResourceManager resourceManager = new ResourceManager(entityManager);

        try {
            String filterUserId = DatabaseFilter.getUserIdFromFilter(filter, userId);
            List<cz.cesnet.shongo.controller.resource.Resource> list = resourceManager.list(filterUserId);

            List<ResourceSummary> summaryList = new ArrayList<ResourceSummary>();
            for (cz.cesnet.shongo.controller.resource.Resource resource : list) {
                ResourceSummary summary = new ResourceSummary();
                summary.setId(EntityIdentifier.formatId(resource));
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
                    summary.setParentResourceId(EntityIdentifier.formatId(parentResource));
                }
                summaryList.add(summary);
            }
            return summaryList;
        }
        finally {
            entityManager.close();
        }
    }

    @Override
    public Resource getResource(SecurityToken token, String resourceId) throws PersistentEntityNotFoundException
    {
        String userId = authorization.validate(token);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ResourceManager resourceManager = new ResourceManager(entityManager);
        EntityIdentifier entityId = EntityIdentifier.parse(resourceId, EntityType.RESOURCE);

        try {
            cz.cesnet.shongo.controller.resource.Resource resource = resourceManager.get(entityId.getPersistenceId());

            authorization.checkPermission(userId, entityId, Permission.READ);

            return resource.toApi(entityManager);
        }
        finally {
            entityManager.close();
        }
    }

    @Override
    public ResourceAllocation getResourceAllocation(SecurityToken token, String resourceId, Interval interval)
            throws PersistentEntityNotFoundException
    {
        String userId = authorization.validate(token);

        if (interval == null) {
            interval = cache.getWorkingInterval();
            if (interval == null) {
                interval = new Interval(DateTime.now(), Period.days(31));
            }
        }

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ResourceManager resourceManager = new ResourceManager(entityManager);
        EntityIdentifier entityId = EntityIdentifier.parse(resourceId, EntityType.RESOURCE);

        try {
            cz.cesnet.shongo.controller.resource.Resource resourceImpl =
                    resourceManager.get(entityId.getPersistenceId());

            authorization.checkPermission(userId, entityId, Permission.READ);

            RoomProviderCapability roomProviderCapability = resourceImpl.getCapability(RoomProviderCapability.class);

            // Setup resource allocation
            ResourceAllocation resourceAllocation = null;
            if (resourceImpl instanceof DeviceResource && roomProviderCapability != null) {
                AvailableRoom availableRoom = cache.getRoomCache().getAvailableRoom(
                        roomProviderCapability, new ReservationTask.Context(userId, cache, interval));
                RoomProviderResourceAllocation allocation = new RoomProviderResourceAllocation();
                allocation.setMaximumLicenseCount(availableRoom.getMaximumLicenseCount());
                allocation.setAvailableLicenseCount(availableRoom.getAvailableLicenseCount());
                resourceAllocation = allocation;
            }
            else {
                resourceAllocation = new ResourceAllocation();
            }
            resourceAllocation.setId(EntityIdentifier.formatId(resourceImpl));
            resourceAllocation.setName(resourceImpl.getName());
            resourceAllocation.setInterval(interval);

            // Fill resource allocations
            Collection<cz.cesnet.shongo.controller.reservation.ResourceReservation> resourceReservations =
                    resourceManager.listResourceReservationsInInterval(entityId.getPersistenceId(), interval);
            for (cz.cesnet.shongo.controller.reservation.ResourceReservation resourceReservation : resourceReservations) {
                resourceAllocation.addReservation(resourceReservation.toApi());
            }

            // Fill alias allocations
            List<cz.cesnet.shongo.controller.resource.AliasProviderCapability> aliasProviders =
                    resourceImpl.getCapabilities(cz.cesnet.shongo.controller.resource.AliasProviderCapability.class);
            for (cz.cesnet.shongo.controller.resource.AliasProviderCapability aliasProvider : aliasProviders) {
                List<cz.cesnet.shongo.controller.reservation.AliasReservation> aliasReservations =
                        resourceManager.listAliasReservationsInInterval(aliasProvider.getId(), interval);
                for (cz.cesnet.shongo.controller.reservation.AliasReservation aliasReservation : aliasReservations) {
                    resourceAllocation.addReservation(aliasReservation.toApi());
                }
            }
            return resourceAllocation;
        }
        finally {
            entityManager.close();
        }
    }
}
