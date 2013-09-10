package cz.cesnet.shongo.controller.api.rpc;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.cache.Cache;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.ResourceManager;
import cz.cesnet.shongo.controller.resource.RoomProviderCapability;
import cz.cesnet.shongo.controller.scheduler.AvailableRoom;
import cz.cesnet.shongo.controller.scheduler.SchedulerContext;
import cz.cesnet.shongo.controller.util.QueryFilter;
import org.hibernate.exception.ConstraintViolationException;
import org.joda.time.DateMidnight;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import javax.persistence.RollbackException;
import java.util.*;

/**
 * Resource service implementation.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ResourceServiceImpl extends AbstractServiceImpl
        implements ResourceService, Component.EntityManagerFactoryAware,
                   Component.AuthorizationAware
{
    private static Logger logger = LoggerFactory.getLogger(ResourceServiceImpl.class);

    /**
     * @see cz.cesnet.shongo.controller.cache.Cache
     */
    private Cache cache;

    /**
     * @see javax.persistence.EntityManagerFactory
     */
    private EntityManagerFactory entityManagerFactory;

    /**
     * @see cz.cesnet.shongo.controller.authorization.Authorization
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
    public String createResource(SecurityToken securityToken, Resource resourceApi)
    {
        authorization.validate(securityToken);

        // Change user id (only root can do that)
        String userId = securityToken.getUserId();
        if (resourceApi.getUserId() != null && authorization.isAdmin(securityToken)) {
            userId = resourceApi.getUserId();
        }

        cz.cesnet.shongo.controller.resource.Resource resource;

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ResourceManager resourceManager = new ResourceManager(entityManager);
        AuthorizationManager authorizationManager = new AuthorizationManager(entityManager, authorization);
        try {
            authorizationManager.beginTransaction();
            entityManager.getTransaction().begin();

            // Create resource from API
            resource = cz.cesnet.shongo.controller.resource.Resource.createFromApi(resourceApi, entityManager);
            resource.setUserId(userId);

            // Save it
            resourceManager.create(resource);

            authorizationManager.createAclRecord(userId, resource, Role.OWNER);

            entityManager.getTransaction().commit();
            authorizationManager.commitTransaction();

            // Add resource to the cache
            if (cache != null) {
                cache.addResource(resource);
            }
        }
        finally {
            if (authorizationManager.isTransactionActive()) {
                authorizationManager.rollbackTransaction();
            }
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            entityManager.close();
        }

        // Return resource shongo-id
        return EntityIdentifier.formatId(resource);
    }

    @Override
    public void modifyResource(SecurityToken securityToken, Resource resourceApi)
    {
        authorization.validate(securityToken);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ResourceManager resourceManager = new ResourceManager(entityManager);
        String resourceId = resourceApi.getId();
        EntityIdentifier entityId = EntityIdentifier.parse(resourceId, EntityType.RESOURCE);

        try {
            entityManager.getTransaction().begin();

            // Get reservation request
            cz.cesnet.shongo.controller.resource.Resource resource =
                    resourceManager.get(entityId.getPersistenceId());

            if (!authorization.hasPermission(securityToken, entityId, Permission.WRITE)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("modify resource %s", entityId);
            }

            // Synchronize from API
            resource.fromApi(resourceApi, entityManager);

            resourceManager.update(resource);

            entityManager.getTransaction().commit();

            // Update resource in the cache
            if (cache != null) {
                cache.updateResource(resource);
            }
        }
        finally {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            entityManager.close();
        }
    }

    @Override
    public void deleteResource(SecurityToken securityToken, String resourceId)
    {
        authorization.validate(securityToken);
        EntityIdentifier entityId = EntityIdentifier.parse(resourceId, EntityType.RESOURCE);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ResourceManager resourceManager = new ResourceManager(entityManager);
        AuthorizationManager authorizationManager = new AuthorizationManager(entityManager, authorization);
        try {
            authorizationManager.beginTransaction();
            entityManager.getTransaction().begin();

            // Get the resource
            cz.cesnet.shongo.controller.resource.Resource resource =
                    resourceManager.get(entityId.getPersistenceId());

            if (!authorization.hasPermission(securityToken, entityId, Permission.WRITE)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("delete resource %s", entityId);
            }

            authorizationManager.deleteAclRecordsForEntity(resource);

            // Delete the resource
            resourceManager.delete(resource);

            entityManager.getTransaction().commit();
            authorizationManager.commitTransaction();

            // Remove resource from the cache
            if (cache != null) {
                cache.removeResource(resource);
            }
        }
        catch (RollbackException exception) {
            if (exception.getCause() != null && exception.getCause() instanceof PersistenceException) {
                PersistenceException cause = (PersistenceException) exception.getCause();
                if (cause.getCause() != null && cause.getCause() instanceof ConstraintViolationException) {
                    logger.warn("Resource '" + resourceId + "' cannot be deleted because is still referenced.",
                            exception);
                    ControllerReportSetHelper.throwEntityNotDeletableReferencedFault(
                            Resource.class, entityId.getPersistenceId());
                    return;
                }
            }
            throw exception;
        }
        finally {
            if (authorizationManager.isTransactionActive()) {
                authorizationManager.rollbackTransaction();
            }
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            entityManager.close();
        }
    }

    @Override
    public Collection<ResourceSummary> listResources(SecurityToken securityToken, Map<String, Object> filter)
    {
        authorization.validate(securityToken);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ResourceManager resourceManager = new ResourceManager(entityManager);

        try {
            Set<Long> resourceIds = authorization.getEntitiesWithPermission(
                    securityToken, EntityType.RESOURCE, Permission.READ);
            String filterUserId = QueryFilter.getUserIdFromFilter(filter);
            List<cz.cesnet.shongo.controller.resource.Resource> list = resourceManager.list(resourceIds, filterUserId);

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
    public Resource getResource(SecurityToken securityToken, String resourceId)
    {
        authorization.validate(securityToken);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ResourceManager resourceManager = new ResourceManager(entityManager);
        EntityIdentifier entityId = EntityIdentifier.parse(resourceId, EntityType.RESOURCE);

        try {
            cz.cesnet.shongo.controller.resource.Resource resource = resourceManager.get(entityId.getPersistenceId());

            if (!authorization.hasPermission(securityToken, entityId, Permission.READ)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("read resource %s", entityId);
            }

            return resource.toApi(entityManager);
        }
        finally {
            entityManager.close();
        }
    }

    @Override
    public ResourceAllocation getResourceAllocation(SecurityToken securityToken, String resourceId, Interval interval)
    {
        authorization.validate(securityToken);

        if (interval == null) {
            interval = new Interval(DateMidnight.now(), Period.days(31));
        }

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ResourceManager resourceManager = new ResourceManager(entityManager);
        EntityIdentifier entityId = EntityIdentifier.parse(resourceId, EntityType.RESOURCE);

        try {
            cz.cesnet.shongo.controller.resource.Resource resourceImpl =
                    resourceManager.get(entityId.getPersistenceId());

            if (!authorization.hasPermission(securityToken, entityId, Permission.READ)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("read allocation for resource %s", entityId);
            }

            RoomProviderCapability roomProviderCapability = resourceImpl.getCapability(RoomProviderCapability.class);

            // Setup resource allocation
            ResourceAllocation resourceAllocation = null;
            if (resourceImpl instanceof DeviceResource && roomProviderCapability != null) {
                SchedulerContext schedulerContext = new SchedulerContext(cache, entityManager, authorization, interval);
                AvailableRoom availableRoom = schedulerContext.getAvailableRoom(roomProviderCapability);
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
                resourceAllocation.addReservation(resourceReservation.toApi(authorization.isAdmin(securityToken)));
            }

            // Fill alias allocations
            List<cz.cesnet.shongo.controller.resource.AliasProviderCapability> aliasProviders =
                    resourceImpl.getCapabilities(cz.cesnet.shongo.controller.resource.AliasProviderCapability.class);
            for (cz.cesnet.shongo.controller.resource.AliasProviderCapability aliasProvider : aliasProviders) {
                List<cz.cesnet.shongo.controller.reservation.AliasReservation> aliasReservations =
                        resourceManager.listAliasReservationsInInterval(aliasProvider.getId(), interval);
                for (cz.cesnet.shongo.controller.reservation.AliasReservation aliasReservation : aliasReservations) {
                    resourceAllocation.addReservation(aliasReservation.toApi(authorization.isAdmin(securityToken)));
                }
            }
            return resourceAllocation;
        }
        finally {
            entityManager.close();
        }
    }
}
