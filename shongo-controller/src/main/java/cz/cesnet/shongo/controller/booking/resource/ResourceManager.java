package cz.cesnet.shongo.controller.booking.resource;

import cz.cesnet.shongo.AbstractManager;
import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.api.Converter;
import cz.cesnet.shongo.controller.ControllerReportSetHelper;
import cz.cesnet.shongo.controller.ObjectType;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.alias.AliasProviderCapability;
import cz.cesnet.shongo.controller.booking.domain.Domain;
import cz.cesnet.shongo.controller.booking.domain.DomainResource;
import cz.cesnet.shongo.controller.booking.value.ValueProviderCapability;
import cz.cesnet.shongo.controller.booking.alias.AliasReservation;
import cz.cesnet.shongo.controller.booking.room.RoomReservation;
import cz.cesnet.shongo.controller.booking.value.ValueReservation;
import cz.cesnet.shongo.controller.booking.value.provider.FilteredValueProvider;
import cz.cesnet.shongo.controller.booking.value.provider.ValueProvider;
import cz.cesnet.shongo.controller.scheduler.SchedulerReport;
import org.apache.commons.lang.RandomStringUtils;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import java.util.List;

/**
 * Manager for {@link Resource}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see AbstractManager
 */
public class ResourceManager extends AbstractManager
{
    /**
     * Constructor.
     *
     * @param entityManager
     */
    public ResourceManager(EntityManager entityManager)
    {
        super(entityManager);

    }

    /**
     * Create a new tag object in the database.
     *
     * @param tag
     */
    public void createTag(Tag tag)
    {
        super.create(tag);
    }

    /**
     * Create a new resourceTag object in the database.
     *
     * @param resourceTag
     */
    public void createResourceTag(ResourceTag resourceTag)
    {
        super.create(resourceTag);
    }

    /**
     * Create a new foreignResources object in the database.
     *
     * @param foreignResources
     */
    public void createForeignResources(ForeignResources foreignResources)
    {
        super.create(foreignResources);
    }

    /**
     * Create a new resource in the database.
     *
     * @param resource
     */
    public void create(Resource resource)
    {
        resource.validate();
        // Set calendar URI key if calendar is public
        if (resource.isCalendarPublic()) {
            String calendarUriKey = generateUniqueKey(8);
            resource.setCalendarUriKey(calendarUriKey);
        }
        else {
            resource.setCalendarUriKey(null);
        }
        super.create(resource);
    }

    /**
     * Update existing resource in the database.
     *
     * @param resource
     */
    public void update(Resource resource)
    {
        resource.validate();
        if (resource.isCalendarPublic()) {
            if (resource.getCalendarUriKey() == null || resource.getCalendarUriKey().length() != 8) {
                String calendarUriKey = generateUniqueKey(8);
                resource.setCalendarUriKey(calendarUriKey);
            }
        }
        else {
            resource.setCalendarUriKey(null);
        }
        super.update(resource);
    }

    /**
     * Returns new unique random calendarUriKey for {@link cz.cesnet.shongo.controller.booking.resource.Resource}
     * @param length of the key
     * @return
     */
    private String generateUniqueKey(int length) {
        // Get all existing resource's calendarUriKeys
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<String> query = criteriaBuilder.createQuery(String.class);
        Root<Resource> resourceRoot = query.from(Resource.class);
        query.select(resourceRoot.get("calendarUriKey").as(String.class));
        query.where(criteriaBuilder.equal(resourceRoot.get("calendarPublic").as(boolean.class), true));

        List<String> calendarUriKeys = entityManager.createQuery(query).getResultList();

        String calendarUriKey = null;
        while (calendarUriKey == null) {
            String generatedUriKey = RandomStringUtils.random(length, true, true);
            if (!calendarUriKeys.contains(generatedUriKey)) {
                calendarUriKey = generatedUriKey;
            }
        }
        return calendarUriKey;
    }

    /**
     * Delete a tag object from the database.
     *
     * @param tag
     */
    public void deleteTag(Tag tag)
    {
        super.delete(tag);
    }

