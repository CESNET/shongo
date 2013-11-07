package cz.cesnet.shongo.controller.booking.request;

import cz.cesnet.shongo.Temporal;
import cz.cesnet.shongo.controller.AbstractDatabaseTest;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.booking.compartment.CompartmentSpecification;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.junit.Assert;
import org.junit.Test;

import javax.persistence.EntityManager;

/**
 * Tests for changing state of a {@link cz.cesnet.shongo.controller.booking.request.ReservationRequestSet}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class PreprocessorStateTest extends AbstractDatabaseTest
{
    @Test
    public void test() throws Exception
    {
        EntityManager entityManager = createEntityManager();
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        entityManager.getTransaction().begin();

        // Create reservation request set and manager that will manage it's states
        ReservationRequestSet reservationRequestSet = new ReservationRequestSet();
        reservationRequestSet.setCreatedBy(Authorization.ROOT_USER_ID);
        reservationRequestSet.setUpdatedBy(Authorization.ROOT_USER_ID);
        reservationRequestSet.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequestSet.setSpecification(new CompartmentSpecification());
        reservationRequestManager.create(reservationRequestSet);
        PreprocessorStateManager stateManager = new PreprocessorStateManager(entityManager, reservationRequestSet);

        // Check setter without interval
        stateManager.setState(PreprocessorState.NOT_PREPROCESSED);
        Assert.assertEquals(0, stateManager.getRecordCount());
        try {
            stateManager.setState(PreprocessorState.PREPROCESSED);
            Assert.fail("Exception should be thrown, because setting preprocessed state doesn't make sense!");
        }
        catch (IllegalArgumentException exception) {
        }
        Assert.assertEquals(PreprocessorState.NOT_PREPROCESSED,
                stateManager.getState(Temporal.INTERVAL_INFINITE));

        // Set not-preprocessed state do nothing
        stateManager.setState(PreprocessorState.NOT_PREPROCESSED,
                DateTime.parse("2012-01-01"), DateTime.parse("2012-01-31"));
        Assert.assertEquals(0, stateManager.getRecordCount());

        // Set preprocessed adds record
        stateManager.setState(PreprocessorState.PREPROCESSED,
                DateTime.parse("2012-01-01"), DateTime.parse("2012-01-31T23:59:59"));
        Assert.assertEquals(1, stateManager.getRecordCount());
        // Check before/after state, should be not-preprocessed
        Assert.assertEquals(PreprocessorState.NOT_PREPROCESSED,
                stateManager.getState(
                        DateTime.parse("2011-12-31")));
        Assert.assertEquals(PreprocessorState.NOT_PREPROCESSED,
                stateManager.getState(
                        DateTime.parse("2012-02-01")));
        // Check state inside interval, should be preprocessed
        Assert.assertEquals(PreprocessorState.PREPROCESSED,
                stateManager.getState(
                        DateTime.parse("2012-01-01")));
        Assert.assertEquals(PreprocessorState.PREPROCESSED,
                stateManager.getState(
                        DateTime.parse("2012-01-31")));
        Assert.assertEquals(PreprocessorState.PREPROCESSED,
                stateManager.getState(
                        DateTime.parse("2012-01-02"), DateTime.parse("2012-01-30")));
        // Check state of intersection of preprocessed and not-preprocessed, should be not-preprocessed
        Assert.assertEquals(PreprocessorState.NOT_PREPROCESSED,
                stateManager.getState(
                        DateTime.parse("2012-01-15"), DateTime.parse("2012-02-15")));
        // Check getting sub-interval from interval in which the state is equal to given
        Assert.assertEquals(Interval.parse("2012-02-01/2012-02-15"), stateManager.getInterval(
                PreprocessorState.NOT_PREPROCESSED,
                DateTime.parse("2012-01-15"), DateTime.parse("2012-02-15")));
        Assert.assertEquals(Interval.parse("2011-12-15/2011-12-31T23:59:59"), stateManager.getInterval(
                PreprocessorState.NOT_PREPROCESSED,
                DateTime.parse("2011-12-15"), DateTime.parse("2012-01-15")));

        stateManager.refresh();

        // Check modification of already defined interval
        stateManager.setState(PreprocessorState.NOT_PREPROCESSED,
                DateTime.parse("2012-01-15"), DateTime.parse("2012-02-15T23:59:59"));
        Assert.assertEquals(1, stateManager.getRecordCount());
        stateManager.setState(PreprocessorState.PREPROCESSED,
                DateTime.parse("2012-01-15"), DateTime.parse("2012-01-30T23:59:59"));
        Assert.assertEquals(1, stateManager.getRecordCount());

        stateManager.refresh();

        // Add next new interval
        Assert.assertEquals(1, stateManager.getRecordCount());
        stateManager.setState(PreprocessorState.PREPROCESSED,
                DateTime.parse("2012-02-15"), DateTime.parse("2012-02-28T23:59:59"));
        Assert.assertEquals(2, stateManager.getRecordCount());

        stateManager.refresh();

        // Split first interval
        Assert.assertEquals(2, stateManager.getRecordCount());
        stateManager.setState(PreprocessorState.NOT_PREPROCESSED,
                DateTime.parse("2012-01-13"), DateTime.parse("2012-01-17T23:59:59"));
        Assert.assertEquals(3, stateManager.getRecordCount());

        stateManager.refresh();

        // Merge all intervals
        Assert.assertEquals(3, stateManager.getRecordCount());
        stateManager.setState(PreprocessorState.PREPROCESSED,
                DateTime.parse("2012-01-30"), DateTime.parse("2012-02-15T23:59:59"));
        Assert.assertEquals(2, stateManager.getRecordCount());
        stateManager.setState(PreprocessorState.PREPROCESSED,
                DateTime.parse("2012-01-13"), DateTime.parse("2012-02-17T23:59:59"));
        Assert.assertEquals(1, stateManager.getRecordCount());

        stateManager.refresh();

        // Split to multiple intervals
        Assert.assertEquals(1, stateManager.getRecordCount());
        stateManager.setState(PreprocessorState.NOT_PREPROCESSED,
                DateTime.parse("2012-01-13"), DateTime.parse("2012-01-17T23:59:59"));
        stateManager.setState(PreprocessorState.NOT_PREPROCESSED,
                DateTime.parse("2012-01-31"), DateTime.parse("2012-02-15T23:59:59"));
        Assert.assertEquals(3, stateManager.getRecordCount());

        stateManager.refresh();

        // Merge them at once
        Assert.assertEquals(3, stateManager.getRecordCount());
        stateManager.setState(PreprocessorState.PREPROCESSED,
                DateTime.parse("2012-01-13"), DateTime.parse("2012-02-17T23:59:59"));
        Assert.assertEquals(1, stateManager.getRecordCount());

        entityManager.getTransaction().commit();
        entityManager.close();
    }
}
