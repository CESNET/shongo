package cz.cesnet.shongo.controller.usecase;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.AbstractControllerTest;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.api.*;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * Tests for allocation of {@link cz.cesnet.shongo.controller.api.AliasReservation}
 * by {@link cz.cesnet.shongo.controller.api.AliasSpecification}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AliasTest extends AbstractControllerTest
{
    /**
     * Test allocation of aliases.
     *
     * @throws Exception
     */
    @Test
    public void test() throws Exception
    {
        Resource aliasProvider = new Resource();
        aliasProvider.setName("aliasProvider");
        aliasProvider.setAllocatable(true);
        aliasProvider.addCapability(new AliasProviderCapability("test", AliasType.ROOM_NAME));
        getResourceService().createResource(SECURITY_TOKEN, aliasProvider);

        ReservationRequest reservationRequestFirst = new ReservationRequest();
        reservationRequestFirst.setSlot("2012-01-01T00:00", "P1Y");
        reservationRequestFirst.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequestFirst.setSpecification(new AliasSpecification(AliasType.ROOM_NAME));
        AliasReservation aliasReservation = (AliasReservation) allocateAndCheck(reservationRequestFirst);
        assertEquals("Requested value should be allocated.", "test", aliasReservation.getValue());

        ReservationRequest reservationRequestSecond = new ReservationRequest();
        reservationRequestSecond.setSlot("2012-01-01T00:00", "P1Y");
        reservationRequestSecond.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequestSecond.setSpecification(new AliasSpecification(AliasType.ROOM_NAME));
        allocateAndCheckFailed(reservationRequestSecond);
    }

    /**
     * Test allocation of requested alias value.
     *
     * @throws Exception
     */
    @Test
    public void testRequestedValue() throws Exception
    {
        Resource aliasProvider = new Resource();
        aliasProvider.setName("aliasProvider");
        aliasProvider.setAllocatable(true);
        aliasProvider.addCapability(
                new AliasProviderCapability("{string}", AliasType.ADOBE_CONNECT_NAME));
        getResourceService().createResource(SECURITY_TOKEN, aliasProvider);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-01-01T00:00", "P1Y");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new AliasSpecification(AliasType.ADOBE_CONNECT_NAME).withValue("test_1"));
        AliasReservation aliasReservation = (AliasReservation) allocateAndCheck(reservationRequest);
        assertEquals("Requested value should be allocated.", "test_1", aliasReservation.getValue());
    }

    /**
     * Test preference of not owner resource restricted alias.
     *
     * @throws Exception
     */
    @Test
    public void testPreferNotRestrictedAlias() throws Exception
    {
        DeviceResource connectServer = new DeviceResource();
        connectServer.setName("connectServer");
        connectServer.setAllocatable(true);
        connectServer.addTechnology(Technology.ADOBE_CONNECT);
        connectServer.addCapability(
                new AliasProviderCapability("fake", AliasType.ADOBE_CONNECT_NAME).withRestrictedToResource());
        getResourceService().createResource(SECURITY_TOKEN, connectServer);

        Resource aliasProvider = new Resource();
        aliasProvider.setName("aliasProvider");
        aliasProvider.setAllocatable(true);
        aliasProvider.addCapability(
                new AliasProviderCapability("test", AliasType.ADOBE_CONNECT_NAME));
        getResourceService().createResource(SECURITY_TOKEN, aliasProvider);

        ReservationRequest aliasReservationRequest = new ReservationRequest();
        aliasReservationRequest.setSlot("2012-01-01T00:00", "P1Y");
        aliasReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        aliasReservationRequest.setSpecification(new AliasSpecification(AliasType.ADOBE_CONNECT_NAME));
        AliasReservation aliasReservation = (AliasReservation) allocateAndCheck(aliasReservationRequest);
        assertEquals("Not restricted alias should be allocated.", "test", aliasReservation.getValue());
    }
}
