package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.controller.AbstractDatabaseTest;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.junit.Test;

import javax.persistence.EntityManager;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

/**
 * Test for state of {@link ReservationRequest}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationRequestStateTest extends AbstractDatabaseTest
{
    @Test
    public void test() throws Exception
    {
        EntityManager entityManager = getEntityManager();
        entityManager.getTransaction().begin();

        // Create reservation request and manager that will manage it's states
        ReservationRequest reservationRequest = new ReservationRequest();
        entityManager.persist(reservationRequest);
        ReservationRequestStateManager stateManager = new ReservationRequestStateManager(entityManager, reservationRequest);

        // Check setter without interval
        stateManager.setState(ReservationRequest.State.NOT_PREPROCESSED);
        assertEquals(0, stateManager.getRecordCount());
        try {
            stateManager.setState(ReservationRequest.State.PREPROCESSED);
            fail("Exception should be thrown, because setting preprocessed state doesn't make sense!");
        }
        catch (IllegalArgumentException exception) {
        }
        assertEquals(ReservationRequest.State.NOT_PREPROCESSED,
                stateManager.getState(DateTime.parse("0001-01-01"), DateTime.parse("9999-01-01")));

        // Set not-preprocessed state do nothing
        stateManager.setState(ReservationRequest.State.NOT_PREPROCESSED,
                DateTime.parse("2012-01-01"), DateTime.parse("2012-01-31"));
        assertEquals(0, stateManager.getRecordCount());

        // Set preprocessed adds record
        stateManager.setState(ReservationRequest.State.PREPROCESSED,
                DateTime.parse("2012-01-01"), DateTime.parse("2012-01-31T23:59:59"));
        assertEquals(1, stateManager.getRecordCount());
        // Check before/after state, should be not-preprocessed
        assertEquals(ReservationRequest.State.NOT_PREPROCESSED, stateManager.getState(
                DateTime.parse("2011-12-31")));
        assertEquals(ReservationRequest.State.NOT_PREPROCESSED, stateManager.getState(
                DateTime.parse("2012-02-01")));
        // Check state inside interval, should be preprocessed
        assertEquals(ReservationRequest.State.PREPROCESSED, stateManager.getState(
                DateTime.parse("2012-01-01")));
        assertEquals(ReservationRequest.State.PREPROCESSED, stateManager.getState(
                DateTime.parse("2012-01-31")));
        assertEquals(ReservationRequest.State.PREPROCESSED, stateManager.getState(
                DateTime.parse("2012-01-02"), DateTime.parse("2012-01-30")));
        // Check state of intersection of preprocessed and not-preprocessed, should be not-preprocessed
        assertEquals(ReservationRequest.State.NOT_PREPROCESSED, stateManager.getState(
                DateTime.parse("2012-01-15"), DateTime.parse("2012-02-15")));
        // Check getting sub-interval from interval in which the state is equal to given
        assertEquals(Interval.parse("2012-02-01/2012-02-15"), stateManager.getInterval(
                ReservationRequest.State.NOT_PREPROCESSED, DateTime.parse("2012-01-15"), DateTime.parse("2012-02-15")));
        assertEquals(Interval.parse("2011-12-15/2011-12-31T23:59:59"), stateManager.getInterval(
                ReservationRequest.State.NOT_PREPROCESSED, DateTime.parse("2011-12-15"), DateTime.parse("2012-01-15")));

        stateManager.refresh();

        // Check modification of already defined interval
        stateManager.setState(ReservationRequest.State.NOT_PREPROCESSED,
                DateTime.parse("2012-01-15"), DateTime.parse("2012-02-15T23:59:59"));
        assertEquals(1, stateManager.getRecordCount());
        stateManager.setState(ReservationRequest.State.PREPROCESSED,
                DateTime.parse("2012-01-15"), DateTime.parse("2012-01-30T23:59:59"));
        assertEquals(1, stateManager.getRecordCount());

        stateManager.refresh();

        // Add next new interval
        assertEquals(1, stateManager.getRecordCount());
        stateManager.setState(ReservationRequest.State.PREPROCESSED,
                DateTime.parse("2012-02-15"), DateTime.parse("2012-02-28T23:59:59"));
        assertEquals(2, stateManager.getRecordCount());

        stateManager.refresh();

        // Split first interval
        assertEquals(2, stateManager.getRecordCount());
        stateManager.setState(ReservationRequest.State.NOT_PREPROCESSED,
                DateTime.parse("2012-01-13"), DateTime.parse("2012-01-17T23:59:59"));
        assertEquals(3, stateManager.getRecordCount());

        stateManager.refresh();

        // Merge all intervals
        assertEquals(3, stateManager.getRecordCount());
        stateManager.setState(ReservationRequest.State.PREPROCESSED,
                DateTime.parse("2012-01-30"), DateTime.parse("2012-02-15T23:59:59"));
        assertEquals(2, stateManager.getRecordCount());
        stateManager.setState(ReservationRequest.State.PREPROCESSED,
                DateTime.parse("2012-01-13"), DateTime.parse("2012-02-17T23:59:59"));
        assertEquals(1, stateManager.getRecordCount());

        stateManager.refresh();

        // Split to multiple intervals
        assertEquals(1, stateManager.getRecordCount());
        stateManager.setState(ReservationRequest.State.NOT_PREPROCESSED,
                DateTime.parse("2012-01-13"), DateTime.parse("2012-01-17T23:59:59"));
        stateManager.setState(ReservationRequest.State.NOT_PREPROCESSED,
                DateTime.parse("2012-01-31"), DateTime.parse("2012-02-15T23:59:59"));
        assertEquals(3, stateManager.getRecordCount());

        stateManager.refresh();

        // Merge them at once
        assertEquals(3, stateManager.getRecordCount());
        stateManager.setState(ReservationRequest.State.PREPROCESSED,
                DateTime.parse("2012-01-13"), DateTime.parse("2012-02-17T23:59:59"));
        assertEquals(1, stateManager.getRecordCount());

        entityManager.getTransaction().commit();
    }
}
