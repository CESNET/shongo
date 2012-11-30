package cz.cesnet.shongo.controller.usecase;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.AbstractControllerTest;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.api.*;
import org.joda.time.Period;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * Tests for {@link cz.cesnet.shongo.controller.api.ReservationRequestSet}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationRequestSetTest extends AbstractControllerTest
{
    /**
     * Test modify {@link CompartmentSpecification} to {@link cz.cesnet.shongo.controller.api.RoomSpecification} and delete the request).
     *
     * @throws Exception
     */
    @Test
    public void testModification() throws Exception
    {
        DeviceResource mcu = new DeviceResource();
        mcu.setName("firstMcu");
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(new RoomProviderCapability(10));
        mcu.addCapability(new AliasProviderCapability(Technology.H323, AliasType.E164, "95[d]"));
        mcu.setAllocatable(true);
        String mcuIdentifier = getResourceService().createResource(SECURITY_TOKEN, mcu);

        ReservationRequestSet reservationRequest = new ReservationRequestSet();
        reservationRequest.setName("test");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.addSlot(new DateTimeSlot(
                new PeriodicDateTime("2012-01-01T00:00", "P1W", "2012-01-01"), Period.parse("PT1H")));
        CompartmentSpecification compartmentSpecification = new CompartmentSpecification();
        compartmentSpecification.addSpecification(new ExternalEndpointSetSpecification(Technology.H323, 3));
        reservationRequest.addSpecification(compartmentSpecification);

        String identifier = getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequest);
        runPreprocessor();
        runScheduler();
        checkAllocated(identifier);

        reservationRequest =
                (ReservationRequestSet) getReservationService().getReservationRequest(SECURITY_TOKEN, identifier);
        reservationRequest.removeSpecification(reservationRequest.getSpecifications().get(0));
        RoomSpecification virtualRoomSpecification = new RoomSpecification();
        virtualRoomSpecification.addTechnology(Technology.H323);
        virtualRoomSpecification.setParticipantCount(5);
        reservationRequest.addSpecification(virtualRoomSpecification);
        getReservationService().modifyReservationRequest(SECURITY_TOKEN, reservationRequest);

        getReservationService().deleteReservationRequest(SECURITY_TOKEN, identifier);
    }
}