    /**
     * Delete a resourceTag object from the database.
     *
     * @param resourceTag
     */
    public void deleteResourceTag(ResourceTag resourceTag)
    {
        super.delete(resourceTag);
    }

    /**
     * Delete a domain object from the database.
     *
     * @param domain
     */
    public void deleteDomain(Domain domain)
    {
        super.delete(domain);
    }

    /**
     * Delete a domainResource object from the database.
     *
     * @param domainResource
     */
    public void deleteDomainResource(DomainResource domainResource)
    {
        super.delete(domainResource);
    }

    /**
     * Delete existing resource in the database
     *
     * @param resource
     */
    public void delete(Resource resource)
    {
        // Delete scheduler reports
        List reportIds = entityManager.createNativeQuery(
                "SELECT id FROM scheduler_report WHERE resource_id = :resourceId")
                .setParameter("resourceId", resource.getId())
                .getResultList();
        if (reportIds.size() > 0) {
            List<SchedulerReport> reports = entityManager.createQuery("SELECT report FROM SchedulerReport report"
                    + " WHERE report.id IN(:reportIds)", SchedulerReport.class)
                    .setParameter("reportIds", Converter.convertToSet(reportIds, Long.class))
                    .getResultList();
            for (SchedulerReport report : reports) {
                entityManager.remove(report);
            }
        }

        for (Capability capability : resource.getCapabilities()) {
            if (capability instanceof AliasProviderCapability) {
                AliasProviderCapability aliasProviderCapability = (AliasProviderCapability) capability;
                ValueProvider valueProvider = aliasProviderCapability.getValueProvider();
                deleteValueProvider(valueProvider, capability);
            }
            else if (capability instanceof ValueProviderCapability) {
                ValueProviderCapability valueProviderCapability = (ValueProviderCapability) capability;
                ValueProvider valueProvider = valueProviderCapability.getValueProvider();
                deleteValueProvider(valueProvider, capability);
            }
        }
        super.delete(resource);
    }

    /**
     * Delete given {@code valueProvider} if it should be deleted while deleting the {@code capability}.
     *
     * @param valueProvider to be deleted
     * @param capability    which is being deleted
     */
    public void deleteValueProvider(ValueProvider valueProvider, Capability capability)
    {
        if (valueProvider instanceof FilteredValueProvider) {
            FilteredValueProvider filteredValueProvider = (FilteredValueProvider) valueProvider;
            deleteValueProvider(filteredValueProvider.getValueProvider(), capability);
        }
        if (valueProvider.getCapability().equals(capability)) {
            super.delete(valueProvider);
        }
    }

    /**
     * @return list of all resources in the database
     */
    public List<Resource> list()
    {
        TypedQuery<Resource> query = entityManager.createQuery("SELECT resource FROM Resource resource",
                Resource.class);
        return query.getResultList();
    }

    /**
     * @param resourceId
     * @return {@link Resource} with given {@code resourceId}
     * @throws cz.cesnet.shongo.CommonReportSet.ObjectNotExistsException when resource doesn't exist
     */
    public Resource get(Long resourceId) throws CommonReportSet.ObjectNotExistsException
    {
        try {
            Resource resource = entityManager.createQuery(
                    "SELECT resource FROM Resource resource WHERE resource.id = :id",
                    Resource.class).setParameter("id", resourceId)
                    .getSingleResult();
            return resource;
        }
        catch (NoResultException exception) {
            return ControllerReportSetHelper.throwObjectNotExistFault(Resource.class, resourceId);
        }
    }

    /**
     * @param deviceResourceId
     * @return {@link DeviceResource} with given {@code deviceResourceId}
     * @throws cz.cesnet.shongo.CommonReportSet.ObjectNotExistsException when device resource doesn't exist
     */
    public DeviceResource getDevice(Long deviceResourceId) throws CommonReportSet.ObjectNotExistsException
    {
        try {
            DeviceResource deviceResource = entityManager.createQuery(
                    "SELECT device FROM DeviceResource device WHERE device.id = :id",
                    DeviceResource.class).setParameter("id", deviceResourceId)
                    .getSingleResult();
            return deviceResource;
        }
        catch (NoResultException exception) {
            return ControllerReportSetHelper.throwObjectNotExistFault(DeviceResource.class, deviceResourceId);
        }
    }

