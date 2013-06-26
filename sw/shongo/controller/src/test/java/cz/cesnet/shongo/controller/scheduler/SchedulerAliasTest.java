package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.controller.AbstractControllerTest;
import cz.cesnet.shongo.controller.FilterType;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.api.*;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * Tests for allocation of {@link cz.cesnet.shongo.controller.api.AliasReservation}
 * by {@link cz.cesnet.shongo.controller.api.AliasSpecification}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class SchedulerAliasTest extends AbstractControllerTest
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
        Assert.assertEquals("Requested value should be allocated.", "test", aliasReservation.getValue());

        ReservationRequest reservationRequestSecond = new ReservationRequest();
        reservationRequestSecond.setSlot("2012-01-01T00:00", "P1Y");
        reservationRequestSecond.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequestSecond.setSpecification(new AliasSpecification(AliasType.ROOM_NAME));
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
        Assert.assertEquals("Requested value should be allocated.", "test", aliasReservation.getValue());

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
        valueProvider.addCapability(new ValueProviderCapability("shongo-{hash:6}").withAllowedAnyRequestedValue());
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
        Assert.assertEquals(2, aliasReservation.getAliases().size());
        Assert.assertEquals(aliasReservation.getAlias(AliasType.ROOM_NAME).getValue(),
                aliasReservation.getAlias(AliasType.ADOBE_CONNECT_URI).getValue());

        reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-01-01T00:00", "P1Y");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new AliasSpecification(AliasType.ROOM_NAME).withValue("Test Test"));
        aliasReservation = (AliasReservation) allocateAndCheck(reservationRequest);
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
        AliasProviderCapability aliasProviderCapability = new AliasProviderCapability();
        aliasProviderCapability.setValueProvider(
                new ValueProvider.Filtered(FilterType.CONVERT_TO_URL, new ValueProvider.Pattern("{hash}")));

        aliasProviderCapability.addAlias(new Alias(AliasType.ROOM_NAME, "{requested-value}"));
        aliasProviderCapability.addAlias(new Alias(AliasType.ADOBE_CONNECT_URI, "{value}"));
        aliasProvider.addCapability(aliasProviderCapability);
        getResourceService().createResource(SECURITY_TOKEN, aliasProvider);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-01-01T00:00", "P1Y");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new AliasSpecification(AliasType.ROOM_NAME));
        AliasReservation aliasReservation = (AliasReservation) allocateAndCheck(reservationRequest);
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
        String valueProviderId = getResourceService().createResource(SECURITY_TOKEN, valueProvider);

        Resource aliasProvider = new Resource();
        aliasProvider.setName("firstAliasProvider");
        aliasProvider.setAllocatable(true);
        AliasProviderCapability aliasProviderCapability = new AliasProviderCapability();
        aliasProviderCapability.setValueProvider(
                new ValueProvider.Filtered(FilterType.CONVERT_TO_URL,
                        new ValueProvider.Filtered(FilterType.CONVERT_TO_URL, valueProviderId)));
        aliasProviderCapability.addAlias(new Alias(AliasType.ROOM_NAME, "{requested-value}"));
        aliasProviderCapability.addAlias(new Alias(AliasType.ADOBE_CONNECT_URI, "{value}"));
        aliasProvider.addCapability(aliasProviderCapability);
        getResourceService().createResource(SECURITY_TOKEN, aliasProvider);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-01-01T00:00", "P1Y");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new AliasSpecification(AliasType.ROOM_NAME));
        AliasReservation aliasReservation = (AliasReservation) allocateAndCheck(reservationRequest);
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
                new AliasProviderCapability("{hash}", AliasType.ADOBE_CONNECT_URI).withAllowedAnyRequestedValue());
        getResourceService().createResource(SECURITY_TOKEN, aliasProvider);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-01-01T00:00", "P1Y");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new AliasSpecification(AliasType.ADOBE_CONNECT_URI).withValue("test"));
        AliasReservation aliasReservation = (AliasReservation) allocateAndCheck(reservationRequest);
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
        AliasProviderCapability aliasProviderCapability = new AliasProviderCapability();
        aliasProviderCapability.addAlias(new Alias(AliasType.H323_E164, "950087{value}"));
        aliasProviderCapability.setValueProvider(new ValueProvider.Pattern("{number:090:099}"));
        aliasProvider.addCapability(aliasProviderCapability);
        getResourceService().createResource(SECURITY_TOKEN, aliasProvider);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-01-01T00:00", "P1Y");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new AliasSpecification(AliasType.H323_E164).withValue("950087095"));
        AliasReservation aliasReservation = (AliasReservation) allocateAndCheck(reservationRequest);
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
        firstAliasProvider.addCapability(new AliasProviderCapability("{hash}", AliasType.H323_URI));
        getResourceService().createResource(SECURITY_TOKEN, firstAliasProvider);

        Resource secondAliasProvider = new Resource();
        secondAliasProvider.setName("secondAliasProvider");
        secondAliasProvider.setAllocatable(true);
        secondAliasProvider.addCapability(new AliasProviderCapability("{hash}", AliasType.SIP_URI));
        getResourceService().createResource(SECURITY_TOKEN, secondAliasProvider);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-01-01T00:00", "P1Y");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        AliasSetSpecification aliasSetSpecification = new AliasSetSpecification();
        aliasSetSpecification.addAlias(new AliasSpecification(AliasType.H323_URI));
        aliasSetSpecification.addAlias(new AliasSpecification(AliasType.SIP_URI));
        reservationRequest.setSpecification(aliasSetSpecification);
        Reservation reservation = allocateAndCheck(reservationRequest);
        List<String> childReservationIds = reservation.getChildReservationIds();
        Assert.assertEquals("Reservation should have two child alias reservations.", 2, childReservationIds.size());
        AliasReservation reservationFirst = (AliasReservation) getReservationService().getReservation(SECURITY_TOKEN,
                childReservationIds.get(0));
        Assert.assertEquals(firstAliasProvider.getName(), reservationFirst.getValueReservation().getResourceName());
        AliasReservation reservationSecond = (AliasReservation) getReservationService().getReservation(SECURITY_TOKEN,
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
        AliasProviderCapability aliasProviderCapability =
                new AliasProviderCapability("{hash}").withAllowedAnyRequestedValue();
        aliasProviderCapability.addAlias(new Alias(AliasType.ROOM_NAME, "{requested-value}"));
        aliasProviderCapability.addAlias(new Alias(AliasType.SIP_URI, "{value}"));
        aliasProvider.addCapability(aliasProviderCapability);
        getResourceService().createResource(SECURITY_TOKEN, aliasProvider);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-01-01T00:00", "P1Y");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        AliasSpecification aliasSpecification = new AliasSpecification();
        aliasSpecification.addAliasType(AliasType.ROOM_NAME);
        aliasSpecification.addTechnology(Technology.H323);
        aliasSpecification.addTechnology(Technology.SIP);
        aliasSpecification.setValue("test");
        reservationRequest.setSpecification(aliasSpecification);
        AliasReservation aliasReservation = (AliasReservation) allocateAndCheck(reservationRequest);
        Assert.assertEquals("Room name alias for H.323 or SIP should be allocated.", "test", aliasReservation.getValue());
    }
}