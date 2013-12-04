package cz.cesnet.shongo.controller.booking.room;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.ControllerReportSetHelper;
import cz.cesnet.shongo.controller.api.Synchronization;
import cz.cesnet.shongo.controller.booking.executable.Executable;
import cz.cesnet.shongo.controller.booking.executable.ExecutableManager;
import cz.cesnet.shongo.controller.booking.participant.AbstractParticipant;
import cz.cesnet.shongo.controller.booking.EntityIdentifier;
import cz.cesnet.shongo.controller.booking.room.settting.RoomSetting;
import cz.cesnet.shongo.controller.booking.alias.AliasSpecification;
import cz.cesnet.shongo.controller.booking.specification.ExecutableServiceSpecification;
import cz.cesnet.shongo.controller.booking.specification.Specification;
import cz.cesnet.shongo.controller.booking.alias.Alias;
import cz.cesnet.shongo.controller.booking.resource.DeviceResource;
import cz.cesnet.shongo.controller.booking.resource.ResourceManager;
import cz.cesnet.shongo.controller.scheduler.ReservationTask;
import cz.cesnet.shongo.controller.scheduler.ReservationTaskProvider;
import cz.cesnet.shongo.controller.scheduler.SchedulerContext;
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
     * Specifies {@link RoomEndpoint} which should be reused.
     *
     * @see #isReusement
     */
    private RoomEndpoint reusedRoomEndpoint;

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
     * @return {@link #reusedRoomEndpoint}
     */
    @ManyToOne
    public RoomEndpoint getReusedRoomEndpoint()
    {
        return reusedRoomEndpoint;
    }

    /**
     * @param roomEndpoint sets the {@link #reusedRoomEndpoint}
     */
    public void setReusedRoomEndpoint(RoomEndpoint roomEndpoint)
    {
        this.reusedRoomEndpoint = roomEndpoint;
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
        return participantCount == null && reusedRoomEndpoint == null;
    }

    /**
     * TODO: describe what is reusement of a room
     *
     * @return true whether capacity should be allocated, false otherwise
     */
    @Transient
    public boolean isReusement()
    {
        return participantCount != null && reusedRoomEndpoint != null;
    }

    @Override
    public boolean synchronizeFrom(Specification specification)
    {
        RoomSpecification roomSpecification = (RoomSpecification) specification;

        boolean modified = super.synchronizeFrom(specification);
        modified |= !ObjectHelper.isSame(getParticipantCount(), roomSpecification.getParticipantCount());
        modified |= !ObjectHelper.isSame(getReusedRoomEndpoint(), roomSpecification.getReusedRoomEndpoint());
        modified |= !ObjectHelper.isSame(getDeviceResource(), roomSpecification.getDeviceResource());

        setParticipantCount(roomSpecification.getParticipantCount());
        setReusedRoomEndpoint(roomSpecification.getReusedRoomEndpoint());
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
    public ReservationTask createReservationTask(SchedulerContext schedulerContext)
    {
        RoomProviderCapability roomProviderCapability = null;
        if (deviceResource != null) {
            roomProviderCapability = deviceResource.getCapabilityRequired(RoomProviderCapability.class);
        }

        Set<Technology> technologies = getTechnologies();
        if (technologies.size() == 0) {
            if (isReusement()) {
                // When no technologies are requested, set technologies from requested aliases
                technologies = reusedRoomEndpoint.getTechnologies();
            }
            else {
                // When no technologies are requested, set technologies from requested aliases
                technologies = getAliasTechnologies();
            }
        }

        RoomReservationTask roomReservationTask = new RoomReservationTask(schedulerContext, getParticipantCount());
        roomReservationTask.setReusedRoomEndpoint(getReusedRoomEndpoint());
        roomReservationTask.addTechnologyVariant(technologies);
        roomReservationTask.addRoomSettings(getRoomSettings());
        roomReservationTask.addAliasSpecifications(getAliasSpecifications());
        roomReservationTask.setRoomProviderCapability(roomProviderCapability);
        roomReservationTask.addParticipants(getParticipants());
        roomReservationTask.addServiceSpecifications(getServiceSpecifications());
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
        for (ExecutableServiceSpecification serviceSpecification : getServiceSpecifications()) {
            abstractRoomSpecificationApi.addServiceSpecification(serviceSpecification.toApi());
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
        }

        // Fill UsedRoomSpecification
        if (abstractRoomSpecificationApi instanceof cz.cesnet.shongo.controller.api.UsedRoomSpecification) {
            cz.cesnet.shongo.controller.api.UsedRoomSpecification usedRoomSpecificationApi =
                    (cz.cesnet.shongo.controller.api.UsedRoomSpecification) specificationApi;

            if (reusedRoomEndpoint != null) {
                usedRoomSpecificationApi.setReusedRoomExecutableId(EntityIdentifier.formatId(reusedRoomEndpoint));
            }
            usedRoomSpecificationApi.setParticipantCount(getParticipantCount());
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
        Synchronization.synchronizeCollection(serviceSpecifications, abstractRoomSpecificationApi.getServiceSpecifications(),
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
        if (abstractRoomSpecificationApi instanceof cz.cesnet.shongo.controller.api.RoomSpecification) {
            cz.cesnet.shongo.controller.api.RoomSpecification roomSpecificationApi =
                    (cz.cesnet.shongo.controller.api.RoomSpecification) specificationApi;

            setParticipantCount(roomSpecificationApi.getParticipantCount());
        }
        // Load UsedRoomSpecification
        else if (abstractRoomSpecificationApi instanceof cz.cesnet.shongo.controller.api.UsedRoomSpecification) {
            cz.cesnet.shongo.controller.api.UsedRoomSpecification usedRoomSpecificationApi =
                    (cz.cesnet.shongo.controller.api.UsedRoomSpecification) specificationApi;

            Long reusedRoomEndpointId = EntityIdentifier.parseId(
                    cz.cesnet.shongo.controller.booking.room.RoomEndpoint.class,
                    usedRoomSpecificationApi.getReusedRoomExecutableId());
            ExecutableManager executableManager = new ExecutableManager(entityManager);
            Executable executable = executableManager.get(reusedRoomEndpointId);
            if (!(executable instanceof RoomEndpoint)) {
                throw new CommonReportSet.EntityNotExistsException(
                        RoomEndpoint.class.getSimpleName(), reusedRoomEndpointId.toString());
            }
            setReusedRoomEndpoint((RoomEndpoint) executable);
            setParticipantCount(usedRoomSpecificationApi.getParticipantCount());
        }
        else {
            setParticipantCount(null);
        }



        // Check alias specifications
        for (Technology requestedTechnology : getAliasTechnologies()) {
            if (!requestedTechnology.isCompatibleWith(technologies)) {
                throw new RuntimeException("Cannot request alias in technology which the room doesn't support.");
            }
        }

        super.fromApi(specificationApi, entityManager);
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