    /**
     * @param capabilityType
     * @return list of all {@link Resource}s which have capability with given {@code capabilityType}
     */
    public List<Resource> listResourcesWithCapability(Class<? extends Capability> capabilityType)
    {
        List<Resource> resources = entityManager.createQuery("SELECT resource FROM Resource resource"
                + " WHERE resource.id IN("
                + "  SELECT resource.id FROM Resource resource"
                + "  INNER JOIN resource.capabilities capability"
                + "  WHERE TYPE(capability) = :capability"
                + "  GROUP BY resource.id"
                + " )", Resource.class)
                .setParameter("capability", capabilityType)
                .getResultList();
        return resources;
    }

    /**
     * @return list of all managed device resource in the database
     */
    public List<DeviceResource> listManagedDevices()
    {
        List<DeviceResource> resourceList = entityManager
                .createQuery("SELECT device FROM DeviceResource device WHERE device.mode.class = ManagedMode",
                        DeviceResource.class)
                .getResultList();
        return resourceList;
    }

    /**
     * @param agentName
     * @return managed device resource which has assigned given {@code agentName}
     *         in the {@link ManagedMode#connectorAgentName} or null if it doesn't exist
     */
    public DeviceResource getManagedDeviceByAgent(String agentName)
    {
        try {
            DeviceResource deviceResource = entityManager.createQuery(
                    "SELECT device FROM DeviceResource device " +
                            "WHERE device.mode.class = ManagedMode AND device.mode.connectorAgentName = :name",
                    DeviceResource.class).setParameter("name", agentName)
                    .getSingleResult();
            return deviceResource;
        }
        catch (NoResultException exception) {
            return null;
        }
    }

    /**
     * @param capabilityType
     * @return list of all {@link Capability}s of given {@code capabilityType}
     */
    public <T extends Capability> List<T> listCapabilities(Class<T> capabilityType)
    {
        List<T> capabilities = entityManager.createQuery("SELECT capability"
                + " FROM " + capabilityType.getSimpleName() + " capability", capabilityType)
                .getResultList();
        return capabilities;
    }

    /**
     * @return list of all {@link cz.cesnet.shongo.controller.booking.value.provider.PatternValueProvider}s
     */
    public List<ValueProvider> listValueProviders()
    {
        List<ValueProvider> valueProviders = entityManager.createQuery("SELECT valueProvider"
                + " FROM ValueProvider valueProvider", ValueProvider.class)
                .getResultList();
        return valueProviders;
    }

    /**
     * @param resourceId
     * @param interval
     * @return list of all {@link ResourceReservation}s for {@link Resource} with given {@code resourceId} which
     *         intersects given {@code interval}
     */
    public List<ResourceReservation> listResourceReservationsInInterval(Long resourceId, Interval interval)
    {
        List<ResourceReservation> resourceReservations = entityManager.createQuery("SELECT reservation"
                + " FROM ResourceReservation reservation"
                + " WHERE reservation.resource.id = :id"
                + " AND NOT(reservation.slotStart >= :end OR reservation.slotEnd <= :start)"
                + " ORDER BY reservation.slotStart", ResourceReservation.class)
                .setParameter("id", resourceId)
                .setParameter("start", interval.getStart())
                .setParameter("end", interval.getEnd())
                .getResultList();
        return resourceReservations;
    }

