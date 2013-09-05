package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.request.*;
import cz.cesnet.shongo.controller.reservation.*;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.Resource;
import cz.cesnet.shongo.controller.resource.RoomProviderCapability;

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
public class Target
{
    protected String resourceId;

    protected String resourceName;

    private Target()
    {
    }

    public String getResourceId()
    {
        return resourceId;
    }

    public String getResourceName()
    {
        return resourceName;
    }

    protected void setResource(Resource resource)
    {
        resourceId = EntityIdentifier.formatId(resource);
        resourceName = resource.getName();
    }

    public String getType()
    {
        return getClass().getSimpleName().toLowerCase();
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
            setResource(reservation.getValueProvider().getCapabilityResource());
            values.add(reservation.getValue());
        }
    }

    public static class Alias extends Target
    {
        private boolean permanentRoom;

        private Set<Technology> technologies = new HashSet<Technology>();

        private List<Alias> aliases = new LinkedList<Alias>();

        public Alias(AliasSpecification specification)
        {
            //To change body of created methods use File | Settings | File Templates.
        }

        public Alias(AliasSetSpecification specification)
        {
            //To change body of created methods use File | Settings | File Templates.
        }

        public Alias(AliasReservation reservation)
        {
            //To change body of created methods use File | Settings | File Templates.
        }

        public Alias(List<AliasReservation> aliasReservations)
        {
            //To change body of created methods use File | Settings | File Templates.
        }
    }

    public static class Room extends Target
    {
        private Alias alias;

        private Set<Technology> technologies = new HashSet<Technology>();

        private int licenseCount;

        private int availableLicenseCount;

        private List<Alias> aliases = new LinkedList<Alias>();

        public Room(RoomSpecification specification)
        {
            licenseCount = specification.getParticipantCount();
        }

        public Room(RoomReservation reservation, EntityManager entityManager)
        {
            setResource(reservation.getDeviceResource());

            RoomProviderCapability roomProviderCapability = reservation.getRoomProviderCapability();
            licenseCount = reservation.getRoomConfiguration().getLicenseCount();
            availableLicenseCount = roomProviderCapability.getLicenseCount();

            ReservationManager reservationManager = new ReservationManager(entityManager);
            List<RoomReservation> roomReservations =
                    reservationManager.getRoomReservations(roomProviderCapability, reservation.getSlot());
            for (RoomReservation roomReservation : roomReservations) {
                availableLicenseCount -= roomReservation.getRoomConfiguration().getLicenseCount();
            }
        }

        public int getLicenseCount()
        {
            return licenseCount;
        }

        public int getAvailableLicenseCount()
        {
            return availableLicenseCount;
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
            throw new TodoImplementException(specification.getClass());
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
                throw new TodoImplementException(reservation.getClass());
            }
        }
    }
}
