package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.ObjectType;
import cz.cesnet.shongo.controller.booking.Allocation;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.alias.AliasReservation;
import cz.cesnet.shongo.controller.booking.alias.AliasSetSpecification;
import cz.cesnet.shongo.controller.booking.alias.AliasSpecification;
import cz.cesnet.shongo.controller.booking.domain.Domain;
import cz.cesnet.shongo.controller.booking.executable.Executable;
import cz.cesnet.shongo.controller.booking.recording.RecordingCapability;
import cz.cesnet.shongo.controller.booking.recording.RecordingServiceReservation;
import cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.booking.reservation.ExistingReservation;
import cz.cesnet.shongo.controller.booking.reservation.ForeignRoomReservation;
import cz.cesnet.shongo.controller.booking.reservation.Reservation;
import cz.cesnet.shongo.controller.booking.reservation.ReservationManager;
import cz.cesnet.shongo.controller.booking.resource.DeviceResource;
import cz.cesnet.shongo.controller.booking.resource.ForeignResourceReservation;
import cz.cesnet.shongo.controller.booking.resource.ResourceReservation;
import cz.cesnet.shongo.controller.booking.resource.ResourceSpecification;
import cz.cesnet.shongo.controller.booking.room.*;
import cz.cesnet.shongo.controller.booking.room.settting.FreePBXRoomSetting;
import cz.cesnet.shongo.controller.booking.room.settting.H323RoomSetting;
import cz.cesnet.shongo.controller.booking.room.settting.PexipRoomSetting;
import cz.cesnet.shongo.controller.booking.room.settting.RoomSetting;
import cz.cesnet.shongo.controller.booking.specification.Specification;
import cz.cesnet.shongo.controller.booking.value.ValueReservation;
import cz.cesnet.shongo.controller.booking.value.ValueSpecification;
import org.joda.time.DateTime;
import org.joda.time.Hours;
import org.joda.time.Interval;
import org.joda.time.Period;

