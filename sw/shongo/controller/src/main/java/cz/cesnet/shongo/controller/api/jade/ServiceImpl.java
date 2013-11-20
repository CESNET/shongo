package cz.cesnet.shongo.controller.api.jade;

import cz.cesnet.shongo.PersonInformation;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.Room;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.connector.api.jade.recording.CreateRecordingFolder;
import cz.cesnet.shongo.controller.ControllerAgent;
import cz.cesnet.shongo.controller.Role;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.booking.executable.ExecutableManager;
import cz.cesnet.shongo.controller.booking.person.AbstractPerson;
import cz.cesnet.shongo.controller.booking.executable.ExecutableService;
import cz.cesnet.shongo.controller.booking.recording.RecordableEndpoint;
import cz.cesnet.shongo.controller.booking.recording.RecordingCapability;
import cz.cesnet.shongo.controller.booking.room.RoomEndpoint;
import cz.cesnet.shongo.controller.executor.ExecutionReportSet;
import cz.cesnet.shongo.controller.notification.SimpleMessageNotification;
import cz.cesnet.shongo.controller.notification.manager.NotificationManager;
import cz.cesnet.shongo.controller.booking.resource.DeviceResource;
import cz.cesnet.shongo.controller.booking.resource.ResourceManager;
import cz.cesnet.shongo.jade.SendLocalCommand;

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
     * @see ControllerAgent
     */
    private ControllerAgent controllerAgent;

    /**
     * Constructor.
     */
    public ServiceImpl(EntityManagerFactory entityManagerFactory, NotificationManager notificationManager,
            ControllerAgent controllerAgent)
    {
        this.entityManagerFactory = entityManagerFactory;
        this.notificationManager = notificationManager;
        this.controllerAgent = controllerAgent;
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
        Long deviceResourceId = getDeviceResourceByAgentName(agentName).getId();
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            ExecutableManager executableManager = new ExecutableManager(entityManager);
            RoomEndpoint roomEndpoint = executableManager.getRoomEndpoint(deviceResourceId, roomId);
            if (roomEndpoint == null) {
                throw new CommandException(
                        String.format("No room '%s' was found for resource with agent '%s'.", roomId, agentName));
            }
            return roomEndpoint.getRoomApi(executableManager);
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
                DeviceResource deviceResource = getDeviceResourceByAgentName(agentName);
                Long deviceResourceId = deviceResource.getId();
                EntityManager entityManager = entityManagerFactory.createEntityManager();
                try {
                    ExecutableManager executableManager = new ExecutableManager(entityManager);
                    RoomEndpoint roomEndpoint =
                            executableManager.getRoomEndpoint(deviceResourceId, targetId);
                    if (roomEndpoint == null) {
                        throw new CommandException(String.format(
                                "No room '%s' was found for resource with agent '%s'.", targetId, agentName));
                    }
                    Authorization authorization = Authorization.getInstance();
                    for (UserInformation user : authorization.getUsersWithRole(roomEndpoint, Role.OWNER)) {
                        recipients.add(user);
                    }
                    for (AbstractPerson resourceAdministrator : deviceResource.getAdministrators()) {
                        recipients.add(resourceAdministrator.getInformation());
                    }
                }
                finally {
                    entityManager.close();
                }
                break;
            default:
                throw new TodoImplementException(targetType);
        }

        SimpleMessageNotification simpleMessageNotification = new SimpleMessageNotification(title, message);
        simpleMessageNotification.addRecipients(recipients);
        notificationManager.executeNotification(simpleMessageNotification);
    }

    @Override
    public String getRecordingFolderId(String agentName, String roomId) throws CommandException
    {
        Long deviceResourceId = getDeviceResourceByAgentName(agentName).getId();
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            ExecutableManager executableManager = new ExecutableManager(entityManager);
            RoomEndpoint roomEndpoint =
                    executableManager.getRoomEndpoint(deviceResourceId, roomId);
            DeviceResource deviceResource = roomEndpoint.getDeviceResource();
            RecordingCapability recordingCapability = deviceResource.getCapabilityRequired(RecordingCapability.class);
            String recordingFolderId;
            synchronized (RecordableEndpoint.SYNCHRONIZATION.get(roomEndpoint.getId())) {
                recordingFolderId = roomEndpoint.getRecordingFolderId(recordingCapability);
                if (recordingFolderId == null) {
                    SendLocalCommand sendLocalCommand = controllerAgent.sendCommand(agentName,
                            new CreateRecordingFolder(roomEndpoint.getRecordingFolderDescription()));
                    if (!SendLocalCommand.State.SUCCESSFUL.equals(sendLocalCommand.getState())) {
                        throw new CommandException(sendLocalCommand.getJadeReport().toString());
                    }
                    recordingFolderId = (String) sendLocalCommand.getResult();
                }
            }
            return recordingFolderId;
        }
        finally {
            entityManager.close();
        }
    }

    /**
     * Gets device resource identifier based on agent name.
     *
     * @param agentName of the managed device resource
     * @return device resource identifier
     */
    private DeviceResource getDeviceResourceByAgentName(String agentName) throws CommandException
    {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            ResourceManager resourceManager = new ResourceManager(entityManager);
            DeviceResource deviceResource = resourceManager.getManagedDeviceByAgent(agentName);
            if (deviceResource != null) {
                return deviceResource;
            }
            throw new CommandException(String.format("No device resource is configured with agent '%s'.", agentName));
        }
        finally {
            entityManager.close();
        }
    }
}