    /**
     * @param valueProviderId
     * @param interval
     * @return list of all {@link ValueReservation}s (Tuple of [id, value, slotStar, slotEnd]) for value provider with given {@code valueProviderId}
     *         which intersects given {@code interval}
     */
    public List<Tuple> listValueReservationsInInterval(Long valueProviderId, Interval interval)
    {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

        CriteriaQuery<Tuple> query = criteriaBuilder.createQuery(Tuple.class);
        Root<ValueReservation> reservationRoot = query.from(ValueReservation.class);

        Predicate providerIdParam = criteriaBuilder.equal(reservationRoot.get("valueProvider").get("id"), valueProviderId);
        Predicate startParam = criteriaBuilder.greaterThanOrEqualTo(reservationRoot.<DateTime>get("slotStart"), interval.getEnd());
        Predicate endParam = criteriaBuilder.lessThanOrEqualTo(reservationRoot.<DateTime>get("slotEnd"), interval.getStart());
        Predicate intervalParam = criteriaBuilder.or(startParam, endParam).not();
        Order orderBy = criteriaBuilder.asc(reservationRoot.get("slotStart"));

        CompoundSelection<Tuple> selection = criteriaBuilder.tuple(reservationRoot.get("id"), reservationRoot.get("value"), reservationRoot.get("slotStart"), reservationRoot.get("slotEnd"));

        query.select(selection).where(criteriaBuilder.and(providerIdParam, intervalParam)).orderBy(orderBy);

        TypedQuery<Tuple> typedQuery = entityManager.createQuery(query);

        return typedQuery.getResultList();
    }

    /**
     * @param roomProviderCapabilityId
     * @param interval
     * @return list of all {@link RoomReservation}s for room provider with given {@code roomProviderCapabilityId}
     *         which intersects given {@code interval}
     */
    public List<RoomReservation> listRoomReservationsInInterval(Long roomProviderCapabilityId, Interval interval)
    {
        List<RoomReservation> roomReservations = entityManager.createQuery("SELECT reservation"
                + " FROM RoomReservation reservation"
                + " WHERE reservation.roomProviderCapability.id = :id"
                + " AND NOT(reservation.slotStart >= :end OR reservation.slotEnd <= :start)"
                + " ORDER BY reservation.slotStart", RoomReservation.class)
                .setParameter("id", roomProviderCapabilityId)
                .setParameter("start", interval.getStart())
                .setParameter("end", interval.getEnd())
                .getResultList();
        return roomReservations;
    }

    /**
     * @param aliasProviderCapabilityId
     * @param interval
     * @return list of all {@link AliasReservation}s for alias provider with given {@code aliasProviderCapabilityId}
     *         which intersects given {@code interval}
     */
    public List<AliasReservation> listAliasReservationsInInterval(Long aliasProviderCapabilityId, Interval interval)
    {
        List<AliasReservation> aliasReservations = entityManager.createQuery("SELECT reservation"
                + " FROM AliasReservation reservation"
                + " WHERE reservation.aliasProviderCapability.id = :id"
                + " AND NOT(reservation.slotStart >= :end OR reservation.slotEnd <= :start)"
                + " ORDER BY reservation.slotStart", AliasReservation.class)
                .setParameter("id", aliasProviderCapabilityId)
                .setParameter("start", interval.getStart())
                .setParameter("end", interval.getEnd())
                .getResultList();
        return aliasReservations;
    }

    public List<Tag> listAllTags()
    {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

        CriteriaQuery<Tag> query = criteriaBuilder.createQuery(Tag.class);
        Root<Tag> tagRoot = query.from(Tag.class);
        query.select(tagRoot);

        TypedQuery<Tag> typedQuery = entityManager.createQuery(query);

        return typedQuery.getResultList();
    }

    public Tag getTag(Long tagId)
    {
        return entityManager.find(Tag.class, tagId);
    }

    public ForeignResources getForeignResources(Long foreignResourceId)
    {
        return entityManager.find(ForeignResources.class, foreignResourceId);
    }

    public ForeignResources findForeignResourcesByType(Domain domain, String type)
    {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

        CriteriaQuery<ForeignResources> query = criteriaBuilder.createQuery(ForeignResources.class);
        Root<ForeignResources> tagRoot = query.from(ForeignResources.class);
        Predicate param1 = criteriaBuilder.equal(tagRoot.get("domain").as(String.class), domain.getId());
        Predicate param2 = criteriaBuilder.equal(tagRoot.get("type").as(String.class), type);
        query.select(tagRoot).where(param1, param2);

        TypedQuery<ForeignResources> typedQuery = entityManager.createQuery(query);

        return typedQuery.getSingleResult();
    }

