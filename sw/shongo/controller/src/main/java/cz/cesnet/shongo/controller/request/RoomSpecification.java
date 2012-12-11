package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.common.RoomSetting;
import cz.cesnet.shongo.controller.resource.Alias;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.ResourceManager;
import cz.cesnet.shongo.controller.resource.RoomProviderCapability;
import cz.cesnet.shongo.controller.scheduler.ReservationTask;
import cz.cesnet.shongo.controller.scheduler.ReservationTaskProvider;
import cz.cesnet.shongo.controller.scheduler.RoomReservationTask;
import cz.cesnet.shongo.fault.EntityNotFoundException;
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
     * Set of technologies which the virtual room shall support.
     */
    private Set<Technology> technologies = new HashSet<Technology>();

    /**
     * Number of participants which shall be able to join to the virtual room.
     */
    private int participantCount = 0;

    /**
     * Specifies whether {@link Alias} should be allocated for each {@link Technology}
     * from {@link #technologies}.
     */
    private boolean withAlias;

    /**
     * List of {@link cz.cesnet.shongo.controller.common.RoomSetting}s for the {@link cz.cesnet.shongo.controller.common.RoomConfiguration}
     * (e.g., {@link Technology} specific).
     */
    private List<RoomSetting> roomSettings = new ArrayList<RoomSetting>();

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
     * @return {@link #technologies}
     */
    @ElementCollection
    @Enumerated(EnumType.STRING)
    @Access(AccessType.FIELD)
    public Set<Technology> getTechnologies()
    {
        return Collections.unmodifiableSet(technologies);
    }

    /**
     * @param technologies sets the {@link #technologies}
     */
    public void setTechnologies(Set<Technology> technologies)
    {
        this.technologies.clear();
        for (Technology technology : technologies) {
            this.technologies.add(technology);
        }
    }

    /**
     * @param technology technology to be added to the set of technologies that the device support.
     */
    public void addTechnology(Technology technology)
    {
        technologies.add(technology);
    }

    /**
     * @param technology technology to be removed from the {@link #technologies}
     */
    public void removeTechnology(Technology technology)
    {
        technologies.remove(technology);
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
     * @return {@link #withAlias}
     */
    @Column(nullable = false)
    public boolean isWithAlias()
    {
        return withAlias;
    }

    /**
     * @param withAlias sets the {@link #withAlias}
     */
    public void setWithAlias(boolean withAlias)
    {
        this.withAlias = withAlias;
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
     * @throws EntityNotFoundException when the {@link RoomSetting} doesn't exist
     */
    private RoomSetting getRoomSettingById(Long id) throws EntityNotFoundException
    {
        for (RoomSetting roomSetting : roomSettings) {
            if (roomSetting.getId().equals(id)) {
                return roomSetting;
            }
        }
        throw new EntityNotFoundException(RoomSetting.class, id);
    }


    /**
     * @param roomSettings sets the {@link #roomSettings}
     */
    public void setRoomSettings(List<RoomSetting> roomSettings)
    {
        this.roomSettings.clear();
        for ( RoomSetting roomConfiguration : roomSettings) {
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

    @Override
    public boolean synchronizeFrom(Specification specification)
    {
        RoomSpecification roomSpecification = (RoomSpecification) specification;

        boolean modified = super.synchronizeFrom(specification);
        modified |= !ObjectUtils.equals(getParticipantCount(), roomSpecification.getParticipantCount());
        modified |= !ObjectUtils.equals(isWithAlias(), roomSpecification.isWithAlias());
        modified |= !ObjectUtils.equals(getDeviceResource(), roomSpecification.getDeviceResource());

        setParticipantCount(roomSpecification.getParticipantCount());
        setWithAlias(roomSpecification.isWithAlias());
        setDeviceResource(roomSpecification.getDeviceResource());

        if (!technologies.equals(roomSpecification.getTechnologies())) {
            setTechnologies(roomSpecification.getTechnologies());
            modified = true;
        }

        if (!roomSettings.equals(roomSpecification.getRoomSettings())) {
            setRoomSettings(roomSpecification.getRoomSettings());
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
        roomReservationTask.setDeviceResource(getDeviceResource());
        roomReservationTask.setWithAlias(isWithAlias());
        return roomReservationTask;
    }

    @Override
    protected cz.cesnet.shongo.controller.api.Specification createApi()
    {
        return new cz.cesnet.shongo.controller.api.RoomSpecification();
    }

    @Override
    public void toApi(cz.cesnet.shongo.controller.api.Specification specificationApi, Domain domain)
    {
        cz.cesnet.shongo.controller.api.RoomSpecification roomSpecificationApi =
                (cz.cesnet.shongo.controller.api.RoomSpecification) specificationApi;
        if (deviceResource != null) {
            roomSpecificationApi.setResourceId(domain.formatId(deviceResource.getId()));
        }
        for (Technology technology : getTechnologies()) {
            roomSpecificationApi.addTechnology(technology);
        }
        roomSpecificationApi.setParticipantCount(getParticipantCount());
        roomSpecificationApi.setWithAlias(isWithAlias());
        for (RoomSetting roomSetting : getRoomSettings()) {
            roomSpecificationApi.addRoomSetting(roomSetting.toApi());
        }
        super.toApi(specificationApi, domain);
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.Specification specificationApi, EntityManager entityManager,
            Domain domain) throws FaultException
    {
        cz.cesnet.shongo.controller.api.RoomSpecification roomSpecificationApi =
                (cz.cesnet.shongo.controller.api.RoomSpecification) specificationApi;
        if (roomSpecificationApi.isPropertyFilled(roomSpecificationApi.PARTICIPANT_COUNT)) {
            setParticipantCount(roomSpecificationApi.getParticipantCount());
        }
        if (roomSpecificationApi.isPropertyFilled(roomSpecificationApi.WITH_ALIAS)) {
            setWithAlias(roomSpecificationApi.getWithAlias());
        }
        if (roomSpecificationApi.isPropertyFilled(roomSpecificationApi.RESOURCE_ID)) {
            if (roomSpecificationApi.getResourceId() == null) {
                setDeviceResource(null);
            }
            else {
                Long resourceId = domain.parseId(roomSpecificationApi.getResourceId());
                ResourceManager resourceManager = new ResourceManager(entityManager);
                setDeviceResource(resourceManager.getDevice(resourceId));
            }
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

        // Create technologies
        for (Technology technology : roomSpecificationApi.getTechnologies()) {
            if (specificationApi.isPropertyItemMarkedAsNew(roomSpecificationApi.TECHNOLOGIES, technology)) {
                addTechnology(technology);
            }
        }
        // Delete technologies
        Set<Technology> technologies =
                specificationApi.getPropertyItemsMarkedAsDeleted(roomSpecificationApi.TECHNOLOGIES);
        for (Technology technology : technologies) {
            removeTechnology(technology);
        }

        super.fromApi(specificationApi, entityManager, domain);
    }
}
