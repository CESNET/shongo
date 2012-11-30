package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.common.Room;
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
import java.util.Set;

/**
 * Represents a {@link Specification} for {@link Room}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class RoomSpecification extends Specification implements ReservationTaskProvider
{
    /**
     * {@link Room} which is specified.
     */
    private Room room = new Room();

    /**
     * {@link DeviceResource} with {@link RoomProviderCapability} in which the {@link Room} should be allocated.
     */
    private DeviceResource deviceResource;

    /**
     * Specifies whether {@link Alias} should be allocated for each {@link Technology}
     * from {@link #room#getTechnologies()}.
     */
    private boolean withAlias;

    /**
     * Constructor.
     */
    public RoomSpecification()
    {
    }

    /**
     * @return {@link #room}
     */
    @Embedded
    @Access(AccessType.FIELD)
    public Room getRoom()
    {
        return room;
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

    @Override
    public boolean synchronizeFrom(Specification specification)
    {
        RoomSpecification roomSpecification = (RoomSpecification) specification;

        boolean modified = super.synchronizeFrom(specification);
        modified |= room.synchronizeFrom(roomSpecification.getRoom());
        modified |= !ObjectUtils.equals(isWithAlias(), roomSpecification.isWithAlias());
        modified |= !ObjectUtils.equals(getDeviceResource(), roomSpecification.getDeviceResource());

        setWithAlias(roomSpecification.isWithAlias());
        setDeviceResource(roomSpecification.getDeviceResource());

        return modified;
    }

    @Override
    public ReservationTask createReservationTask(ReservationTask.Context context)
    {
        RoomReservationTask roomReservationTask = new RoomReservationTask(context);
        roomReservationTask.addRoomVariant(getRoom());
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
        cz.cesnet.shongo.controller.api.RoomSpecification virtualRoomSpecificationApi =
                (cz.cesnet.shongo.controller.api.RoomSpecification) specificationApi;
        for (Technology technology : room.getTechnologies()) {
            virtualRoomSpecificationApi.addTechnology(technology);
        }
        virtualRoomSpecificationApi.setParticipantCount(room.getParticipantCount());
        virtualRoomSpecificationApi.setWithAlias(isWithAlias());
        if (deviceResource != null) {
            virtualRoomSpecificationApi.setResourceIdentifier(domain.formatIdentifier(deviceResource.getId()));
        }
        super.toApi(specificationApi, domain);
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.Specification specificationApi, EntityManager entityManager,
            Domain domain) throws FaultException
    {
        cz.cesnet.shongo.controller.api.RoomSpecification virtualRoomSpecificationApi =
                (cz.cesnet.shongo.controller.api.RoomSpecification) specificationApi;
        if (virtualRoomSpecificationApi.isPropertyFilled(virtualRoomSpecificationApi.PARTICIPANT_COUNT)) {
            room.setParticipantCount(virtualRoomSpecificationApi.getParticipantCount());
        }
        if (virtualRoomSpecificationApi.isPropertyFilled(virtualRoomSpecificationApi.WITH_ALIAS)) {
            setWithAlias(virtualRoomSpecificationApi.getWithAlias());
        }
        if (virtualRoomSpecificationApi.isPropertyFilled(virtualRoomSpecificationApi.RESOURCE_IDENTIFIER)) {
            if (virtualRoomSpecificationApi.getResourceIdentifier() == null) {
                setDeviceResource(null);
            }
            else {
                Long resourceId = domain.parseIdentifier(virtualRoomSpecificationApi.getResourceIdentifier());
                ResourceManager resourceManager = new ResourceManager(entityManager);
                setDeviceResource(resourceManager.getDevice(resourceId));
            }
        }

        // Create technologies
        for (Technology technology : virtualRoomSpecificationApi.getTechnologies()) {
            if (specificationApi.isPropertyItemMarkedAsNew(
                    cz.cesnet.shongo.controller.api.DeviceResource.TECHNOLOGIES, technology)) {
                room.addTechnology(technology);
            }
        }
        // Delete technologies
        Set<Technology> technologies = specificationApi.getPropertyItemsMarkedAsDeleted(
                cz.cesnet.shongo.controller.api.DeviceResource.TECHNOLOGIES);
        for (Technology technology : technologies) {
            room.removeTechnology(technology);
        }

        super.fromApi(specificationApi, entityManager, domain);
    }
}
