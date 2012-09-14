package cz.cesnet.shongo.controller.resource;

import cz.cesnet.shongo.AbstractManager;
import cz.cesnet.shongo.controller.allocationaold.AllocatedAlias;
import cz.cesnet.shongo.controller.allocationaold.AllocatedResource;
import cz.cesnet.shongo.fault.EntityNotFoundException;
import cz.cesnet.shongo.fault.FaultException;
import org.joda.time.Interval;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
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
     * Create a new resource allocation in the database.
     *
     * @param allocatedResource
     */
    public void createAllocation(AllocatedResource allocatedResource)
    {
        super.create(allocatedResource);
    }

    /**
     * @return list of all resources in the database
     */
    public List<Resource> list()
    {
        List<Resource> resourceList = entityManager
                .createQuery("SELECT resource FROM Resource resource", Resource.class)
                .getResultList();
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
                + " FROM " + capabilityType.getSimpleName() + " capability"
                + " WHERE capability.resource IS NOT NULL", capabilityType)
                .getResultList();
        return capabilities;
    }

    /**
     * @param aliasProviderCapabilityId
     * @param interval
     * @return list of all alias allocations for alias provider with given {@code aliasProviderCapabilityId}
     *         which intersects given {@code interval}
     */
    public List<AllocatedAlias> listAllocatedAliasesInInterval(Long aliasProviderCapabilityId, Interval interval)
    {
        List<AllocatedAlias> allocatedResourceList = entityManager.createQuery("SELECT allocation"
                + " FROM AllocatedAlias allocation"
                + " WHERE allocation.aliasProviderCapability.id = :id"
                + " AND NOT(allocation.slotStart >= :end OR allocation.slotEnd <= :start)", AllocatedAlias.class)
                .setParameter("id", aliasProviderCapabilityId)
                .setParameter("start", interval.getStart())
                .setParameter("end", interval.getEnd())
                .getResultList();
        return allocatedResourceList;
    }

    /**
     * @param resourceId
     * @param interval
     * @return list of all resource allocations for resource with given {@code resourceId} which intersects
     *         given {@code interval}
     */
    public List<AllocatedResource> listAllocatedResourcesInInterval(Long resourceId, Interval interval)
    {
        List<AllocatedResource> allocatedResourceList = entityManager.createQuery("SELECT allocation"
                + " FROM AllocatedResource allocation"
                + " WHERE allocation.resource.id = :id"
                + " AND NOT(allocation.slotStart >= :end OR allocation.slotEnd <= :start)", AllocatedResource.class)
                .setParameter("id", resourceId)
                .setParameter("start", interval.getStart())
                .setParameter("end", interval.getEnd())
                .getResultList();
        return allocatedResourceList;
    }
}
