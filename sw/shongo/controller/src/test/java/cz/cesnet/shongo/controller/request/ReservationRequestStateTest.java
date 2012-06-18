package cz.cesnet.shongo.controller.request;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

/**
 * Test for {@link ReservationRequestState}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationRequestStateTest
{
    @Test
    public void test() throws Exception
    {
        ReservationRequestState reservationRequestState = new ReservationRequestState();

        // Check setter without interval
        reservationRequestState.setState(ReservationRequestState.NOT_PREPROCESSED);
        assertEquals(0, reservationRequestState.getRecordCount());
        try {
            reservationRequestState.setState(ReservationRequestState.PREPROCESSED);
            fail("Exception should be thrown, because setting preprocessed state doesn't make sense!");
        }
        catch (IllegalArgumentException exception) {
        }
        assertEquals(ReservationRequestState.NOT_PREPROCESSED,
                reservationRequestState.getState(DateTime.parse("0001-01-01"), DateTime.parse("9999-01-01")));

        // Set not-preprocessed state do nothing
        reservationRequestState.setState(ReservationRequestState.NOT_PREPROCESSED,
                DateTime.parse("2012-01-01"), DateTime.parse("2012-01-31"));
        assertEquals(0, reservationRequestState.getRecordCount());

        // Set preprocessed adds record
        reservationRequestState.setState(ReservationRequestState.PREPROCESSED,
                DateTime.parse("2012-01-01"), DateTime.parse("2012-01-31T23:59:59"));
        assertEquals(1, reservationRequestState.getRecordCount());
        // Check before/after state, should be not-preprocessed
        assertEquals(ReservationRequestState.NOT_PREPROCESSED, reservationRequestState.getState(
                DateTime.parse("2011-12-31")));
        assertEquals(ReservationRequestState.NOT_PREPROCESSED, reservationRequestState.getState(
                DateTime.parse("2012-02-01")));
        // Check state inside interval, should be preprocessed
        assertEquals(ReservationRequestState.PREPROCESSED, reservationRequestState.getState(
                DateTime.parse("2012-01-01")));
        assertEquals(ReservationRequestState.PREPROCESSED, reservationRequestState.getState(
                DateTime.parse("2012-01-31")));
        assertEquals(ReservationRequestState.PREPROCESSED, reservationRequestState.getState(
                DateTime.parse("2012-01-02"), DateTime.parse("2012-01-30")));
        // Check state of intersection of preprocessed and not-preprocessed, should be not-preprocessed
        assertEquals(ReservationRequestState.NOT_PREPROCESSED, reservationRequestState.getState(
                DateTime.parse("2012-01-15"), DateTime.parse("2012-02-15")));
        // Check getting sub-interval from interval in which the state is equal to given
        assertEquals(Interval.parse("2012-02-01/2012-02-15"), reservationRequestState.getInterval(
                ReservationRequestState.NOT_PREPROCESSED, DateTime.parse("2012-01-15"), DateTime.parse("2012-02-15")));
        assertEquals(Interval.parse("2011-12-15/2011-12-31T23:59:59"), reservationRequestState.getInterval(
                ReservationRequestState.NOT_PREPROCESSED, DateTime.parse("2011-12-15"), DateTime.parse("2012-01-15")));

        // Check modification of already defined interval
        reservationRequestState.setState(ReservationRequestState.NOT_PREPROCESSED,
                DateTime.parse("2012-01-15"), DateTime.parse("2012-02-15T23:59:59"));
        assertEquals(1, reservationRequestState.getRecordCount());
        reservationRequestState.setState(ReservationRequestState.PREPROCESSED,
                DateTime.parse("2012-01-15"), DateTime.parse("2012-01-31T23:59:59"));
        assertEquals(1, reservationRequestState.getRecordCount());

        // Add next new interval
        reservationRequestState.setState(ReservationRequestState.PREPROCESSED,
                DateTime.parse("2012-02-15"), DateTime.parse("2012-02-28T23:59:59"));
        assertEquals(2, reservationRequestState.getRecordCount());

        // Split first interval
        reservationRequestState.setState(ReservationRequestState.NOT_PREPROCESSED,
                DateTime.parse("2012-01-13"), DateTime.parse("2012-01-17T23:59:59"));
        assertEquals(3, reservationRequestState.getRecordCount());

        // Merge all intervals
        reservationRequestState.setState(ReservationRequestState.PREPROCESSED,
                DateTime.parse("2012-01-31"), DateTime.parse("2012-02-15T23:59:59"));
        assertEquals(2, reservationRequestState.getRecordCount());
        reservationRequestState.setState(ReservationRequestState.PREPROCESSED,
                DateTime.parse("2012-01-13"), DateTime.parse("2012-02-17T23:59:59"));
        assertEquals(1, reservationRequestState.getRecordCount());

        // Split to multiple intervals and merge it at once
        reservationRequestState.setState(ReservationRequestState.NOT_PREPROCESSED,
                DateTime.parse("2012-01-13"), DateTime.parse("2012-01-17T23:59:59"));
        reservationRequestState.setState(ReservationRequestState.NOT_PREPROCESSED,
                DateTime.parse("2012-01-31"), DateTime.parse("2012-02-15T23:59:59"));
        assertEquals(3, reservationRequestState.getRecordCount());
        reservationRequestState.setState(ReservationRequestState.PREPROCESSED,
                DateTime.parse("2012-01-13"), DateTime.parse("2012-02-17T23:59:59"));
        assertEquals(1, reservationRequestState.getRecordCount());
    }
}
