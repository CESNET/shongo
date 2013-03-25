package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.ControllerFaultSet;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.common.RoomSetting;
import cz.cesnet.shongo.controller.resource.Alias;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.ResourceManager;
import cz.cesnet.shongo.controller.resource.RoomProviderCapability;
import cz.cesnet.shongo.controller.scheduler.ReservationTask;
import cz.cesnet.shongo.controller.scheduler.ReservationTaskProvider;
import cz.cesnet.shongo.controller.scheduler.RoomReservationTask;
import cz.cesnet.shongo.fault.FaultException;
import org.apache.commons.lang.ObjectUtils;

import javax.persistence.*;
import java.util.*;

/**
 * Represents a {@link Specification} for {@link cz.cesnet.shongo.controller.common.RoomConfiguration}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class RoomSpecification extends Specification implements ReservationTaskProvider
{
    /**
     * {@link DeviceResource} with {@link RoomProviderCapability} in which the {@link cz.cesnet.shongo.controller.common.RoomConfiguration} should be allocated.
     */
    private DeviceResource deviceResource;

    /**
     * Number of participants which shall be able to join to the virtual room.
     */
    private int participantCount = 0;

    /**
     * List of {@link cz.cesnet.shongo.controller.common.RoomSetting}s for the {@link cz.cesnet.shongo.controller.common.RoomConfiguration}
     * (e.g., {@link Technology} specific).
     */
    private List<RoomSetting> roomSettings = new ArrayList<RoomSetting>();

    /**
     * List of {@link AliasSpecification} for {@link Alias}es which should be allocated for the room.
     */
    private List<AliasSpecification> aliasSpecifications = new ArrayList<AliasSpecification>();

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
    @Column(nullable = false)
    public int getParticipantCount()
    {
        return participantCount;
    }

    /**
     * @param participantCount sets the {@link #participantCount}
     */
    public void setParticipantCount(int participantCount)
    {
        this.participantCount = participantCount;
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
     * @param id of the requested {@link RoomSetting}
     * @return {@link RoomSetting} with given {@code id}
     * @throws FaultException when the {@link RoomSetting} doesn't exist
     */
    @Transient
    private RoomSetting getRoomSettingById(Long id) throws FaultException
    {
        for (RoomSetting roomSetting : roomSettings) {
            if (roomSetting.getId().equals(id)) {
                return roomSetting;
            }
        }
        return ControllerFaultSet.throwEntityNotFoundFault(RoomSetting.class, id);
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
     * @param id of the requested {@link AliasSpecification}
     * @return {@link AliasSpecification} with given {@code id}
     * @throws FaultException when the {@link AliasSpecification} doesn't exist
     */
    @Transient
    private AliasSpecification getAliasSpecificationById(Long id) throws FaultException
    {
        for (AliasSpecification aliasSpecification : aliasSpecifications) {
            if (aliasSpecification.getId().equals(id)) {
                return aliasSpecification;
            }
        }
        return ControllerFaultSet.throwEntityNotFoundFault(AliasSpecification.class, id);
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

    @Override
    public boolean synchronizeFrom(Specification specification)
    {
        RoomSpecification roomSpecification = (RoomSpecification) specification;

        boolean modified = super.synchronizeFrom(specification);
        modified |= !ObjectUtils.equals(getParticipantCount(), roomSpecification.getParticipantCount());
        modified |= !ObjectUtils.equals(getDeviceResource(), roomSpecification.getDeviceResource());

        setParticipantCount(roomSpecification.getParticipantCount());
        setDeviceResource(roomSpecification.getDeviceResource());

        if (!roomSettings.equals(roomSpecification.getRoomSettings())) {
            setRoomSettings(roomSpecification.getRoomSettings());
            modified = true;
        }

        if (!aliasSpecifications.equals(roomSpecification.getAliasSpecifications())) {
            setAliasSpecifications(roomSpecification.getAliasSpecifications());
            modified = true;
        }

        return modified;
    }

    @Override
    public ReservationTask createReservationTask(ReservationTask.Context context)
    {
        RoomReservationTask roomReservationTask = new RoomReservationTask(context, getParticipantCount());
        roomReservationTask.addTechnologyVariant(getTechnologies());
        roomReservationTask.addRoomSettings(getRoomSettings());
        roomReservationTask.addAliasSpecifications(getAliasSpecifications());
        roomReservationTask.setDeviceResource(getDeviceResource());
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
        if (deviceResource != null) {
            roomSpecificationApi.setResourceId(EntityIdentifier.formatId(deviceResource));
        }
        for (Technology technology : getTechnologies()) {
            roomSpecificationApi.addTechnology(technology);
        }
        roomSpecificationApi.setParticipantCount(getParticipantCount());
        for (RoomSetting roomSetting : getRoomSettings()) {
            roomSpecificationApi.addRoomSetting(roomSetting.toApi());
        }
        for (AliasSpecification aliasSpecification : getAliasSpecifications()) {
            roomSpecificationApi.addAlias(aliasSpecification.toApi());
        }
        super.toApi(specificationApi);
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.Specification specificationApi, EntityManager entityManager)
            throws FaultException
    {
        cz.cesnet.shongo.controller.api.RoomSpecification roomSpecificationApi =
                (cz.cesnet.shongo.controller.api.RoomSpecification) specificationApi;
        if (roomSpecificationApi.isPropertyFilled(roomSpecificationApi.PARTICIPANT_COUNT)) {
            setParticipantCount(roomSpecificationApi.getParticipantCount());
        }
        if (roomSpecificationApi.isPropertyFilled(roomSpecificationApi.RESOURCE_ID)) {
            if (roomSpecificationApi.getResourceId() == null) {
                setDeviceResource(null);
            }
            else {
                Long resourceId = EntityIdentifier.parseId(cz.cesnet.shongo.controller.resource.Resource.class,
                        roomSpecificationApi.getResourceId());
                ResourceManager resourceManager = new ResourceManager(entityManager);
                setDeviceResource(resourceManager.getDevice(resourceId));
            }
        }

        // Create technologies
        for (Technology technology : roomSpecificationApi.getTechnologies()) {
            if (specificationApi.isPropertyItemMarkedAsNew(roomSpecificationApi.TECHNOLOGIES, technology)) {
                addTechnology(technology);
            }
        }
        // Delete technologies
        Set<Technology> technologiesToDelete =
                specificationApi.getPropertyItemsMarkedAsDeleted(roomSpecificationApi.TECHNOLOGIES);
        for (Technology technology : technologiesToDelete) {
            removeTechnology(technology);
        }

        // Create/update room settings
        for (cz.cesnet.shongo.api.RoomSetting roomSettingApi : roomSpecificationApi.getRoomSettings()) {
            if (specificationApi.isPropertyItemMarkedAsNew(roomSpecificationApi.ROOM_SETTINGS, roomSettingApi)) {
                addRoomSetting(RoomSetting.createFromApi(roomSettingApi));
            }
            else {
                RoomSetting roomSetting = getRoomSettingById(roomSettingApi.notNullIdAsLong());
                roomSetting.fromApi(roomSettingApi);
            }
        }
        // Delete room settings
        Set<cz.cesnet.shongo.api.RoomSetting> roomSettingsToDelete =
                specificationApi.getPropertyItemsMarkedAsDeleted(roomSpecificationApi.ROOM_SETTINGS);
        for (cz.cesnet.shongo.api.RoomSetting roomSettingApi : roomSettingsToDelete) {
            removeRoomSetting(getRoomSettingById(roomSettingApi.notNullIdAsLong()));
        }

        // Create/update alias specifications
        Set<Technology> technologies = getTechnologies();
        for (cz.cesnet.shongo.controller.api.AliasSpecification aliasApi :
                roomSpecificationApi.getAliases()) {
            Set<Technology> requestedTechnologies = new HashSet<Technology>();
            for (Technology technology : aliasApi.getTechnologies()) {
                requestedTechnologies.add(technology);
            }
            for (AliasType aliasType : aliasApi.getAliasTypes()) {
                requestedTechnologies.add(aliasType.getTechnology());
            }
            for (Technology requestedTechnology : requestedTechnologies) {
                if (!requestedTechnology.isCompatibleWith(technologies)) {
                    throw new IllegalStateException(
                            "Cannot request alias in technology which the room doesn't support.");
                }
            }
            if (specificationApi.isPropertyItemMarkedAsNew(roomSpecificationApi.ALIASES, aliasApi)) {
                AliasSpecification aliasSpecification = new AliasSpecification();
                aliasSpecification.fromApi(aliasApi, entityManager);
                addAliasSpecification(aliasSpecification);
            }
            else {
                AliasSpecification aliasSpecification = getAliasSpecificationById(aliasApi.notNullIdAsLong());
                aliasSpecification.fromApi(aliasApi, entityManager);
            }
        }
        // Delete room settings
        Set<cz.cesnet.shongo.controller.api.AliasSpecification> aliasSpecificationsToDelete =
                specificationApi.getPropertyItemsMarkedAsDeleted(roomSpecificationApi.ALIASES);
        for (cz.cesnet.shongo.controller.api.AliasSpecification aliasSpecificationApi : aliasSpecificationsToDelete) {
            removeAliasSpecification(getAliasSpecificationById(aliasSpecificationApi.notNullIdAsLong()));
        }

        super.fromApi(specificationApi, entityManager);
    }
}
