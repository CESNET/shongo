package cz.cesnet.shongo.controller.resource;

import cz.cesnet.shongo.AbstractManager;
import cz.cesnet.shongo.controller.reservation.AliasReservation;
import cz.cesnet.shongo.controller.reservation.ResourceReservation;
import cz.cesnet.shongo.controller.reservation.ValueReservation;
import cz.cesnet.shongo.controller.resource.value.PatternValueProvider;
import cz.cesnet.shongo.controller.resource.value.ValueProvider;
import cz.cesnet.shongo.controller.util.DatabaseFilter;
import cz.cesnet.shongo.fault.EntityNotFoundException;
import cz.cesnet.shongo.fault.FaultException;
import org.joda.time.Interval;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
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
     * Create a new resource in the database.
     *
     * @param resource
     * @throws FaultException when the creating fail
     */
    public void create(Resource resource) throws FaultException
    {
        resource.validate();
        super.create(resource);
    }

    /**
     * Update existing resource in the database.
     *
     * @param resource
     * @throws FaultException when the updating fail
     */
    public void update(Resource resource) throws FaultException
    {
        resource.validate();
        super.update(resource);
    }

    /**
     * Delete existing resource in the database
     *
     * @param resource
     */
    public void delete(Resource resource)
    {
        super.delete(resource);
    }

    /**
     * @return list of all resources in the database
     */
    public List<Resource> list(String userId)
    {
        DatabaseFilter filter = new DatabaseFilter("resource");
        filter.addUserId(userId);
        TypedQuery<Resource> query = entityManager.createQuery("SELECT resource FROM Resource resource"
                + " WHERE " + filter.toQueryWhere(),
                Resource.class);
        filter.fillQueryParameters(query);
        List<Resource> resourceList = query.getResultList();
        return resourceList;
    }

    /**
     * @param resourceId
     * @return {@link Resource} with given {@code resourceId}
     * @throws EntityNotFoundException when resource doesn't exist
     */
    public Resource get(Long resourceId) throws EntityNotFoundException
    {
        try {
            Resource resource = entityManager.createQuery(
                    "SELECT resource FROM Resource resource WHERE resource.id = :id",
                    Resource.class).setParameter("id", resourceId)
                    .getSingleResult();
            return resource;
        }
        catch (NoResultException exception) {
            throw new EntityNotFoundException(Resource.class, resourceId);
        }
    }

    /**
     * @param deviceResourceId
     * @return {@link DeviceResource} with given {@code deviceResourceId}
     * @throws EntityNotFoundException when device resource doesn't exist
     */
    public DeviceResource getDevice(Long deviceResourceId) throws EntityNotFoundException
    {
        try {
            DeviceResource deviceResource = entityManager.createQuery(
                    "SELECT device FROM DeviceResource device WHERE device.id = :id",
                    DeviceResource.class).setParameter("id", deviceResourceId)
                    .getSingleResult();
            return deviceResource;
        }
        catch (NoResultException exception) {
            throw new EntityNotFoundException(DeviceResource.class, deviceResourceId);
        }
    }

    /**
     * @param capabilityType
     * @return list of all device resources which have capability with given {@code capabilityType}
     */
    public List<DeviceResource> listDevicesWithCapability(Class<? extends Capability> capabilityType)
    {
        List<DeviceResource> deviceResources = entityManager.createQuery("SELECT device FROM DeviceResource device"
                + " WHERE device.id IN("
                + "  SELECT device.id FROM DeviceResource device"
                + "  INNER JOIN device.capabilities capability"
                + "  WHERE TYPE(capability) = :capability"
                + "  GROUP BY device.id"
                + " )", DeviceResource.class)
                .setParameter("capability", capabilityType)
                .getResultList();
        return deviceResources;
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
     * @return list of all capabilities of given {@code capabilityType}
     */
    public <T extends Capability> List<T> listCapabilities(Class<T> capabilityType)
    {
        List<T> capabilities = entityManager.createQuery("SELECT capability"
                + " FROM " + capabilityType.getSimpleName() + " capability", capabilityType)
                .getResultList();
        return capabilities;
    }

    /**
     * @return list of all {@link cz.cesnet.shongo.controller.resource.value.PatternValueProvider}s
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
}
