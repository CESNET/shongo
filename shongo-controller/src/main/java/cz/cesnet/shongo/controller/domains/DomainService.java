package cz.cesnet.shongo.controller.domains;

import cz.cesnet.shongo.*;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.api.Domain;
import cz.cesnet.shongo.controller.api.ReservationSummary;
import cz.cesnet.shongo.controller.api.domains.response.*;
import cz.cesnet.shongo.controller.api.domains.response.ResourceSpecification;
import cz.cesnet.shongo.controller.api.request.*;
import cz.cesnet.shongo.controller.api.rpc.AbstractServiceImpl;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.booking.Allocation;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.ObjectTypeResolver;
import cz.cesnet.shongo.controller.booking.person.ForeignPerson;
import cz.cesnet.shongo.controller.booking.request.ReservationRequest;
import cz.cesnet.shongo.controller.booking.request.ReservationRequestManager;
import cz.cesnet.shongo.controller.booking.reservation.ReservationManager;
import cz.cesnet.shongo.controller.booking.resource.*;
import cz.cesnet.shongo.controller.cache.Cache;
import cz.cesnet.shongo.controller.cache.DomainCache;
import cz.cesnet.shongo.controller.util.NativeQuery;
import cz.cesnet.shongo.controller.util.QueryFilter;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Domain service implementation only for controller.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class DomainService extends AbstractServiceImpl implements Component.EntityManagerFactoryAware, Component.AuthorizationAware
{
    private static Logger logger = LoggerFactory.getLogger(DomainService.class);

    /**
     * @see javax.persistence.EntityManagerFactory
     */
    private EntityManagerFactory entityManagerFactory;

    private final ExpirationMap<ReservationListRequest, List<Reservation>> reservationsCache = new ExpirationMap<>();

    private final ReentrantReadWriteLock reservationCacheLock = new ReentrantReadWriteLock();

    /**
     * @see cz.cesnet.shongo.controller.cache.Cache
     */
    private Cache cache;
    /**
     * @see cz.cesnet.shongo.controller.authorization.Authorization
     */
    private Authorization authorization;

    public DomainService(EntityManagerFactory entityManagerFactory, Authorization authorization, Cache cache)
    {
        this.entityManagerFactory = entityManagerFactory;
        this.authorization = authorization;
        this.reservationsCache.setExpiration(Duration.standardMinutes(5));
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
//        checkDependency(cache, Cache.class);
        checkDependency(entityManagerFactory, EntityManagerFactory.class);
        checkDependency(authorization, Authorization.class);
        //TODO: overit zda tu configuration potrebuji
//        super.init(configuration);
    }


    public DomainCache getDomainCache()
    {
        return cache.getDomainCache();
    }

    //    @Override
//    public String getServiceName()
//    {
//        return "Resource";
//    }

    /**
     * List all domains without local domain. Every domain will have null status.
     *
     * @return list of {@link Domain}
     */
    public List<Domain> listForeignDomains()
    {
        return listDomains(true, null);
    }

    /**
     * List all domains included local domain depending on {@code onlyForeignDomains}. Every domain will have null status except local.
     * By param {@code allocatable} can be filtered only allocatable domains.
     *
     * @param onlyForeignDomains will be listed
     * @return list of {@link Domain}
     */
    public List<Domain> listDomains(boolean onlyForeignDomains, Boolean allocatable)
    {
        DomainCache domainCache = cache.getDomainCache();
        List<Domain> domainList = new ArrayList<>();
        if (!onlyForeignDomains) {
            domainList.add(LocalDomain.getLocalDomain().toApi());
        }
        for (cz.cesnet.shongo.controller.booking.domain.Domain domain : domainCache.getObjects()) {
            if (allocatable != null && !allocatable.equals(domain.isAllocatable())) {
                continue;
            }
            domainList.add(domain.toApi());
        }
        return Collections.unmodifiableList(domainList);
    }

//    public ListResponse<ResourceSummary> listLocalResources(ResourceListRequest request) {
//        checkNotNull("request", request);
//        SecurityToken securityToken = request.getSecurityToken();
//
//        EntityManager entityManager = entityManagerFactory.createEntityManager();
//        ResourceManager resourceManager = new ResourceManager(entityManager);
//        try {
//            Set<Long> readableResourceIds = authorization.getEntitiesWithPermission(securityToken,
//                    cz.cesnet.shongo.controller.booking.resource.Resource.class, ObjectPermission.READ);
//            if (readableResourceIds != null) {
//                // Allow to add/delete items in the set
//                readableResourceIds = new HashSet<Long>(readableResourceIds);
//            }
//
//            QueryFilter queryFilter = new QueryFilter("resource_summary", true);
//            queryFilter.addFilterIn("id", readableResourceIds);
//
//            // Filter requested resource-ids
//            if (request.getResourceIds().size() > 0) {
//                Set<Long> requestedResourceIds = new HashSet<Long>();
//                Set<Long> notReadableResourceIds = new HashSet<Long>();
//                for (String resourceApiId : request.getResourceIds()) {
//                    Long resourceId = ObjectIdentifier.parseLocalId(resourceApiId, ObjectType.RESOURCE);
//                    if (readableResourceIds != null && !readableResourceIds.contains(resourceId)) {
//                        notReadableResourceIds.add(resourceId);
//                    }
//                    requestedResourceIds.add(resourceId);
//                }
//                queryFilter.addFilter("id IN(:resourceIds)");
//                queryFilter.addFilterParameter("resourceIds", requestedResourceIds);
//
//                // Check if user has any reservations for not readable resources for them to become readable
//                if (notReadableResourceIds.size() > 0) {
//                    Set<Long> readableReservationIds = authorization.getEntitiesWithPermission(securityToken,
//                            cz.cesnet.shongo.controller.booking.reservation.Reservation.class, ObjectPermission.READ);
//                    List readableResources = entityManager.createNativeQuery("SELECT resource_reservation.resource_id"
//                            + " FROM resource_reservation"
//                            + " WHERE resource_reservation.id IN (:reservationIds)"
//                            + " AND resource_reservation.resource_id IN(:resourceIds)")
//                            .setParameter("reservationIds", readableReservationIds)
//                            .setParameter("resourceIds", notReadableResourceIds)
//                            .getResultList();
//                    for (Object readableResourceId : readableResources) {
//                        readableReservationIds.add(((Number) readableResourceId).longValue());
//                    }
//                }
//            }
//
//            // Filter requested tag-id
//            if (request.getTagId() != null) {
//                queryFilter.addFilter("resource_summary.id IN ("
//                        + " SELECT resource_id FROM resource_tag "
//                        + " WHERE tag_id = :tagId)", "tagId", ObjectIdentifier.parseLocalId(request.getTagId(), ObjectType.TAG));
//            }
//
//            // Filter requested tag-name
//            if (request.getTagName() != null) {
//                queryFilter.addFilter("resource_summary.id IN ("
//                        + " SELECT resource_tag.resource_id FROM resource_tag "
//                        + " LEFT JOIN tag ON tag.id = resource_tag.tag_id"
//                        + " WHERE tag.name = :tagName)", "tagName", request.getTagName());
//            }
//
//            // Filter requested by foreign domain
//            if (request.getDomainId() != null) {
//                queryFilter.addFilter("resource_summary.id IN ("
//                        + " SELECT resource_id FROM domain_resource "
//                        + " WHERE domain_id = :domainId)", "domainId", ObjectIdentifier.parseLocalId(request.getDomainId(), ObjectType.DOMAIN));
//            }
//
//            // Filter user-ids
//            Set<String> userIds = request.getUserIds();
//            if (userIds != null && !userIds.isEmpty()) {
//                queryFilter.addFilterIn("resource_summary.user_id", userIds);
//            }
//
//            // Filter name
//            if (request.getName() != null) {
//                queryFilter.addFilter("resource_summary.name = :name", "name", request.getName());
//            }
//
//            // Capability type
//            if (request.getCapabilityClasses().size() > 0) {
//                StringBuilder capabilityClassFilter = new StringBuilder();
//                for (Class<? extends Capability> capabilityClass : request.getCapabilityClasses()) {
//                    if (capabilityClassFilter.length() > 0) {
//                        capabilityClassFilter.append(" OR ");
//                    }
//                    if (capabilityClass.equals(cz.cesnet.shongo.controller.api.RoomProviderCapability.class)) {
//                        capabilityClassFilter.append("resource_summary.id IN ("
//                                + " SELECT capability.resource_id FROM room_provider_capability"
//                                + " LEFT JOIN capability ON capability.id = room_provider_capability.id)");
//                    } else if (capabilityClass.equals(RecordingCapability.class)) {
//                        capabilityClassFilter.append("resource_summary.id IN ("
//                                + " SELECT capability.resource_id FROM recording_capability"
//                                + " LEFT JOIN capability ON capability.id = recording_capability.id)");
//                    } else {
//                        throw new TodoImplementException(capabilityClass);
//                    }
//                }
//                queryFilter.addFilter(capabilityClassFilter.toString());
//            }
//
//
//            // Technologies
//            Set<Technology> technologies = request.getTechnologies();
//            if (technologies.size() > 0) {
//                queryFilter.addFilter("resource_summary.id IN ("
//                        + " SELECT device_resource.id FROM device_resource "
//                        + " LEFT JOIN device_resource_technologies ON device_resource_technologies.device_resource_id = device_resource.id"
//                        + " WHERE device_resource_technologies.technologies IN(:technologies))");
//                queryFilter.addFilterParameter("technologies", technologies);
//            }
//
//            // Allocatable
//            if (request.isAllocatable()) {
//                queryFilter.addFilter("resource_summary.allocatable = TRUE");
//            }
//
//            // Query order by
//            String queryOrderBy =
//                    "resource_summary.allocatable DESC, resource_summary.allocation_order, resource_summary.id";
//
//            Map<String, String> parameters = new HashMap<String, String>();
//            parameters.put("filter", queryFilter.toQueryWhere());
//            parameters.put("order", queryOrderBy);
//            String query = NativeQuery.getNativeQuery(NativeQuery.RESOURCE_LIST, parameters);
//
//            ListResponse<ResourceSummary> response = new ListResponse<ResourceSummary>();
//            List<Object[]> records = performNativeListRequest(query, queryFilter, request, response, entityManager);
//            for (Object[] record : records) {
//                ResourceSummary resourceSummary = new ResourceSummary();
//                resourceSummary.setId(ObjectIdentifier.formatId(ObjectType.RESOURCE, record[0].toString()));
//                if (record[1] != null) {
//                    resourceSummary.setParentResourceId(
//                            ObjectIdentifier.formatId(ObjectType.RESOURCE, record[1].toString()));
//                }
//                resourceSummary.setUserId(record[2].toString());
//                resourceSummary.setName(record[3].toString());
//                resourceSummary.setAllocatable(record[4] != null && (Boolean) record[4]);
//                resourceSummary.setAllocationOrder(record[5] != null ? (Integer) record[5] : null);
//                resourceSummary.setDescription(record[7] != null ? record[7].toString() : "");
//                if (record[6] != null) {
//                    String recordTechnologies = record[6].toString();
//                    if (!recordTechnologies.isEmpty()) {
//                        for (String technology : recordTechnologies.split(",")) {
//                            resourceSummary.addTechnology(Technology.valueOf(technology.trim()));
//                        }
//                    }
//                }
//                resourceSummary.setCalendarPublic((Boolean) record[8]);
//                if (resourceSummary.isCalendarPublic()) {
//                    resourceSummary.setCalendarUriKey(record[9].toString());
//                }
//                response.addItem(resourceSummary);
//            }
//            return response;
//        } finally {
//            entityManager.close();
//        }
//    }

//    public Resource getResource(SecurityToken securityToken, String resourceId) {
//        checkNotNull("resourceId", resourceId);
//
//        EntityManager entityManager = entityManagerFactory.createEntityManager();
//        ResourceManager resourceManager = new ResourceManager(entityManager);
//        ObjectIdentifier objectId = ObjectIdentifier.parse(resourceId, ObjectType.RESOURCE);
//        try {
//            cz.cesnet.shongo.controller.booking.resource.Resource resource = resourceManager.get(
//                    objectId.getPersistenceId());
//
//            if (!authorization.hasObjectPermission(securityToken, resource, ObjectPermission.READ)) {
//                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("read resource %s", objectId);
//            }
//
//            return resource.toApi(entityManager);
//        } finally {
//            entityManager.close();
//        }
//    }

    public List<DomainCapability> listLocalResourcesByDomain(Long domainId, DomainCapability.Type capabilityType,
                                                             Integer licenseCount, List<Set<Technology>> technologyVariants)
    {
        checkNotNull("domainId", domainId);
        checkNotNull("capabilityType", capabilityType);

        EntityManager entityManager = entityManagerFactory.createEntityManager();

        try {
            // Filter requested by foreign domain
            QueryFilter queryFilter = new QueryFilter("resource_summary", true);
            queryFilter.addFilter("domain_id = :domainId", "domainId", domainId);

            if (licenseCount != null && licenseCount > -1) {
                queryFilter.addFilter("license_count >= :licenseCount", "licenseCount", licenseCount);
            }
            //TODO: vytahnout zasedacky podle tagu
            // Filter requested tag-id
//            if (request.getTagId() != null) {
//                queryFilter.addFilter("resource_summary.id IN ("
//                        + " SELECT resource_id FROM resource_tag "
//                        + " WHERE tag_id = :tagId)", "tagId", ObjectIdentifier.parseLocalId(request.getTagId(), ObjectType.TAG));
//            }
//             Filter requested tag-name
//            if (request.getTagName() != null) {
//                queryFilter.addFilter("resource_summary.id IN ("
//                        + " SELECT resource_tag.resource_id FROM resource_tag "
//                        + " LEFT JOIN tag ON tag.id = resource_tag.tag_id"
//                        + " WHERE tag.name = :tagName)", "tagName", request.getTagName());
//            }

            // Filter by type
            String type = capabilityType.toDb();
            queryFilter.addFilter("resource_summary.type = '" + type + "'");

            //TODO vylistovat podle capabilities
            // Capability type
//            if (request.getCapabilityClasses().size() > 0) {
//                StringBuilder capabilityClassFilter = new StringBuilder();
//                for (Class<? extends Capability> capabilityClass : request.getCapabilityClasses()) {
//                    if (capabilityClassFilter.length() > 0) {
//                        capabilityClassFilter.append(" OR ");
//                    }
//                    if (capabilityClass.equals(cz.cesnet.shongo.controller.api.RoomProviderCapability.class)) {
//                        capabilityClassFilter.append("resource_summary.id IN ("
//                                + " SELECT capability.resource_id FROM room_provider_capability"
//                                + " LEFT JOIN capability ON capability.id = room_provider_capability.id)");
//                    }
//                    else if (capabilityClass.equals(RecordingCapability.class)) {
//                        capabilityClassFilter.append("resource_summary.id IN ("
//                                + " SELECT capability.resource_id FROM recording_capability"
//                                + " LEFT JOIN capability ON capability.id = recording_capability.id)");
//                    }
//                    else {
//                        throw new TodoImplementException(capabilityClass);
//                    }
//                }
//                queryFilter.addFilter(capabilityClassFilter.toString());
//            }

            // Technologies
            if (technologyVariants != null && !technologyVariants.isEmpty()) {
                int noOfVariant = 0;
                StringBuilder variantsBuilder = new StringBuilder();
                variantsBuilder.append("( ");
                for (Set<Technology> technologies : technologyVariants) {
                    if (technologies.size() > 0) {
                        noOfVariant++;
                        if (noOfVariant != 1) {
                            variantsBuilder.append(" OR ");
                        }

//                        variantsBuilder.append("resource_summary.id IN ("
//                                + " SELECT device_resource.id FROM device_resource "
//                                + " LEFT JOIN device_resource_technologies ON device_resource_technologies.device_resource_id = device_resource.id"
//                                + " WHERE device_resource_technologies.technologies IN(:technologies" + noOfVariant + " ))");
//                        queryFilter.addFilterParameter("technologies" + noOfVariant, technologies);
                        int noOfTechnology = 0;
                        for (Technology technology : technologies) {
                            noOfTechnology++;
                            if (noOfTechnology != 1) {
                                variantsBuilder.append(" AND ");
                            }
                            variantsBuilder.append("resource_summary.id IN ("
                                    + " SELECT device_resource.id FROM device_resource "
                                    + " LEFT JOIN device_resource_technologies ON device_resource_technologies.device_resource_id = device_resource.id"
                                    + " WHERE device_resource_technologies.technologies = :technology" + noOfVariant + "_" + noOfTechnology + " )");
                            queryFilter.addFilterParameter("technology" + noOfVariant + "_" + noOfTechnology, technology.toString());
                        }
                    }
                }
                variantsBuilder.append(" )");
                queryFilter.addFilter(variantsBuilder.toString());
            }
            // Allocatable
            queryFilter.addFilter("resource_summary.allocatable = TRUE");

            // Query order by
            String queryOrderBy =
                    "resource_summary.allocatable DESC, resource_summary.allocation_order, resource_summary.id";

            Map<String, String> parameters = new HashMap<>();
            parameters.put("filter", queryFilter.toQueryWhere());
            parameters.put("order", queryOrderBy);
            String query = NativeQuery.getNativeQuery(NativeQuery.DOMAIN_RESOURCE_LIST, parameters);

            List<DomainCapability> response = new ArrayList<>();
            ListRequest listRequest = new ListRequest(0, -1);
            ListResponse listResponse = new ListResponse();
            List<Object[]> records = performNativeListRequest(query, queryFilter, listRequest, listResponse, entityManager);
            for (Object[] record : records) {
                DomainCapability domainCapability = new DomainCapability();
                domainCapability.setId(ObjectIdentifier.formatId(ObjectType.RESOURCE, record[0].toString()));
                domainCapability.setName(record[1].toString());
                domainCapability.setDescription(record[2] != null ? record[2].toString() : "");
                domainCapability.setCalendarPublic((Boolean) record[3]);
                if ((Boolean) record[3]) {
                    domainCapability.setCalendarUriKey(record[4].toString());
                }
                domainCapability.setLicenseCount((Integer) record[5]);
                domainCapability.setPrice((Integer) record[6]);
                if (record[7] != null) {
                    String recordTechnologies = record[7].toString();
                    if (!recordTechnologies.isEmpty()) {
                        for (String technology : recordTechnologies.split(",")) {
                            domainCapability.addTechnology(Technology.valueOf(technology.trim()));
                        }
                    }
                }
                domainCapability.setCapabilityType(DomainCapability.Type.createFromDB(record[8].toString().trim()));
                response.add(domainCapability);
            }
            return response;
        } finally {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            entityManager.close();
        }
    }

    /**
     * List reservations for given resources or all readable resources (@see listReadableResourcesIds())
     *
     * @param request
     * @return list of {@link Reservation}
     */
    public List<Reservation> listPublicReservations(ReservationListRequest request)
    {
        if (request.getInterval() != null) {
            request.setInterval(Temporal.roundIntervalToDays(request.getInterval()));
        }

        List<Reservation> response;
        reservationCacheLock.readLock().lock();
        try {
            response = reservationsCache.get(request);
            if (response != null) {
                return response;
            }
        }
        finally {
            reservationCacheLock.readLock().unlock();
        }

        reservationCacheLock.writeLock().lock();

        QueryFilter queryFilter = new QueryFilter("reservation_summary");
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        response = new ArrayList<>();
        try {
            queryFilter.addFilter("reservation_summary.type='RESOURCE'");
            //ROOM_PROVIDER

            // List reservations for given resource IDs or for all readable resources
            Set<Long> resourceIds = listReadableResourcesIds(request.getResourceIds());
            // If there is no readable resource
            if (resourceIds.isEmpty()) {
                logger.debug("No resources readable for foreign domain.");
                return response;
            }
            logger.debug("Readable resources used for reservation lists: " + Arrays.toString(resourceIds.toArray()));
            queryFilter.addFilter("reservation_summary.resource_id IN(:resourceIds)");
                queryFilter.addFilterParameter("resourceIds", resourceIds);


            // List only reservations in requested interval
            Interval interval = request.getInterval();
            if (interval != null) {
                queryFilter.addFilter("reservation_summary.slot_start < :slotEnd");
                queryFilter.addFilter("reservation_summary.slot_end > :slotStart");
                queryFilter.addFilterParameter("slotStart", interval.getStart().toDate());
                queryFilter.addFilterParameter("slotEnd", interval.getEnd().toDate());
            }

            // Sort query part
            String queryOrderBy;
            ReservationListRequest.Sort sort = request.getSort();
            if (sort != null) {
                switch (sort) {
                    case SLOT:
                        queryOrderBy = "reservation_summary.slot_start";
                        break;
                    default:
                        throw new TodoImplementException(sort);
                }
            }
            else {
                queryOrderBy = "reservation_summary.slot_start";
            }
            Boolean sortDescending = request.getSortDescending();
            sortDescending = (sortDescending != null ? sortDescending : false);
            if (sortDescending) {
                queryOrderBy = queryOrderBy + " DESC";
            }

            Map<String, String> parameters = new HashMap<>();
            parameters.put("filter", queryFilter.toQueryWhere());
            parameters.put("order", queryOrderBy);
            String query = NativeQuery.getNativeQuery(NativeQuery.RESERVATION_LIST, parameters);

            List<Object[]> records = performNativeListRequest(query, queryFilter, request, new ListResponse(), entityManager);
            for (Object[] record : records) {
                Reservation reservation = getReservation(record);
                response.add(reservation);
            }
            reservationsCache.put(request, response);
            reservationsCache.clearExpired(DateTime.now());
            return response;
        }
        finally {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            entityManager.close();
            reservationCacheLock.writeLock().unlock();
        }
    }

    /**
     * List resources with at least read permissions for group everyone.
     *
     * @param resourceIds to verify if readable
     * @return set of resources IDs
     */
    private Set<Long> listReadableResourcesIds(Set<String> resourceIds)
    {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            QueryFilter queryFilter = new QueryFilter("acl_entry", true);

            // List only records which are requested
//            if (request.getEntryIds().size() > 0) {
//                queryFilter.addFilter("acl_entry.id IN (:aclEntryIds)");
//                Set<Long> aclEntryIds = new HashSet<>();
//                for (String aclEntryId : request.getEntryIds()) {
//                    aclEntryIds.add(Long.valueOf(aclEntryId));
//                }
//                queryFilter.addFilterParameter("aclEntryIds", aclEntryIds);
//            }

            // List only records for entities which are requested
            StringBuilder objectFilter = new StringBuilder();
            if (resourceIds.size() > 0) {
//                AclProvider aclProvider = authorization.getAclProvider();
//                Set<Long> objectClassesIds = new HashSet<Long>();
                Set<Long> objectIdentityIds = new HashSet<>();
                for (String objectId : resourceIds) {
                    ObjectIdentifier objectIdentifier = ObjectIdentifier.parse(objectId);
                    // TODO: delete: Check object existence
//                    PersistentObject object = checkObjectExistence(objectIdentifier, entityManager);
//                    AclObjectIdentity objectIdentity = aclProvider.getObjectIdentity(object);
                    objectIdentityIds.add(objectIdentifier.getPersistenceId());
                }

                // List only given resources
                if (objectIdentityIds.size() > 0) {
                    if (objectFilter.length() > 0) {
                        objectFilter.append(" OR ");
                    } else {
                        objectFilter.append("(");
                    }
                    objectFilter.append("acl_entry.object_id IN(:objectIds)");
                    queryFilter.addFilterParameter("objectIds", objectIdentityIds);
                }
                if (objectIdentityIds.size() > 0) {
                    objectFilter.append(")");
                    objectFilter.append(" AND ");
                }
            }
            objectFilter.append("acl_entry.object_class=:objectClass");
            queryFilter.addFilterParameter("objectClass", ObjectTypeResolver.getObjectType(Resource.class).toString());

            queryFilter.addFilter(objectFilter.toString());

            // ACL must exist for group EVERYONE
            queryFilter.addFilter("acl_entry.identity_type = 'GROUP' AND acl_entry.identity_principal_id=:groupId");
            queryFilter.addFilterParameter("groupId", Authorization.EVERYONE_GROUP_ID);

            //TODO: NOTICE: List only records with READ permissions (at this moment true by default)
//            if (ObjectRole.values() > 0) {
//                queryFilter.addFilter("acl_entry.role IN (:roles)");
//                queryFilter.addFilterParameter("roles", request.getRoles());
//            }

            Map<String, String> parameters = new HashMap<String, String>();
            parameters.put("filter", queryFilter.toQueryWhere());
            String query = NativeQuery.getNativeQuery(NativeQuery.ACL_ENTRY_LIST, parameters);

            Set<Long> response = new HashSet<>();
            List<Object[]> aclEntries = performNativeListRequest(query, queryFilter, new ListRequest(), new ListResponse(), entityManager);

            // Fill reservations to response
            for (Object[] aclEntry : aclEntries) {
//                AclEntry aclEntryApi = new AclEntry();
//                aclEntryApi.setId(aclEntry[0].toString());
//                aclEntryApi.setIdentityType(AclIdentityType.valueOf(aclEntry[2].toString()));
//                aclEntryApi.setIdentityPrincipalId(aclEntry[3].toString());
//                aclEntryApi.setObjectId(new ObjectIdentifier(
//                        ObjectType.valueOf(aclEntry[6].toString()), ((Number) aclEntry[7]).longValue()).toId());
//                aclEntryApi.setRole(ObjectRole.valueOf(aclEntry[8].toString()));
//                aclEntryApi.setDeletable(((Number) aclEntry[9]).intValue() == 0);
//                response.addItem(aclEntryApi);
                response.add(((Number) aclEntry[7]).longValue());
            }
            return response;
        }
//        catch (Exception e) {
//            logger.error(e.toString(), e);
//            throw e;
//        }
        finally {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            entityManager.close();
        }
    }

    /**
     * @param record
     * @return {@link ReservationSummary} from given {@code record}
     */
    private ReservationSummary getReservationSummary(Object[] record)
    {
        ReservationSummary reservationSummary = new ReservationSummary();
        reservationSummary.setId(ObjectIdentifier.formatId(ObjectType.RESERVATION, record[0].toString()));
        reservationSummary.setUserId(record[1] != null ? record[1].toString() : null);
        reservationSummary.setReservationRequestId(record[2] != null ?
                ObjectIdentifier.formatId(ObjectType.RESERVATION_REQUEST, record[2].toString()) : null);
        reservationSummary.setType(ReservationSummary.Type.valueOf(record[3].toString().trim()));
        reservationSummary.setSlot(new Interval(new DateTime(record[4]), new DateTime(record[5])));
        if (record[6] != null) {
            reservationSummary.setResourceId(ObjectIdentifier.formatId(ObjectType.RESOURCE, record[6].toString()));
        }
        if (record[7] != null) {
            EntityManager entityManager = entityManagerFactory.createEntityManager();
            ResourceManager resourceManager = new ResourceManager(entityManager);
            try {
                ForeignResources foreignResources = resourceManager.getForeignResources(((Number) record[7]).longValue());
                String domain = foreignResources.getDomain().getName();
                Long resourceId = foreignResources.getForeignResourceId();
                reservationSummary.setResourceId(ObjectIdentifier.formatId(domain, ObjectType.RESOURCE, resourceId));
            }
            finally {
                entityManager.close();
            }
        }
        if (record[8] != null) {
            reservationSummary.setRoomLicenseCount(record[8] != null ? ((Number) record[8]).intValue() : null);
        }
        if (record[9] != null) {
            reservationSummary.setRoomName(record[9] != null ? record[9].toString() : null);
        }
        if (record[10] != null) {
            reservationSummary.setAliasTypes(record[10] != null ? record[10].toString() : null);
        }
        if (record[11] != null) {
            reservationSummary.setValue(record[11] != null ? record[11].toString() : null);
        }
        if (record[12] != null) {
            reservationSummary.setReservationRequestDescription(record[12] != null ? record[12].toString() : null);
        }
        return reservationSummary;
    }

    /**
     * @param record
     * @return {@link Reservation} from given {@code record}
     */
    private Reservation getReservation(Object[] record)
    {
        Reservation reservation = new Reservation();
        reservation.setForeignReservationId(ObjectIdentifier.formatId(ObjectType.RESERVATION, record[0].toString()));
        String userId = record[1] != null ? record[1].toString() : null;
        reservation.setUserId(UserInformation.parseUserId(userId));
        reservation.setForeignReservationRequestId(record[2] != null ?
                ObjectIdentifier.formatId(ObjectType.RESERVATION_REQUEST, record[2].toString()) : null);
        switch (ReservationSummary.Type.valueOf(record[3].toString().trim())) {
            case RESOURCE:
                reservation.setType(DomainCapability.Type.RESOURCE);
                if (record[6] != null) {
                    cz.cesnet.shongo.controller.api.domains.response.ResourceSpecification resourceSpecification;
                    resourceSpecification = new ResourceSpecification(ObjectIdentifier.formatId(ObjectType.RESOURCE, record[6].toString()));
                    reservation.setSpecification(resourceSpecification);
                }
                break;
            case ROOM:
                reservation.setType(DomainCapability.Type.VIRTUAL_ROOM);
                //TODO: add RoomSpecification
//        if (record[8] != null) {
//            reservation.setRoomLicenseCount(record[8] != null ? ((Number) record[8]).intValue() : null);
//        }
//        if (record[9] != null) {
//            reservation.setRoomName(record[9] != null ? record[9].toString() : null);
//        }
            default:
                throw new TodoImplementException();
        }
        reservation.setSlot(new Interval(new DateTime(record[4]), new DateTime(record[5])));

//        TODO: delete
//        if (record[10] != null) {
//            reservation.setAliasTypes(record[10] != null ? record[10].toString() : null);
//        }
//        if (record[11] != null) {
//            reservation.setValue(record[11] != null ? record[11].toString() : null);
//        }
        if (record[12] != null) {
            reservation.setReservationRequestDescription(record[12] != null ? record[12].toString() : null);
        }
        return reservation;
    }

    public void deleteReservationRequest(ReservationRequest reservationRequest)
    {
        checkNotNull("reservationRequest", reservationRequest);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        AuthorizationManager authorizationManager = new AuthorizationManager(entityManager, authorization);
        try {
            authorizationManager.beginTransaction();
            entityManager.getTransaction().begin();

            String reservationRequestId = ObjectIdentifier.formatId(reservationRequest);
            switch (reservationRequest.getState()) {
                case MODIFIED:
                    throw new ControllerReportSet.ReservationRequestNotDeletableException(reservationRequestId);
                case DELETED:
                    throw new ControllerReportSet.ReservationRequestDeletedException(reservationRequestId);
            }
            ReservationManager reservationManager = new ReservationManager(entityManager);
            if (!isReservationRequestDeletable(reservationRequest, reservationManager)) {
                throw new ControllerReportSet.ReservationRequestNotDeletableException(
                        ObjectIdentifier.formatId(reservationRequest));
            }

            reservationRequestManager.softDelete(reservationRequest, authorizationManager);

            entityManager.getTransaction().commit();
            authorizationManager.commitTransaction(null);
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

    public cz.cesnet.shongo.controller.api.Domain getDomain(String domainId)
    {
        checkNotNull("domain-id", domainId);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ResourceManager resourceManager = new ResourceManager(entityManager);
        try {
            cz.cesnet.shongo.controller.booking.domain.Domain domain = resourceManager.getDomain(
                    ObjectIdentifier.parseLocalId(domainId, ObjectType.DOMAIN));

//            if (!authorization.hasObjectPermission(token, domain, ObjectPermission.READ)) {
//                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("read domain %s", domainId);
//            }

            if (domain == null) {
                return null;
            }
            return domain.toApi();
        } finally {
            entityManager.close();
        }
    }

    /**
     * Get domain by it's name from domainCache or add to cache if missing.
     *
     * @param domainName
     * @return {@link Domain}
     */
    public cz.cesnet.shongo.controller.api.Domain findDomainByName(String domainName)
    {
        checkNotNull("domain-name", domainName);

        DomainCache domainCache = cache.getDomainCache();
        cz.cesnet.shongo.controller.booking.domain.Domain domain = domainCache.getDomainByName(domainName);
        if (domain == null) {
            domain = getDomainByName(domainName);
            domainCache.addObject(domain);
        }
        return domain.toApi();
    }

    private cz.cesnet.shongo.controller.booking.domain.Domain getDomainByName(String domainName)
    {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ResourceManager resourceManager = new ResourceManager(entityManager);
        try {
            cz.cesnet.shongo.controller.booking.domain.Domain domain = resourceManager.getDomainByName(domainName);

            return domain;
        } finally {
            entityManager.close();
        }
    }

    /**
     * Check whether {@code abstractReservationRequest} can be deleted.
     *
     * @param abstractReservationRequest
     * @return true when the given {@code abstractReservationRequest} can be deleted, otherwise false
     */
    private boolean isReservationRequestDeletable(
            cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest abstractReservationRequest,
            ReservationManager reservationManager)
    {
        Allocation allocation = abstractReservationRequest.getAllocation();

        // Check if reservation request is not created by controller
        if (abstractReservationRequest instanceof ReservationRequest) {
            ReservationRequest reservationRequestImpl =
                    (ReservationRequest) abstractReservationRequest;
            if (reservationRequestImpl.getParentAllocation() != null) {
                return false;
            }
        }

        // Check allocated reservations
        if (reservationManager.isAllocationReused(allocation)) {
            return false;
        }

        // Check child reservation requests
        for (ReservationRequest reservationRequestImpl :
                allocation.getChildReservationRequests()) {
            if (isReservationRequestDeletable(reservationRequestImpl, reservationManager)) {
                return false;
            }
        }

        return true;
    }

    public cz.cesnet.shongo.controller.api.ForeignPerson findForeignPerson(String foreignUserId)
    {
        checkNotNull("foreign-user-id", foreignUserId);
        String userId = UserInformation.parseUserId(foreignUserId);
        Long domainId = Long.valueOf(UserInformation.parseDomainId(foreignUserId));
        checkNotNull("user-id", userId);
        checkNotNull("domain-id", domainId);


        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

            CriteriaQuery<ForeignPerson> query = criteriaBuilder.createQuery(ForeignPerson.class);
            Root<ForeignPerson> domainRoot = query.from(ForeignPerson.class);
            javax.persistence.criteria.Predicate paramDomain = criteriaBuilder.equal(domainRoot.get("domain"), domainId);
            javax.persistence.criteria.Predicate paramUser = criteriaBuilder.equal(domainRoot.get("userId"), userId);
            query.select(domainRoot);
            query.where(paramDomain, paramUser);

            TypedQuery<ForeignPerson> typedQuery = entityManager.createQuery(query);

            return typedQuery.getSingleResult().toApi();
        } finally {
            entityManager.close();
        }
    }
}