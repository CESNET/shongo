package cz.cesnet.shongo.controller.executor;

import cz.cesnet.shongo.ParticipantRole;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.Room;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.ControllerReportSet;
import cz.cesnet.shongo.controller.Executor;
import cz.cesnet.shongo.controller.Role;
import cz.cesnet.shongo.controller.api.AbstractRoomExecutable;
import cz.cesnet.shongo.controller.api.ExecutableConfiguration;
import cz.cesnet.shongo.controller.api.RoomExecutableParticipantConfiguration;
import cz.cesnet.shongo.controller.api.Synchronization;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.common.*;
import cz.cesnet.shongo.controller.resource.Alias;
import cz.cesnet.shongo.report.Report;
import org.joda.time.DateTime;

import javax.persistence.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents an room {@link Endpoint} in which multiple other {@link Endpoint}s can be interconnected.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public abstract class RoomEndpoint extends Endpoint
{
    /**
     * @see RoomConfiguration
     */
    private RoomConfiguration roomConfiguration;

    /**
     * Description of the room which can be displayed to the user.
     */
    private String roomDescription;

    /**
     * List of {@link cz.cesnet.shongo.controller.common.AbstractParticipant}s for the {@link RoomEndpoint}.
     */
    private List<AbstractParticipant> participants = new LinkedList<AbstractParticipant>();

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
            return String.format("[exe:%d] %s", getId(), roomDescription);
        }
        else {
            return String.format("[exe:%d]", getId());
        }
    }

    /**
     * @return {@link #participants}
     */
    @OneToMany(cascade = CascadeType.ALL)
    @Access(AccessType.FIELD)
    public List<AbstractParticipant> getParticipants()
    {
        return participants;
    }

    /**
     * @param participants sets the {@link #participants}
     */
    public void setParticipants(List<AbstractParticipant> participants)
    {
        this.participants.clear();
        for (AbstractParticipant participant : participants) {
            this.participants.add(participant.clone());
        }
    }

    /**
     * @param participant to be added to the {@link #participants}
     */
    public void addParticipant(AbstractParticipant participant)
    {
        participants.add(participant);
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
     * @return {@link Technology} specific id of the {@link RoomConfiguration}.
     */
    @Transient
    public abstract String getRoomId();

    /**
     * @return {@link cz.cesnet.shongo.api.Room} representing the current room for the {@link RoomEndpoint}
     */
    @Transient
    public final Room getRoomApi()
    {
        Room roomApi = new Room();
        fillRoomApi(roomApi);

        // If roomApi doesn't contain any participant with ParticipantRole#ADMIN, fill the owners of this room
        Authorization authorization = Authorization.getInstance();
        if (!roomApi.hasParticipantWithRole(ParticipantRole.ADMIN)) {
            for (UserInformation executableOwner : authorization.getUsersWithRole(this, Role.OWNER)) {
                roomApi.addParticipant(executableOwner, ParticipantRole.ADMIN);
            }
        }

        return roomApi;
    }

    /**
     * @param roomApi to be filled
     */
    public void fillRoomApi(Room roomApi)
    {
        roomApi.setDescription(getRoomDescriptionApi());
        for (AbstractParticipant participant : getParticipants()) {
            if (participant instanceof PersonParticipant) {
                PersonParticipant personParticipant = (PersonParticipant) participant;
                AbstractPerson person = personParticipant.getPerson();
                if (person instanceof UserPerson) {
                    UserPerson userPerson = (UserPerson) person;
                    roomApi.addParticipant(userPerson.getInformation(), personParticipant.getRole());
                }
            }
        }
    }

    @Override
    public void toApi(cz.cesnet.shongo.controller.api.Executable executableApi, Report.UserType userType)
    {
        super.toApi(executableApi, userType);

        AbstractRoomExecutable abstractRoomExecutableApi =
                (AbstractRoomExecutable) executableApi;

        RoomExecutableParticipantConfiguration participantConfiguration = new RoomExecutableParticipantConfiguration();
        for (AbstractParticipant participant : participants) {
            participantConfiguration.addParticipant(participant.toApi());
        }
        abstractRoomExecutableApi.setParticipantConfiguration(participantConfiguration);
    }

    @Override
    public boolean updateFromExecutableConfigurationApi(ExecutableConfiguration executableConfiguration,
            final EntityManager entityManager)
            throws ControllerReportSet.ExecutableInvalidConfigurationException
    {
        if (executableConfiguration instanceof RoomExecutableParticipantConfiguration) {
            RoomExecutableParticipantConfiguration participantConfiguration =
                    (RoomExecutableParticipantConfiguration) executableConfiguration;

            return Synchronization.synchronizeCollection(participants, participantConfiguration.getParticipants(),
                    new Synchronization.Handler<AbstractParticipant, cz.cesnet.shongo.controller.api.AbstractParticipant>(
                            AbstractParticipant.class)
                    {
                        @Override
                        public AbstractParticipant createFromApi(cz.cesnet.shongo.controller.api.AbstractParticipant objectApi)
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
            return super.updateFromExecutableConfigurationApi(executableConfiguration, entityManager);
        }
    }

    /**
     *
     * @param roomApi to be modified
     * @param executor to be used
     * @param executableManager
     * @throws ExecutorReportSet.RoomNotStartedException, ExecutorReportSet.CommandFailedException
     */
    public abstract void modifyRoom(Room roomApi, Executor executor,
            ExecutableManager executableManager)
            throws ExecutorReportSet.RoomNotStartedException, ExecutorReportSet.CommandFailedException;

    @Override
    protected void onCreate()
    {
        super.onCreate();

        onUpdate();
    }

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
}