    public ForeignResources findForeignResourcesByResourceId(ObjectIdentifier objectIdentifier)
    {
        if (!ObjectType.RESOURCE.equals(objectIdentifier.getObjectType())) {
            throw new IllegalArgumentException("ObjectIdentifier must be of type RESOURCE.");
        }
        String domainName = objectIdentifier.getDomainName();
        Long resourceId = objectIdentifier.getPersistenceId();
        return findForeignResourcesByResourceId(domainName, resourceId);
    }

    public ForeignResources findForeignResourcesByResourceId(String domainName, Long resourceId)
    {
        try {
            CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

            CriteriaQuery<ForeignResources> query = criteriaBuilder.createQuery(ForeignResources.class);
            Root<ForeignResources> tagRoot = query.from(ForeignResources.class);
            Predicate param1 = criteriaBuilder.equal(tagRoot.get("domain"), getDomainByName(domainName));
            Predicate param2 = criteriaBuilder.equal(tagRoot.get("foreignResourceId"), resourceId);
            query.select(tagRoot).where(param1, param2);

            TypedQuery<ForeignResources> typedQuery = entityManager.createQuery(query);

            return typedQuery.getSingleResult();
        } catch (NoResultException exception) {
            return ControllerReportSetHelper.throwObjectNotExistFault(domainName, ForeignResources.class, resourceId);
        }
    }

    public ForeignResources findOrCreateForeignResources(String domainName, Long resourceId)
    {
        ForeignResources foreignResources;
        try {
            foreignResources = findForeignResourcesByResourceId(domainName, resourceId);
        }
        catch (CommonReportSet.ObjectNotExistsException ex) {
            cz.cesnet.shongo.controller.booking.domain.Domain domain = getDomainByName(domainName);
            foreignResources = new ForeignResources();
            foreignResources.setDomain(domain);
            foreignResources.setForeignResourceId(resourceId);
        }
        return foreignResources;
    }

    public List<ForeignResources> listForeignResources(Domain domain)
    {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

        CriteriaQuery<ForeignResources> query = criteriaBuilder.createQuery(ForeignResources.class);
        Root<ForeignResources> tagRoot = query.from(ForeignResources.class);
        Predicate param1 = criteriaBuilder.equal(tagRoot.get("domain"), domain);
        query.select(tagRoot).where(param1);

        TypedQuery<ForeignResources> typedQuery = entityManager.createQuery(query);

        return typedQuery.getResultList();
    }

    public PersistentObject findResourcesPersistentObject(String resourceId)
    {
        if (ObjectIdentifier.parseType(resourceId) == null) {
            resourceId = ObjectIdentifier.formatId(ObjectType.RESOURCE, resourceId);
        }
        ObjectIdentifier objectIdentifier = ObjectIdentifier.parseForeignId(resourceId);

        switch (objectIdentifier.getObjectType()) {
            case RESOURCE:
                if (objectIdentifier.isLocal()) {
                    return get(objectIdentifier.getPersistenceId());
                }
                else {
                    cz.cesnet.shongo.controller.booking.domain.Domain domain;
                    domain = getDomainByName(objectIdentifier.getDomainName());
                    PersistentObject persistentObject = domain;

                    // Tries to find {@link ForeignResources} if exists
                    try {
                        Long foreignResourceId = objectIdentifier.getPersistenceId();
                        ForeignResources foreignResources = findForeignResourcesByResourceId(domain.getName(), foreignResourceId);
                        persistentObject = foreignResources;
                    }
                    catch (CommonReportSet.ObjectNotExistsException ex) {
                        // If no {@link ForeignResources} exists, use {@link Domain} (e.g. for ACL)
                    }
                    return persistentObject;
                }
            case FOREIGN_RESOURCES:
                return getForeignResources(objectIdentifier.getPersistenceId());
            default:
                throw new IllegalArgumentException("Unsupported type of object '" + objectIdentifier.getObjectType() + "'.");
        }
    }

    public void deleteForeignResources(ForeignResources foreignResources)
    {
        super.delete(foreignResources);
    }

