package cz.cesnet.shongo.controller.booking.resource;

import cz.cesnet.shongo.AbstractManager;
import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.controller.ControllerReportSetHelper;
import cz.cesnet.shongo.controller.booking.alias.AliasProviderCapability;
import cz.cesnet.shongo.controller.booking.value.ValueProviderCapability;
import cz.cesnet.shongo.controller.booking.alias.AliasReservation;
import cz.cesnet.shongo.controller.booking.room.RoomReservation;
import cz.cesnet.shongo.controller.booking.value.ValueReservation;
import cz.cesnet.shongo.controller.booking.value.provider.FilteredValueProvider;
import cz.cesnet.shongo.controller.booking.value.provider.ValueProvider;
import org.joda.time.Interval;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Root;
import javax.sql.rowset.Predicate;
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
     * Create a new resource in the database.
     *
     * @param resource
     */
    public void create(Resource resource)
    {
        resource.validate();
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
        super.update(resource);
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
     * Delete existing resource in the database
     *
     * @param resource
     */
    public void delete(Resource resource)
    {
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
     * @return list of all {@link ValueReservation}s for value provider with given {@code valueProviderId}
     *         which intersects given {@code interval}
     */
    public List<ValueReservation> listValueReservationsInInterval(Long valueProviderId, Interval interval)
    {
        List<ValueReservation> valueReservations = entityManager.createQuery("SELECT reservation"
                + " FROM ValueReservation reservation"
                + " WHERE reservation.valueProvider.id = :id"
                + " AND NOT(reservation.slotStart >= :end OR reservation.slotEnd <= :start)"
                + " ORDER BY reservation.slotStart", ValueReservation.class)
                .setParameter("id", valueProviderId)
                .setParameter("start", interval.getStart())
                .setParameter("end", interval.getEnd())
                .getResultList();
        return valueReservations;
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
        Root<Tag> c = query.from(Tag.class);
        query.select(c);

        TypedQuery<Tag> typedQuery = entityManager.createQuery(query);

        return typedQuery.getResultList();
    }

    public Tag getTag(Long tagId)
    {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

        CriteriaQuery<Tag> query = criteriaBuilder.createQuery(Tag.class);
        Root<Tag> from = query.from(Tag.class);
        javax.persistence.criteria.Predicate param1 = criteriaBuilder.equal(from.get("id").as(Long.class), tagId);
        query.select(from).where(param1);

        TypedQuery<Tag> typedQuery = entityManager.createQuery(query);

        return typedQuery.getSingleResult();
    }

    public Tag findTag(String name)
    {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

        CriteriaQuery<Tag> query = criteriaBuilder.createQuery(Tag.class);
        Root<Tag> from = query.from(Tag.class);
        javax.persistence.criteria.Predicate param1 = criteriaBuilder.equal(from.get("name").as(String.class), name);
        query.select(from).where(param1);

        TypedQuery<Tag> typedQuery = entityManager.createQuery(query);

        return typedQuery.getSingleResult();
    }

    public ResourceTag getResourceTag(Long resourceId, Long tagId)
    {
        try {
            CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

            CriteriaQuery<ResourceTag> query = criteriaBuilder.createQuery(ResourceTag.class);
            Root<ResourceTag> from = query.from(ResourceTag.class);
            javax.persistence.criteria.Predicate param1 = criteriaBuilder.equal(from.get("resource"), resourceId);
            javax.persistence.criteria.Predicate param2 = criteriaBuilder.equal(from.get("tag"), tagId);
            query.select(from);
            query.where(param1);
            query.where(param2);

            TypedQuery<ResourceTag> typedQuery = entityManager.createQuery(query);

            return typedQuery.getSingleResult();
        } catch (NoResultException exception) {
            return ControllerReportSetHelper.throwObjectNotExistFault(ResourceTag.class, tagId);
            //TODO:MR add resourceId
        }
    }

    public List<ResourceTag> getResourceTags(Long resourceId)
    {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

        CriteriaQuery<ResourceTag> query = criteriaBuilder.createQuery(ResourceTag.class);
        Root<ResourceTag> from = query.from(ResourceTag.class);
        javax.persistence.criteria.Predicate param1 = criteriaBuilder.equal(from.get("resource"), resourceId);
        query.select(from).where(param1);

        TypedQuery<ResourceTag> typedQuery = entityManager.createQuery(query);

        return typedQuery.getResultList();
    }

    public List<ResourceTag> getResourceTagsByTag(Long tagId)
    {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

        CriteriaQuery<ResourceTag> query = criteriaBuilder.createQuery(ResourceTag.class);
        Root<ResourceTag> from = query.from(ResourceTag.class);
        javax.persistence.criteria.Predicate param1 = criteriaBuilder.equal(from.get("tag"), tagId);
        query.select(from).where(param1);

        TypedQuery<ResourceTag> typedQuery = entityManager.createQuery(query);

        return typedQuery.getResultList();
    }
}