import javax.persistence.EntityManager;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Represents an target in {@link AbstractNotification}s which is requested by a reservation request (e.g., it's specification)
 * or which is allocated by a reservation.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class Target
{
    protected String executableId;

    protected String type;

    protected String resourceId;

    protected String resourceName;

    private Target()
    {
    }

    private Target(cz.cesnet.shongo.controller.booking.resource.Resource resource)
    {
        setResource(resource);
    }

    public String getExecutableId()
    {
        return executableId;
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
        resourceId = ObjectIdentifier.formatId(resource);
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

        private Resource(cz.cesnet.shongo.controller.booking.resource.Resource resource)
        {
            setResource(resource);
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
            super(reservation.getAllocatedResource());

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
            super(aliasReservation.getAllocatedResource());

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
            cz.cesnet.shongo.controller.booking.alias.Alias.sort(aliases);
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
        private Period slotBefore;

        private Period slotAfter;

        private Room reusedRoom;

        private Set<Technology> technologies = new HashSet<Technology>();

        private String name;

        private int licenseCount = 0;

        private AvailableLicenseCountHandler availableLicenseCountHandler;

        private String pin;

        private String userPin;

        private String adminPin;

        private List<cz.cesnet.shongo.controller.booking.alias.Alias> aliases =
                new LinkedList<cz.cesnet.shongo.controller.booking.alias.Alias>();

        public Room(RoomSpecification roomSpecification, Target reusedTarget, EntityManager entityManager)
        {
            slotBefore = (roomSpecification.getSlotMinutesBefore() > 0 ?
                                  Period.minutes(roomSpecification.getSlotMinutesBefore()) : null);
            slotAfter = (roomSpecification.getSlotMinutesAfter() > 0 ?
                                 Period.minutes(roomSpecification.getSlotMinutesAfter()) : null);

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
                } else if (roomSetting instanceof FreePBXRoomSetting) {
                    FreePBXRoomSetting freePBXRoomSetting = (FreePBXRoomSetting) roomSetting;
                    if (freePBXRoomSetting.getAdminPin() != null) {
                        userPin = freePBXRoomSetting.getUserPin();
                        adminPin = freePBXRoomSetting.getAdminPin();
                    }
                } else if (roomSetting instanceof PexipRoomSetting) {
                    PexipRoomSetting pexipRoomSetting = (PexipRoomSetting) roomSetting;
                    if (pexipRoomSetting.getGuestPin() != null) {
                        userPin = pexipRoomSetting.getGuestPin();
                    }
                    if (pexipRoomSetting.getHostPin() != null) {
                        adminPin = pexipRoomSetting.getHostPin();
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
            super(reservation.getAllocatedResource());

            RoomEndpoint roomEndpoint = (RoomEndpoint) reservation.getExecutable();
            if (roomEndpoint != null) {
                initFrom(roomEndpoint, entityManager);
            }
            else {
                licenseCount = reservation.getLicenseCount();
                technologies.addAll(reservation.getAllocatedResource().getTechnologies());
                initFrom(reservation.getSlot(), reservation.getRoomProviderCapability(), entityManager);
            }
        }

        public Room(RoomEndpoint roomEndpoint, EntityManager entityManager)
        {
            DeviceResource deviceResource = initFrom(roomEndpoint, entityManager);
            setResource(deviceResource);
        }

        private DeviceResource initFrom(RoomEndpoint roomEndpoint, EntityManager entityManager)
        {
            RoomConfiguration roomConfiguration = roomEndpoint.getRoomConfiguration();
            executableId = ObjectIdentifier.formatId(roomEndpoint);
            slotBefore = (roomEndpoint.getSlotMinutesBefore() > 0 ?
                                  Period.minutes(roomEndpoint.getSlotMinutesBefore()) : null);
            slotAfter = (roomEndpoint.getSlotMinutesAfter() > 0 ?
                                 Period.minutes(roomEndpoint.getSlotMinutesAfter()) : null);
            licenseCount = roomConfiguration.getLicenseCount();
            technologies.addAll(roomConfiguration.getTechnologies());
            for (RoomSetting roomSetting : roomConfiguration.getRoomSettings()) {
                if (roomSetting instanceof H323RoomSetting) {
                    H323RoomSetting h323RoomSetting = (H323RoomSetting) roomSetting;
                    if (h323RoomSetting.getPin() != null) {
                        pin = h323RoomSetting.getPin();
                    }
                } else if (roomSetting instanceof FreePBXRoomSetting) {
                    FreePBXRoomSetting freePBXRoomSetting = (FreePBXRoomSetting) roomSetting;
                    if (freePBXRoomSetting.getAdminPin() != null) {
                        userPin = freePBXRoomSetting.getUserPin();
                        adminPin = freePBXRoomSetting.getAdminPin();
                    }
                } else if (roomSetting instanceof PexipRoomSetting) {
                    PexipRoomSetting pexipRoomSetting = (PexipRoomSetting) roomSetting;
                    if (pexipRoomSetting.getHostPin() != null) {
                        adminPin = pexipRoomSetting.getHostPin();
                    }
                    if (pexipRoomSetting.getGuestPin() != null) {
                        userPin = pexipRoomSetting.getGuestPin();
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
            cz.cesnet.shongo.controller.booking.alias.Alias.sort(aliases);
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

                        DateTime startDate = slot.getStart().hourOfDay().roundFloorCopy();
                        DateTime endDate = slot.getEnd().hourOfDay().roundCeilingCopy();

                        int hours = Hours.hoursBetween(startDate, endDate).getHours();
                        short licencesHourly[] = new short[hours];

                        for (RoomReservation roomReservation : roomReservations) {
                            int startingHour = Hours.hoursBetween(startDate, roomReservation.getSlotStart()).getHours();
                            int endingHour = Hours.hoursBetween(startDate, roomReservation.getSlotEnd()).getHours();
                            // do not count another hour if reservation ends on the hour
                            if(roomReservation.getSlotEnd().getMinuteOfHour() == 0) {
                                endingHour--;
                            }

                            //if reservation starts/ends before/after the main reservation set range to border values
                            if (startingHour < 0) {
                                startingHour = 0;
                            }
                            if (endingHour >= hours) {
                                endingHour = hours-1;
                            }

                            for (int i = startingHour; i <= endingHour; i++) {
                                licencesHourly[i] += roomReservation.getLicenseCount();
                            }
                        }

                        short maxLicences = 0;

                        for (short n : licencesHourly) {
                            if (n > maxLicences) {
                                maxLicences = n;
                            }
                            if (maxLicences == availableLicenseCount) {
                                break;
                            }
                        }

                        availableLicenseCount -= maxLicences;
                        return availableLicenseCount;
                    }
                };
            }
        }

        public Period getSlotBefore()
        {
            return slotBefore;
        }

        public Period getSlotAfter()
        {
            return slotAfter;
        }

        public Room getReusedRoom()
        {
            return reusedRoom;
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

        public String getUserPin() { return userPin; }

        public String getAdminPin() { return adminPin; }

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

    public static class Reused extends Target
    {
        private String reusedReservationId;

        private Reused(ExistingReservation existingReservation)
        {
            reusedReservationId = ObjectIdentifier.formatId(existingReservation.getReusedReservation());
        }

        public String getReusedReservationId()
        {
            return reusedReservationId;
        }
    }

    public static class RecordingService extends Target
    {
        private RecordingServiceReservation reservation;

        private ReservationManager reservationManager;

        public RecordingService(final RecordingServiceReservation reservation, EntityManager entityManager)
        {
            super(reservation.getAllocatedResource());

            this.reservation = reservation;
            reservationManager = new ReservationManager(entityManager);
        }

        public int getAvailableLicenseCount()
        {
            int availableLicenseCount = reservation.getRecordingCapability().getLicenseCount();

            Interval slot = reservation.getSlot();
            RecordingCapability recordingCapability = reservation.getRecordingCapability();

            List<RecordingServiceReservation> recordingServiceReservations =
                    reservationManager.getRecordingServiceReservations(recordingCapability,slot);
            for (RecordingServiceReservation recordingReservation : recordingServiceReservations) {
                availableLicenseCount -= 1;
            }
            return availableLicenseCount;
        }
    }

    public static class ForeignRoom extends Target
    {
        private Set<Technology> technologies = new HashSet<Technology>();

        private String name;

        private int licenseCount = 0;

        private String pin;

        private List<cz.cesnet.shongo.controller.booking.alias.Alias> aliases =
                new LinkedList<cz.cesnet.shongo.controller.booking.alias.Alias>();

        public ForeignRoom(ForeignRoomReservation foreignRoomReservation)
        {
            ForeignRoomEndpoint foreignRoomEndpoint = foreignRoomReservation.getForeignEndpoint();
            if (foreignRoomEndpoint != null) {
                this.technologies = foreignRoomEndpoint.getTechnologies();
                this.name = foreignRoomEndpoint.getRoomName();
                this.licenseCount = foreignRoomEndpoint.getLicenseCount();
                this.pin = foreignRoomEndpoint.getPin();
                this.aliases = foreignRoomEndpoint.getAliases();
            }
        }

        public Set<Technology> getTechnologies()
        {
            return technologies;
        }

        public void setTechnologies(Set<Technology> technologies)
        {
            this.technologies = technologies;
        }

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public int getLicenseCount()
        {
            return licenseCount;
        }

        public void setLicenseCount(int licenseCount)
        {
            this.licenseCount = licenseCount;
        }

        public String getPin()
        {
            return pin;
        }

        public void setPin(String pin)
        {
            this.pin = pin;
        }

        public List<cz.cesnet.shongo.controller.booking.alias.Alias> getAliases()
        {
            return aliases;
        }

        public void setAliases(List<cz.cesnet.shongo.controller.booking.alias.Alias> aliases)
        {
            this.aliases = aliases;
        }
    }

    public static class ForeignResource extends Target
    {
        private ForeignResource(ForeignResourceReservation foreignResourceReservation)
        {
            Long id = foreignResourceReservation.getForeignResources().getForeignResourceId();
            Domain domain = foreignResourceReservation.getForeignResources().getDomain();
            String  resourceId = ObjectIdentifier.formatId(domain.getName(), ObjectType.RESOURCE, id);
            this.resourceId = resourceId;
            this.resourceName = foreignResourceReservation.getResourceName();
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
        else if (specification instanceof ResourceSpecification) {
            return new Resource(((ResourceSpecification) specification).getResource());
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
        else if (reservation instanceof ExistingReservation) {
            return new Reused((ExistingReservation) reservation);
        }
        else if (reservation instanceof RecordingServiceReservation) {
            return new RecordingService((RecordingServiceReservation) reservation, entityManager);
        }
        else if (reservation instanceof ForeignRoomReservation) {
            return new ForeignRoom((ForeignRoomReservation) reservation);
        }
        else if (reservation instanceof ForeignResourceReservation) {
            return new ForeignResource((ForeignResourceReservation) reservation);
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
