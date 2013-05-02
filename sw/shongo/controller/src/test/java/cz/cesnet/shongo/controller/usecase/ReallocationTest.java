package cz.cesnet.shongo.controller.usecase;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.controller.AbstractControllerTest;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.api.*;
import org.junit.Assert;
import org.junit.Test;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReallocationTest extends AbstractControllerTest
{
    @Test
    public void testExtension() throws Exception
    {
        /*Resource aliasProvider = new Resource();
        aliasProvider.setName("aliasProvider");
        aliasProvider.setAllocatable(true);
        aliasProvider.addCapability(new AliasProviderCapability("test", AliasType.ROOM_NAME));
        getResourceService().createResource(SECURITY_TOKEN, aliasProvider);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-01-01T00:00", "P1Y");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new AliasSpecification(AliasType.ROOM_NAME));
        String reservationRequestId = allocate(reservationRequest);
        AliasReservation aliasReservation1 = (AliasReservation) checkAllocated(reservationRequestId);

        reservationRequest = (ReservationRequest) getReservationService().getReservationRequest(
                SECURITY_TOKEN, reservationRequestId);

        reservationRequest.setSlot("2012-01-01T00:00", "P2Y");
        AliasReservation aliasReservation2 = (AliasReservation) allocateAndCheck(reservationRequest);

        Assert.assertEquals(aliasReservation1.getId(), aliasReservation2.getId());*/
    }
}
