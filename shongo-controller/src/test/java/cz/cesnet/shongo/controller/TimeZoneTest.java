package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.api.request.ReservationListRequest;
import org.junit.Assert;
import org.joda.time.*;
import org.junit.Test;

import java.util.List;

/**
 * Tests for {@link org.joda.time.DateTimeZone} handling.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class TimeZoneTest extends AbstractControllerTest
{
    /**
     * {@link org.joda.time.DateTimeZone} to be used in {@link cz.cesnet.shongo.controller.Controller} for this test.
     */
    static private final DateTimeZone CONTROLLER_TIMEZONE = DateTimeZone.forID("UTC");

    @Override
    public void configureSystemProperties()
    {
        super.configureSystemProperties();

        System.setProperty(ControllerConfiguration.TIMEZONE, CONTROLLER_TIMEZONE.getID());
    }

    /**
     * Test that controller stores date/times in configured timezone and returns it in responses.
     *
     * @throws Exception
     */
    @Test
    public void testResponseInUTC() throws Exception
    {
        Resource resource = new Resource();
        resource.setName("resource");
        resource.setAllocatable(true);
        String resourceId = getResourceService().createResource(SECURITY_TOKEN, resource);

        DateTime startDateTime = new DateTime("2014-01-01T08:00", DateTimeZone.forOffsetHours(8));
        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setPurpose(ReservationRequestPurpose.MAINTENANCE);
        reservationRequest.setSlot(startDateTime, Period.parse("PT1H"));
        reservationRequest.setSpecification(new ResourceSpecification(resourceId));
        Reservation reservation = allocateAndCheck(reservationRequest);
        Assert.assertEquals("Date/times should be returned in controller timezone",
                startDateTime.withZone(CONTROLLER_TIMEZONE), reservation.getSlot().getStart());
    }

    /**
     * Test periodic date/time for daylight savings.
     *
     * @throws Exception
     */
    @Test
    public void testPeriodicDateTimeWithZone() throws Exception
    {
        LocalTime localTime = new LocalTime("14:00");
        DateTimeZone timeZone = DateTimeZone.forID("Europe/Prague");

        Resource resource = new Resource();
        resource.setName("resource");
        resource.setAllocatable(true);
        String resourceId = getResourceService().createResource(SECURITY_TOKEN, resource);

        // Daylight saving takes place on 2014-03-30 in Europe/Prague timezone
        DateTime startDateTime = new DateTime("2014-03-24T" + localTime.toString(), timeZone);
        ReservationRequestSet reservationRequest = new ReservationRequestSet();
        reservationRequest.setPurpose(ReservationRequestPurpose.MAINTENANCE);
        reservationRequest.addSlot(new PeriodicDateTimeSlot(
                startDateTime, Period.parse("PT1H"), Period.parse("P1W"), LocalDate.parse("2014-04-07")));
        reservationRequest.setSpecification(new ResourceSpecification(resourceId));
        String reservationRequestId = allocate(SECURITY_TOKEN, reservationRequest);

        // Check that reservations are allocated at the same time before and after the daylight saving change
        List<Reservation> reservations =
                getReservationService().getReservationRequestReservations(SECURITY_TOKEN, reservationRequestId);
        for (Reservation reservation : reservations) {
            Interval slot = reservation.getSlot();
            slot = new Interval(slot.getStart().withZone(timeZone), slot.getEnd().withZone(timeZone));
            LocalTime slotStartLocalTime = slot.getStart().toLocalTime();
            Assert.assertEquals(slot.toString(), localTime, slotStartLocalTime);
        }
    }
}