    public Tag findTag(String name)
    {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

        CriteriaQuery<Tag> query = criteriaBuilder.createQuery(Tag.class);
        Root<Tag> tagRoot = query.from(Tag.class);
        Predicate param1 = criteriaBuilder.equal(tagRoot.get("name").as(String.class), name);
        query.select(tagRoot).where(param1);

        TypedQuery<Tag> typedQuery = entityManager.createQuery(query);

        return typedQuery.getSingleResult();
    }

    /**
     * Returns {@link ResourceTag} for given local {@link Resource} and {@link Tag} or null
     * @param resourceId
     * @param tagId
     * @return
     */
    public ResourceTag getResourceTag(Long resourceId, Long tagId)
    {
        try {
            CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

            CriteriaQuery<ResourceTag> query = criteriaBuilder.createQuery(ResourceTag.class);
            Root<ResourceTag> resourceTagRoot = query.from(ResourceTag.class);
            Predicate param1 = criteriaBuilder.equal(resourceTagRoot.get("resource").get("id"), resourceId);
            Predicate param2 = criteriaBuilder.equal(resourceTagRoot.get("tag").get("id"), tagId);
            query.select(resourceTagRoot);
            query.where(param1,param2);

            TypedQuery<ResourceTag> typedQuery = entityManager.createQuery(query);

            return typedQuery.getSingleResult();
        } catch (NoResultException exception) {
            return ControllerReportSetHelper.throwObjectNotExistFault(ResourceTag.class, tagId);
            //TODO:MR add resourceId
        }
    }

    /**
     * Returns {@link ResourceTag} for given foreign {@link Resource} and {@link Tag} or null
     * @param foreignResourceId
     * @param tagId
     * @return
     */
    public ResourceTag getForeignResourceTag(Long foreignResourceId, Long tagId)
    {
        try {
            CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

            CriteriaQuery<ResourceTag> query = criteriaBuilder.createQuery(ResourceTag.class);
            Root<ResourceTag> resourceTagRoot = query.from(ResourceTag.class);
            Predicate param1 = criteriaBuilder.equal(resourceTagRoot.get("foreignResources"), foreignResourceId);
            Predicate param2 = criteriaBuilder.equal(resourceTagRoot.get("tag"), tagId);
            query.select(resourceTagRoot);
            query.where(param1,param2);

            TypedQuery<ResourceTag> typedQuery = entityManager.createQuery(query);

            return typedQuery.getSingleResult();
        } catch (NoResultException exception) {
            return ControllerReportSetHelper.throwObjectNotExistFault(ResourceTag.class, tagId);
            //TODO:MR add foreignResourceId
        }
    }

    /**
     * List {@link ResourceTag} for given {@link Resource}
     * @param resourceId
     * @return
     */
    public List<ResourceTag> getResourceTags(Long resourceId)
    {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

        CriteriaQuery<ResourceTag> query = criteriaBuilder.createQuery(ResourceTag.class);
        Root<ResourceTag> resourceTagRoot = query.from(ResourceTag.class);
        Predicate param1 = criteriaBuilder.equal(resourceTagRoot.get("resource"), resourceId);
        query.select(resourceTagRoot).where(param1);

        TypedQuery<ResourceTag> typedQuery = entityManager.createQuery(query);

        return typedQuery.getResultList();
    }

    /**
     * Returns list of {@link Tag} for given {@link ForeignResources}
     * @param foreignResources
     * @return
     */
    public List<ResourceTag> getForeignResourceTags(ForeignResources foreignResources)
    {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

        CriteriaQuery<ResourceTag> query = criteriaBuilder.createQuery(ResourceTag.class);
        Root<ResourceTag> resourceTagRoot = query.from(ResourceTag.class);
        Predicate param1 = criteriaBuilder.equal(resourceTagRoot.get("foreignResources"), foreignResources);
        query.select(resourceTagRoot).where(param1);

        TypedQuery<ResourceTag> typedQuery = entityManager.createQuery(query);

        return typedQuery.getResultList();
    }

