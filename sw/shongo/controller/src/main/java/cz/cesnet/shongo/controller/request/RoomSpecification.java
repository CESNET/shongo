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
    private List<RoomSetting> roomConfigurations = new ArrayList<RoomSetting>();

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
     * @return {@link #roomConfigurations}
     */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @Access(AccessType.FIELD)
    public List<RoomSetting> getRoomConfigurations()
    {
        return roomConfigurations;
    }

    /**
     * @param roomConfigurations sets the {@link #roomConfigurations}
     */
    public void setRoomConfigurations(List<RoomSetting> roomConfigurations)
    {
        this.roomConfigurations.clear();
        for ( RoomSetting roomConfiguration : roomConfigurations) {
            this.roomConfigurations.add(roomConfiguration);
        }
    }

    /**
     * @param roomConfiguration to be added to the {@link #roomConfigurations}
     */
    public void addRoomConfiguration(RoomSetting roomConfiguration)
    {
        roomConfigurations.add(roomConfiguration);
    }

    /**
     * @param roomConfiguration to be removed from the {@link #roomConfigurations}
     */
    public void removeRoomConfiguration(RoomSetting roomConfiguration)
    {
        roomConfigurations.remove(roomConfiguration);
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

        if (!roomConfigurations.equals(roomSpecification.getRoomConfigurations())) {
            setRoomConfigurations(roomSpecification.getRoomConfigurations());
            modified = true;
        }

        return modified;
    }

    @Override
    public ReservationTask createReservationTask(ReservationTask.Context context)
    {
        RoomReservationTask roomReservationTask = new RoomReservationTask(context, getParticipantCount());
        roomReservationTask.addTechnologyVariant(getTechnologies());
        roomReservationTask.addRoomSettings(getRoomConfigurations());
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
        for (Technology technology : getTechnologies()) {
            roomSpecificationApi.addTechnology(technology);
        }
        roomSpecificationApi.setParticipantCount(getParticipantCount());
        roomSpecificationApi.setWithAlias(isWithAlias());
        if (deviceResource != null) {
            roomSpecificationApi.setResourceIdentifier(domain.formatIdentifier(deviceResource.getId()));
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
        if (roomSpecificationApi.isPropertyFilled(roomSpecificationApi.RESOURCE_IDENTIFIER)) {
            if (roomSpecificationApi.getResourceIdentifier() == null) {
                setDeviceResource(null);
            }
            else {
                Long resourceId = domain.parseIdentifier(roomSpecificationApi.getResourceIdentifier());
                ResourceManager resourceManager = new ResourceManager(entityManager);
                setDeviceResource(resourceManager.getDevice(resourceId));
            }
        }

        // Create technologies
        for (Technology technology : roomSpecificationApi.getTechnologies()) {
            if (specificationApi.isPropertyItemMarkedAsNew(
                    cz.cesnet.shongo.controller.api.DeviceResource.TECHNOLOGIES, technology)) {
                addTechnology(technology);
            }
        }
        // Delete technologies
        Set<Technology> technologies = specificationApi.getPropertyItemsMarkedAsDeleted(
                cz.cesnet.shongo.controller.api.DeviceResource.TECHNOLOGIES);
        for (Technology technology : technologies) {
            removeTechnology(technology);
        }

        super.fromApi(specificationApi, entityManager, domain);
    }
}
