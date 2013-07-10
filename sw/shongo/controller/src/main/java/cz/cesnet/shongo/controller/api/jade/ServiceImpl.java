package cz.cesnet.shongo.controller.api.jade;

import cz.cesnet.shongo.PersonInformation;
import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.Room;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.Role;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.executor.ExecutableManager;
import cz.cesnet.shongo.controller.executor.RoomEndpoint;
import cz.cesnet.shongo.controller.notification.MessageNotification;
import cz.cesnet.shongo.controller.notification.Notification;
import cz.cesnet.shongo.controller.notification.NotificationManager;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.ResourceManager;
import cz.cesnet.shongo.TodoImplementException;
import org.joda.time.DateTime;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.LinkedList;
import java.util.List;

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
     * @see NotificationManager
     */
    private NotificationManager notificationManager;

    /**
     * Constructor.
     */
    public ServiceImpl(EntityManagerFactory entityManagerFactory, NotificationManager notificationManager)
    {
        this.entityManagerFactory = entityManagerFactory;
        this.notificationManager = notificationManager;
    }

    @Override
    public UserInformation getUserInformation(String userId) throws CommandException
    {
        return Authorization.getInstance().getUserInformation(userId);
    }

    @Override
    public UserInformation getUserInformationByOriginalId(String originalUserId) throws CommandException
    {
        for (UserInformation userInformation : Authorization.getInstance().listUserInformation()) {
            if (originalUserId.equals(userInformation.getOriginalId())) {
                return userInformation;
            }
        }
        return null;
    }

    @Override
    public Room getRoom(String agentName, String roomId) throws CommandException
    {
        Long deviceResourceId = getDeviceResourceIdByAgentName(agentName);
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            ExecutableManager executableManager = new ExecutableManager(entityManager);
            RoomEndpoint roomEndpoint = executableManager.getRoomEndpoint(deviceResourceId, roomId, DateTime.now());
            if (roomEndpoint == null) {
                throw new CommandException(
                        String.format("No room '%s' was found for resource with agent '%s'.", roomId, agentName));
            }
            return roomEndpoint.getRoomApi();
        }
        finally {
            entityManager.close();
        }
    }

    @Override
    public void notifyTarget(String agentName, NotifyTargetType targetType, String targetId,
            String title, String message) throws CommandException
    {
        List<PersonInformation> recipients = new LinkedList<PersonInformation>();
        switch (targetType) {
            case USER:
                try {
                    recipients.add(Authorization.getInstance().getUserInformation(targetId));
                }
                catch (Exception exception) {
                    throw new CommandException(String.format("Cannot notify user with id '%s'.", targetId), exception);
                }
                break;
            case ROOM_OWNERS:
                Long deviceResourceId = getDeviceResourceIdByAgentName(agentName);
                EntityManager entityManager = entityManagerFactory.createEntityManager();
                try {
                    ExecutableManager executableManager = new ExecutableManager(entityManager);
                    RoomEndpoint roomEndpoint =
                            executableManager.getRoomEndpoint(deviceResourceId, targetId, DateTime.now());
                    if (roomEndpoint == null) {
                        throw new CommandException(String.format(
                                "No room '%s' was found for resource with agent '%s'.", targetId, agentName));
                    }
                    Authorization authorization = Authorization.getInstance();
                    for (UserInformation user : authorization.getUsersWithRole(roomEndpoint, Role.OWNER)) {
                        recipients.add(user);
                    }
                }
                finally {
                    entityManager.close();
                }
                break;
            default:
                throw new TodoImplementException(targetType.toString());
        }

        MessageNotification messageNotification = new MessageNotification(title, message);
        messageNotification.addRecipients(Notification.RecipientGroup.USER, recipients);
        notificationManager.executeNotification(messageNotification);
    }

    /**
     * Gets device resource identifier based on agent name.
     *
     * @param agentName of the managed device resource
     * @return device resource identifier
     */
    private Long getDeviceResourceIdByAgentName(String agentName) throws CommandException
    {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            ResourceManager resourceManager = new ResourceManager(entityManager);
            DeviceResource deviceResource = resourceManager.getManagedDeviceByAgent(agentName);
            if (deviceResource != null) {
                return deviceResource.getId();
            }
            throw new CommandException(String.format("No device resource is configured with agent '%s'.", agentName));
        }
        finally {
            entityManager.close();
        }
    }
}