    /**
     * List {@link ResourceTag} by given {@link Tag} id
     * @param tagId
     * @return
     */
    public List<ResourceTag> getResourceTagsByTag(Long tagId)
    {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

        CriteriaQuery<ResourceTag> query = criteriaBuilder.createQuery(ResourceTag.class);
        Root<ResourceTag> resourceTagRoot = query.from(ResourceTag.class);
        Predicate param1 = criteriaBuilder.equal(resourceTagRoot.get("tag").get("id"), tagId);
        query.select(resourceTagRoot).where(param1);

        TypedQuery<ResourceTag> typedQuery = entityManager.createQuery(query);

        return typedQuery.getResultList();
    }

    public void createDomain(Domain domain)
    {
        domain.validate();
        super.create(domain);
    }

    public void updateDomain(Domain domain)
    {
        domain.validate();
        super.update(domain);
    }

    public Domain getDomain(Long domainId)
    {
        return entityManager.find(Domain.class, domainId);
    }

    public Domain getDomainByName(String domainName)
    {
        try {
            CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

            CriteriaQuery<Domain> query = criteriaBuilder.createQuery(Domain.class);
            Root<Domain> domainRoot = query.from(Domain.class);
            Predicate param1 = criteriaBuilder.equal(domainRoot.get("name"), domainName);
            query.select(domainRoot);
            query.where(param1);

            TypedQuery<Domain> typedQuery = entityManager.createQuery(query);

            return typedQuery.getSingleResult();
        }
        catch (NoResultException ex) {
            return ControllerReportSetHelper.throwObjectNotExistFault(Domain.class, 0L);
        }
    }

    public void createDomainResource(DomainResource domainResource)
    {
        super.create(domainResource);
    }

    public DomainResource getDomainResource(Long domainId, Long resourceId)
    {
        try {
            CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

            CriteriaQuery<DomainResource> query = criteriaBuilder.createQuery(DomainResource.class);
            Root<DomainResource> domainResourceRoot = query.from(DomainResource.class);
            Predicate param1 = criteriaBuilder.equal(domainResourceRoot.get("domain").get("id"), domainId);
            Predicate param2 = criteriaBuilder.equal(domainResourceRoot.get("resource").get("id"), resourceId);
            query.select(domainResourceRoot);
            query.where(param1,param2);

            TypedQuery<DomainResource> typedQuery = entityManager.createQuery(query);

            return typedQuery.getSingleResult();
        } catch (NoResultException exception) {
            return ControllerReportSetHelper.throwObjectNotExistFault(DomainResource.class, domainId);
            //TODO:add resourceId to exception
        }
    }

    public List<Long> listResourceIdsByDomain(Long domainId)
    {
        try {
            CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

            CriteriaQuery<Long> query = criteriaBuilder.createQuery(Long.class);
            Root<DomainResource> domainResourceRoot = query.from(DomainResource.class);
            query.multiselect(domainResourceRoot.get("resource").get("id"));
            Predicate param1 = criteriaBuilder.equal(domainResourceRoot.get("domain"), domainId);
            query.where(param1);

            TypedQuery<Long> typedQuery = entityManager.createQuery(query);

            return typedQuery.getResultList();
        } catch (NoResultException exception) {
            return ControllerReportSetHelper.throwObjectNotExistFault(DomainResource.class, domainId);
        }
    }

    /**
     * List all domains
     * @return
     */
    public List<Domain> listAllDomains()
    {
        return listAllDomains(null);
    }

    /**
     * List all domains. All domains if {@code allocatable} is null, filtered by this param otherwise.
     * @param allocatable
     * @return
     */
    public List<Domain> listAllDomains(Boolean allocatable)
    {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

        CriteriaQuery<Domain> query = criteriaBuilder.createQuery(Domain.class);
        Root<Domain> domainRoot = query.from(Domain.class);
        query.select(domainRoot);
        if (allocatable != null) {
            Predicate param1 = criteriaBuilder.equal(domainRoot.get("allocatable"), allocatable);
            query.where(param1);
        }

        TypedQuery<Domain> typedQuery = entityManager.createQuery(query);

        return typedQuery.getResultList();
    }
}
