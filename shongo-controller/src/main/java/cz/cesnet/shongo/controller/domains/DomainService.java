package cz.cesnet.shongo.controller.domains;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.api.Domain;
import cz.cesnet.shongo.controller.api.domains.response.DomainCapability;
import cz.cesnet.shongo.controller.api.request.*;
import cz.cesnet.shongo.controller.api.rpc.AbstractServiceImpl;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.resource.*;
import cz.cesnet.shongo.controller.util.NativeQuery;
import cz.cesnet.shongo.controller.util.QueryFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.*;

/**
 * Domain service implementation only for controller.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class DomainService extends AbstractServiceImpl implements Component.EntityManagerFactoryAware
{
    private static Logger logger = LoggerFactory.getLogger(DomainService.class);

//    /**
//     * @see cz.cesnet.shongo.controller.cache.Cache
//     */
//    private Cache cache;

    /**
     * @see javax.persistence.EntityManagerFactory
     */
    private EntityManagerFactory entityManagerFactory;

//    /**
//     * @see cz.cesnet.shongo.controller.authorization.Authorization
//     */
//    private Authorization authorization;

//    /**
//     * Constructor.
//     *
//     * @param cache sets the {@link #cache}
//     */
//    public DomainService(Cache cache)
//    {
//        this.cache = cache;
//    }


    public DomainService(EntityManagerFactory entityManagerFactory)
    {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory)
    {
        this.entityManagerFactory = entityManagerFactory;
    }

//    @Override
//    public void setAuthorization(Authorization authorization)
//    {
//        this.authorization = authorization;
//    }

    @Override
    public void init(ControllerConfiguration configuration)
    {
//        checkDependency(cache, Cache.class);
        checkDependency(entityManagerFactory, EntityManagerFactory.class);
//        checkDependency(authorization, Authorization.class);
        //TODO: overit zda tu configuration potrebuji
//        super.init(configuration);
    }

//    @Override
//    public String getServiceName()
//    {
//        return "Resource";
//    }

    /**
     * List all domains included local domain. Every domain will have null status except local.
     *
     * @return
     */
    public List<Domain> listDomains()
    {
        return listDomains(false);
    }

    /**
     * List all domains included local domain depending on {@code onlyForeignDomains}. Every domain will have null status except local.
     *
     * @param onlyForeignDomains
     * @return
     */
    public List<Domain> listDomains(boolean onlyForeignDomains)
    {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ResourceManager resourceManager = new ResourceManager(entityManager);
        try {
            List<Domain> domainList = new ArrayList<Domain>();
            if (!onlyForeignDomains) {
                domainList.add(LocalDomain.getLocalDomain().toApi());
            }
            for (cz.cesnet.shongo.controller.booking.domain.Domain domain : resourceManager.listAllDomains()) {
                domainList.add(domain.toApi());
            }
            return Collections.unmodifiableList(domainList);
        } finally {
            entityManager.close();
        }
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
//                    Long resourceId = ObjectIdentifier.parseId(resourceApiId, ObjectType.RESOURCE);
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
//                        + " WHERE tag_id = :tagId)", "tagId", ObjectIdentifier.parseId(request.getTagId(), ObjectType.TAG));
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
//                        + " WHERE domain_id = :domainId)", "domainId", ObjectIdentifier.parseId(request.getDomainId(), ObjectType.DOMAIN));
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

    public List<DomainCapability> listLocalResourcesByDomain(DomainCapabilityListRequest request)
    {
        checkNotNull("request", request);
        checkNotNull("domainId", request.getDomainId());
        checkNotNull("type", request.getType());

        EntityManager entityManager = entityManagerFactory.createEntityManager();

        try {
            // Filter requested by foreign domain
            QueryFilter queryFilter = new QueryFilter("resource_summary", true);
            queryFilter.addFilter("domain_id = :domainId", "domainId", ObjectIdentifier.parseId(request.getDomainId(), ObjectType.DOMAIN));

            //TODO: vytahnout zasedacky podle tagu
            // Filter requested tag-id
//            if (request.getTagId() != null) {
//                queryFilter.addFilter("resource_summary.id IN ("
//                        + " SELECT resource_id FROM resource_tag "
//                        + " WHERE tag_id = :tagId)", "tagId", ObjectIdentifier.parseId(request.getTagId(), ObjectType.TAG));
//            }
//             Filter requested tag-name
//            if (request.getTagName() != null) {
//                queryFilter.addFilter("resource_summary.id IN ("
//                        + " SELECT resource_tag.resource_id FROM resource_tag "
//                        + " LEFT JOIN tag ON tag.id = resource_tag.tag_id"
//                        + " WHERE tag.name = :tagName)", "tagName", request.getTagName());
//            }

            // Filter by type
            String type = "";
            switch (request.getType()) {
                case VIRTUAL_ROOM:
                    type = "ROOM_PROVIDER";
                    break;
                case RESOURCE:
                    type = "RESOURCE";
                    break;
            }
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

            //TODO vylistovat podle technologie
            if (request.getTechnology() != null) {
                queryFilter.addFilter("resource_summary.id IN ("
                        + " SELECT device_resource.id FROM device_resource "
                        + " LEFT JOIN device_resource_technologies ON device_resource_technologies.device_resource_id = device_resource.id"
                        + " WHERE device_resource_technologies.technologies = '" + request.getTechnology() + "')");
            }
            //
            // Technologies
//            Set<Technology> technologies = request.getTechnology();
//            if (technologies.size() > 0) {
//                queryFilter.addFilter("resource_summary.id IN ("
//                        + " SELECT device_resource.id FROM device_resource "
//                        + " LEFT JOIN device_resource_technologies ON device_resource_technologies.device_resource_id = device_resource.id"
//                        + " WHERE device_resource_technologies.technologies IN(:technologies))");
//                queryFilter.addFilterParameter("technologies", technologies);
//            }

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
//                if (record[6] != null) {
//                    String recordTechnologies = record[6].toString();
//                    if (!recordTechnologies.isEmpty()) {
//                        for (String technology : recordTechnologies.split(",")) {
//                            domainResource.addTechnology(Technology.valueOf(technology.trim()));
//                        }
//                    }
//                }
                domainCapability.setCalendarPublic((Boolean) record[3]);
                if ((Boolean) record[3]) {
                    domainCapability.setCalendarUriKey(record[4].toString());
                }
                domainCapability.setLicenseCount((Integer) record[5]);
                domainCapability.setPrice((Integer) record[6]);
                response.add(domainCapability);
            }
            return response;
        } finally {
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
                    ObjectIdentifier.parseId(domainId, ObjectType.DOMAIN));

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

    public cz.cesnet.shongo.controller.api.Domain findDomainByCode(String domainCode)
    {
        checkNotNull("domain-code", domainCode);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ResourceManager resourceManager = new ResourceManager(entityManager);
        try {
            cz.cesnet.shongo.controller.booking.domain.Domain domain = resourceManager.getDomainByCode(domainCode);

            return (domain == null ? null : domain.toApi());
        } finally {
            entityManager.close();
        }
    }
}