package cz.cesnet.shongo.controller.api.jade;

import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.Room;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.Authorization;
import cz.cesnet.shongo.controller.executor.ExecutableManager;
import cz.cesnet.shongo.controller.executor.RoomEndpoint;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.ResourceManager;
import org.joda.time.DateTime;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

/**
 * Implementation of {@link Service}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ServiceImpl implements Service
{
    /**
     * @see EntityManagerFactory
     */
    private EntityManagerFactory entityManagerFactory;

    /**
     * Constructor.
     */
    public ServiceImpl(EntityManagerFactory entityManagerFactory)
    {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public UserInformation getUserInformation(String userId) throws CommandException
    {
        return Authorization.getInstance().getUserInformation(userId);
    }

    @Override
    public Room getRoom(String agentName, String roomId) throws CommandException
    {
        Long deviceResourceId = getDeviceResourceIdByAgentName(agentName);
        if (deviceResourceId == null) {
            throw new CommandException(String.format("No device resource is configured with agent '%s'.", agentName));
        }
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            ExecutableManager executableManager = new ExecutableManager(entityManager);
            RoomEndpoint roomEndpoint = executableManager.getRoomEndpoint(deviceResourceId, roomId, DateTime.now());
            return roomEndpoint.getRoomApi();
        }
        finally {
            entityManager.close();
        }
    }

    @Override
    public void notifyRoomOwners(String roomId, String message) throws CommandException
    {
        throw new RuntimeException("TODO: Implement ServiceImpl.notifyRoomOwners");
    }

    /**
     * Gets device resource identifier based on agent name.
     *
     * @param agentName of the managed device resource
     * @return device resource identifier
     */
    private Long getDeviceResourceIdByAgentName(String agentName)
    {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            ResourceManager resourceManager = new ResourceManager(entityManager);
            DeviceResource deviceResource = resourceManager.getManagedDeviceByAgent(agentName);
            if (deviceResource != null) {
                return deviceResource.getId();
            }
            else {
                return null;
            }
        }
        finally {
            entityManager.close();
        }
    }
}
