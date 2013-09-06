package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.executor.Executable;
import cz.cesnet.shongo.controller.executor.RoomEndpoint;
import cz.cesnet.shongo.controller.request.*;
import cz.cesnet.shongo.controller.reservation.*;
import cz.cesnet.shongo.controller.resource.Alias;
import cz.cesnet.shongo.controller.resource.RoomProviderCapability;
import org.hibernate.engine.internal.StatefulPersistenceContext;

import javax.persistence.EntityManager;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * TODO:
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

    private Target(cz.cesnet.shongo.controller.resource.Resource resource)
    {
        setResource(resource);
    }

    public String getType()
    {
        if (type == null) {
            type = getClass().getSimpleName().toLowerCase();
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

    protected void setResource(cz.cesnet.shongo.controller.resource.Resource resource)
    {
        resourceId = EntityIdentifier.formatId(resource);
        resourceName = resource.getName();
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
    }

    public static class Alias extends Target
    {
        private boolean permanentRoom;

        private Set<Technology> technologies = new HashSet<Technology>();

        private List<cz.cesnet.shongo.controller.resource.Alias> aliases =
                new LinkedList<cz.cesnet.shongo.controller.resource.Alias>();

        public Alias(AliasSpecification specification)
        {
        }

        public Alias(AliasSetSpecification specification)
        {
        }

        public Alias(AliasReservation reservation)
        {
            super(reservation.getAliasProviderCapability().getResource());

            for (cz.cesnet.shongo.controller.resource.Alias alias : reservation.getAliases()) {
                aliases.add(alias);
            }
        }

        public Alias(List<AliasReservation> aliasReservations)
        {
        }

        public List<cz.cesnet.shongo.controller.resource.Alias> getAliases()
        {
            return aliases;
        }
    }

    public static class Room extends Target
    {
        private Alias alias;

        private Set<Technology> technologies = new HashSet<Technology>();

        private String name;

        private int licenseCount;

        private int availableLicenseCount;

        private List<cz.cesnet.shongo.controller.resource.Alias> aliases =
                new LinkedList<cz.cesnet.shongo.controller.resource.Alias>();

        public Room(RoomSpecification specification)
        {
            licenseCount = specification.getParticipantCount();
        }

        public Room(RoomReservation reservation, EntityManager entityManager)
        {
            super(reservation.getDeviceResource());

            RoomProviderCapability roomProviderCapability = reservation.getRoomProviderCapability();
            licenseCount = reservation.getRoomConfiguration().getLicenseCount();
            availableLicenseCount = roomProviderCapability.getLicenseCount();

            ReservationManager reservationManager = new ReservationManager(entityManager);
            List<RoomReservation> roomReservations =
                    reservationManager.getRoomReservations(roomProviderCapability, reservation.getSlot());
            for (RoomReservation roomReservation : roomReservations) {
                availableLicenseCount -= roomReservation.getRoomConfiguration().getLicenseCount();
            }

            RoomEndpoint roomEndpoint = (RoomEndpoint) reservation.getExecutable();
            if (roomEndpoint != null) {
                for (cz.cesnet.shongo.controller.resource.Alias alias : roomEndpoint.getAliases()) {
                    if (alias.getType().equals(AliasType.ROOM_NAME)) {
                        name = alias.getValue();
                    }
                    else {
                        aliases.add(alias);
                    }
                }
            }
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
            return availableLicenseCount;
        }

        public List<cz.cesnet.shongo.controller.resource.Alias> getAliases()
        {
            return aliases;
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

    public static Target createInstance(Specification specification)
    {
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
            return new Room((RoomSpecification) specification);
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
            List<Reservation> childReservations = reservation.getChildReservations();

            // Check if all child reservations have same class
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
