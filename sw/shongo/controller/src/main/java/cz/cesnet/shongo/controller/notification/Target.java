package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.booking.alias.AliasReservation;
import cz.cesnet.shongo.controller.booking.alias.AliasSetSpecification;
import cz.cesnet.shongo.controller.booking.alias.AliasSpecification;
import cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.booking.Allocation;
import cz.cesnet.shongo.controller.booking.reservation.Reservation;
import cz.cesnet.shongo.controller.booking.reservation.ReservationManager;
import cz.cesnet.shongo.controller.booking.resource.DeviceResource;
import cz.cesnet.shongo.controller.booking.resource.ResourceReservation;
import cz.cesnet.shongo.controller.booking.room.*;
import cz.cesnet.shongo.controller.booking.value.ValueReservation;
import cz.cesnet.shongo.controller.booking.value.ValueSpecification;
import cz.cesnet.shongo.controller.booking.EntityIdentifier;
import cz.cesnet.shongo.controller.booking.room.settting.H323RoomSetting;
import cz.cesnet.shongo.controller.booking.room.settting.RoomSetting;
import cz.cesnet.shongo.controller.booking.specification.Specification;
import cz.cesnet.shongo.controller.booking.executable.Executable;
import cz.cesnet.shongo.controller.scheduler.SchedulerContext;
import org.joda.time.Interval;

