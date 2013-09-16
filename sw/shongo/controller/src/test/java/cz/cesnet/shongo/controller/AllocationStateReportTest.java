package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import org.junit.Assert;
import org.junit.Test;

import java.util.Locale;

/**
 * Tests for {@link AllocationStateReport}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AllocationStateReportTest extends AbstractControllerTest
{
    @Test
    public void testReusedReservationRequestSlot() throws Exception
    {
    }

    @Test
    public void testReusedReservationRequestAlreadyUsed() throws Exception
    {
    }

    @Test
    public void testExceedMaximumFuture() throws Exception
    {
    }

    @Test
    public void testExceedRoomCapacity() throws Exception
    {
    }

    @Test
    public void testAliasValueAlreadyAllocated() throws Exception
    {
        Resource aliasProvider = new Resource();
        aliasProvider.setName("aliasProvider");
        aliasProvider.setAllocatable(true);
        aliasProvider.addCapability(new AliasProviderCapability("{has}", AliasType.ROOM_NAME).withAllowedAnyRequestedValue());
        getResourceService().createResource(SECURITY_TOKEN, aliasProvider);

        ReservationRequest reservationRequestFirst = new ReservationRequest();
        reservationRequestFirst.setSlot("2012-01-01T00:00", "P1Y");
        reservationRequestFirst.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequestFirst.setSpecification(new AliasSpecification(AliasType.ROOM_NAME).withValue("test"));
        AliasReservation aliasReservation = (AliasReservation) allocateAndCheck(reservationRequestFirst);
        Assert.assertEquals("Requested value should be allocated.", "test", aliasReservation.getValue());

        ReservationRequest reservationRequestSecond = new ReservationRequest();
        reservationRequestSecond.setSlot("2012-06-01T00:00", "P1M");
        reservationRequestSecond.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequestSecond.setSpecification(new AliasSpecification(AliasType.ROOM_NAME).withValue("test"));
        String reservationRequestSecondId = allocate(reservationRequestSecond);
        checkAllocationFailed(reservationRequestSecondId);

        finish(reservationRequestSecondId, AllocationStateReport.AliasAlreadyAllocated.class);
    }

    @Test
    public void testAliasValueNotAvailable() throws Exception
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
        reservationRequestSecond.setSlot("2012-06-01T00:00", "P1Y");
        reservationRequestSecond.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequestSecond.setSpecification(new AliasSpecification(AliasType.ROOM_NAME));
        String reservationRequestSecondId = allocate(reservationRequestSecond);
        checkAllocationFailed(reservationRequestSecondId);

        finish(reservationRequestSecondId, AllocationStateReport.AliasNotAvailable.class);
    }

    private void finish(String reservationRequestId, Class<? extends AllocationStateReport.AllocationError> requiredType)
    {
        Locale locale = UserSettings.LOCALE_CZECH;
        ReservationService reservationService = getReservationService();
        ReservationRequest reservationRequest = (ReservationRequest)
                reservationService.getReservationRequest(SECURITY_TOKEN, reservationRequestId);
        AllocationStateReport allocationStateReport = reservationRequest.getAllocationStateReport();
        AllocationStateReport.AllocationError allocationError = allocationStateReport.toAllocationError();
        System.err.println(allocationError.getMessage(locale));
        if (allocationError.isUnknown()) {
            System.err.println(allocationStateReport.toString(locale).trim());
        }
        Assert.assertEquals(requiredType, allocationError.getClass());
    }
}
