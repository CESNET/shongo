package cz.cesnet.shongo.controller.api.jade;

import cz.cesnet.shongo.PersonInformation;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.Room;
import cz.cesnet.shongo.api.RoomSummary;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.controller.ControllerReportSet;
import cz.cesnet.shongo.controller.ObjectRole;
import cz.cesnet.shongo.controller.ObjectType;
import cz.cesnet.shongo.controller.RoomNotExistsException;
import cz.cesnet.shongo.controller.api.ForeignPerson;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.executable.Executable;
import cz.cesnet.shongo.controller.booking.executable.ExecutableManager;
import cz.cesnet.shongo.controller.booking.recording.RecordableEndpoint;
import cz.cesnet.shongo.controller.booking.recording.RecordingCapability;
import cz.cesnet.shongo.controller.booking.resource.DeviceResource;
import cz.cesnet.shongo.controller.booking.resource.ResourceManager;
import cz.cesnet.shongo.controller.booking.room.RoomEndpoint;
import cz.cesnet.shongo.controller.domains.InterDomainAgent;
import cz.cesnet.shongo.controller.executor.ExecutionReportSet;
import cz.cesnet.shongo.controller.executor.Executor;
import cz.cesnet.shongo.controller.notification.AbstractNotification;
import cz.cesnet.shongo.controller.notification.ConfigurableNotification;
import cz.cesnet.shongo.controller.notification.NotificationManager;
import cz.cesnet.shongo.controller.notification.NotificationMessage;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.*;

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
     * @see Executor
     */
    private Executor executor;

    /**
     * @see Authorization
     */
    private Authorization authorization;

    /**
     * Constructor.
     */
    public ServiceImpl(EntityManagerFactory entityManagerFactory, NotificationManager notificationManager,
            Executor executor, Authorization authorization)
    {
        this.entityManagerFactory = entityManagerFactory;
        this.notificationManager = notificationManager;
        this.executor = executor;
        this.authorization = authorization;
    }

    @Override
    public UserInformation getUserInformation(String userId)
    {
        try {
            return Authorization.getInstance().getUserInformation(userId);
        }
        catch (ControllerReportSet.UserNotExistsException exception) {
            return null;
        }
    }

    @Override
    public UserInformation getUserInformationByPrincipalName(String userPrincipalName)
    {
        try {
            return authorization.getUserInformationByPrincipalName(userPrincipalName);
        }
        catch (ControllerReportSet.UserNotExistsException exception) {
            return null;
        }
    }

    @Override
    public Room getRoom(String agentName, String roomId) throws CommandException
    {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            Long deviceResourceId = getDeviceResourceByAgentName(agentName, entityManager).getId();
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
            final Map<String, String> titles, final Map<String, String> messages) throws CommandException
    {
        Authorization authorization = Authorization.getInstance();
        List<PersonInformation> recipients = new LinkedList<PersonInformation>();
        ObjectRole role = null;
        switch (targetType) {
            case USER: {
                try {
                    recipients.add(authorization.getUserInformation(targetId));
                }
                catch (Exception exception) {
                    throw new CommandException(String.format("Cannot notify user with id '%s'.", targetId), exception);
                }
                break;
            }
            case ROOM_OWNERS: {
                EntityManager entityManager = entityManagerFactory.createEntityManager();
                try {
                    DeviceResource resource = getDeviceResourceByAgentName(agentName, entityManager);
                    Long deviceResourceId = resource.getId();
                    ExecutableManager executableManager = new ExecutableManager(entityManager);
                    RoomEndpoint roomEndpoint = executableManager.getRoomEndpoint(deviceResourceId, targetId);
                    if (roomEndpoint == null) {
                        throw new CommandException(String.format(
                                "No room '%s' was found for resource with agent '%s'.", targetId, agentName));
                    }
                    for (UserInformation user : authorization.getUsersWithRole(roomEndpoint, ObjectRole.OWNER)) {
                        recipients.add(user);
                    }
                    for (PersonInformation administrator : resource.getAdministrators(entityManager, authorization)) {
                        recipients.add(administrator);
                    }
                }
                finally {
                    entityManager.close();
                }
                break;
            }
            case RESOURCE_ADMINS: {
                if (targetId != null) {
                    throw new IllegalArgumentException("The targetId argument must be null.");
                }
                EntityManager entityManager = entityManagerFactory.createEntityManager();
                try {
                    DeviceResource resource = getDeviceResourceByAgentName(agentName, entityManager);
                    for (PersonInformation administrator : resource.getAdministrators(entityManager, authorization)) {
                        recipients.add(administrator);
                    }
                }
                finally {
                    entityManager.close();
                }
                break;
            }
            case REC_FOLDER_OWNERS:
                role = ObjectRole.OWNER;
            case REC_FOLDER_READERS:
                if (role == null) {
                    role = ObjectRole.READER;
                }
                EntityManager entityManager = entityManagerFactory.createEntityManager();
                try {
                    DeviceResource resource = getDeviceResourceByAgentName(agentName, entityManager);
                    ExecutableManager executableManager = new ExecutableManager(entityManager);
                    Executable executable = executableManager.getExecutableByRecordingFolder(resource, targetId);
                    if (executable == null) {
                        throw new CommandException(String.format(
                                "No room was found for recording folder '%s' with agent '%s'.", targetId, agentName));
                    }
                    for (UserInformation user : authorization.getUsersWithRole(executable, role)) {
                        recipients.add(user);
                    }

                    if (executable instanceof RoomEndpoint) {
                        Room room = ((RoomEndpoint) executable).getRoomApi(executableManager);
                        String roomName = room.getName();
                        for (Map.Entry<String, String> entry : titles.entrySet()) {
                            String lang = entry.getKey();
                            String title = entry.getValue();
                            if ("en".equals(lang)) {
                                title += " (room: " + roomName + ")";
                            } else if ("cs".equals(lang)) {
                                title += " (místnost: " + roomName +")";
                            }
                            titles.put(entry.getKey(), title);
                        }
                    }
                }
                finally {
                    entityManager.close();
                }
                break;
            default:
                throw new TodoImplementException(targetType);
        }

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            AbstractNotification notification = new ConfigurableNotification(recipients)
            {
                @Override
                protected Collection<Locale> getAvailableLocals()
                {
                    Collection<Locale> locales = new LinkedList<Locale>();
                    for (Locale locale : NotificationMessage.AVAILABLE_LOCALES) {
                        if (titles.containsKey(locale.getLanguage())) {
                            locales.add(locale);
                        }
                    }
                    return locales;
                }

                @Override
                protected NotificationMessage renderMessage(Configuration configuration,
                        NotificationManager manager)
                {
                    Locale locale = configuration.getLocale();
                    String language = locale.getLanguage();
                    String title = titles.get(language);
                    if (title == null) {
                        if (titles.containsKey(null)) {
                            title = titles.get(null);
                        }
                        else if (titles.containsKey("en")) {
                            title = titles.get("en");
                        }
                        else {
                            throw new RuntimeException("No title in appropriate language was found.");
                        }
                    }
                    String message = messages.get(language);
                    if (message == null) {
                        if (messages.containsKey(null)) {
                            message = messages.get(null);
                        }
                        else if (messages.containsKey("en")) {
                            message = messages.get("en");
                        }
                        else {
                            throw new RuntimeException("No message in appropriate language was found.");
                        }
                    }
                    return new NotificationMessage(language, title, message);
                }
            };
            notificationManager.addNotification(notification, entityManager);
        }
        finally {
            entityManager.close();
        }
    }

    @Override
    public String getRecordingFolderId(String agentName, String roomId) throws CommandException
    {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            Long deviceResourceId = getDeviceResourceByAgentName(agentName, entityManager).getId();
            ExecutableManager executableManager = new ExecutableManager(entityManager);
            RecordableEndpoint recordableEndpoint =
                    (RecordableEndpoint) executableManager.getRoomEndpoint(deviceResourceId, roomId);
            if (recordableEndpoint == null) {
                throw new RoomNotExistsException(
                        roomId, ObjectIdentifier.formatId(ObjectType.RESOURCE, deviceResourceId));
            }
            DeviceResource resource = recordableEndpoint.getResource();
            RecordingCapability recordingCapability = resource.getCapabilityRequired(RecordingCapability.class);
            entityManager.getTransaction().begin();
            String recordingFolderId = executor.getRecordingFolderId(recordableEndpoint, recordingCapability);
            entityManager.getTransaction().commit();
            return recordingFolderId;
        }
        catch (ExecutionReportSet.CommandFailedException exception) {
            throw new CommandException(exception.getMessage());
        }
        finally {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            entityManager.close();
        }
    }

    /**
     * Gets device resource identifier based on agent name.
     *
     * @param agentName of the managed device resource
     * @return device resource identifier
     */
    private DeviceResource getDeviceResourceByAgentName(String agentName, EntityManager entityManager)
            throws CommandException
    {
        ResourceManager resourceManager = new ResourceManager(entityManager);
        DeviceResource deviceResource = resourceManager.getManagedDeviceByAgent(agentName);
        if (deviceResource != null) {
            deviceResource.loadLazyProperties();
            return deviceResource;
        }
        throw new CommandException(String.format("No device resource is configured with agent '%s'.", agentName));
    }
}
