package cz.cesnet.shongo.controller.booking.room;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.controller.AbstractControllerTest;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.ReservationRequestReusement;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.AbstractPerson;
import cz.cesnet.shongo.controller.api.AnonymousPerson;
import cz.cesnet.shongo.controller.api.CompartmentSpecification;
import cz.cesnet.shongo.controller.api.ExternalEndpointParticipant;
import cz.cesnet.shongo.controller.api.InvitedPersonParticipant;
import cz.cesnet.shongo.controller.api.ReservationRequest;
import cz.cesnet.shongo.controller.api.ReservationRequestSet;
import cz.cesnet.shongo.controller.api.RoomProviderCapability;
import cz.cesnet.shongo.controller.api.RoomReservation;
import cz.cesnet.shongo.controller.api.RoomSpecification;
import cz.cesnet.shongo.controller.api.request.AvailabilityCheckRequest;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.booking.compartment.*;
import cz.cesnet.shongo.controller.booking.participant.*;
import cz.cesnet.shongo.controller.booking.person.*;
import cz.cesnet.shongo.controller.booking.request.*;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.junit.Assert;
import org.junit.Test;

import javax.persistence.EntityManager;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Tests for allocation of {@link RoomReservation} by {@link RoomSpecification}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class RoomTest extends AbstractControllerTest
{
    /**
     * Test allocation of virtual room.
     *
     * @throws Exception
     */
    @Test
    public void test() throws Exception
    {
        DeviceResource firstMcu = new DeviceResource();
        firstMcu.setName("firstMcu");
        firstMcu.addTechnology(Technology.H323);
        firstMcu.addTechnology(Technology.SIP);
        firstMcu.addCapability(new cz.cesnet.shongo.controller.api.RoomProviderCapability(10));
        firstMcu.setAllocatable(true);
        String firstMcuId = createResource(firstMcu);

        DeviceResource secondMcu = new DeviceResource();
        secondMcu.setName("secondMcu");
        secondMcu.addTechnology(Technology.H323);
        secondMcu.addTechnology(Technology.SIP);
        secondMcu.addCapability(new cz.cesnet.shongo.controller.api.RoomProviderCapability(10));
        secondMcu.setAllocatable(true);
        String secondMcuId = createResource(secondMcu);

        ReservationRequest firstReservationRequest = new ReservationRequest();
        firstReservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        firstReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        RoomSpecification roomSpecification = new RoomSpecification(5);
        RoomEstablishment roomEstablishment = roomSpecification.createEstablishment();
        roomEstablishment.addTechnology(Technology.H323);
        roomEstablishment.addTechnology(Technology.SIP);
        roomEstablishment.setResourceId(secondMcuId);
        firstReservationRequest.setSpecification(roomSpecification);

        RoomReservation firstReservation = (RoomReservation) allocateAndCheck(firstReservationRequest);
        Assert.assertEquals("Virtual room should be allocated on second mcu, because it was specified as preferred",
                secondMcuId, firstReservation.getResourceId());

        ReservationRequest secondReservationRequest = new ReservationRequest();
        secondReservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        secondReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        roomSpecification = new RoomSpecification(3);
        roomEstablishment = roomSpecification.createEstablishment();
        roomEstablishment.addTechnology(Technology.H323);
        roomEstablishment.addTechnology(Technology.SIP);
        roomEstablishment.setResourceId(firstMcuId);
        secondReservationRequest.setSpecification(roomSpecification);

        RoomReservation secondReservation = (RoomReservation) allocateAndCheck(secondReservationRequest);
        Assert.assertEquals("Virtual room should be allocated on first mcu, because it was specified as preferred",
                firstMcuId, secondReservation.getResourceId());

        ReservationRequest thirdReservationRequest = new ReservationRequest();
        thirdReservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        thirdReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        roomSpecification = new RoomSpecification(5);
        roomEstablishment = roomSpecification.createEstablishment();
        roomEstablishment.addTechnology(Technology.H323);
        roomEstablishment.addTechnology(Technology.SIP);
        thirdReservationRequest.setSpecification(roomSpecification);

        RoomReservation thirdReservation = (RoomReservation) allocateAndCheck(thirdReservationRequest);
        Assert.assertEquals("Virtual room should be allocated on second mcu, because it was the most filled",
                secondMcuId, thirdReservation.getResourceId());
    }

    /**
     * Test allocation of virtual room with aliases.
     */
    @Test
    public void testAliases() throws Exception
    {
        Resource firstAliasProvider = new Resource();
        firstAliasProvider.setName("firstAliasProvider");
        AliasProviderCapability aliasProviderCapability = new AliasProviderCapability("1-{digit:1}");
        aliasProviderCapability.addAlias(new Alias(AliasType.H323_URI, "{device.address}/{value}"));
        aliasProviderCapability.addAlias(new Alias(AliasType.SIP_URI, "{value}@cesnet.cz"));
        firstAliasProvider.addCapability(aliasProviderCapability);
        firstAliasProvider.setAllocatable(true);
        createResource(firstAliasProvider);

        Resource secondAliasProvider = new Resource();
        secondAliasProvider.setName("secondAliasProvider");
        aliasProviderCapability = new AliasProviderCapability("2-{digit:1}");
        aliasProviderCapability.addAlias(new Alias(AliasType.ROOM_NAME, "{value}"));
        aliasProviderCapability.addAlias(new Alias(AliasType.H323_URI, "{device.address}#{value}"));
        aliasProviderCapability.addAlias(new Alias(AliasType.SIP_URI, "{value}@cesnet.cz"));
        secondAliasProvider.addCapability(aliasProviderCapability);
        secondAliasProvider.setAllocatable(true);
        createResource(secondAliasProvider);

        Resource thirdAliasProvider = new Resource();
        thirdAliasProvider.setName("thirdAliasProvider");
        aliasProviderCapability = new AliasProviderCapability("3-{digit:1}");
        aliasProviderCapability.addAlias(new Alias(AliasType.H323_E164, "{value}"));
        aliasProviderCapability.addAlias(new Alias(AliasType.H323_URI, "{device.address}/{value}"));
        aliasProviderCapability.addAlias(new Alias(AliasType.SIP_URI, "{value}@cesnet.cz"));
        thirdAliasProvider.addCapability(aliasProviderCapability);
        thirdAliasProvider.setAllocatable(true);
        createResource(thirdAliasProvider);

        // Rooms in first MCU require H323_URI and SIP_URI
        DeviceResource firstMcu = new DeviceResource();
        firstMcu.setName("firstMcu");
        firstMcu.addTechnology(Technology.H323);
        firstMcu.addTechnology(Technology.SIP);
        firstMcu.addCapability(new cz.cesnet.shongo.controller.api.RoomProviderCapability(10,
                new AliasType[]{AliasType.H323_URI, AliasType.SIP_URI}));
        firstMcu.setAllocatable(true);
        String firstMcuId = createResource(firstMcu);

        // Rooms in second MCU require ROOM_NAME and H323_E164
        DeviceResource secondMcu = new DeviceResource();
        secondMcu.setName("secondMcu");
        secondMcu.addTechnology(Technology.H323);
        secondMcu.addTechnology(Technology.SIP);
        secondMcu.addCapability(new cz.cesnet.shongo.controller.api.RoomProviderCapability(10,
                new AliasType[]{AliasType.ROOM_NAME, AliasType.H323_E164}));
        secondMcu.setAllocatable(true);
        String secondMcuId = createResource(secondMcu);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        RoomSpecification roomSpecification = new RoomSpecification(5);
        RoomEstablishment roomEstablishment = roomSpecification.createEstablishment();
        roomEstablishment.addTechnology(Technology.H323);
        roomEstablishment.addTechnology(Technology.SIP);
        roomEstablishment.setResourceId(firstMcuId);
        reservationRequest.setSpecification(roomSpecification);
        RoomReservation reservation =
                (RoomReservation) allocateAndCheck(reservationRequest);
        RoomExecutable reservationRoom = (RoomExecutable) reservation.getExecutable();
        // Only aliases from first alias provider should be allocated
        checkAliasTypes(new AliasType[]{AliasType.H323_URI, AliasType.SIP_URI}, reservationRoom.getAliases());

        reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        roomSpecification = new RoomSpecification(5);
        roomEstablishment = roomSpecification.createEstablishment();
        roomEstablishment.addTechnology(Technology.H323);
        roomEstablishment.addTechnology(Technology.SIP);
        roomEstablishment.setResourceId(secondMcuId);
        reservationRequest.setSpecification(roomSpecification);
        reservation = (RoomReservation) allocateAndCheck(reservationRequest);
        reservationRoom = (RoomExecutable) reservation.getExecutable();
        // Aliases from second and third alias provider should be allocated
        checkAliasTypes(
                new AliasType[]{
                        AliasType.ROOM_NAME, AliasType.H323_URI, AliasType.SIP_URI,
                        AliasType.H323_E164, AliasType.H323_URI, AliasType.SIP_URI
                },
                reservationRoom.getAliases());

        reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        roomSpecification = new RoomSpecification(5);
        roomEstablishment = roomSpecification.createEstablishment();
        roomEstablishment.addTechnology(Technology.H323);
        roomEstablishment.setResourceId(secondMcuId);
        reservationRequest.setSpecification(roomSpecification);
        reservation = (RoomReservation) allocateAndCheck(reservationRequest);
        reservationRoom = (RoomExecutable) reservation.getExecutable();
        // Only H323 aliases from second and third alias provider should be allocated
        checkAliasTypes(
                new AliasType[]{AliasType.ROOM_NAME, AliasType.H323_URI, AliasType.H323_E164, AliasType.H323_URI},
                reservationRoom.getAliases());
    }

    /**
     * Test allocation of virtual room with specific aliases.
     */
    @Test
    public void testRequestAlias() throws Exception
    {
        Resource firstAliasProvider = new Resource();
        firstAliasProvider.setName("firstAliasProvider");
        AliasProviderCapability aliasProviderCapability = new AliasProviderCapability("1-{digit:1}");
        aliasProviderCapability.addAlias(new Alias(AliasType.ROOM_NAME, "{value}"));
        aliasProviderCapability.addAlias(new Alias(AliasType.H323_URI, "{device.address}#{value}"));
        firstAliasProvider.addCapability(aliasProviderCapability);
        firstAliasProvider.setAllocatable(true);
        createResource(firstAliasProvider);

        Resource secondAliasProvider = new Resource();
        secondAliasProvider.setName("secondAliasProvider");
        aliasProviderCapability = new AliasProviderCapability("2-{digit:1}");
        aliasProviderCapability.addAlias(new Alias(AliasType.H323_E164, "{value}"));
        aliasProviderCapability.addAlias(new Alias(AliasType.H323_URI, "{device.address}/{value}"));
        secondAliasProvider.addCapability(aliasProviderCapability);
        secondAliasProvider.setAllocatable(true);
        createResource(secondAliasProvider);

        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.addTechnology(Technology.H323);
        mcu.addTechnology(Technology.SIP);
        mcu.addCapability(new RoomProviderCapability(15, new AliasType[]{AliasType.H323_URI}));
        mcu.setAllocatable(true);
        createResource(mcu);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new RoomSpecification(5, Technology.H323));
        RoomReservation reservation = (RoomReservation) allocateAndCheck(reservationRequest);
        RoomExecutable reservationRoom = (RoomExecutable) reservation.getExecutable();
        checkAliasTypes(new AliasType[]{AliasType.ROOM_NAME, AliasType.H323_URI}, reservationRoom.getAliases());

        reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        RoomSpecification roomSpecification = new RoomSpecification(5);
        RoomEstablishment roomEstablishment = roomSpecification.createEstablishment();
        roomEstablishment.addTechnology(Technology.H323);
        roomEstablishment.addAliasSpecification(new AliasSpecification(AliasType.H323_URI, "2-5"));
        reservationRequest.setSpecification(roomSpecification);
        reservation = (RoomReservation) allocateAndCheck(reservationRequest);
        reservationRoom = (RoomExecutable) reservation.getExecutable();
        checkAliasTypes(new AliasType[]{AliasType.H323_E164, AliasType.H323_URI}, reservationRoom.getAliases());

        reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        roomSpecification = new RoomSpecification(5);
        roomEstablishment = roomSpecification.createEstablishment();
        roomEstablishment.addTechnology(Technology.H323);
        roomEstablishment.addAliasSpecification(new AliasSpecification(AliasType.H323_URI, "1-5"));
        reservationRequest.setSpecification(roomSpecification);
        reservation = (RoomReservation) allocateAndCheck(reservationRequest);
        reservationRoom = (RoomExecutable) reservation.getExecutable();
        checkAliasTypes(new AliasType[]{AliasType.ROOM_NAME, AliasType.H323_URI}, reservationRoom.getAliases());
    }

    /**
     * Test allocation of virtual room which starts/ends specific amount of minutes before/after.
     *
     * @throws Exception
     */
    @Test
    public void testRoomBeforeAndAfter() throws Exception
    {
        Interval slot = Interval.parse("2014-01-01T14:00/2014-01-01T16:00");

        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.addTechnology(Technology.H323);
        mcu.addTechnology(Technology.SIP);
        mcu.addCapability(new RoomProviderCapability(10));
        mcu.setAllocatable(true);
        createResource(mcu);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot(slot);
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        RoomSpecification roomSpecification = new RoomSpecification(Technology.H323);
        RoomAvailability roomAvailability = roomSpecification.createAvailability();
        roomAvailability.setSlotMinutesBefore(10);
        roomAvailability.setSlotMinutesAfter(5);
        roomAvailability.setParticipantCount(5);
        reservationRequest.setSpecification(roomSpecification);
        String reservationRequestId = allocate(reservationRequest);
        RoomReservation reservation = (RoomReservation) checkAllocated(reservationRequestId);
        RoomExecutable room = (RoomExecutable) reservation.getExecutable();
        Assert.assertEquals(new Interval(
                slot.getStart().minusMinutes(roomAvailability.getSlotMinutesBefore()),
                slot.getEnd().plusMinutes(roomAvailability.getSlotMinutesAfter())),
                room.getSlot());
        Assert.assertEquals(slot, room.getOriginalSlot());

        reservationRequest = getReservationRequest(reservationRequestId, ReservationRequest.class);
        reservationRequestId = allocate(reservationRequest, slot.getStart().plusMinutes(5));
        checkAllocated(reservationRequestId);
    }

    /**
     * Test modification of virtual room which starts/ends specific amount of minutes before/after.
     *
     * @throws Exception
     */
    @Test
    public void testRoomBeforeModified() throws Exception
    {
        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.addTechnology(Technology.H323);
        mcu.addTechnology(Technology.SIP);
        mcu.addCapability(new RoomProviderCapability(10));
        mcu.setAllocatable(true);
        createResource(mcu);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2014-01-01T14:00/2014-01-01T16:00");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        RoomSpecification roomSpecification = new RoomSpecification(Technology.H323);
        RoomAvailability roomAvailability = roomSpecification.createAvailability();
        roomAvailability.setSlotMinutesBefore(10);
        roomAvailability.setSlotMinutesAfter(5);
        roomAvailability.setParticipantCount(5);
        reservationRequest.setSpecification(roomSpecification);
        String reservationRequestId = allocate(reservationRequest, DateTime.parse("2014-01-01T13:51:30"));
        checkAllocated(reservationRequestId);

        reservationRequest = getReservationRequest(reservationRequestId, ReservationRequest.class);
        reservationRequestId = allocate(reservationRequest, DateTime.parse("2014-01-01T13:51:35"));
        checkAllocated(reservationRequestId);
    }

    @Test
    public void testRoomBeforeNotInHistory() throws Exception
    {
        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.setAllocatable(true);
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(new RoomProviderCapability(10));
        createResource(mcu);

        DateTime dateTime = DateTime.now();

        ReservationRequest permanentRoomReservationRequest = new ReservationRequest();
        permanentRoomReservationRequest.setSlot(dateTime.minusDays(1), Period.days(2));
        permanentRoomReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        permanentRoomReservationRequest.setSpecification(new RoomSpecification(Technology.H323));
        permanentRoomReservationRequest.setReusement(ReservationRequestReusement.ARBITRARY);
        String permanentRoomReservationRequestId = allocate(permanentRoomReservationRequest, dateTime);
        Reservation permanentRoomReservation = checkAllocated(permanentRoomReservationRequestId);
        Assert.assertEquals(permanentRoomReservation.getSlot().getStart(), dateTime);

        ReservationRequest capacityReservationRequest = new ReservationRequest();
        capacityReservationRequest.setSlot(dateTime, Period.hours(2));
        capacityReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        capacityReservationRequest.setReusedReservationRequestId(permanentRoomReservationRequestId, true);
        RoomSpecification roomSpecification = new RoomSpecification(5);
        RoomAvailability roomAvailability = roomSpecification.getAvailability();
        roomAvailability.setSlotMinutesBefore(10);
        capacityReservationRequest.setSpecification(roomSpecification);
        AvailabilityCheckRequest availabilityCheckRequest = new AvailabilityCheckRequest();
        availabilityCheckRequest.setSecurityToken(SECURITY_TOKEN);
        availabilityCheckRequest.setSpecification(capacityReservationRequest.getSpecification());
        availabilityCheckRequest.setReservationRequestId(capacityReservationRequest.getReusedReservationRequestId());
        availabilityCheckRequest.addSlot(capacityReservationRequest.getSlot());
        Assert.assertEquals(Boolean.TRUE, getReservationService().checkPeriodicAvailability(availabilityCheckRequest));
        String capacityReservationRequestId = allocate(capacityReservationRequest, dateTime);
        checkAllocated(capacityReservationRequestId);

        getReservationService().deleteReservationRequest(SECURITY_TOKEN, capacityReservationRequestId);
        getReservationService().deleteReservationRequest(SECURITY_TOKEN, permanentRoomReservationRequestId);
        runScheduler();
    }

    private void checkAliasTypes(AliasType[] requiredAliasTypes, Collection<Alias> givenAliases)
    {
        Map<AliasType, Integer> requiredAliasTypeCount = new HashMap<AliasType, Integer>();
        for (AliasType requiredAliasType : requiredAliasTypes) {
            Integer count = requiredAliasTypeCount.get(requiredAliasType);
            if (count == null) {
                count = 0;
            }
            requiredAliasTypeCount.put(requiredAliasType, count + 1);
        }

        Map<AliasType, Integer> givenAliasTypeCount = new HashMap<AliasType, Integer>();
        for (Alias givenAlias : givenAliases) {
            AliasType givenAliasType = givenAlias.getType();
            Integer count = givenAliasTypeCount.get(givenAliasType);
            if (count == null) {
                count = 0;
            }
            givenAliasTypeCount.put(givenAliasType, count + 1);
        }

        Assert.assertEquals(requiredAliasTypes.length, givenAliases.size());
        Assert.assertEquals(requiredAliasTypeCount.keySet(), givenAliasTypeCount.keySet());
        for (AliasType aliasType : requiredAliasTypeCount.keySet()) {
            Assert.assertEquals(aliasType.toString(),
                    requiredAliasTypeCount.get(aliasType), givenAliasTypeCount.get(aliasType));
        }
    }
}
