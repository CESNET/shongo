package cz.cesnet.shongo.controller.booking.room;

import cz.cesnet.shongo.ParticipantRole;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.Room;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.ControllerReportSet;
import cz.cesnet.shongo.controller.LocalDomain;
import cz.cesnet.shongo.controller.ObjectRole;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.booking.executable.Endpoint;
import cz.cesnet.shongo.controller.booking.executable.EndpointExecutableService;
import cz.cesnet.shongo.controller.booking.executable.ExecutableManager;
import cz.cesnet.shongo.controller.booking.executable.ExecutableService;
import cz.cesnet.shongo.controller.booking.participant.AbstractParticipant;
import cz.cesnet.shongo.controller.booking.participant.PersonParticipant;
import cz.cesnet.shongo.controller.booking.person.AbstractPerson;
import cz.cesnet.shongo.controller.booking.person.UserPerson;
import cz.cesnet.shongo.controller.booking.person.ForeignPerson;
import cz.cesnet.shongo.controller.booking.resource.DeviceResource;
import cz.cesnet.shongo.controller.booking.room.settting.*;
import cz.cesnet.shongo.controller.executor.ExecutionReportSet;
import cz.cesnet.shongo.controller.executor.Executor;
import cz.cesnet.shongo.controller.notification.NotificationState;
import cz.cesnet.shongo.controller.notification.RoomAvailableNotification;
import cz.cesnet.shongo.report.Report;
import cz.cesnet.shongo.report.ReportException;
import org.eclipse.jetty.server.UserIdentity;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;

