package cz.cesnet.shongo.controller.booking.room;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.api.RoomAvailability;
import cz.cesnet.shongo.controller.api.RoomEstablishment;
import cz.cesnet.shongo.controller.api.Synchronization;
import cz.cesnet.shongo.controller.booking.Allocation;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.participant.AbstractParticipant;
import cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.booking.request.ReservationRequestManager;
import cz.cesnet.shongo.controller.booking.reservation.Reservation;
import cz.cesnet.shongo.controller.booking.room.settting.RoomSetting;
import cz.cesnet.shongo.controller.booking.alias.AliasSpecification;
import cz.cesnet.shongo.controller.booking.specification.ExecutableServiceSpecification;
import cz.cesnet.shongo.controller.booking.specification.Specification;
import cz.cesnet.shongo.controller.booking.alias.Alias;
import cz.cesnet.shongo.controller.booking.resource.DeviceResource;
import cz.cesnet.shongo.controller.booking.resource.ResourceManager;
import cz.cesnet.shongo.controller.scheduler.*;
import cz.cesnet.shongo.util.ObjectHelper;
import org.joda.time.Interval;

import javax.persistence.*;
import java.util.*;

/**
 * Represents a {@link cz.cesnet.shongo.controller.booking.specification.Specification} for {@link RoomConfiguration}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class RoomSpecification extends Specification implements ReservationTaskProvider
{
    /**
     * Number of minutes which the room shall be available before requested time slot.
     */
    private int slotMinutesBefore;

    /**
     * Number of minutes which the room shall be available after requested time slot.
     */
    private int slotMinutesAfter;

    /**
     * {@link DeviceResource} with {@link RoomProviderCapability} in which the {@link RoomConfiguration} should be allocated.
     */
    private DeviceResource deviceResource;

    /**
     * Specifies whether some reusable {@link RoomEndpoint} should be reused.
     */
    private boolean reusedRoom;

    /**
     * Number of participants which shall be able to join to the virtual room. Zero means that the room shall be permanent.
     *
     * @see #isPermanent
     */
    private Integer participantCount;

    /**
     * List of {@link AbstractParticipant}s for the room.
     */
    private List<AbstractParticipant> participants = new LinkedList<AbstractParticipant>();

    /**
     * List of {@link cz.cesnet.shongo.controller.booking.room.settting.RoomSetting}s for the {@link RoomConfiguration}
     * (e.g., {@link Technology} specific).
     */
    private List<RoomSetting> roomSettings = new ArrayList<RoomSetting>();

    /**
     * List of {@link cz.cesnet.shongo.controller.booking.alias.AliasSpecification} for {@link Alias}es which should be allocated for the room.
     */
    private List<AliasSpecification> aliasSpecifications = new ArrayList<AliasSpecification>();

    /**
     * List of {@link cz.cesnet.shongo.controller.booking.specification.ExecutableServiceSpecification}s for the room.
     */
    private List<ExecutableServiceSpecification> serviceSpecifications = new LinkedList<ExecutableServiceSpecification>();

    /**
     * Constructor.
     */
    public RoomSpecification()
    {
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
     * @return {@link #deviceResource}
     */
    @OneToOne
    public DeviceResource getDeviceResource()
    {
        return deviceResource;
    }

    /**
     * @param deviceResource sets the {@link #deviceResource}
     */
    public void setDeviceResource(DeviceResource deviceResource)
    {
        this.deviceResource = deviceResource;
    }

    /**
     * @return {@link #reusedRoom}
     */
    @Column(nullable = false, columnDefinition = "boolean default false")
    public boolean isReusedRoom()
    {
        return reusedRoom;
    }

    /**
     * @param reusedRoom sets the {@link #reusedRoom}
     */
    public void setReusedRoom(boolean reusedRoom)
    {
        this.reusedRoom = reusedRoom;
    }

    /**
     * @return {@link #participantCount}
     */
    @Column
    public Integer getParticipantCount()
    {
        return participantCount;
    }

    /**
     * @param participantCount sets the {@link #participantCount}
     */
    public void setParticipantCount(Integer participantCount)
    {
        this.participantCount = participantCount;
    }

    /**
     * @return {@link #participants}
     */
    @OneToMany(cascade = CascadeType.ALL)
    @Access(AccessType.FIELD)
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
            this.participants.add(participant.clone());
        }
    }

    /**
     * @return {@link #roomSettings}
     */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @Access(AccessType.FIELD)
    public List<RoomSetting> getRoomSettings()
    {
        return roomSettings;
    }

    /**
     * @param roomSettings sets the {@link #roomSettings}
     */
    public void setRoomSettings(List<RoomSetting> roomSettings)
    {
        this.roomSettings.clear();
        for (RoomSetting roomConfiguration : roomSettings) {
            this.roomSettings.add(roomConfiguration.clone());
        }
    }

    /**
     * Remove all {@link #roomSettings}.
     */
    public void clearRoomSettings()
    {
        roomSettings.clear();
    }

    /**
     * @param roomSetting to be added to the {@link #roomSettings}
     */
    public void addRoomSetting(RoomSetting roomSetting)
    {
        roomSettings.add(roomSetting);
    }

    /**
     * @param roomSetting to be removed from the {@link #roomSettings}
     */
    public void removeRoomSetting(RoomSetting roomSetting)
    {
        roomSettings.remove(roomSetting);
    }

    /**
     * @return {@link #aliasSpecifications}
     */
    @OneToMany(cascade = CascadeType.ALL)
    @Access(AccessType.FIELD)
    public List<AliasSpecification> getAliasSpecifications()
    {
        return Collections.unmodifiableList(aliasSpecifications);
    }

    /**
     * @param aliasSpecifications sets the {@link #aliasSpecifications}
     */
    public void setAliasSpecifications(List<AliasSpecification> aliasSpecifications, EntityManager entityManager)
    {
        this.aliasSpecifications.clear();
        for (AliasSpecification aliasSpecification : aliasSpecifications) {
            this.aliasSpecifications.add(aliasSpecification.clone(entityManager));
        }
    }

    /**
     * @param aliasSpecification to be added to the {@link #aliasSpecifications}
     */
    public void addAliasSpecification(AliasSpecification aliasSpecification)
    {
        aliasSpecifications.add(aliasSpecification);
    }

    /**
     * @param aliasSpecification to be removed from the {@link #aliasSpecifications}
     */
    public void removeAliasSpecification(AliasSpecification aliasSpecification)
    {
        aliasSpecifications.remove(aliasSpecification);
    }

    /**
     * @return {@link #serviceSpecifications}
     */
    @OneToMany(cascade = CascadeType.ALL)
    @Access(AccessType.FIELD)
    public List<ExecutableServiceSpecification> getServiceSpecifications()
    {
        return Collections.unmodifiableList(serviceSpecifications);
    }

    /**
     * @param serviceSpecifications sets the {@link #serviceSpecifications}
     */
    public void setServiceSpecifications(List<ExecutableServiceSpecification> serviceSpecifications, EntityManager entityManager)
    {
        this.serviceSpecifications.clear();
        for (ExecutableServiceSpecification serviceSpecification : serviceSpecifications) {
            this.serviceSpecifications.add(serviceSpecification.clone(entityManager));
        }
    }

    /**
     * TODO: describe what is permanent room
     *
     * @return true whether permanent room should be allocated, false otherwise
     */
    @Transient
    public boolean isPermanent()
    {
        return participantCount == null && !reusedRoom;
    }

    @Override
    public boolean synchronizeFrom(Specification specification, EntityManager entityManager)
    {
        RoomSpecification roomSpecification = (RoomSpecification) specification;

        boolean modified = super.synchronizeFrom(specification, entityManager);
        modified |= !ObjectHelper.isSame(getSlotMinutesBefore(), roomSpecification.getSlotMinutesBefore());
        modified |= !ObjectHelper.isSame(getSlotMinutesAfter(), roomSpecification.getSlotMinutesAfter());
        modified |= !ObjectHelper.isSamePersistent(getDeviceResource(), roomSpecification.getDeviceResource());
        modified |= !ObjectHelper.isSame(isReusedRoom(), roomSpecification.isReusedRoom());
        modified |= !ObjectHelper.isSame(getParticipantCount(), roomSpecification.getParticipantCount());

        setSlotMinutesBefore(roomSpecification.getSlotMinutesBefore());
        setSlotMinutesAfter(roomSpecification.getSlotMinutesAfter());
        setDeviceResource(roomSpecification.getDeviceResource());
        setReusedRoom(roomSpecification.isReusedRoom());
        setParticipantCount(roomSpecification.getParticipantCount());

        if (!ObjectHelper.isSame(roomSettings, roomSpecification.getRoomSettings())) {
            setRoomSettings(roomSpecification.getRoomSettings());
            modified = true;
        }

        if (!ObjectHelper.isSame(aliasSpecifications, roomSpecification.getAliasSpecifications())) {
            setAliasSpecifications(roomSpecification.getAliasSpecifications(), entityManager);
            modified = true;
        }

        if (!ObjectHelper.isSame(participants, roomSpecification.getParticipants())) {
            setParticipants(roomSpecification.getParticipants());
            modified = true;
        }

        if (!ObjectHelper.isSame(serviceSpecifications, roomSpecification.getServiceSpecifications())) {
            setServiceSpecifications(roomSpecification.getServiceSpecifications(), entityManager);
            modified = true;
        }

        return modified;
    }

    @Override
    public ReservationTask createReservationTask(SchedulerContext schedulerContext, Interval slot) throws SchedulerException
    {
        RoomProviderCapability roomProviderCapability = null;
        if (deviceResource != null) {
            roomProviderCapability = deviceResource.getCapabilityRequired(RoomProviderCapability.class);
        }

        RoomReservationTask roomReservationTask =
                new RoomReservationTask(schedulerContext, slot, slotMinutesBefore, slotMinutesAfter);
        roomReservationTask.setParticipantCount(getParticipantCount());
        roomReservationTask.addRoomSettings(getRoomSettings());
        roomReservationTask.addAliasSpecifications(getAliasSpecifications());
        roomReservationTask.setRoomProviderCapability(roomProviderCapability);
        roomReservationTask.addParticipants(getParticipants());
        roomReservationTask.addServiceSpecifications(getServiceSpecifications());

        if (reusedRoom) {
            SchedulerContextState schedulerContextState = schedulerContext.getState();
            Collection<AvailableExecutable<RoomEndpoint>> availableRoomEndpoints =
                    schedulerContextState.getAvailableExecutables(RoomEndpoint.class);
            if (availableRoomEndpoints.size() != 1) {
                throw new SchedulerReportSet.RoomExecutableNotExistsException();
            }
            RoomEndpoint reusedRoomEndpoint = availableRoomEndpoints.iterator().next().getExecutable();
            roomReservationTask.setReusedRoomEndpoint(reusedRoomEndpoint);
        }
        else {
            Set<Technology> technologies = getTechnologies();
            if (technologies.size() == 0) {
                // When no technologies are requested, set technologies from requested aliases
                technologies = getAliasTechnologies();
            }
            roomReservationTask.addTechnologyVariant(technologies);
        }

        return roomReservationTask;
    }

    @Override
    protected cz.cesnet.shongo.controller.api.Specification createApi()
    {
        return new cz.cesnet.shongo.controller.api.RoomSpecification();
    }

    @Override
    public void toApi(cz.cesnet.shongo.controller.api.Specification specificationApi)
    {
        cz.cesnet.shongo.controller.api.RoomSpecification roomSpecificationApi =
                (cz.cesnet.shongo.controller.api.RoomSpecification) specificationApi;

        for (RoomSetting roomSetting : getRoomSettings()) {
            roomSpecificationApi.addRoomSetting(roomSetting.toApi());
        }
        for (AbstractParticipant participant : getParticipants()) {
            roomSpecificationApi.addParticipant(participant.toApi());
        }

        if (!isReusedRoom()) {
            RoomEstablishment establishmentApi =
                    new RoomEstablishment();
            if (deviceResource != null) {
                establishmentApi.setResourceId(ObjectIdentifier.formatId(deviceResource));
            }
            for (Technology technology : getTechnologies()) {
                establishmentApi.addTechnology(technology);
            }
            for (AliasSpecification aliasSpecification : getAliasSpecifications()) {
                establishmentApi.addAliasSpecification(aliasSpecification.toApi());
            }
            roomSpecificationApi.setEstablishment(establishmentApi);
        }

        if (!isPermanent()) {
            RoomAvailability availabilityApi =
                    new RoomAvailability();
            availabilityApi.setSlotMinutesBefore(slotMinutesBefore);
            availabilityApi.setSlotMinutesAfter(slotMinutesAfter);
            availabilityApi.setParticipantCount(participantCount != null ? participantCount : 0);
            for (ExecutableServiceSpecification serviceSpecification : getServiceSpecifications()) {
                availabilityApi.addServiceSpecification(serviceSpecification.toApi());
            }
            roomSpecificationApi.setAvailability(availabilityApi);
        }

        super.toApi(specificationApi);
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.Specification specificationApi,
            final EntityManager entityManager)
    {
        cz.cesnet.shongo.controller.api.RoomSpecification roomSpecificationApi =
                (cz.cesnet.shongo.controller.api.RoomSpecification) specificationApi;

        Synchronization.synchronizeCollection(roomSettings, roomSpecificationApi.getRoomSettings(),
                new Synchronization.Handler<RoomSetting, cz.cesnet.shongo.api.RoomSetting>(
                        RoomSetting.class)
                {
                    @Override
                    public RoomSetting createFromApi(
                            cz.cesnet.shongo.api.RoomSetting objectApi)
                    {
                        return RoomSetting.createFromApi(objectApi);
                    }

                    @Override
                    public void updateFromApi(RoomSetting object,
                            cz.cesnet.shongo.api.RoomSetting objectApi)
                    {
                        object.fromApi(objectApi);
                    }
                });
        Synchronization.synchronizeCollection(participants, roomSpecificationApi.getParticipants(),
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

        RoomEstablishment establishmentApi =
                roomSpecificationApi.getEstablishment();
        if (establishmentApi != null) {
            setReusedRoom(false);

            // Preferred device resource
            if (establishmentApi.getResourceId() == null) {
                setDeviceResource(null);
            }
            else {
                Long resourceId = ObjectIdentifier.parseId(cz.cesnet.shongo.controller.booking.resource.Resource.class,
                        establishmentApi.getResourceId());
                ResourceManager resourceManager = new ResourceManager(entityManager);
                setDeviceResource(resourceManager.getDevice(resourceId));
            }

            // Technologies
            Synchronization.synchronizeCollection(technologies, establishmentApi.getTechnologies());

            // Alias specifications
            Synchronization.synchronizeCollection(aliasSpecifications,
                    establishmentApi.getAliasSpecifications(),
                    new Synchronization.Handler<AliasSpecification, cz.cesnet.shongo.controller.api.AliasSpecification>(
                            AliasSpecification.class)
                    {
                        @Override
                        public AliasSpecification createFromApi(
                                cz.cesnet.shongo.controller.api.AliasSpecification objectApi)
                        {
                            AliasSpecification aliasSpecification = new AliasSpecification();
                            aliasSpecification.fromApi(objectApi, entityManager);
                            return aliasSpecification;
                        }

                        @Override
                        public void updateFromApi(AliasSpecification object,
                                cz.cesnet.shongo.controller.api.AliasSpecification objectApi)
                        {
                            object.fromApi(objectApi, entityManager);
                        }
                    });
        }
        else {
            setReusedRoom(true);
            setDeviceResource(null);
            technologies.clear();
            aliasSpecifications.clear();
        }

        RoomAvailability availabilityApi =
                roomSpecificationApi.getAvailability();
        if (availabilityApi != null) {
            setSlotMinutesBefore(availabilityApi.getSlotMinutesBefore());
            setSlotMinutesAfter(availabilityApi.getSlotMinutesAfter());
            setParticipantCount(availabilityApi.getParticipantCount());

            Synchronization.synchronizeCollection(this.serviceSpecifications, availabilityApi.getServiceSpecifications(),
                    new Synchronization.Handler<ExecutableServiceSpecification, cz.cesnet.shongo.controller.api.ExecutableServiceSpecification>(
                            ExecutableServiceSpecification.class)
                    {
                        @Override
                        public ExecutableServiceSpecification createFromApi(
                                cz.cesnet.shongo.controller.api.ExecutableServiceSpecification objectApi)
                        {
                            ExecutableServiceSpecification serviceSpecification =
                                    (ExecutableServiceSpecification) ExecutableServiceSpecification.createFromApi(
                                            objectApi, entityManager);
                            serviceSpecification.fromApi(objectApi, entityManager);
                            return serviceSpecification;
                        }

                        @Override
                        public void updateFromApi(ExecutableServiceSpecification object,
                                cz.cesnet.shongo.controller.api.ExecutableServiceSpecification objectApi)
                        {
                            object.fromApi(objectApi, entityManager);
                        }
                    });
        }
        else {
            setParticipantCount(null);
            serviceSpecifications.clear();
        }

        if (establishmentApi == null && availabilityApi == null) {
            throw new CommonReportSet.ClassAttributeRequiredException(
                    RoomSpecification.class.getSimpleName(), "availability");
        }

        // Check alias specifications
        for (Technology requestedTechnology : getAliasTechnologies()) {
            if (!requestedTechnology.isCompatibleWith(technologies)) {
                throw new RuntimeException("Cannot request alias in technology which the room doesn't support.");
            }
        }

        super.fromApi(specificationApi, entityManager);
    }

    @Override
    public void updateTechnologies(EntityManager entityManager)
    {
        if (reusedRoom) {
            RoomEndpoint reusedRoomEndpoint = getReusedRoomEndpoint(entityManager);
            if (reusedRoomEndpoint != null) {
                setTechnologies(reusedRoomEndpoint.getTechnologies());
            }
        }
    }

    @Transient
    private RoomEndpoint getReusedRoomEndpoint(EntityManager entityManager)
    {
        if (this.id == null) {
            return null;
        }
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        AbstractReservationRequest reservationRequest = reservationRequestManager.getBySpecification(this);
        Allocation reusedAllocation = reservationRequest.getReusedAllocation();
        if (reusedAllocation == null) {
            return null;
        }
        Reservation reusedReservation = reusedAllocation.getCurrentReservation();
        if (reusedReservation == null || !(reusedReservation.getExecutable() instanceof RoomEndpoint)) {
            return null;
        }
        return (RoomEndpoint) reusedReservation.getExecutable();
    }

    /**
     * @return set of {@link Technology}s for {@link #aliasSpecifications}
     */
    @Transient
    private Set<Technology> getAliasTechnologies()
    {
        Set<Technology> aliasTechnologies = new HashSet<Technology>();
        for (AliasSpecification aliasSpecification : aliasSpecifications) {
            for (Technology technology : aliasSpecification.getTechnologies()) {
                aliasTechnologies.add(technology);
            }
            for (AliasType aliasType : aliasSpecification.getAliasTypes()) {
                aliasTechnologies.add(aliasType.getTechnology());
            }
        }
        aliasTechnologies.remove(Technology.ALL);
        return aliasTechnologies;
    }
}
