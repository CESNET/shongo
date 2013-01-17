package cz.cesnet.shongo.controller.usecase;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.controller.AbstractControllerTest;
import cz.cesnet.shongo.controller.FilterType;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.api.*;
import org.junit.Test;

import java.util.List;

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
     * Test allocation of aliases.
     *
     * @throws Exception
     */
    @Test
    public void testSharedValueProvider() throws Exception
    {
        Resource valueProvider = new Resource();
        valueProvider.setName("valueProvider");
        valueProvider.setAllocatable(true);
        valueProvider.addCapability(new ValueProviderCapability("test"));
        String valueProviderId = getResourceService().createResource(SECURITY_TOKEN, valueProvider);

        Resource firstAliasProvider = new Resource();
        firstAliasProvider.setName("firstAliasProvider");
        firstAliasProvider.setAllocatable(true);
        AliasProviderCapability aliasProviderCapability = new AliasProviderCapability();
        aliasProviderCapability.setValueProvider(valueProviderId);
        aliasProviderCapability.addAlias(new Alias(AliasType.ROOM_NAME, "{value}"));
        firstAliasProvider.addCapability(aliasProviderCapability);
        String firstAliasProviderId = getResourceService().createResource(SECURITY_TOKEN, firstAliasProvider);

        Resource secondAliasProvider = new Resource();
        secondAliasProvider.setName("secondAliasProvider");
        secondAliasProvider.setAllocatable(true);
        aliasProviderCapability = new AliasProviderCapability();
        aliasProviderCapability.setValueProvider(valueProviderId);
        aliasProviderCapability.addAlias(new Alias(AliasType.ROOM_NAME, "{value}"));
        secondAliasProvider.addCapability(aliasProviderCapability);
        String secondAliasProviderId = getResourceService().createResource(SECURITY_TOKEN, secondAliasProvider);

        ReservationRequest firstReservationRequest = new ReservationRequest();
        firstReservationRequest.setSlot("2012-01-01T00:00", "P1Y");
        firstReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        firstReservationRequest.setSpecification(
                new AliasSpecification(AliasType.ROOM_NAME).withResourceId(firstAliasProviderId));
        AliasReservation aliasReservation = (AliasReservation) allocateAndCheck(firstReservationRequest);
        assertEquals("Requested value should be allocated.", "test", aliasReservation.getValue());

        ReservationRequest reservationRequestSecond = new ReservationRequest();
        reservationRequestSecond.setSlot("2012-01-01T00:00", "P1Y");
        reservationRequestSecond.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequestSecond.setSpecification(
                new AliasSpecification(AliasType.ROOM_NAME).withResourceId(secondAliasProviderId));
        allocateAndCheckFailed(reservationRequestSecond);

        // Test also without resource id
        ReservationRequest reservationRequestThird = new ReservationRequest();
        reservationRequestThird.setSlot("2013-01-01T00:00", "P1Y");
        reservationRequestThird.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequestThird.setSpecification(new AliasSpecification(AliasType.ROOM_NAME));
        allocateAndCheck(reservationRequestThird);
    }

    /**
     * Test allocation of aliases.
     *
     * @throws Exception
     */
    @Test
    public void testFilteredValue() throws Exception
    {
        Resource valueProvider = new Resource();
        valueProvider.setName("valueProvider");
        valueProvider.setAllocatable(true);
        valueProvider.addCapability(new ValueProviderCapability("{string}"));
        String valueProviderId = getResourceService().createResource(SECURITY_TOKEN, valueProvider);

        Resource aliasProvider = new Resource();
        aliasProvider.setName("firstAliasProvider");
        aliasProvider.setAllocatable(true);
        AliasProviderCapability aliasProviderCapability = new AliasProviderCapability();
        aliasProviderCapability.setValueProvider(
                new ValueProvider.Filtered(FilterType.CONVERT_TO_URL, valueProviderId));
        aliasProviderCapability.addAlias(new Alias(AliasType.ROOM_NAME, "{requested-value}"));
        aliasProviderCapability.addAlias(new Alias(AliasType.ADOBE_CONNECT_URI, "{value}"));
        aliasProvider.addCapability(aliasProviderCapability);
        getResourceService().createResource(SECURITY_TOKEN, aliasProvider);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-01-01T00:00", "P1Y");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new AliasSpecification(AliasType.ROOM_NAME));
        AliasReservation aliasReservation = (AliasReservation) allocateAndCheck(reservationRequest);
        assertEquals(2, aliasReservation.getAliases().size());
        assertEquals(aliasReservation.getAlias(AliasType.ROOM_NAME).getValue(),
                aliasReservation.getAlias(AliasType.ADOBE_CONNECT_URI).getValue());

        reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-01-01T00:00", "P1Y");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new AliasSpecification(AliasType.ROOM_NAME).withValue("Test Test"));
        aliasReservation = (AliasReservation) allocateAndCheck(reservationRequest);
        assertEquals("Requested value should be filtered.", "test-test", aliasReservation.getValue());
        assertEquals(2, aliasReservation.getAliases().size());
        assertEquals("Test Test", aliasReservation.getAlias(AliasType.ROOM_NAME).getValue());
        assertEquals("test-test", aliasReservation.getAlias(AliasType.ADOBE_CONNECT_URI).getValue());
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
                new AliasProviderCapability("{string}", AliasType.ADOBE_CONNECT_URI));
        getResourceService().createResource(SECURITY_TOKEN, aliasProvider);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-01-01T00:00", "P1Y");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new AliasSpecification(AliasType.ADOBE_CONNECT_URI).withValue("test_1"));
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
                new AliasProviderCapability("fake", AliasType.ADOBE_CONNECT_URI).withRestrictedToResource());
        getResourceService().createResource(SECURITY_TOKEN, connectServer);

        Resource aliasProvider = new Resource();
        aliasProvider.setName("aliasProvider");
        aliasProvider.setAllocatable(true);
        aliasProvider.addCapability(
                new AliasProviderCapability("test", AliasType.ADOBE_CONNECT_URI));
        getResourceService().createResource(SECURITY_TOKEN, aliasProvider);

        ReservationRequest aliasReservationRequest = new ReservationRequest();
        aliasReservationRequest.setSlot("2012-01-01T00:00", "P1Y");
        aliasReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        aliasReservationRequest.setSpecification(new AliasSpecification(AliasType.ADOBE_CONNECT_URI));
        AliasReservation aliasReservation = (AliasReservation) allocateAndCheck(aliasReservationRequest);
        assertEquals("Not restricted alias should be allocated.", "test", aliasReservation.getValue());
    }
}