import jakarta.persistence.*;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Represents an room {@link cz.cesnet.shongo.controller.booking.executable.Endpoint} in which multiple other {@link cz.cesnet.shongo.controller.booking.executable.Endpoint}s can be interconnected.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public abstract class RoomEndpoint extends Endpoint
{
    /**
     * Specifies the name of the meeting which will take place in the room.
     */
    private String meetingName;

    /**
     * Specifies the description of the meeting which will take place in the room.
     */
    private String meetingDescription;

    /**
     * Number of minutes which the room is be available before requested time slot.
     */
    private int slotMinutesBefore;

    /**
     * Number of minutes which the room is be available after requested time slot.
     */
    private int slotMinutesAfter;

    /**
     * @see RoomConfiguration
     */
    private RoomConfiguration roomConfiguration;

    /**
     * Description of the room which can be displayed to the user.
     */
    private String roomDescription;

    /**
     * Temporary value of PIN.
     */
    private String tmpPin;

    /**
     * List of {@link cz.cesnet.shongo.controller.booking.participant.AbstractParticipant}s for the {@link RoomEndpoint}.
     */
    @OneToMany(cascade = CascadeType.ALL)
    @Access(AccessType.FIELD)
    private List<AbstractParticipant> participants = new LinkedList<AbstractParticipant>();

    /**
     * Specifies whether configured participants should  be notified about the room.
     */
    private boolean participantNotificationEnabled;

    /**
     * {@link NotificationState} for participant notifications.
     */
    private NotificationState participantNotificationState = new NotificationState();

    /**
     * @return {@link #meetingName}
     */
    @Column
    public String getMeetingName()
    {
        return meetingName;
    }

    /**
     * @param meetingName sets the {@link #meetingName}
     */
    public void setMeetingName(String meetingName)
    {
        this.meetingName = meetingName;
    }

    /**
     * @return {@link #meetingDescription}
     */
    @Column
    public String getMeetingDescription()
    {
        return meetingDescription;
    }

    /**
     * @param meetingDescription sets the {@link #meetingDescription}
     */
    public void setMeetingDescription(String meetingDescription)
    {
        this.meetingDescription = meetingDescription;
    }

    /**
     * @return {@link #slotMinutesBefore}
     */
    @Column(nullable = false, columnDefinition = "integer default 0")
    public int getSlotMinutesBefore()
    {
        return slotMinutesBefore;
    }

    /**
     * @param slotMinutesBefore sets the {@link #slotMinutesBefore}
     */
    public void setSlotMinutesBefore(int slotMinutesBefore)
    {
        this.slotMinutesBefore = slotMinutesBefore;
    }

    /**
     * @return {@link #slotMinutesAfter}
     */
    @Column(nullable = false, columnDefinition = "integer default 0")
    public int getSlotMinutesAfter()
    {
        return slotMinutesAfter;
    }

    /**
     * @param slotMinutesAfter sets the {@link #slotMinutesAfter}
     */
    public void setSlotMinutesAfter(int slotMinutesAfter)
    {
        this.slotMinutesAfter = slotMinutesAfter;
    }

    /**
     * @return {@link #roomConfiguration}
     */
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @Access(AccessType.FIELD)
    public RoomConfiguration getRoomConfiguration()
    {
        return roomConfiguration;
    }

    /**
     * @param roomConfiguration sets the {@link #roomConfiguration}
     */
    public void setRoomConfiguration(RoomConfiguration roomConfiguration)
    {
        this.roomConfiguration = roomConfiguration;
    }

    /**
     * @return {@link #roomDescription}
     */
    @Column
    public String getRoomDescription()
    {
        return roomDescription;
    }

    /**
     * @param roomDescription sets the {@link #roomDescription}
     */
    public void setRoomDescription(String roomDescription)
    {
        this.roomDescription = roomDescription;
    }

    @Transient
    public final String getRoomDescriptionApi()
    {
        if (roomDescription != null) {
            return String.format("[%s:%d] %s", LocalDomain.getLocalDomainShortName(), getId(), roomDescription);
        }
        else {
            return String.format("[%s:%d]", LocalDomain.getLocalDomainShortName(), getId());
        }
    }

    /**
     * @return list of all {@link AbstractParticipant}s which are able to join this {@link RoomEndpoint}
     */
    public List<AbstractParticipant> getParticipants()
    {
        return Collections.unmodifiableList(participants);
    }

    /**
     * @param participants sets the {@link #participants}
     */

    public void setParticipants(List<AbstractParticipant> participants)
    {
        this.participants.clear();
        for (AbstractParticipant participant : participants) {
            try {
                this.participants.add(participant.clone());
            }
            catch (CloneNotSupportedException exception) {
                throw new RuntimeException(exception);
            }
        }
    }

    /**
     * @param participant to be added to the {@link #participants}
     */
    public void addParticipant(AbstractParticipant participant)
    {
        participants.add(participant);
    }

    /**
     * @return {@link #participantNotificationEnabled}
     */
    @Column(nullable = false, columnDefinition = "boolean default false")
    public boolean getParticipantNotificationEnabled()
    {
        return participantNotificationEnabled;
    }

    /**
     * @param participantNotificationEnabled sets the {@link #participantNotificationEnabled}
     */
    public void setParticipantNotificationEnabled(boolean participantNotificationEnabled)
    {
        this.participantNotificationEnabled = participantNotificationEnabled;
    }

    /**
     * @return {@link #participantNotificationState}
     */
    @ManyToOne(cascade = CascadeType.PERSIST)
    public NotificationState getParticipantNotificationState()
    {
        return participantNotificationState;
    }

    /**
     * @param participantNotificationState sets the {@link #participantNotificationState}
     */
    public void setParticipantNotificationState(NotificationState participantNotificationState)
    {
        this.participantNotificationState = participantNotificationState;
    }

    /**
     * @return true whether {@link #getParticipants()} should be notified about {@link RoomEndpoint},
     * false otherwise
     */
    @Transient
    public boolean isParticipantNotificationEnabled()
    {
        // Participation is enabled for the room or the room is permanent room and thus we must create
        // notifications to allow active usages notifications
        return participantNotificationEnabled || roomConfiguration.getLicenseCount() == 0;
    }

    /**
     * @return {@link RoomConfiguration#licenseCount}
     */
    @Transient
    public int getEndpointServiceCount()
    {
        int endpointServiceCount = 0;
        for (ExecutableService service : services) {
            if (service instanceof EndpointExecutableService && service.isActive()) {
                // Do not count the Pexip services. It has higher limit by default.
                if ((roomConfiguration.getRoomSettings() != null && roomConfiguration.getRoomSettings().size() == 1
                        && roomConfiguration.getRoomSettings().get(0) instanceof PexipRoomSetting)) {
                    continue;
                }
                EndpointExecutableService endpointService = (EndpointExecutableService) service;
                if (endpointService.isEndpoint()) {
                    endpointServiceCount += 1;
                }
            }
        }
        return endpointServiceCount;
    }

    @Override
    @Transient
    public int getCount()
    {
        return 0;
    }

    @Override
    @Transient
    public Set<Technology> getTechnologies()
    {
        if (roomConfiguration == null) {
            throw new IllegalStateException("Room configuration hasn't been set yet.");
        }
        return roomConfiguration.getTechnologies();
    }

    /**
     * @return {@link DeviceResource} for this {@link RoomEndpoint}
     */
    @Transient
    public abstract DeviceResource getResource();

    /**
     * @return {@link Technology} specific id of the {@link RoomConfiguration}.
     */
    @Transient
    public abstract String getRoomId();

    /**
     * @return {@link #getSlot()} without {@link #slotMinutesBefore} and {@link #slotMinutesAfter} applied
     */
    @Transient
    public Interval getOriginalSlot()
    {
        return getOriginalSlot(getSlot(), Period.minutes(slotMinutesBefore), Period.minutes(slotMinutesAfter));
    }

    /**
     * @param slot
     * @param slotBefore
     * @param slotAfter
     * @return original slot from given {@code slot} and {@code slotBefore} and {@code slotAfter}
     */
    public static Interval getOriginalSlot(Interval slot, Period slotBefore, Period slotAfter)
    {
        DateTime start = slot.getStart().plus(slotBefore);
        DateTime end = slot.getEnd().minus(slotAfter);
        if (end.isBefore(start)) {
            end = start;
        }
        return new Interval(start, end);
    }

    @Transient
    public String getAdminPin () {
        String tmpAdminPin = null;
            RoomConfiguration roomConfiguration = getRoomConfiguration();
            Set<Technology> technologies = roomConfiguration.getTechnologies();
            if (technologies.contains(Technology.FREEPBX)) {
                tmpAdminPin = getAdminPin(Technology.FREEPBX);
            } else if (technologies.contains(Technology.H323)) {
                tmpAdminPin = getAdminPin(Technology.H323);
            }
            if (tmpAdminPin == null) {
                // Empty means no PIN
                tmpAdminPin = "";
            }

        if (!tmpAdminPin.isEmpty()) {
            return tmpAdminPin;
        }
        else {
            return null;
        }
    }

    @Transient
    public String getAdminPin(Technology technology)
    {
        String pin = null;
        RoomConfiguration roomConfiguration = getRoomConfiguration();
        for (RoomSetting setting : roomConfiguration.getRoomSettings()) {
            if (setting instanceof FreePBXRoomSetting && Technology.FREEPBX.equals(technology)) {
                FreePBXRoomSetting freePBXRoomSetting = (FreePBXRoomSetting) setting;
                if (freePBXRoomSetting.getAdminPin() != null) {
                    pin = freePBXRoomSetting.getAdminPin();
                }
            } else if (setting instanceof PexipRoomSetting && Technology.H323.equals(technology)) {
                PexipRoomSetting pexipRoomSetting = (PexipRoomSetting) setting;
                if (pexipRoomSetting.getHostPin() != null) {
                    pin = pexipRoomSetting.getHostPin();
                }
            }
        }
        return pin;
    }

    @Transient
    public String getPin()
    {
        if (tmpPin == null) {
            RoomConfiguration roomConfiguration = getRoomConfiguration();
            Set<Technology> technologies = roomConfiguration.getTechnologies();
            if (technologies.contains(Technology.H323)) {
                tmpPin = getPin(Technology.H323);
            }
            else if (technologies.contains(Technology.ADOBE_CONNECT)) {
                tmpPin = getPin(Technology.ADOBE_CONNECT);
            }
            else if (technologies.contains(Technology.FREEPBX)) {
                tmpPin = getPin(Technology.FREEPBX);
            }
            if (tmpPin == null) {
                // Empty means no PIN
                tmpPin = "";
            }
        }
        if (!tmpPin.isEmpty()) {
            return tmpPin;
        }
        else {
            return null;
        }
    }

    @Transient
    public String getPin(Technology technology)
    {
        String pin = null;
        RoomConfiguration roomConfiguration = getRoomConfiguration();
        for (RoomSetting setting : roomConfiguration.getRoomSettings()) {
            if (setting instanceof H323RoomSetting
                    && (Technology.H323.equals(technology) || Technology.SIP.equals(technology))) {
                H323RoomSetting h323RoomSetting = (H323RoomSetting) setting;
                if (h323RoomSetting.getPin() != null) {
                    pin = h323RoomSetting.getPin();
                }
            }
            else if (setting instanceof AdobeConnectRoomSetting && Technology.ADOBE_CONNECT.equals(technology)) {
                AdobeConnectRoomSetting adobeConnectRoomSetting = (AdobeConnectRoomSetting) setting;
                if (adobeConnectRoomSetting.getPin() != null) {
                    pin = adobeConnectRoomSetting.getPin();
                }
            } else if (setting instanceof FreePBXRoomSetting && Technology.FREEPBX.equals(technology)) {
                FreePBXRoomSetting freePBXRoomSetting = (FreePBXRoomSetting) setting;
                if (freePBXRoomSetting.getUserPin() != null) {
                    pin = freePBXRoomSetting.getUserPin();
                }
            } else if (setting instanceof PexipRoomSetting && (Technology.H323.equals(technology) || Technology.SIP.equals(technology)
                    || Technology.SKYPE_FOR_BUSINESS.equals(technology) || Technology.RTMP.equals(technology) || Technology.WEBRTC.equals(technology))) {
                PexipRoomSetting pexipRoomSetting = (PexipRoomSetting) setting;
                if (pexipRoomSetting.getGuestPin() != null) {
                    pin = pexipRoomSetting.getGuestPin();
                }
            }
        }
        return pin;
    }

    /**
     * @param executableManager {@link ExecutableManager} which can be used
     * @return {@link cz.cesnet.shongo.api.Room} representing the current room for the {@link RoomEndpoint}
     */
    @Transient
    public Room getRoomApi(ExecutableManager executableManager)
    {
        Room roomApi = new Room();
        fillRoomApi(roomApi, executableManager);

        // If roomApi doesn't contain any participant with ParticipantRole#ADMINISTRATOR, fill the owners of this room
        Authorization authorization = Authorization.getInstance();
        if (!roomApi.hasParticipantWithRole(ParticipantRole.ADMINISTRATOR)) {
            for (UserInformation executableOwner : authorization.getUsersWithRole(this, ObjectRole.OWNER)) {
                roomApi.addParticipantRole(executableOwner.getUserId(), ParticipantRole.ADMINISTRATOR);
            }
        }

        return roomApi;
    }

    /**
     * @param roomApi           to be filled
     * @param executableManager {@link ExecutableManager} which can be used
     */
    public void fillRoomApi(Room roomApi, ExecutableManager executableManager)
    {
        roomApi.setDescription(getRoomDescriptionApi());
        for (AbstractParticipant participant : participants) {
            if (participant instanceof PersonParticipant) {
                PersonParticipant personParticipant = (PersonParticipant) participant;
                AbstractPerson person = personParticipant.getPerson();
                if (person instanceof UserPerson) {
                    UserPerson userPerson = (UserPerson) person;
                    roomApi.addParticipantRole(userPerson.getUserId(), personParticipant.getRole());
                }
                else if (person instanceof ForeignPerson) {
                    ForeignPerson foreignPerson = ((ForeignPerson) person);
                    String userId = UserInformation.formatForeignUserId(foreignPerson.getUserId(), foreignPerson.getDomain().getId());
                    roomApi.addParticipantRole(userId, personParticipant.getRole());
                }
            }
        }
    }

    @Override
    public void toApi(Executable executableApi, EntityManager entityManager, Report.UserType userType)
    {
        super.toApi(executableApi, entityManager, userType);

        AbstractRoomExecutable abstractRoomExecutableApi =
                (AbstractRoomExecutable) executableApi;

        RoomExecutableParticipantConfiguration participantConfiguration = new RoomExecutableParticipantConfiguration();
        for (AbstractParticipant participant : participants) {
            participantConfiguration.addParticipant(participant.toApi());
        }
        abstractRoomExecutableApi.setParticipantConfiguration(participantConfiguration);
        abstractRoomExecutableApi.setDescription(roomDescription);

        // Determine whether room has recording service and recordings
        // (use executable_summary for used_room_endpoints to be taken into account)
        // Sometimes there is no data yet in executable_summary, so we must use the view
        Object[] result = (Object[]) entityManager.createNativeQuery(
                "SELECT room_has_recording_service, room_has_recordings FROM executable_summary_view WHERE id = :id")
                .setParameter("id", getId())
                .getSingleResult();
        abstractRoomExecutableApi.setHasRecordingService(Boolean.TRUE.equals(result[0]));
        abstractRoomExecutableApi.setHasRecordings(Boolean.TRUE.equals(result[1]));

        // We must compute the original time slot
        abstractRoomExecutableApi.setOriginalSlot(getOriginalSlot());
    }

    @Override
    public boolean updateFromExecutableConfigurationApi(ExecutableConfiguration configuration,
            final EntityManager entityManager)
            throws ControllerReportSet.ExecutableInvalidConfigurationException
    {
        if (configuration instanceof RoomExecutableParticipantConfiguration) {
            RoomExecutableParticipantConfiguration participantConfiguration =
                    (RoomExecutableParticipantConfiguration) configuration;

            return Synchronization.synchronizeCollection(participants, participantConfiguration.getParticipants(),
                    new Synchronization.Handler<AbstractParticipant, cz.cesnet.shongo.controller.api.AbstractParticipant>(
                            AbstractParticipant.class)
                    {
                        @Override
                        public AbstractParticipant createFromApi(
                                cz.cesnet.shongo.controller.api.AbstractParticipant objectApi)
                        {
                            return AbstractParticipant.createFromApi(objectApi, entityManager);
                        }

                        @Override
                        public void updateFromApi(AbstractParticipant object,
                                cz.cesnet.shongo.controller.api.AbstractParticipant objectApi)
                        {
                            object.fromApi(objectApi, entityManager);
                        }
                    });
        }
        else {
            return super.updateFromExecutableConfigurationApi(
                    configuration, entityManager);
        }
    }

    /**
     * @param roomApi  to be modified
     * @param executor to be used
     * @throws cz.cesnet.shongo.controller.executor.ExecutionReportSet.RoomNotStartedException, ExecutionReportSet.CommandFailedException
     */
    public abstract void modifyRoom(Room roomApi, Executor executor)
            throws ExecutionReportSet.RoomNotStartedException, ExecutionReportSet.CommandFailedException;

    @Override
    protected State onStart(Executor executor, ExecutableManager executableManager)
    {
        super.onStart(executor, executableManager);
        return State.STARTED;
    }

    @Override
    protected void onAfterStart(Executor executor)
    {
        super.onAfterStart(executor);

        // Notify participants
        if (roomConfiguration.getLicenseCount() > 0 && participants.size() > 0) {
            if (isParticipantNotificationEnabled()) {
                executor.addNotification(new RoomAvailableNotification(this));
            }
        }
    }

    @Override
    protected void onServiceActivation(ExecutableService service, Executor executor,
            ExecutableManager executableManager) throws ReportException
    {
        super.onServiceActivation(service, executor, executableManager);

        if (service instanceof EndpointExecutableService) {
            EndpointExecutableService endpointService = (EndpointExecutableService) service;
            if (endpointService.isEndpoint()) {
                modifyRoom(getRoomApi(executableManager), executor);
            }
        }
    }

    @Override
    protected void onServiceDeactivation(ExecutableService service, Executor executor,
            ExecutableManager executableManager) throws ReportException
    {
        if (service instanceof EndpointExecutableService) {
            EndpointExecutableService endpointService = (EndpointExecutableService) service;
            if (endpointService.isEndpoint()) {
                modifyRoom(getRoomApi(executableManager), executor);
            }
        }

        super.onServiceDeactivation(service, executor, executableManager);
    }

    @Override
    protected void onCreate()
    {
        super.onCreate();

        onUpdate();
    }

    @PrePersist
    @PreUpdate
    protected void onUpdate()
    {
        if (roomConfiguration == null) {
            throw new IllegalStateException("Room configuration should not be null.");
        }
        if (roomConfiguration.getTechnologies().size() == 0) {
            throw new IllegalStateException("Room configuration should have some technologies.");
        }
    }

    @Override
    public void loadLazyProperties()
    {
        this.participants.size();
        this.roomConfiguration.getTechnologies().size();
        this.roomConfiguration.getRoomSettings().size();
        super.loadLazyProperties();
    }
}
