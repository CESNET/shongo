package cz.cesnet.shongo.controller.booking.room;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.ControllerReportSet;
import cz.cesnet.shongo.controller.EntityType;
import cz.cesnet.shongo.controller.api.Synchronization;
import cz.cesnet.shongo.controller.booking.Allocation;
import cz.cesnet.shongo.controller.booking.executable.Executable;
import cz.cesnet.shongo.controller.booking.executable.ExecutableManager;
import cz.cesnet.shongo.controller.booking.participant.AbstractParticipant;
import cz.cesnet.shongo.controller.booking.EntityIdentifier;
import cz.cesnet.shongo.controller.booking.request.ReservationRequest;
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
     * {@link DeviceResource} with {@link RoomProviderCapability} in which the {@link RoomConfiguration} should be allocated.
     */
    private DeviceResource deviceResource;

    /**
     * Number of participants which shall be able to join to the virtual room. Zero means that the room shall be permanent.
     *
     * @see #isPermanent
     */
    private Integer participantCount;

    /**
     * Specifies {@link Allocation} from which a {@link RoomEndpoint} should be reused.
     *
     * @see #isReusement
     */
    private Allocation reusedAllocation;

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
     * List of {@link AbstractParticipant}s for the room.
     */
    private List<AbstractParticipant> participants = new LinkedList<AbstractParticipant>();

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
     * @return {@link #reusedAllocation}
     */
    @ManyToOne
    public Allocation getReusedAllocation()
    {
        return reusedAllocation;
    }

    /**
     * @param reusedAllocation sets the {@link #reusedAllocation}
     */
    public void setReusedAllocation(Allocation reusedAllocation)
    {
        this.reusedAllocation = reusedAllocation;
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
    public void setAliasSpecifications(List<AliasSpecification> aliasSpecifications)
    {
        this.aliasSpecifications.clear();
        for (AliasSpecification aliasSpecification : aliasSpecifications) {
            this.aliasSpecifications.add(aliasSpecification.clone());
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
    public void setServiceSpecifications(List<ExecutableServiceSpecification> serviceSpecifications)
    {
        this.serviceSpecifications.clear();
        for (ExecutableServiceSpecification serviceSpecification : serviceSpecifications) {
            this.serviceSpecifications.add(serviceSpecification.clone());
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
        return participantCount == null && reusedAllocation == null;
    }

    /**
     * TODO: describe what is reusement of a room
     *
     * @return true whether capacity should be allocated, false otherwise
     */
    @Transient
    public boolean isReusement()
    {
        return participantCount != null && reusedAllocation != null;
    }

    @Transient
    private RoomEndpoint getReusedRoomEndpoint()
    {
        if (reusedAllocation == null) {
            return null;
        }
        Reservation reusedReservation = reusedAllocation.getCurrentReservation();
        if (reusedReservation == null || !(reusedReservation.getExecutable() instanceof RoomEndpoint)) {
            return null;
        }
        return (RoomEndpoint) reusedReservation.getExecutable();
    }

    @Override
    public boolean synchronizeFrom(Specification specification)
    {
        RoomSpecification roomSpecification = (RoomSpecification) specification;

        boolean modified = super.synchronizeFrom(specification);
        modified |= !ObjectHelper.isSame(getParticipantCount(), roomSpecification.getParticipantCount());
        modified |= !ObjectHelper.isSame(getReusedAllocation(), roomSpecification.getReusedAllocation());
        modified |= !ObjectHelper.isSame(getDeviceResource(), roomSpecification.getDeviceResource());

        setParticipantCount(roomSpecification.getParticipantCount());
        setReusedAllocation(roomSpecification.getReusedAllocation());
        setDeviceResource(roomSpecification.getDeviceResource());

        if (!roomSettings.equals(roomSpecification.getRoomSettings())) {
            setRoomSettings(roomSpecification.getRoomSettings());
            modified = true;
        }

        if (!aliasSpecifications.equals(roomSpecification.getAliasSpecifications())) {
            setAliasSpecifications(roomSpecification.getAliasSpecifications());
            modified = true;
        }

        if (!participants.equals(roomSpecification.getParticipants())) {
            setParticipants(roomSpecification.getParticipants());
            modified = true;
        }

        if (!serviceSpecifications.equals(roomSpecification.getServiceSpecifications())) {
            setServiceSpecifications(roomSpecification.getServiceSpecifications());
            modified = true;
        }

        return modified;
    }

    @Override
    public ReservationTask createReservationTask(SchedulerContext schedulerContext) throws SchedulerException
    {
        RoomProviderCapability roomProviderCapability = null;
        if (deviceResource != null) {
            roomProviderCapability = deviceResource.getCapabilityRequired(RoomProviderCapability.class);
        }

        RoomReservationTask roomReservationTask = new RoomReservationTask(schedulerContext, getParticipantCount());
        roomReservationTask.addRoomSettings(getRoomSettings());
        roomReservationTask.addAliasSpecifications(getAliasSpecifications());
        roomReservationTask.setRoomProviderCapability(roomProviderCapability);
        roomReservationTask.addParticipants(getParticipants());
        roomReservationTask.addServiceSpecifications(getServiceSpecifications());

        RoomEndpoint reusedRoomEndpoint = getReusedRoomEndpoint();
        if (reusedAllocation != null && reusedRoomEndpoint == null) {
            throw new SchedulerReportSet.RoomExecutableNotExistsException();
        }
        if (reusedRoomEndpoint != null) {
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
        if (isPermanent()) {
            return new cz.cesnet.shongo.controller.api.PermanentRoomSpecification();
        }
        else if (isReusement()) {
            return new cz.cesnet.shongo.controller.api.UsedRoomSpecification();
        }
        else {
            return new cz.cesnet.shongo.controller.api.RoomSpecification();
        }
    }

    @Override
    public void toApi(cz.cesnet.shongo.controller.api.Specification specificationApi)
    {
        cz.cesnet.shongo.controller.api.AbstractRoomSpecification abstractRoomSpecificationApi =
                (cz.cesnet.shongo.controller.api.AbstractRoomSpecification) specificationApi;

        // Fill AbstractRoomSpecification
        for (RoomSetting roomSetting : getRoomSettings()) {
            abstractRoomSpecificationApi.addRoomSetting(roomSetting.toApi());
        }
        for (AbstractParticipant participant : getParticipants()) {
            abstractRoomSpecificationApi.addParticipant(participant.toApi());
        }

        // Fill StandaloneRoomSpecification
        if (abstractRoomSpecificationApi instanceof cz.cesnet.shongo.controller.api.StandaloneRoomSpecification) {
            cz.cesnet.shongo.controller.api.StandaloneRoomSpecification standaloneRoomSpecificationApi =
                    (cz.cesnet.shongo.controller.api.StandaloneRoomSpecification) abstractRoomSpecificationApi;

            if (deviceResource != null) {
                standaloneRoomSpecificationApi.setResourceId(EntityIdentifier.formatId(deviceResource));
            }
            for (Technology technology : getTechnologies()) {
                standaloneRoomSpecificationApi.addTechnology(technology);
            }
            for (AliasSpecification aliasSpecification : getAliasSpecifications()) {
                standaloneRoomSpecificationApi.addAliasSpecification(aliasSpecification.toApi());
            }
        }

        // Fill RoomSpecification
        if (abstractRoomSpecificationApi instanceof cz.cesnet.shongo.controller.api.RoomSpecification) {
            cz.cesnet.shongo.controller.api.RoomSpecification roomSpecificationApi =
                    (cz.cesnet.shongo.controller.api.RoomSpecification) specificationApi;

            roomSpecificationApi.setParticipantCount(getParticipantCount());
            for (ExecutableServiceSpecification serviceSpecification : getServiceSpecifications()) {
                roomSpecificationApi.addServiceSpecification(serviceSpecification.toApi());
            }
        }

        // Fill UsedRoomSpecification
        if (abstractRoomSpecificationApi instanceof cz.cesnet.shongo.controller.api.UsedRoomSpecification) {
            cz.cesnet.shongo.controller.api.UsedRoomSpecification usedRoomSpecificationApi =
                    (cz.cesnet.shongo.controller.api.UsedRoomSpecification) specificationApi;

            RoomEndpoint reusedRoomEndpoint = getReusedRoomEndpoint();
            if (reusedRoomEndpoint == null) {
                throw new IllegalStateException("Reused room endpoint must be defined.");
            }
            usedRoomSpecificationApi.setReusedRoomExecutableId(EntityIdentifier.formatId(reusedRoomEndpoint));
            usedRoomSpecificationApi.setParticipantCount(getParticipantCount());
            for (ExecutableServiceSpecification serviceSpecification : getServiceSpecifications()) {
                usedRoomSpecificationApi.addServiceSpecification(serviceSpecification.toApi());
            }
        }

        super.toApi(specificationApi);
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.Specification specificationApi,
            final EntityManager entityManager)
    {

        cz.cesnet.shongo.controller.api.AbstractRoomSpecification abstractRoomSpecificationApi =
                (cz.cesnet.shongo.controller.api.AbstractRoomSpecification) specificationApi;

        // Load AbstractRoomSpecification
        Synchronization.synchronizeCollection(roomSettings, abstractRoomSpecificationApi.getRoomSettings(),
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
        Synchronization.synchronizeCollection(participants, abstractRoomSpecificationApi.getParticipants(),
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

        // Load StandaloneRoomSpecification
        if (abstractRoomSpecificationApi instanceof cz.cesnet.shongo.controller.api.StandaloneRoomSpecification) {
            cz.cesnet.shongo.controller.api.StandaloneRoomSpecification standaloneRoomSpecificationApi =
                    (cz.cesnet.shongo.controller.api.StandaloneRoomSpecification) abstractRoomSpecificationApi;

            // Preferred device resource
            if (standaloneRoomSpecificationApi.getResourceId() == null) {
                setDeviceResource(null);
            }
            else {
                Long resourceId = EntityIdentifier.parseId(cz.cesnet.shongo.controller.booking.resource.Resource.class,
                        standaloneRoomSpecificationApi.getResourceId());
                ResourceManager resourceManager = new ResourceManager(entityManager);
                setDeviceResource(resourceManager.getDevice(resourceId));
            }

            // Technologies
            Synchronization.synchronizeCollection(technologies, standaloneRoomSpecificationApi.getTechnologies());

            // Alias specifications
            Synchronization.synchronizeCollection(aliasSpecifications,
                    standaloneRoomSpecificationApi.getAliasSpecifications(),
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

        // Load RoomSpecification
        List<cz.cesnet.shongo.controller.api.ExecutableServiceSpecification> serviceSpecifications = null;
        if (abstractRoomSpecificationApi instanceof cz.cesnet.shongo.controller.api.RoomSpecification) {
            cz.cesnet.shongo.controller.api.RoomSpecification roomSpecificationApi =
                    (cz.cesnet.shongo.controller.api.RoomSpecification) specificationApi;
            serviceSpecifications = roomSpecificationApi.getServiceSpecifications();

            setParticipantCount(roomSpecificationApi.getParticipantCount());
        }
        // Load UsedRoomSpecification
        else if (abstractRoomSpecificationApi instanceof cz.cesnet.shongo.controller.api.UsedRoomSpecification) {
            cz.cesnet.shongo.controller.api.UsedRoomSpecification usedRoomSpecificationApi =
                    (cz.cesnet.shongo.controller.api.UsedRoomSpecification) specificationApi;
            serviceSpecifications = usedRoomSpecificationApi.getServiceSpecifications();

            EntityIdentifier entityIdentifier =
                    EntityIdentifier.parse(usedRoomSpecificationApi.getReusedRoomExecutableId());
            if (entityIdentifier.getEntityType().equals(EntityType.RESERVATION_REQUEST)) {
                ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
                ReservationRequest reservationRequest =
                        reservationRequestManager.getReservationRequest(entityIdentifier.getPersistenceId());
                setReusedAllocation(reservationRequest.getAllocation());
            }
            else if (entityIdentifier.getEntityType().equals(EntityType.EXECUTABLE)) {
                Long reusedRoomEndpointId = entityIdentifier.getPersistenceId();
                ExecutableManager executableManager = new ExecutableManager(entityManager);
                Executable executable = executableManager.get(reusedRoomEndpointId);
                if (!(executable instanceof RoomEndpoint)) {
                    throw new CommonReportSet.EntityNotExistsException(
                            RoomEndpoint.class.getSimpleName(), reusedRoomEndpointId.toString());
                }
                Reservation reservation = executableManager.getReservation(executable);
                if (reservation == null || reservation.getAllocation() == null) {
                    throw new ControllerReportSet.ExecutableNotReusableException(reusedRoomEndpointId.toString());
                }
                setReusedAllocation(reservation.getAllocation());
            }
            else {
                throw new CommonReportSet.EntityNotExistsException(
                        RoomEndpoint.class.getSimpleName(), entityIdentifier.toString());
            }
            RoomEndpoint reusedRoomEndpoint = getReusedRoomEndpoint();
            if (reusedRoomEndpoint == null) {
                throw new CommonReportSet.EntityNotExistsException(
                        RoomEndpoint.class.getSimpleName(), entityIdentifier.toString());
            }
            setParticipantCount(usedRoomSpecificationApi.getParticipantCount());
        }
        else {
            setParticipantCount(null);
        }

        Synchronization.synchronizeCollection(this.serviceSpecifications, serviceSpecifications,
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

        // Check alias specifications
        for (Technology requestedTechnology : getAliasTechnologies()) {
            if (!requestedTechnology.isCompatibleWith(technologies)) {
                throw new RuntimeException("Cannot request alias in technology which the room doesn't support.");
            }
        }

        super.fromApi(specificationApi, entityManager);
    }

    @Override
    public void updateTechnologies()
    {
        RoomEndpoint reusedRoomEndpoint = getReusedRoomEndpoint();
        if (reusedRoomEndpoint != null) {
            setTechnologies(reusedRoomEndpoint.getTechnologies());
        }
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
