package cz.cesnet.shongo.controller.api.rpc;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.AclIdentityType;
import cz.cesnet.shongo.controller.acl.AclObjectClass;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.request.ResourceListRequest;
import cz.cesnet.shongo.controller.authorization.*;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.alias.AliasProviderCapability;
import cz.cesnet.shongo.controller.booking.alias.AliasReservation;
import cz.cesnet.shongo.controller.booking.resource.ResourceReservation;
import cz.cesnet.shongo.controller.cache.Cache;
import cz.cesnet.shongo.controller.booking.resource.DeviceResource;
import cz.cesnet.shongo.controller.booking.resource.ResourceManager;
import cz.cesnet.shongo.controller.booking.room.RoomProviderCapability;
import cz.cesnet.shongo.controller.booking.room.AvailableRoom;
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
    public void init(ControllerConfiguration configuration)
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
        checkNotNull("resource", resourceApi);

        // Change user id (only root can do that)
        String userId = securityToken.getUserId();
        if (resourceApi.getUserId() != null && authorization.isAdministrator(securityToken)) {
            userId = resourceApi.getUserId();
        }

        cz.cesnet.shongo.controller.booking.resource.Resource resource;

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ResourceManager resourceManager = new ResourceManager(entityManager);
        AuthorizationManager authorizationManager = new AuthorizationManager(entityManager, authorization);
        try {
            authorizationManager.beginTransaction();
            entityManager.getTransaction().begin();

            // Create resource from API
            resource = cz.cesnet.shongo.controller.booking.resource.Resource.createFromApi(resourceApi, entityManager);
            resource.setUserId(userId);

            // Save it
            resourceManager.create(resource);

            authorizationManager.createAclEntry(AclIdentityType.USER, userId, resource, ObjectRole.OWNER);

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
        return ObjectIdentifier.formatId(resource);
    }

    @Override
    public void modifyResource(SecurityToken securityToken, Resource resourceApi)
    {
        authorization.validate(securityToken);
        checkNotNull("resource", resourceApi);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ResourceManager resourceManager = new ResourceManager(entityManager);
        String resourceId = resourceApi.getId();
        ObjectIdentifier objectId = ObjectIdentifier.parse(resourceId, ObjectType.RESOURCE);

        try {
            entityManager.getTransaction().begin();

            // Get reservation request
            cz.cesnet.shongo.controller.booking.resource.Resource resource =
                    resourceManager.get(objectId.getPersistenceId());

            if (!authorization.hasObjectPermission(securityToken, resource, ObjectPermission.WRITE)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("modify resource %s", objectId);
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
        checkNotNull("resourceId", resourceId);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ResourceManager resourceManager = new ResourceManager(entityManager);
        AuthorizationManager authorizationManager = new AuthorizationManager(entityManager, authorization);
        ObjectIdentifier objectId = ObjectIdentifier.parse(resourceId, ObjectType.RESOURCE);
        try {
            authorizationManager.beginTransaction();
            entityManager.getTransaction().begin();

            // Get the resource
            cz.cesnet.shongo.controller.booking.resource.Resource resource =
                    resourceManager.get(objectId.getPersistenceId());

            if (!authorization.hasObjectPermission(securityToken, resource, ObjectPermission.WRITE)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("delete resource %s", objectId);
            }

            authorizationManager.deleteAclEntriesForEntity(resource);

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
                    ControllerReportSetHelper.throwObjectNotDeletableReferencedFault(
                            cz.cesnet.shongo.controller.booking.resource.Resource.class, objectId.getPersistenceId());
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
    public Collection<ResourceSummary> listResources(ResourceListRequest request)
    {
        checkNotNull("request", request);
        SecurityToken securityToken = request.getSecurityToken();
        authorization.validate(securityToken);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ResourceManager resourceManager = new ResourceManager(entityManager);
        try {
            AclObjectClass aclObjectClass = authorization.getAclProvider().getObjectClass(
                    cz.cesnet.shongo.controller.booking.resource.Resource.class);
            Set<Long> resourceIds = authorization.getEntitiesWithPermission(securityToken,
                    aclObjectClass, ObjectPermission.READ);

            QueryFilter filter = new QueryFilter("resource");
            filter.addFilterIn("id", resourceIds);

            // Filter user-ids
            Set<String> userIds = request.getUserIds();
            if (userIds != null && !userIds.isEmpty()) {
                filter.addFilterIn("userId", userIds);
            }

            // Filter name
            if (request.getName() != null) {
                filter.addFilter("resource.name = :name", "name", request.getName());
            }

            List<cz.cesnet.shongo.controller.booking.resource.Resource> list = resourceManager.list(filter);
            List<ResourceSummary> summaryList = new ArrayList<ResourceSummary>();
            for (cz.cesnet.shongo.controller.booking.resource.Resource resource : list) {
                ResourceSummary summary = new ResourceSummary();
                summary.setId(ObjectIdentifier.formatId(resource));
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
                cz.cesnet.shongo.controller.booking.resource.Resource parentResource = resource.getParentResource();
                if (parentResource != null) {
                    summary.setParentResourceId(ObjectIdentifier.formatId(parentResource));
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
        checkNotNull("resourceId", resourceId);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ResourceManager resourceManager = new ResourceManager(entityManager);
        ObjectIdentifier objectId = ObjectIdentifier.parse(resourceId, ObjectType.RESOURCE);
        try {
            cz.cesnet.shongo.controller.booking.resource.Resource resource = resourceManager.get(
                    objectId.getPersistenceId());

            if (!authorization.hasObjectPermission(securityToken, resource, ObjectPermission.READ)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("read resource %s", objectId);
            }

            return resource.toApi(entityManager);
        }
        finally {
            entityManager.close();
        }
    }

    @Override
    public ResourceAllocation getResourceAllocation(SecurityToken securityToken, String resourceId, Interval slot)
    {
        authorization.validate(securityToken);
        checkNotNull("resourceId", resourceId);

        if (slot == null) {
            slot = new Interval(DateMidnight.now(), Period.days(31));
        }

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ResourceManager resourceManager = new ResourceManager(entityManager);
        ObjectIdentifier objectId = ObjectIdentifier.parse(resourceId, ObjectType.RESOURCE);

        try {
            cz.cesnet.shongo.controller.booking.resource.Resource resourceImpl =
                    resourceManager.get(objectId.getPersistenceId());

            if (!authorization.hasObjectPermission(securityToken, resourceImpl, ObjectPermission.READ)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("read allocation for resource %s", objectId);
            }

            RoomProviderCapability roomProviderCapability = resourceImpl.getCapability(RoomProviderCapability.class);

            // Setup resource allocation
            ResourceAllocation resourceAllocation = null;
            if (resourceImpl instanceof DeviceResource && roomProviderCapability != null) {
                SchedulerContext schedulerContext = new SchedulerContext(slot.getStart(), cache, entityManager,
                        new AuthorizationManager(entityManager, authorization));
                AvailableRoom availableRoom = schedulerContext.getAvailableRoom(roomProviderCapability, slot, null);
                RoomProviderResourceAllocation allocation = new RoomProviderResourceAllocation();
                allocation.setMaximumLicenseCount(availableRoom.getMaximumLicenseCount());
                allocation.setAvailableLicenseCount(availableRoom.getAvailableLicenseCount());
                resourceAllocation = allocation;
            }
            else {
                resourceAllocation = new ResourceAllocation();
            }
            resourceAllocation.setId(ObjectIdentifier.formatId(resourceImpl));
            resourceAllocation.setName(resourceImpl.getName());
            resourceAllocation.setInterval(slot);

            // Fill resource allocations
            Collection<cz.cesnet.shongo.controller.booking.resource.ResourceReservation> resourceReservations =
                    resourceManager.listResourceReservationsInInterval(objectId.getPersistenceId(), slot);
            for (ResourceReservation resourceReservation : resourceReservations) {
                resourceAllocation.addReservation(
                        resourceReservation.toApi(entityManager, authorization.isAdministrator(securityToken)));
            }

            // Fill alias allocations
            List<cz.cesnet.shongo.controller.booking.alias.AliasProviderCapability> aliasProviders =
                    resourceImpl.getCapabilities(AliasProviderCapability.class);
            for (AliasProviderCapability aliasProvider : aliasProviders) {
                List<cz.cesnet.shongo.controller.booking.alias.AliasReservation> aliasReservations =
                        resourceManager.listAliasReservationsInInterval(aliasProvider.getId(), slot);
                for (AliasReservation aliasReservation : aliasReservations) {
                    resourceAllocation.addReservation(
                            aliasReservation.toApi(entityManager, authorization.isAdministrator(securityToken)));
                }
            }
            return resourceAllocation;
        }
        finally {
            entityManager.close();
        }
    }
}
