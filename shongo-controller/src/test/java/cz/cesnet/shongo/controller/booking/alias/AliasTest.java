package cz.cesnet.shongo.controller.booking.alias;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.controller.AbstractControllerTest;
import cz.cesnet.shongo.controller.FilterType;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.AliasProviderCapability;
import cz.cesnet.shongo.controller.api.AliasReservation;
import cz.cesnet.shongo.controller.api.AliasSetSpecification;
import cz.cesnet.shongo.controller.api.AliasSpecification;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * Tests for allocation of {@link cz.cesnet.shongo.controller.api.AliasReservation}
 * by {@link cz.cesnet.shongo.controller.api.AliasSpecification}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AliasTest extends AbstractControllerTest
{
    /**
     * Test call priority of aliases.
     *
     * @throws Exception
     */
    @Test
    public void testCallPriority() throws Exception
    {
        cz.cesnet.shongo.controller.booking.alias.Alias h323number =
                new cz.cesnet.shongo.controller.booking.alias.Alias(AliasType.H323_E164, "1");
        cz.cesnet.shongo.controller.booking.alias.Alias h323uri =
                new cz.cesnet.shongo.controller.booking.alias.Alias(AliasType.H323_URI, "1@cesnet.cz");
        cz.cesnet.shongo.controller.booking.alias.Alias h323Ip =
                new cz.cesnet.shongo.controller.booking.alias.Alias(AliasType.H323_IP, "1.0.0.0 #1");
        cz.cesnet.shongo.controller.booking.alias.Alias sipUri =
                new cz.cesnet.shongo.controller.booking.alias.Alias(AliasType.SIP_URI, "1@cesnet.cz");
        cz.cesnet.shongo.controller.booking.alias.Alias sipIp =
                new cz.cesnet.shongo.controller.booking.alias.Alias(AliasType.SIP_IP, "1.0.0.0 #1");

        Assert.assertTrue(h323number.hasHigherCallPriorityThan(h323uri));
        Assert.assertTrue(h323number.hasHigherCallPriorityThan(h323Ip));
        Assert.assertTrue(h323number.hasHigherCallPriorityThan(sipUri));
        Assert.assertTrue(h323number.hasHigherCallPriorityThan(sipIp));
        Assert.assertFalse(h323uri.hasHigherCallPriorityThan(h323number));
        Assert.assertFalse(h323Ip.hasHigherCallPriorityThan(h323number));
        Assert.assertFalse(sipUri.hasHigherCallPriorityThan(h323number));
        Assert.assertFalse(sipIp.hasHigherCallPriorityThan(h323number));
    }

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
        aliasProvider.addCapability(new cz.cesnet.shongo.controller.api.AliasProviderCapability("test", AliasType.ROOM_NAME));
        createResource(aliasProvider);

        ReservationRequest reservationRequestFirst = new ReservationRequest();
        reservationRequestFirst.setSlot("2012-01-01T00:00", "P1Y");
        reservationRequestFirst.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequestFirst.setSpecification(new cz.cesnet.shongo.controller.api.AliasSpecification(AliasType.ROOM_NAME));
        cz.cesnet.shongo.controller.api.AliasReservation aliasReservation = (cz.cesnet.shongo.controller.api.AliasReservation) allocateAndCheck(reservationRequestFirst);
        Assert.assertEquals("Requested value should be allocated.", "test", aliasReservation.getValue());

        ReservationRequest reservationRequestSecond = new ReservationRequest();
        reservationRequestSecond.setSlot("2012-01-01T00:00", "P1Y");
        reservationRequestSecond.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequestSecond.setSpecification(new cz.cesnet.shongo.controller.api.AliasSpecification(AliasType.ROOM_NAME));
        allocateAndCheckFailed(reservationRequestSecond);

        runScheduler();
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
        String valueProviderId = createResource(valueProvider);

        Resource firstAliasProvider = new Resource();
        firstAliasProvider.setName("firstAliasProvider");
        firstAliasProvider.setAllocatable(true);
        cz.cesnet.shongo.controller.api.AliasProviderCapability aliasProviderCapability = new cz.cesnet.shongo.controller.api.AliasProviderCapability();
        aliasProviderCapability.setValueProvider(valueProviderId);
        aliasProviderCapability.addAlias(new Alias(AliasType.ROOM_NAME, "{value}"));
        firstAliasProvider.addCapability(aliasProviderCapability);
        String firstAliasProviderId = createResource(firstAliasProvider);

        Resource secondAliasProvider = new Resource();
        secondAliasProvider.setName("secondAliasProvider");
        secondAliasProvider.setAllocatable(true);
        aliasProviderCapability = new cz.cesnet.shongo.controller.api.AliasProviderCapability();
        aliasProviderCapability.setValueProvider(valueProviderId);
        aliasProviderCapability.addAlias(new Alias(AliasType.ROOM_NAME, "{value}"));
        secondAliasProvider.addCapability(aliasProviderCapability);
        String secondAliasProviderId = createResource(secondAliasProvider);

        ReservationRequest firstReservationRequest = new ReservationRequest();
        firstReservationRequest.setSlot("2012-01-01T00:00", "P1Y");
        firstReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        firstReservationRequest.setSpecification(
                new cz.cesnet.shongo.controller.api.AliasSpecification(AliasType.ROOM_NAME).withResourceId(firstAliasProviderId));
        cz.cesnet.shongo.controller.api.AliasReservation aliasReservation = (cz.cesnet.shongo.controller.api.AliasReservation) allocateAndCheck(firstReservationRequest);
        Assert.assertEquals("Requested value should be allocated.", "test", aliasReservation.getValue());

        ReservationRequest reservationRequestSecond = new ReservationRequest();
        reservationRequestSecond.setSlot("2012-01-01T00:00", "P1Y");
        reservationRequestSecond.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequestSecond.setSpecification(
                new cz.cesnet.shongo.controller.api.AliasSpecification(AliasType.ROOM_NAME).withResourceId(secondAliasProviderId));
        allocateAndCheckFailed(reservationRequestSecond);

        // Test also without resource id
        ReservationRequest reservationRequestThird = new ReservationRequest();
        reservationRequestThird.setSlot("2013-01-01T00:00", "P1Y");
        reservationRequestThird.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequestThird.setSpecification(new cz.cesnet.shongo.controller.api.AliasSpecification(AliasType.ROOM_NAME));
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
        valueProvider.addCapability(new ValueProviderCapability("shongo-{hash:6}").withAllowedAnyRequestedValue());
        String valueProviderId = createResource(valueProvider);

        Resource aliasProvider = new Resource();
        aliasProvider.setName("firstAliasProvider");
        aliasProvider.setAllocatable(true);
        cz.cesnet.shongo.controller.api.AliasProviderCapability aliasProviderCapability = new cz.cesnet.shongo.controller.api.AliasProviderCapability();
        aliasProviderCapability.setValueProvider(
                new ValueProvider.Filtered(FilterType.CONVERT_TO_URL, valueProviderId));
        aliasProviderCapability.addAlias(new Alias(AliasType.ROOM_NAME, "{requested-value}"));
        aliasProviderCapability.addAlias(new Alias(AliasType.ADOBE_CONNECT_URI, "{value}"));
        aliasProvider.addCapability(aliasProviderCapability);
        createResource(aliasProvider);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-01-01T00:00", "P1Y");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new cz.cesnet.shongo.controller.api.AliasSpecification(AliasType.ROOM_NAME));
        cz.cesnet.shongo.controller.api.AliasReservation aliasReservation = (cz.cesnet.shongo.controller.api.AliasReservation) allocateAndCheck(reservationRequest);
        Assert.assertEquals(2, aliasReservation.getAliases().size());
        Assert.assertEquals(aliasReservation.getAlias(AliasType.ROOM_NAME).getValue(),
                aliasReservation.getAlias(AliasType.ADOBE_CONNECT_URI).getValue());

        reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-01-01T00:00", "P1Y");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new cz.cesnet.shongo.controller.api.AliasSpecification(AliasType.ROOM_NAME).withValue("Test Test"));
        aliasReservation = (cz.cesnet.shongo.controller.api.AliasReservation) allocateAndCheck(reservationRequest);
        Assert.assertEquals("Requested value should be filtered.", "test-test", aliasReservation.getValue());
        Assert.assertEquals(2, aliasReservation.getAliases().size());
        Assert.assertEquals("Test Test", aliasReservation.getAlias(AliasType.ROOM_NAME).getValue());
        Assert.assertEquals("test-test", aliasReservation.getAlias(AliasType.ADOBE_CONNECT_URI).getValue());
    }

    /**
     * Test allocation of aliases.
     *
     * @throws Exception
     */
    @Test
    public void testFilteredWithInner() throws Exception
    {
        Resource aliasProvider = new Resource();
        aliasProvider.setName("firstAliasProvider");
        aliasProvider.setAllocatable(true);
        cz.cesnet.shongo.controller.api.AliasProviderCapability aliasProviderCapability = new cz.cesnet.shongo.controller.api.AliasProviderCapability();
        aliasProviderCapability.setValueProvider(
                new ValueProvider.Filtered(FilterType.CONVERT_TO_URL, new ValueProvider.Pattern("{hash}")));

        aliasProviderCapability.addAlias(new Alias(AliasType.ROOM_NAME, "{requested-value}"));
        aliasProviderCapability.addAlias(new Alias(AliasType.ADOBE_CONNECT_URI, "{value}"));
        aliasProvider.addCapability(aliasProviderCapability);
        createResource(aliasProvider);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-01-01T00:00", "P1Y");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new cz.cesnet.shongo.controller.api.AliasSpecification(AliasType.ROOM_NAME));
        cz.cesnet.shongo.controller.api.AliasReservation aliasReservation = (cz.cesnet.shongo.controller.api.AliasReservation) allocateAndCheck(reservationRequest);
        Assert.assertEquals(2, aliasReservation.getAliases().size());
        Assert.assertEquals(aliasReservation.getAlias(AliasType.ROOM_NAME).getValue(),
                aliasReservation.getAlias(AliasType.ADOBE_CONNECT_URI).getValue());
    }

    /**
     * Test allocation of aliases.
     *
     * @throws Exception
     */
    @Test
    public void testMultipleFiltered() throws Exception
    {
        Resource valueProvider = new Resource();
        valueProvider.setName("valueProvider");
        valueProvider.setAllocatable(true);
        valueProvider.addCapability(new ValueProviderCapability("{hash}").withAllowedAnyRequestedValue());
        String valueProviderId = createResource(valueProvider);

        Resource aliasProvider = new Resource();
        aliasProvider.setName("firstAliasProvider");
        aliasProvider.setAllocatable(true);
        cz.cesnet.shongo.controller.api.AliasProviderCapability aliasProviderCapability = new cz.cesnet.shongo.controller.api.AliasProviderCapability();
        aliasProviderCapability.setValueProvider(
                new ValueProvider.Filtered(FilterType.CONVERT_TO_URL,
                        new ValueProvider.Filtered(FilterType.CONVERT_TO_URL, valueProviderId)));
        aliasProviderCapability.addAlias(new Alias(AliasType.ROOM_NAME, "{requested-value}"));
        aliasProviderCapability.addAlias(new Alias(AliasType.ADOBE_CONNECT_URI, "{value}"));
        aliasProvider.addCapability(aliasProviderCapability);
        createResource(aliasProvider);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-01-01T00:00", "P1Y");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new cz.cesnet.shongo.controller.api.AliasSpecification(AliasType.ROOM_NAME));
        cz.cesnet.shongo.controller.api.AliasReservation aliasReservation = (cz.cesnet.shongo.controller.api.AliasReservation) allocateAndCheck(reservationRequest);
        Assert.assertEquals(2, aliasReservation.getAliases().size());
        Assert.assertEquals(aliasReservation.getAlias(AliasType.ROOM_NAME).getValue(),
                aliasReservation.getAlias(AliasType.ADOBE_CONNECT_URI).getValue());
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
                new cz.cesnet.shongo.controller.api.AliasProviderCapability("{hash}", AliasType.ADOBE_CONNECT_URI).withAllowedAnyRequestedValue());
        createResource(aliasProvider);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-01-01T00:00", "P1Y");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new cz.cesnet.shongo.controller.api.AliasSpecification(AliasType.ADOBE_CONNECT_URI).withValue("test"));
        cz.cesnet.shongo.controller.api.AliasReservation aliasReservation = (cz.cesnet.shongo.controller.api.AliasReservation) allocateAndCheck(reservationRequest);
        Assert.assertEquals("Requested value should be allocated.", "test", aliasReservation.getValue());
    }

    /**
     * Test allocation of part of requested alias value.
     *
     * @throws Exception
     */
    @Test
    public void testRequestedValuePart() throws Exception
    {
        Resource aliasProvider = new Resource();
        aliasProvider.setName("aliasProvider");
        aliasProvider.setAllocatable(true);
        cz.cesnet.shongo.controller.api.AliasProviderCapability aliasProviderCapability = new cz.cesnet.shongo.controller.api.AliasProviderCapability();
        aliasProviderCapability.addAlias(new Alias(AliasType.H323_E164, "950087{value}"));
        aliasProviderCapability.setValueProvider(new ValueProvider.Pattern("{number:090:099}"));
        aliasProvider.addCapability(aliasProviderCapability);
        createResource(aliasProvider);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-01-01T00:00", "P1Y");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new cz.cesnet.shongo.controller.api.AliasSpecification(AliasType.H323_E164).withValue("950087095"));
        cz.cesnet.shongo.controller.api.AliasReservation aliasReservation = (cz.cesnet.shongo.controller.api.AliasReservation) allocateAndCheck(reservationRequest);
        Assert.assertEquals("Requested value should be allocated.", "095", aliasReservation.getValue());
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
                new cz.cesnet.shongo.controller.api.AliasProviderCapability("fake", AliasType.ADOBE_CONNECT_URI).withRestrictedToResource());
        createResource(connectServer);

        Resource aliasProvider = new Resource();
        aliasProvider.setName("aliasProvider");
        aliasProvider.setAllocatable(true);
        aliasProvider.addCapability(
                new cz.cesnet.shongo.controller.api.AliasProviderCapability("test", AliasType.ADOBE_CONNECT_URI));
        createResource(aliasProvider);

        ReservationRequest aliasReservationRequest = new ReservationRequest();
        aliasReservationRequest.setSlot("2012-01-01T00:00", "P1Y");
        aliasReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        aliasReservationRequest.setSpecification(new cz.cesnet.shongo.controller.api.AliasSpecification(AliasType.ADOBE_CONNECT_URI));
        cz.cesnet.shongo.controller.api.AliasReservation aliasReservation = (cz.cesnet.shongo.controller.api.AliasReservation) allocateAndCheck(aliasReservationRequest);
        Assert.assertEquals("Not restricted alias should be allocated.", "test", aliasReservation.getValue());
    }

    /**
     * Test allocation of requested alias value.
     *
     * @throws Exception
     */
    @Test
    public void testAliasSetSpecification() throws Exception
    {
        Resource firstAliasProvider = new Resource();
        firstAliasProvider.setName("firstAliasProvider");
        firstAliasProvider.setAllocatable(true);
        firstAliasProvider.addCapability(new cz.cesnet.shongo.controller.api.AliasProviderCapability("{hash}", AliasType.H323_URI));
        createResource(firstAliasProvider);

        Resource secondAliasProvider = new Resource();
        secondAliasProvider.setName("secondAliasProvider");
        secondAliasProvider.setAllocatable(true);
        secondAliasProvider.addCapability(new cz.cesnet.shongo.controller.api.AliasProviderCapability("{hash}", AliasType.SIP_URI));
        createResource(secondAliasProvider);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-01-01T00:00", "P1Y");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        cz.cesnet.shongo.controller.api.AliasSetSpecification aliasSetSpecification = new AliasSetSpecification();
        aliasSetSpecification.addAlias(new cz.cesnet.shongo.controller.api.AliasSpecification(AliasType.H323_URI));
        aliasSetSpecification.addAlias(new cz.cesnet.shongo.controller.api.AliasSpecification(AliasType.SIP_URI));
        reservationRequest.setSpecification(aliasSetSpecification);
        Reservation reservation = allocateAndCheck(reservationRequest);
        List<String> childReservationIds = reservation.getChildReservationIds();
        Assert.assertEquals("Reservation should have two child alias reservations.", 2, childReservationIds.size());
        cz.cesnet.shongo.controller.api.AliasReservation reservationFirst = (cz.cesnet.shongo.controller.api.AliasReservation) getReservationService().getReservation(SECURITY_TOKEN,
                childReservationIds.get(0));
        Assert.assertEquals(firstAliasProvider.getName(), reservationFirst.getValueReservation().getResourceName());
        cz.cesnet.shongo.controller.api.AliasReservation reservationSecond = (cz.cesnet.shongo.controller.api.AliasReservation) getReservationService().getReservation(SECURITY_TOKEN,
                childReservationIds.get(1));
        Assert.assertEquals(secondAliasProvider.getName(), reservationSecond.getValueReservation().getResourceName());
    }

    /**
     * Test allocation of requested alias value.
     *
     * @throws Exception
     */
    @Test
    public void testVariantTechnology() throws Exception
    {
        Resource aliasProvider = new Resource();
        aliasProvider.setName("aliasProvider");
        aliasProvider.setAllocatable(true);
        cz.cesnet.shongo.controller.api.AliasProviderCapability aliasProviderCapability =
                new AliasProviderCapability("{hash}").withAllowedAnyRequestedValue();
        aliasProviderCapability.addAlias(new Alias(AliasType.ROOM_NAME, "{requested-value}"));
        aliasProviderCapability.addAlias(new Alias(AliasType.SIP_URI, "{value}"));
        aliasProvider.addCapability(aliasProviderCapability);
        createResource(aliasProvider);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-01-01T00:00", "P1Y");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        cz.cesnet.shongo.controller.api.AliasSpecification aliasSpecification = new AliasSpecification();
        aliasSpecification.addAliasType(AliasType.ROOM_NAME);
        aliasSpecification.addTechnology(Technology.H323);
        aliasSpecification.addTechnology(Technology.SIP);
        aliasSpecification.setValue("test");
        reservationRequest.setSpecification(aliasSpecification);
        cz.cesnet.shongo.controller.api.AliasReservation aliasReservation = (AliasReservation) allocateAndCheck(reservationRequest);
        Assert.assertEquals("Room name alias for H.323 or SIP should be allocated.", "test", aliasReservation.getValue());
    }
}
