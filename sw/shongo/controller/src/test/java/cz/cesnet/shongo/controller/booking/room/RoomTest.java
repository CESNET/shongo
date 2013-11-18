package cz.cesnet.shongo.controller.booking.room;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.controller.AbstractControllerTest;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.RoomProviderCapability;
import cz.cesnet.shongo.controller.api.RoomReservation;
import cz.cesnet.shongo.controller.api.RoomSpecification;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Tests for allocation of {@link cz.cesnet.shongo.controller.api.RoomReservation}
 * by {@link cz.cesnet.shongo.controller.api.RoomSpecification}.
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
        String firstMcuId = getResourceService().createResource(SECURITY_TOKEN, firstMcu);

        DeviceResource secondMcu = new DeviceResource();
        secondMcu.setName("secondMcu");
        secondMcu.addTechnology(Technology.H323);
        secondMcu.addTechnology(Technology.SIP);
        secondMcu.addCapability(new cz.cesnet.shongo.controller.api.RoomProviderCapability(10));
        secondMcu.setAllocatable(true);
        String secondMcuId = getResourceService().createResource(SECURITY_TOKEN, secondMcu);

        ReservationRequest firstReservationRequest = new ReservationRequest();
        firstReservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        firstReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        cz.cesnet.shongo.controller.api.RoomSpecification roomSpecification = new cz.cesnet.shongo.controller.api.RoomSpecification();
        roomSpecification.addTechnology(Technology.H323);
        roomSpecification.addTechnology(Technology.SIP);
        roomSpecification.setParticipantCount(5);
        roomSpecification.setResourceId(secondMcuId);
        firstReservationRequest.setSpecification(roomSpecification);

        cz.cesnet.shongo.controller.api.RoomReservation firstReservation = (cz.cesnet.shongo.controller.api.RoomReservation) allocateAndCheck(firstReservationRequest);
        Assert.assertEquals("Virtual room should be allocated on second mcu, because it was specified as preferred",
                secondMcuId, firstReservation.getResourceId());

        ReservationRequest secondReservationRequest = new ReservationRequest();
        secondReservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        secondReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        roomSpecification = new cz.cesnet.shongo.controller.api.RoomSpecification();
        roomSpecification.addTechnology(Technology.H323);
        roomSpecification.addTechnology(Technology.SIP);
        roomSpecification.setParticipantCount(3);
        roomSpecification.setResourceId(firstMcuId);
        secondReservationRequest.setSpecification(roomSpecification);

        cz.cesnet.shongo.controller.api.RoomReservation secondReservation = (cz.cesnet.shongo.controller.api.RoomReservation) allocateAndCheck(secondReservationRequest);
        Assert.assertEquals("Virtual room should be allocated on first mcu, because it was specified as preferred",
                firstMcuId, secondReservation.getResourceId());

        ReservationRequest thirdReservationRequest = new ReservationRequest();
        thirdReservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        thirdReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        roomSpecification = new cz.cesnet.shongo.controller.api.RoomSpecification();
        roomSpecification.addTechnology(Technology.H323);
        roomSpecification.addTechnology(Technology.SIP);
        roomSpecification.setParticipantCount(5);
        thirdReservationRequest.setSpecification(roomSpecification);

        cz.cesnet.shongo.controller.api.RoomReservation thirdReservation = (cz.cesnet.shongo.controller.api.RoomReservation) allocateAndCheck(thirdReservationRequest);
        Assert.assertEquals("Virtual room should be allocated on second mcu, because it was the most filled",
                secondMcuId, thirdReservation.getResourceId());
    }

    /**
     * Test allocation of aliases for a room.
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
        getResourceService().createResource(SECURITY_TOKEN, firstAliasProvider);

        Resource secondAliasProvider = new Resource();
        secondAliasProvider.setName("secondAliasProvider");
        aliasProviderCapability = new AliasProviderCapability("2-{digit:1}");
        aliasProviderCapability.addAlias(new Alias(AliasType.ROOM_NAME, "{value}"));
        aliasProviderCapability.addAlias(new Alias(AliasType.H323_URI, "{device.address}#{value}"));
        aliasProviderCapability.addAlias(new Alias(AliasType.SIP_URI, "{value}@cesnet.cz"));
        secondAliasProvider.addCapability(aliasProviderCapability);
        secondAliasProvider.setAllocatable(true);
        getResourceService().createResource(SECURITY_TOKEN, secondAliasProvider);

        Resource thirdAliasProvider = new Resource();
        thirdAliasProvider.setName("thirdAliasProvider");
        aliasProviderCapability = new AliasProviderCapability("3-{digit:1}");
        aliasProviderCapability.addAlias(new Alias(AliasType.H323_E164, "{value}"));
        aliasProviderCapability.addAlias(new Alias(AliasType.H323_URI, "{device.address}/{value}"));
        aliasProviderCapability.addAlias(new Alias(AliasType.SIP_URI, "{value}@cesnet.cz"));
        thirdAliasProvider.addCapability(aliasProviderCapability);
        thirdAliasProvider.setAllocatable(true);
        getResourceService().createResource(SECURITY_TOKEN, thirdAliasProvider);

        DeviceResource firstMcu = new DeviceResource();
        firstMcu.setName("firstMcu");
        firstMcu.addTechnology(Technology.H323);
        firstMcu.addTechnology(Technology.SIP);
        firstMcu.addCapability(new cz.cesnet.shongo.controller.api.RoomProviderCapability(10,
                new AliasType[]{AliasType.H323_URI, AliasType.SIP_URI}));
        firstMcu.setAllocatable(true);
        String firstMcuId = getResourceService().createResource(SECURITY_TOKEN, firstMcu);

        DeviceResource secondMcu = new DeviceResource();
        secondMcu.setName("secondMcu");
        secondMcu.addTechnology(Technology.H323);
        secondMcu.addTechnology(Technology.SIP);
        secondMcu.addCapability(new cz.cesnet.shongo.controller.api.RoomProviderCapability(10,
                new AliasType[]{AliasType.ROOM_NAME, AliasType.H323_E164}));
        secondMcu.setAllocatable(true);
        String secondMcuId = getResourceService().createResource(SECURITY_TOKEN, secondMcu);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        cz.cesnet.shongo.controller.api.RoomSpecification roomSpecification = new cz.cesnet.shongo.controller.api.RoomSpecification();
        roomSpecification.addTechnology(Technology.H323);
        roomSpecification.addTechnology(Technology.SIP);
        roomSpecification.setParticipantCount(5);
        roomSpecification.setResourceId(firstMcuId);
        reservationRequest.setSpecification(roomSpecification);
        cz.cesnet.shongo.controller.api.RoomReservation reservation = (cz.cesnet.shongo.controller.api.RoomReservation) allocateAndCheck(reservationRequest);
        RoomExecutable reservationRoom = (RoomExecutable) reservation.getExecutable();
        checkAliasTypes(new AliasType[]{AliasType.H323_URI, AliasType.SIP_URI}, reservationRoom.getAliases());

        reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        roomSpecification = new cz.cesnet.shongo.controller.api.RoomSpecification();
        roomSpecification.addTechnology(Technology.H323);
        roomSpecification.addTechnology(Technology.SIP);
        roomSpecification.setParticipantCount(5);
        roomSpecification.setResourceId(secondMcuId);
        reservationRequest.setSpecification(roomSpecification);
        reservation = (cz.cesnet.shongo.controller.api.RoomReservation) allocateAndCheck(reservationRequest);
        reservationRoom = (RoomExecutable) reservation.getExecutable();
        checkAliasTypes(
                new AliasType[]{
                        AliasType.ROOM_NAME, AliasType.H323_URI, AliasType.SIP_URI,
                        AliasType.H323_E164, AliasType.H323_URI, AliasType.SIP_URI
                },
                reservationRoom.getAliases());

        reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        roomSpecification = new cz.cesnet.shongo.controller.api.RoomSpecification();
        roomSpecification.addTechnology(Technology.H323);
        roomSpecification.setParticipantCount(5);
        roomSpecification.setResourceId(secondMcuId);
        reservationRequest.setSpecification(roomSpecification);
        reservation = (cz.cesnet.shongo.controller.api.RoomReservation) allocateAndCheck(reservationRequest);
        reservationRoom = (RoomExecutable) reservation.getExecutable();
        checkAliasTypes(
                new AliasType[]{AliasType.ROOM_NAME, AliasType.H323_URI, AliasType.H323_E164, AliasType.H323_URI},
                reservationRoom.getAliases());
    }

    /**
     * Test allocation of aliases for a room.
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
        getResourceService().createResource(SECURITY_TOKEN, firstAliasProvider);

        Resource secondAliasProvider = new Resource();
        secondAliasProvider.setName("secondAliasProvider");
        aliasProviderCapability = new AliasProviderCapability("2-{digit:1}");
        aliasProviderCapability.addAlias(new Alias(AliasType.H323_E164, "{value}"));
        aliasProviderCapability.addAlias(new Alias(AliasType.H323_URI, "{device.address}/{value}"));
        secondAliasProvider.addCapability(aliasProviderCapability);
        secondAliasProvider.setAllocatable(true);
        getResourceService().createResource(SECURITY_TOKEN, secondAliasProvider);

        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.addTechnology(Technology.H323);
        mcu.addTechnology(Technology.SIP);
        mcu.addCapability(new RoomProviderCapability(15, new AliasType[]{AliasType.H323_URI}));
        mcu.setAllocatable(true);
        String firstMcuId = getResourceService().createResource(SECURITY_TOKEN, mcu);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new cz.cesnet.shongo.controller.api.RoomSpecification(5, Technology.H323));
        cz.cesnet.shongo.controller.api.RoomReservation reservation = (cz.cesnet.shongo.controller.api.RoomReservation) allocateAndCheck(reservationRequest);
        RoomExecutable reservationRoom = (RoomExecutable) reservation.getExecutable();
        checkAliasTypes(new AliasType[]{AliasType.ROOM_NAME, AliasType.H323_URI}, reservationRoom.getAliases());

        reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(
                new cz.cesnet.shongo.controller.api.RoomSpecification(5, Technology.H323).withAlias(AliasType.H323_URI, "2-5"));
        reservation = (cz.cesnet.shongo.controller.api.RoomReservation) allocateAndCheck(reservationRequest);
        reservationRoom = (RoomExecutable) reservation.getExecutable();
        checkAliasTypes(new AliasType[]{AliasType.H323_E164, AliasType.H323_URI}, reservationRoom.getAliases());

        reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(
                new RoomSpecification(5, Technology.H323).withAlias(AliasType.H323_URI, "1-5"));
        reservation = (RoomReservation) allocateAndCheck(reservationRequest);
        reservationRoom = (RoomExecutable) reservation.getExecutable();
        checkAliasTypes(new AliasType[]{AliasType.ROOM_NAME, AliasType.H323_URI}, reservationRoom.getAliases());
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