import javax.persistence.EntityManager;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Represents an target in {@link Notification}s which is requested by a reservation request (e.g., it's specification )
 * or which is allocated by a reservation.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class Target
{
    private String type;

    protected String resourceId;

    protected String resourceName;

    private Target()
    {
    }

    private Target(cz.cesnet.shongo.controller.booking.resource.Resource resource)
    {
        setResource(resource);
    }

    public String getType()
    {
        if (type == null) {
            type = getTypeName();
        }
        return type;
    }

    public String getResourceId()
    {
        return resourceId;
    }

    public String getResourceName()
    {
        return resourceName;
    }

    protected void setResource(cz.cesnet.shongo.controller.booking.resource.Resource resource)
    {
        resourceId = EntityIdentifier.formatId(resource);
        resourceName = resource.getName();
    }

    protected String getTypeName()
    {
        return getClass().getSimpleName().toLowerCase();
    }

    public static class Resource extends Target
    {
        private Resource(ResourceReservation resourceReservation)
        {
            setResource(resourceReservation.getResource());
        }
    }

    public static class Value extends Target
    {
        private Set<String> values = new HashSet<String>();

        public Value(ValueSpecification specification)
        {
            values.addAll(specification.getValues());
        }

        public Value(ValueReservation reservation)
        {
            super(reservation.getValueProvider().getCapabilityResource());

            values.add(reservation.getValue());
        }

        public Set<String> getValues()
        {
            return values;
        }
    }

    public static class Alias extends Target
    {
        private Set<Technology> technologies = new HashSet<Technology>();

        private List<cz.cesnet.shongo.controller.booking.alias.Alias> aliases =
                new LinkedList<cz.cesnet.shongo.controller.booking.alias.Alias>();

        public Alias(AliasSpecification aliasSpecification)
        {
            initFrom(aliasSpecification);
        }

        public Alias(AliasSetSpecification specification)
        {
            for (AliasSpecification aliasSpecification : specification.getAliasSpecifications()) {
                initFrom(aliasSpecification);
            }
        }

        public Alias(AliasReservation aliasReservation)
        {
            super(aliasReservation.getAliasProviderCapability().getResource());

            initFrom(aliasReservation);
        }

        public Alias(List<AliasReservation> aliasReservations)
        {
            for (AliasReservation aliasReservation : aliasReservations) {
                initFrom(aliasReservation);
            }
        }

        private void initFrom(AliasSpecification aliasSpecification)
        {
            Set<AliasType> aliasTypes = aliasSpecification.getAliasTypes();
            if (aliasTypes.size() == 1) {
                cz.cesnet.shongo.controller.booking.alias.Alias alias = new cz.cesnet.shongo.controller.booking.alias.Alias();
                alias.setType(aliasTypes.iterator().next());
                alias.setValue(aliasSpecification.getValue());
                aliases.add(alias);
            }
            technologies.addAll(aliasSpecification.getTechnologies());
            if (technologies.isEmpty()) {
                for (cz.cesnet.shongo.controller.booking.alias.Alias alias : aliases) {
                    Technology technology = alias.getTechnology();
                    if (!technology.equals(Technology.ALL)) {
                        technologies.add(technology);
                    }
                }
            }
        }

        private void initFrom(AliasReservation aliasReservation)
        {
            for (cz.cesnet.shongo.controller.booking.alias.Alias alias : aliasReservation.getAliases()) {
                aliases.add(alias);
                Technology technology = alias.getTechnology();
                if (!technology.equals(Technology.ALL)) {
                    technologies.add(technology);
                }
            }
        }

        public Set<Technology> getTechnologies()
        {
            return technologies;
        }

        public List<cz.cesnet.shongo.controller.booking.alias.Alias> getAliases()
        {
            return aliases;
        }

        public String getRoomName()
        {
            for (cz.cesnet.shongo.controller.booking.alias.Alias alias : aliases) {
                if (alias.getType().equals(AliasType.ROOM_NAME)) {
                    return alias.getValue();
                }
            }
            return null;
        }

        @Override
        protected String getTypeName()
        {
            return super.getTypeName();
        }
    }

    public static class Room extends Target
    {
        private Room reusedRoom;

        private Set<Technology> technologies = new HashSet<Technology>();

        private String name;

        private int licenseCount = 0;

        private AvailableLicenseCountHandler availableLicenseCountHandler;

        private String pin;

        private List<cz.cesnet.shongo.controller.booking.alias.Alias> aliases =
                new LinkedList<cz.cesnet.shongo.controller.booking.alias.Alias>();

        public Room(RoomSpecification roomSpecification, Target reusedTarget, EntityManager entityManager)
        {
            Integer participantCount = roomSpecification.getParticipantCount();
            if (participantCount != null) {
                licenseCount = participantCount;
            }
            for (AliasSpecification aliasSpecification : roomSpecification.getAliasSpecifications()) {
                if (aliasSpecification.getAliasTypes().contains(AliasType.ROOM_NAME)) {
                    name = aliasSpecification.getValue();
                }
                Set<Technology> aliasSpecificationTechnologies = aliasSpecification.getAliasTechnologies();
                if (aliasSpecificationTechnologies.size() > 0) {
                    technologies.addAll(aliasSpecificationTechnologies);
                }
                else {
                    for (AliasType aliasType : aliasSpecification.getAliasTypes()) {
                        Technology technology = aliasType.getTechnology();
                        if (!technology.equals(Technology.ALL)) {
                            technologies.add(technology);
                        }
                    }
                }
            }
            Set<Technology> roomSpecificationTechnologies = roomSpecification.getTechnologies();
            if (roomSpecificationTechnologies.size() > 0) {
                technologies.clear();
                technologies.addAll(roomSpecificationTechnologies);
            }
            for (RoomSetting roomSetting : roomSpecification.getRoomSettings()) {
                if (roomSetting instanceof H323RoomSetting) {
                    H323RoomSetting h323RoomSetting = (H323RoomSetting) roomSetting;
                    if (h323RoomSetting.getPin() != null) {
                        pin = h323RoomSetting.getPin();
                    }
                }
            }
            if (reusedTarget instanceof Room) {
                reusedRoom = (Room) reusedTarget;
                name = reusedRoom.getName();
            }
        }

        public Room(RoomReservation reservation, EntityManager entityManager)
        {
            super(reservation.getDeviceResource());

            RoomEndpoint roomEndpoint = (RoomEndpoint) reservation.getExecutable();
            if (roomEndpoint != null) {
                initFrom(roomEndpoint, entityManager);
            }
            else {
                licenseCount = reservation.getLicenseCount();
                technologies.addAll(reservation.getDeviceResource().getTechnologies());
                initFrom(reservation.getSlot(), reservation.getRoomProviderCapability(), entityManager);
            }
        }

        public Room(RoomEndpoint roomEndpoint, EntityManager entityManager)
        {
            DeviceResource deviceResource = initFrom(roomEndpoint, entityManager);
            setResource(deviceResource);
        }

        private void initFrom(final Interval slot, final RoomProviderCapability roomProviderCapability,
                final EntityManager entityManager)
        {
            if (licenseCount > 0) {
                availableLicenseCountHandler = new AvailableLicenseCountHandler()
                {
                    @Override
                    public int getAvailableLicenseCount()
                    {
                        int availableLicenseCount = roomProviderCapability.getLicenseCount();

                        ReservationManager reservationManager = new ReservationManager(entityManager);
                        List<RoomReservation> roomReservations =
                                reservationManager.getRoomReservations(roomProviderCapability, slot);
                        for (RoomReservation roomReservation : roomReservations) {
                            availableLicenseCount -= roomReservation.getLicenseCount();
                        }
                        return availableLicenseCount;
                    }
                };
            }
        }

        private DeviceResource initFrom(RoomEndpoint roomEndpoint, EntityManager entityManager)
        {
            RoomConfiguration roomConfiguration = roomEndpoint.getRoomConfiguration();
            licenseCount = roomConfiguration.getLicenseCount();
            technologies.addAll(roomConfiguration.getTechnologies());
            for (RoomSetting roomSetting : roomConfiguration.getRoomSettings()) {
                if (roomSetting instanceof H323RoomSetting) {
                    H323RoomSetting h323RoomSetting = (H323RoomSetting) roomSetting;
                    if (h323RoomSetting.getPin() != null) {
                        pin = h323RoomSetting.getPin();
                    }
                }
            }

            for (cz.cesnet.shongo.controller.booking.alias.Alias alias : roomEndpoint.getAliases()) {
                if (alias.getType().equals(AliasType.ROOM_NAME)) {
                    name = alias.getValue();
                }
                else {
                    aliases.add(alias);
                }
            }
            if (roomEndpoint instanceof ResourceRoomEndpoint) {
                ResourceRoomEndpoint resourceRoomEndpoint = (ResourceRoomEndpoint) roomEndpoint;
                RoomProviderCapability roomProviderCapability = resourceRoomEndpoint.getRoomProviderCapability();
                initFrom(roomEndpoint.getSlot(), roomProviderCapability, entityManager);
                return roomProviderCapability.getDeviceResource();
            }
            else if (roomEndpoint instanceof UsedRoomEndpoint) {
                UsedRoomEndpoint usedRoomEndpoint = (UsedRoomEndpoint) roomEndpoint;
                reusedRoom = new Room(usedRoomEndpoint.getReusedRoomEndpoint(), entityManager);
                RoomProviderCapability roomProviderCapability =
                        usedRoomEndpoint.getResource().getCapabilityRequired(RoomProviderCapability.class);
                initFrom(roomEndpoint.getSlot(), roomProviderCapability, entityManager);
                return roomProviderCapability.getDeviceResource();
            }
            else {
                throw new TodoImplementException(roomEndpoint.getClass());
            }
        }

        public boolean isPermanent()
        {
            return licenseCount == 0 && reusedRoom == null;
        }

        public boolean isCapacity()
        {
            return licenseCount > 0 && reusedRoom != null;
        }

        public String getName()
        {
            return name;
        }

        public int getLicenseCount()
        {
            return licenseCount;
        }

        public int getAvailableLicenseCount()
        {
            return availableLicenseCountHandler != null ? availableLicenseCountHandler.getAvailableLicenseCount() : 0;
        }

        public Set<Technology> getTechnologies()
        {
            return technologies;
        }

        public List<cz.cesnet.shongo.controller.booking.alias.Alias> getAliases()
        {
            return aliases;
        }

        public String getPin()
        {
            return pin;
        }

        @Override
        protected String getTypeName()
        {
            if (isPermanent()) {
                return "roomPermanent";
            }
            else if (isCapacity()) {
                return "roomCapacity";
            }
            else {
                return super.getTypeName();
            }
        }

        private static interface AvailableLicenseCountHandler
        {
            public int getAvailableLicenseCount();
        }
    }

    public static class Other extends Target
    {
        private String description;

        public Other(Specification specification)
        {
            description = specification.getClass().getSimpleName();
        }

        public Other(Reservation reservation)
        {
            description = reservation.getClass().getSimpleName();
            Executable executable = reservation.getExecutable();
            if (executable != null) {
                description += " (" + executable.getClass().getSimpleName() + ")";
            }
        }

        public String getDescription()
        {
            return description;
        }
    }

    public static Target createInstance(AbstractReservationRequest reservationRequest, EntityManager entityManager)
    {
        Specification specification = reservationRequest.getSpecification();
        if (specification instanceof ValueSpecification) {
            return new Value((ValueSpecification) specification);
        }
        else if (specification instanceof AliasSpecification) {
            return new Alias((AliasSpecification) specification);
        }
        else if (specification instanceof AliasSetSpecification) {
            return new Alias((AliasSetSpecification) specification);
        }
        else if (specification instanceof RoomSpecification) {
            Target reusedTarget = null;
            Allocation reusedAllocation = reservationRequest.getReusedAllocation();
            if (reusedAllocation != null) {
                Reservation reusedReservation = reusedAllocation.getCurrentReservation();
                if (reusedReservation != null) {
                    reusedTarget = createInstance(reusedReservation, entityManager);
                }
            }
            return new Room((RoomSpecification) specification, reusedTarget, entityManager);
        }
        else {
            return new Other(specification);
        }
    }

    public static Target createInstance(Reservation reservation, EntityManager entityManager)
    {
        if (reservation instanceof ValueReservation) {
            return new Value((ValueReservation) reservation);
        }
        else if (reservation instanceof AliasReservation) {
            return new Alias((AliasReservation) reservation);
        }
        else if (reservation instanceof RoomReservation) {
            return new Room((RoomReservation) reservation, entityManager);
        }
        else if (reservation instanceof ResourceReservation) {
            return new Resource((ResourceReservation) reservation);
        }
        else {
            Executable executable = reservation.getExecutable();
            if (executable instanceof RoomEndpoint) {
                return new Room((RoomEndpoint) executable, entityManager);
            }
            // Check if all child reservations have same class
            List<Reservation> childReservations = reservation.getChildReservations();
            Class<? extends Reservation> sameChildReservationClass = null;
            for (Reservation childReservation : childReservations) {
                if (sameChildReservationClass != null) {
                    if (!childReservation.getClass().equals(sameChildReservationClass)) {
                        // Children have different classes
                        sameChildReservationClass = null;
                        break;
                    }
                }
                else {
                    sameChildReservationClass = childReservation.getClass();
                }
            }
            if (AliasReservation.class.equals(sameChildReservationClass)) {
                @SuppressWarnings("unchecked")
                List<AliasReservation> childAliasReservations = (List) childReservations;
                return new Alias(childAliasReservations);
            }
            else {
                return new Other(reservation);
            }
        }
    }
}
