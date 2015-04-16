package cz.cesnet.shongo.controller.api.rpc;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.AclIdentityType;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.Capability;
import cz.cesnet.shongo.controller.api.Domain;
import cz.cesnet.shongo.controller.api.DomainResource;
import cz.cesnet.shongo.controller.api.Resource;
import cz.cesnet.shongo.controller.api.Tag;
import cz.cesnet.shongo.controller.api.request.*;
import cz.cesnet.shongo.controller.authorization.*;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.alias.AliasProviderCapability;
import cz.cesnet.shongo.controller.booking.alias.AliasReservation;
import cz.cesnet.shongo.controller.booking.domain.*;
import cz.cesnet.shongo.controller.booking.resource.*;
import cz.cesnet.shongo.controller.booking.resource.DeviceResource;
import cz.cesnet.shongo.controller.booking.resource.ResourceReservation;
import cz.cesnet.shongo.controller.cache.Cache;
import cz.cesnet.shongo.controller.booking.room.RoomProviderCapability;
import cz.cesnet.shongo.controller.booking.room.AvailableRoom;
import cz.cesnet.shongo.controller.scheduler.SchedulerContext;
import cz.cesnet.shongo.controller.util.NativeQuery;
import cz.cesnet.shongo.controller.util.QueryFilter;
import org.hibernate.exception.ConstraintViolationException;
import org.joda.time.DateMidnight;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
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
    public ListResponse<ResourceSummary> listResources(ResourceListRequest request)
    {
        checkNotNull("request", request);
        SecurityToken securityToken = request.getSecurityToken();
        authorization.validate(securityToken);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ResourceManager resourceManager = new ResourceManager(entityManager);
        try {
            Set<Long> readableResourceIds = authorization.getEntitiesWithPermission(securityToken,
                    cz.cesnet.shongo.controller.booking.resource.Resource.class, ObjectPermission.READ);
            if (readableResourceIds != null) {
                // Allow to add/delete items in the set
                readableResourceIds = new HashSet<Long>(readableResourceIds);
            }

            QueryFilter queryFilter = new QueryFilter("resource_summary", true);
            queryFilter.addFilterIn("id", readableResourceIds);

            // Filter requested resource-ids
            if (request.getResourceIds().size() > 0) {
                Set<Long> requestedResourceIds = new HashSet<Long>();
                Set<Long> notReadableResourceIds = new HashSet<Long>();
                for (String resourceApiId : request.getResourceIds()) {
                    Long resourceId = ObjectIdentifier.parseId(resourceApiId, ObjectType.RESOURCE);
                    if (readableResourceIds != null && !readableResourceIds.contains(resourceId)) {
                        notReadableResourceIds.add(resourceId);
                    }
                    requestedResourceIds.add(resourceId);
                }
                queryFilter.addFilter("id IN(:resourceIds)");
                queryFilter.addFilterParameter("resourceIds", requestedResourceIds);

                // Check if user has any reservations for not readable resources for them to become readable
                if (notReadableResourceIds.size() > 0) {
                    Set<Long> readableReservationIds = authorization.getEntitiesWithPermission(securityToken,
                            cz.cesnet.shongo.controller.booking.reservation.Reservation.class, ObjectPermission.READ);
                    List readableResources = entityManager.createNativeQuery("SELECT resource_reservation.resource_id"
                            + " FROM resource_reservation"
                            + " WHERE resource_reservation.id IN (:reservationIds)"
                            + " AND resource_reservation.resource_id IN(:resourceIds)")
                            .setParameter("reservationIds", readableReservationIds)
                            .setParameter("resourceIds", notReadableResourceIds)
                            .getResultList();
                    for (Object readableResourceId : readableResources) {
                        readableReservationIds.add(((Number) readableResourceId).longValue());
                    }
                }
            }

            // Filter requested tag-id
            if (request.getTagId() != null) {
                queryFilter.addFilter("resource_summary.id IN ("
                        + " SELECT resource_id FROM resource_tag "
                        + " WHERE tag_id = :tagId)", "tagId", request.getTagId());
            }

            // Filter requested tag-name
            if (request.getTagNames() != null && !request.getTagNames().isEmpty()) {
                queryFilter.addFilter("resource_summary.id IN ("
                        + " SELECT resource_tag.resource_id FROM resource_tag "
                        + " LEFT JOIN tag ON tag.id = resource_tag.tag_id"
                        + " WHERE tag.name IN(:tagNames))", "tagNames", request.getTagNames());
            }

            // Filter user-ids
            Set<String> userIds = request.getUserIds();
            if (userIds != null && !userIds.isEmpty()) {
                queryFilter.addFilterIn("resource_summary.user_id", userIds);
            }

            // Filter name
            if (request.getName() != null) {
                queryFilter.addFilter("resource_summary.name = :name", "name", request.getName());
            }

            // Capability type
            if (request.getCapabilityClasses().size() > 0) {
                StringBuilder capabilityClassFilter = new StringBuilder();
                for (Class<? extends Capability> capabilityClass : request.getCapabilityClasses()) {
                    if (capabilityClassFilter.length() > 0) {
                        capabilityClassFilter.append(" OR ");
                    }
                    if (capabilityClass.equals(cz.cesnet.shongo.controller.api.RoomProviderCapability.class)) {
                        capabilityClassFilter.append("resource_summary.id IN ("
                                + " SELECT capability.resource_id FROM room_provider_capability"
                                + " LEFT JOIN capability ON capability.id = room_provider_capability.id)");
                    }
                    else if (capabilityClass.equals(RecordingCapability.class)) {
                        capabilityClassFilter.append("resource_summary.id IN ("
                                + " SELECT capability.resource_id FROM recording_capability"
                                + " LEFT JOIN capability ON capability.id = recording_capability.id)");
                    }
                    else {
                        throw new TodoImplementException(capabilityClass);
                    }
                }
                queryFilter.addFilter(capabilityClassFilter.toString());
            }


            // Technologies
            Set<Technology> technologies = request.getTechnologies();
            if (technologies.size() > 0) {
                queryFilter.addFilter("resource_summary.id IN ("
                        + " SELECT device_resource.id FROM device_resource "
                        + " LEFT JOIN device_resource_technologies ON device_resource_technologies.device_resource_id = device_resource.id"
                        + " WHERE device_resource_technologies.technologies IN(:technologies))");
                queryFilter.addFilterParameter("technologies", technologies);
            }

            // Allocatable
            if (request.isAllocatable()) {
                queryFilter.addFilter("resource_summary.allocatable = TRUE");
            }

            // Query order by
            String queryOrderBy =
                    "resource_summary.allocatable DESC, resource_summary.allocation_order, resource_summary.id";

            Map<String, String> parameters = new HashMap<String, String>();
            parameters.put("filter", queryFilter.toQueryWhere());
            parameters.put("order", queryOrderBy);
            String query = NativeQuery.getNativeQuery(NativeQuery.RESOURCE_LIST, parameters);

            ListResponse<ResourceSummary> response = new ListResponse<ResourceSummary>();
            List<Object[]> records = performNativeListRequest(query, queryFilter, request, response, entityManager);
            for (Object[] record : records) {
                ResourceSummary resourceSummary = new ResourceSummary();
                resourceSummary.setId(ObjectIdentifier.formatId(ObjectType.RESOURCE, record[0].toString()));
                if (record[1] != null) {
                    resourceSummary.setParentResourceId(
                            ObjectIdentifier.formatId(ObjectType.RESOURCE, record[1].toString()));
                }
                resourceSummary.setUserId(record[2].toString());
                resourceSummary.setName(record[3].toString());
                resourceSummary.setAllocatable(record[4] != null && (Boolean) record[4]);
                resourceSummary.setAllocationOrder(record[5] != null ? (Integer) record[5] : null);
                resourceSummary.setDescription(record[7] != null ? record[7].toString() : "");
                if (record[6] != null) {
                    String recordTechnologies = record[6].toString();
                    if (!recordTechnologies.isEmpty()) {
                        for (String technology : recordTechnologies.split(",")) {
                            resourceSummary.addTechnology(Technology.valueOf(technology.trim()));
                        }
                    }
                }
                resourceSummary.setCalendarPublic((Boolean)record[8]);
                if (resourceSummary.isCalendarPublic()) {
                    resourceSummary.setCalendarUriKey(record[9].toString());
                }
                response.addItem(resourceSummary);
            }
            return response;
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
                        resourceReservation.toApi(entityManager, authorization.isOperator(securityToken)));
            }

            // Fill alias allocations
            List<cz.cesnet.shongo.controller.booking.alias.AliasProviderCapability> aliasProviders =
                    resourceImpl.getCapabilities(AliasProviderCapability.class);
            for (AliasProviderCapability aliasProvider : aliasProviders) {
                List<cz.cesnet.shongo.controller.booking.alias.AliasReservation> aliasReservations =
                        resourceManager.listAliasReservationsInInterval(aliasProvider.getId(), slot);
                for (AliasReservation aliasReservation : aliasReservations) {
                    resourceAllocation.addReservation(
                            aliasReservation.toApi(entityManager, authorization.isOperator(securityToken)));
                }
            }
            return resourceAllocation;
        }
        finally {
            entityManager.close();
        }
    }

    @Override
    public String createTag(SecurityToken securityToken, Tag tagApi) {
        authorization.validate(securityToken);
        checkNotNull("tag", tagApi);

        cz.cesnet.shongo.controller.booking.resource.Tag tag = new cz.cesnet.shongo.controller.booking.resource.Tag();

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ResourceManager resourceManager = new ResourceManager(entityManager);
        AuthorizationManager authorizationManager = new AuthorizationManager(entityManager, authorization);

        try {
            authorizationManager.beginTransaction();
            entityManager.getTransaction().begin();

            // Create tag from API
            tag.fromApi(tagApi);

            // Save it
            resourceManager.createTag(tag);

            authorizationManager.createAclEntry(AclIdentityType.USER, securityToken.getUserId(), tag, ObjectRole.OWNER);

            entityManager.getTransaction().commit();
            authorizationManager.commitTransaction();
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

        // Return tag shongo-id
        return ObjectIdentifier.formatId(tag);
    }

    @Override
    public List<Tag> listTags(TagListRequest request) {
        checkNotNull("request", request);
        SecurityToken securityToken = request.getSecurityToken();
        authorization.validate(securityToken);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ResourceManager resourceManager = new ResourceManager(entityManager);
        try {
            List<Tag> tagList = new ArrayList();
            List<ResourceTag> resourceTags;
            if (request.getResourceId() == null) {
                for (cz.cesnet.shongo.controller.booking.resource.Tag tag : resourceManager.listAllTags()) {
                    tagList.add(tag.toApi());
                }
            } else {
                Long persistenceResourceId = ObjectIdentifier.parseId(request.getResourceId(),ObjectType.RESOURCE);
                resourceTags = resourceManager.getResourceTags(persistenceResourceId);

                for (ResourceTag resourceTag : resourceTags) {
                    Tag tag = resourceTag.getTag().toApi();

                    tagList.add(tag);
                }
            }

            return tagList;
        }
        finally {
            entityManager.close();
        }
    }

    @Override
    public Tag getTag(SecurityToken token, String tagId) {
        checkNotNull("tag-id", tagId);
        authorization.validate(token);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ResourceManager resourceManager = new ResourceManager(entityManager);
        try {
            return resourceManager.getTag(ObjectIdentifier.parseId(tagId, ObjectType.TAG)).toApi();
        }
        finally {
            entityManager.close();
        }
    }

    @Override
    public Tag findTag(SecurityToken token, String name) {
        checkNotNull("name", name);
        authorization.validate(token);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ResourceManager resourceManager = new ResourceManager(entityManager);
        try {
            return resourceManager.findTag(name).toApi();
        }
        finally {
            entityManager.close();
        }
    }

    @Override
    public void modifyTag(SecurityToken token, Tag tag) {
        throw new TodoImplementException("MR");
    }

    @Override
    public void deleteTag(SecurityToken token, String tagId) {
        authorization.validate(token);
        checkNotNull("tag", tagId);
        Long persistanceId = ObjectIdentifier.parseId(tagId,ObjectType.TAG);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ResourceManager resourceManager = new ResourceManager(entityManager);
        AuthorizationManager authorizationManager = new AuthorizationManager(entityManager, authorization);

        try {
            authorizationManager.beginTransaction();
            entityManager.getTransaction().begin();

            // Delete the tag
            cz.cesnet.shongo.controller.booking.resource.Tag persistanceTag;
            persistanceTag = entityManager.find(cz.cesnet.shongo.controller.booking.resource.Tag.class, persistanceId);
            authorizationManager.deleteAclEntriesForEntity(persistanceTag);
            resourceManager.deleteTag(persistanceTag);

            entityManager.getTransaction().commit();
            authorizationManager.commitTransaction();
        }
        catch (RollbackException exception) {
            if (exception.getCause() != null && exception.getCause() instanceof PersistenceException) {
                PersistenceException cause = (PersistenceException) exception.getCause();
                if (cause.getCause() != null && cause.getCause() instanceof ConstraintViolationException) {
                    logger.warn("Tag '" + tagId + "' cannot be deleted because is still referenced.",
                            exception);
                    ControllerReportSetHelper.throwObjectNotDeletableReferencedFault(
                            cz.cesnet.shongo.controller.booking.resource.Tag.class, persistanceId);
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
    public void assignResourceTag(SecurityToken token, String resourceId, String tagId) {
        authorization.validate(token);
        checkNotNull("resource-id", resourceId);
        checkNotNull("tag-id",tagId);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ResourceManager resourceManager = new ResourceManager(entityManager);
        AuthorizationManager authorizationManager = new AuthorizationManager(entityManager, authorization);

        try {
            authorizationManager.beginTransaction();
            entityManager.getTransaction().begin();

            cz.cesnet.shongo.controller.booking.resource.Resource resource;
            resource = resourceManager.get(ObjectIdentifier.parseId(resourceId,ObjectType.RESOURCE));
            cz.cesnet.shongo.controller.booking.resource.Tag tag;
            tag = resourceManager.getTag(ObjectIdentifier.parseId(tagId,ObjectType.TAG));

            ResourceTag resourceTag = new ResourceTag();

            resourceTag.setResource(resource);
            resourceTag.setTag(tag);

            resourceManager.createResourceTag(resourceTag);

            authorizationManager.createAclEntriesForChildEntity(tag, resource);


            entityManager.getTransaction().commit();
            authorizationManager.commitTransaction();
        }
        catch (RollbackException exception) {
            if (exception.getCause() != null && exception.getCause() instanceof PersistenceException) {
                PersistenceException cause = (PersistenceException) exception.getCause();
                if (cause.getCause() != null && cause.getCause() instanceof ConstraintViolationException) {
                    throw new IllegalArgumentException("Tag (tag-id: " + tagId + " has been allready assigned to this resource (resource-id: " + resourceId + ").",exception);
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
    public void removeResourceTag(SecurityToken token, String resourceId, String tagId) {
        authorization.validate(token);
        checkNotNull("resource", resourceId);
        checkNotNull("tag", tagId);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ResourceManager resourceManager = new ResourceManager(entityManager);
        AuthorizationManager authorizationManager = new AuthorizationManager(entityManager, authorization);


        try {
            authorizationManager.beginTransaction();
            entityManager.getTransaction().begin();

            // Delete the resourceTag
            Long persistenceTagId = ObjectIdentifier.parseId(tagId, ObjectType.TAG);
            Long persistenceResourceId = ObjectIdentifier.parseId(resourceId,ObjectType.RESOURCE);
            ResourceTag resourceTag = resourceManager.getResourceTag(persistenceResourceId, persistenceTagId);

            authorizationManager.deleteAclEntriesForChildEntity(resourceTag.getTag(), resourceTag.getResource());

            resourceManager.deleteResourceTag(resourceTag);

            entityManager.getTransaction().commit();
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
    public String createDomain(SecurityToken securityToken, Domain domainApi) {
        authorization.validate(securityToken);
        checkNotNull("domain", domainApi);

        cz.cesnet.shongo.controller.booking.domain.Domain domain = new cz.cesnet.shongo.controller.booking.domain.Domain();

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ResourceManager resourceManager = new ResourceManager(entityManager);
        AuthorizationManager authorizationManager = new AuthorizationManager(entityManager, authorization);

        try {
            authorizationManager.beginTransaction();
            entityManager.getTransaction().begin();

            // Create tag from API
            domain.fromApi(domainApi);

            // Save it
            resourceManager.createDomain(domain);

            authorizationManager.createAclEntry(AclIdentityType.USER, securityToken.getUserId(), domain, ObjectRole.OWNER);

            entityManager.getTransaction().commit();
            authorizationManager.commitTransaction();
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

        // Return domain shongo-id
        return ObjectIdentifier.formatId(ObjectType.DOMAIN, domain.getId());
    }

    @Override
    public List<DomainResource> listDomainResources(DomainListRequest request)
    {
        throw new TodoImplementException("listDomainResources");
//        checkNotNull("request", request);
//        SecurityToken securityToken = request.getSecurityToken();
//        authorization.validate(securityToken);
//
//        EntityManager entityManager = entityManagerFactory.createEntityManager();
//        ResourceManager resourceManager = new ResourceManager(entityManager);
//        try {
//            List<Tag> tagList = new ArrayList();
//            List<ResourceTag> resourceTags;
//            if (request.getResourceId() == null) {
//                for (cz.cesnet.shongo.controller.booking.resource.Tag tag : resourceManager.listAllTags()) {
//                    tagList.add(tag.toApi());
//                }
//            } else {
//                Long persistenceResourceId = ObjectIdentifier.parseId(request.getResourceId(),ObjectType.RESOURCE);
//                resourceTags = resourceManager.getResourceTags(persistenceResourceId);
//
//                for (ResourceTag resourceTag : resourceTags) {
//                    Tag tag = resourceTag.getTag().toApi();
//
//                    tagList.add(tag);
//                }
//            }
//
//            return tagList;
//        }
//        finally {
//            entityManager.close();
//        }
    }

    @Override
    public Domain getDomain(SecurityToken token, String domainId) {
        checkNotNull("domain-id", domainId);
        authorization.validate(token);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ResourceManager resourceManager = new ResourceManager(entityManager);
        try {
            cz.cesnet.shongo.controller.booking.domain.Domain domain = resourceManager.getDomain(
                    ObjectIdentifier.parseId(domainId, ObjectType.DOMAIN));

            if (!authorization.hasObjectPermission(token, domain, ObjectPermission.READ)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("read domain %s", domainId);
            }

            return domain.toApi();
        }
        finally {
            entityManager.close();
        }
    }

    @Override
    public void modifyDomain(SecurityToken token, Domain domain) {
        throw new TodoImplementException();
    }

    @Override
    public void deleteDomain(SecurityToken token, String domainId) {
        authorization.validate(token);
        checkNotNull("domain-id", domainId);
        Long persistanceId = ObjectIdentifier.parseId(domainId,ObjectType.DOMAIN);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ResourceManager resourceManager = new ResourceManager(entityManager);
        AuthorizationManager authorizationManager = new AuthorizationManager(entityManager, authorization);

        try {
            authorizationManager.beginTransaction();
            entityManager.getTransaction().begin();

            // Delete the domain
            cz.cesnet.shongo.controller.booking.domain.Domain persistanceDomain = resourceManager.getDomain(
                    ObjectIdentifier.parseId(domainId, ObjectType.DOMAIN));

            if (!authorization.hasObjectPermission(token, persistanceDomain, ObjectPermission.WRITE)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("delete domain %s", domainId);
            }

            authorizationManager.deleteAclEntriesForEntity(persistanceDomain);
            resourceManager.deleteDomain(persistanceDomain);

            entityManager.getTransaction().commit();
            authorizationManager.commitTransaction();
        }
        catch (RollbackException exception) {
            if (exception.getCause() != null && exception.getCause() instanceof PersistenceException) {
                PersistenceException cause = (PersistenceException) exception.getCause();
                if (cause.getCause() != null && cause.getCause() instanceof ConstraintViolationException) {
                    logger.warn("Domain '" + domainId + "' cannot be deleted because is still referenced.",
                            exception);
                    ControllerReportSetHelper.throwObjectNotDeletableReferencedFault(
                            cz.cesnet.shongo.controller.booking.resource.Tag.class, persistanceId);
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
    public void addDomainResource(SecurityToken token, DomainResource domainResourceApi, String domainId, String resourceId) {
        authorization.validate(token);
        checkNotNull("domain-resource", domainResourceApi);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ResourceManager resourceManager = new ResourceManager(entityManager);
        AuthorizationManager authorizationManager = new AuthorizationManager(entityManager, authorization);

        try {
            authorizationManager.beginTransaction();
            entityManager.getTransaction().begin();

            cz.cesnet.shongo.controller.booking.resource.Resource resource;
            resource = resourceManager.get(ObjectIdentifier.parseId(resourceId, ObjectType.RESOURCE));
            cz.cesnet.shongo.controller.booking.domain.Domain domain;
            domain = resourceManager.getDomain(ObjectIdentifier.parseId(domainId, ObjectType.DOMAIN));

            if (!authorization.hasObjectPermission(token, resource, ObjectPermission.WRITE)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("add domain (%s) for resource (%s)", domainId, resourceId);
            }

            cz.cesnet.shongo.controller.booking.domain.DomainResource domainResource;
            domainResource = new cz.cesnet.shongo.controller.booking.domain.DomainResource();

            domainResource.setDomain(domain);
            domainResource.setResource(resource);
            domainResource.setLicenseCount(domainResourceApi.getLicenseCount());
            domainResource.setPrice(domainResourceApi.getPrice());
            domainResource.setPriority(domainResourceApi.getPriority());

            resourceManager.createDomainResource(domainResource);

            entityManager.getTransaction().commit();
            authorizationManager.commitTransaction();
        }
        catch (RollbackException exception) {
            if (exception.getCause() != null && exception.getCause() instanceof PersistenceException) {
                PersistenceException cause = (PersistenceException) exception.getCause();
                if (cause.getCause() != null && cause.getCause() instanceof ConstraintViolationException) {
                    throw new IllegalArgumentException("Resource (resource-id: " + resourceId + " has been allready assigned to this resource (resource-id: " + resourceId + ").", exception);
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
    public void removeDomainResource(SecurityToken token, String domainId, String resourceId) {
        authorization.validate(token);
        checkNotNull("domain-id", domainId);
        checkNotNull("resource-id", resourceId);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ResourceManager resourceManager = new ResourceManager(entityManager);
        AuthorizationManager authorizationManager = new AuthorizationManager(entityManager, authorization);


        try {
            authorizationManager.beginTransaction();
            entityManager.getTransaction().begin();

            // Delete the domainResource
            Long persistenceDomainId = ObjectIdentifier.parseId(domainId, ObjectType.DOMAIN);
            Long persistenceResourceId = ObjectIdentifier.parseId(resourceId,ObjectType.RESOURCE);
            cz.cesnet.shongo.controller.booking.domain.DomainResource domainResource = resourceManager.getDomainResource(persistenceDomainId, persistenceResourceId);

            if (!authorization.hasObjectPermission(token, domainResource.getDomain(), ObjectPermission.WRITE)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("add domain (%s) for resource (%s)", domainId, resourceId);
            }

            resourceManager.deleteDomainResource(domainResource);

            entityManager.getTransaction().commit();
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
    public ListResponse<ResourceSummary> getResourceIdsWithPublicCalendar()
    {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ResourceManager resourceManager = new ResourceManager(entityManager);
        try {
            QueryFilter queryFilter = new QueryFilter("resource_summary", true);
            // List only resources with public calendar
            queryFilter.addFilter("resource_summary.calendar_public  = TRUE");

            // Query order by
            String queryOrderBy = "resource_summary.id";

            Map<String, String> parameters = new HashMap<String, String>();
            parameters.put("filter", queryFilter.toQueryWhere());
            parameters.put("order", queryOrderBy);
            String query = NativeQuery.getNativeQuery(NativeQuery.RESOURCE_LIST, parameters);

            ListResponse<ResourceSummary> response = new ListResponse<ResourceSummary>();
            ListRequest request = new ListRequest(0,-1);
            List<Object[]> records = performNativeListRequest(query, queryFilter, request, response, entityManager);
            for (Object[] record : records) {
                ResourceSummary resourceSummary = new ResourceSummary();
                resourceSummary.setId(ObjectIdentifier.formatId(ObjectType.RESOURCE, record[0].toString()));
                resourceSummary.setCalendarUriKey(record[9].toString());
                response.addItem(resourceSummary);
            }
            return response;
        }
        finally {
            entityManager.close();
        }
    }
}