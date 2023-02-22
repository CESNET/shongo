package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.controller.booking.reservation.Reservation;
import cz.cesnet.shongo.controller.booking.resource.Resource;
import org.joda.time.Interval;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

/**
 * Tests for {@link SchedulerContext}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class SchedulerContextTest
{
    @Test
    public void test() throws Exception
    {
        Interval interval = new Interval("2012/2013");

        Resource resource1 = new Resource();
        Resource resource2 = new Resource();
        Resource resource3 = new Resource();
        Reservation allocatedReservation1 = new Reservation();
        Reservation allocatedReservation2 = new Reservation();
        Reservation allocatedReservation3 = new Reservation();
        AvailableReservation<Reservation> reusableReservation1 = AvailableReservation.create(new Reservation());
        AvailableReservation<Reservation> reusableReservation2 = AvailableReservation.create(new Reservation());
        AvailableReservation<Reservation> reusableReservation3 = AvailableReservation.create(new Reservation());

        // Init cache transaction
        SchedulerContext schedulerContext = new SchedulerContext(interval.getStart(), null, null, null);
        SchedulerContextState schedulerContextState = schedulerContext.getState();
        schedulerContextState.addReferencedResource(resource1);
        schedulerContextState.addAllocatedReservation(allocatedReservation1);
        schedulerContextState.addAvailableReservation(reusableReservation1);

        ////////////////////////////////////////////////////////////////////////////////

        // Add objects by savepoint
        SchedulerContextState.Savepoint savepoint1 = schedulerContextState.createSavepoint();
        schedulerContextState.addReferencedResource(resource2);
        schedulerContextState.addAllocatedReservation(allocatedReservation2);
        schedulerContextState.addAvailableReservation(reusableReservation2);
        Assert.assertEquals(savepoint1, schedulerContextState.getCurrentSavepoint());

        // Add objects by another savepoint
        SchedulerContextState.Savepoint savepoint2 = schedulerContextState.createSavepoint();
        schedulerContextState.addReferencedResource(resource3);
        schedulerContextState.addAllocatedReservation(allocatedReservation3);
        schedulerContextState.addAvailableReservation(reusableReservation3);
        Assert.assertEquals(savepoint2, schedulerContextState.getCurrentSavepoint());

        // Test that cache transaction contains all added objects
        Assert.assertEquals(buildSet(resource1, resource2, resource3),
                schedulerContextState.getReferencedResources());
        Assert.assertEquals(buildSet(allocatedReservation1, allocatedReservation2, allocatedReservation3),
                schedulerContextState.getAllocatedReservations());
        Assert.assertEquals(buildSet(reusableReservation1, reusableReservation2, reusableReservation3),
                schedulerContextState.getAvailableReservations());

        // Revert second savepoint and check the state of the transaction
        savepoint2.revert();
        Assert.assertEquals(savepoint1, schedulerContextState.getCurrentSavepoint());
        Assert.assertEquals(buildSet(resource1, resource2),
                schedulerContextState.getReferencedResources());
        Assert.assertEquals(buildSet(allocatedReservation1, allocatedReservation2),
                schedulerContextState.getAllocatedReservations());
        Assert.assertEquals(buildSet(reusableReservation1, reusableReservation2),
                schedulerContextState.getAvailableReservations());

        // Revert first savepoint and check the state of the transaction
        savepoint1.revert();
        Assert.assertEquals(null, schedulerContextState.getCurrentSavepoint());
        Assert.assertEquals(buildSet(resource1), schedulerContextState.getReferencedResources());
        Assert.assertEquals(buildSet(allocatedReservation1), schedulerContextState.getAllocatedReservations());
        Assert.assertEquals(buildSet(reusableReservation1), schedulerContextState.getAvailableReservations());

        ////////////////////////////////////////////////////////////////////////////////

        // Create new savepoint and add some objects
        savepoint1 = schedulerContextState.createSavepoint();
        schedulerContextState.addReferencedResource(resource2);
        schedulerContextState.addReferencedResource(resource3);
        schedulerContextState.addAllocatedReservation(allocatedReservation2);
        schedulerContextState.addAllocatedReservation(allocatedReservation3);
        schedulerContextState.addAvailableReservation(reusableReservation2);
        schedulerContextState.addAvailableReservation(reusableReservation3);
        Assert.assertEquals(savepoint1, schedulerContextState.getCurrentSavepoint());

        // Create another savepoint and remove some added objects
        savepoint2 = schedulerContextState.createSavepoint();
        schedulerContextState.removeReferencedResource(resource2);
        schedulerContextState.removeAllocatedReservation(allocatedReservation2);
        schedulerContextState.removeAvailableReservation(reusableReservation2);
        Assert.assertEquals(savepoint2, schedulerContextState.getCurrentSavepoint());

        // Check proper state of cache transaction
        Assert.assertEquals(buildSet(resource1, resource3),
                schedulerContextState.getReferencedResources());
        Assert.assertEquals(buildSet(allocatedReservation1, allocatedReservation3),
                schedulerContextState.getAllocatedReservations());
        Assert.assertEquals(buildSet(reusableReservation1, reusableReservation3),
                schedulerContextState.getAvailableReservations());

        // Revert savepoint which removed some objects and check that objects were restored
        savepoint2.revert();
        Assert.assertEquals(savepoint1, schedulerContextState.getCurrentSavepoint());
        Assert.assertEquals(buildSet(resource1, resource2, resource3),
                schedulerContextState.getReferencedResources());
        Assert.assertEquals(buildSet(allocatedReservation1, allocatedReservation2, allocatedReservation3),
                schedulerContextState.getAllocatedReservations());
        Assert.assertEquals(buildSet(reusableReservation1, reusableReservation2, reusableReservation3),
                schedulerContextState.getAvailableReservations());

        // Revert first savepoint and check the starting state of cache transaction
        savepoint1.revert();
        Assert.assertEquals(null, schedulerContextState.getCurrentSavepoint());
        Assert.assertEquals(buildSet(resource1), schedulerContextState.getReferencedResources());
        Assert.assertEquals(buildSet(allocatedReservation1), schedulerContextState.getAllocatedReservations());
        Assert.assertEquals(buildSet(reusableReservation1), schedulerContextState.getAvailableReservations());

        ////////////////////////////////////////////////////////////////////////////////

        // Create new savepoint and add the rest of reservations
        savepoint1 = schedulerContextState.createSavepoint();
        schedulerContextState.addAllocatedReservation(allocatedReservation2);
        schedulerContextState.addAllocatedReservation(allocatedReservation3);
        schedulerContextState.addAvailableReservation(reusableReservation2);
        schedulerContextState.addAvailableReservation(reusableReservation3);
        Assert.assertEquals(savepoint1, schedulerContextState.getCurrentSavepoint());

        // Create parent reservations with child which is already added
        Reservation allocatedReservationParent1 = new Reservation();
        allocatedReservationParent1.addChildReservation(allocatedReservation1);
        Reservation allocatedReservationParent2 = new Reservation();
        allocatedReservationParent2.addChildReservation(allocatedReservation2);
        Reservation allocatedReservationParent3 = new Reservation();
        allocatedReservationParent3.addChildReservation(allocatedReservation3);
        AvailableReservation<Reservation> reusableReservationParent1 = AvailableReservation.create(new Reservation());
        reusableReservationParent1.getOriginalReservation().addChildReservation(
                reusableReservation1.getOriginalReservation());
        AvailableReservation<Reservation> reusableReservationParent2 = AvailableReservation.create(new Reservation());
        reusableReservationParent2.getOriginalReservation().addChildReservation(
                reusableReservation2.getOriginalReservation());
        AvailableReservation<Reservation> reusableReservationParent3 = AvailableReservation.create(new Reservation());
        reusableReservationParent3.getOriginalReservation().addChildReservation(
                reusableReservation3.getOriginalReservation());

        // Add parent reservations to the cache transaction
        savepoint2 = schedulerContextState.createSavepoint();
        schedulerContextState.addAllocatedReservation(allocatedReservationParent1);
        schedulerContextState.addAllocatedReservation(allocatedReservationParent2);
        schedulerContextState.addAllocatedReservation(allocatedReservationParent3);
        schedulerContextState.addAvailableReservation(reusableReservationParent1);
        schedulerContextState.addAvailableReservation(reusableReservationParent2);
        schedulerContextState.addAvailableReservation(reusableReservationParent3);
        Assert.assertEquals(savepoint2, schedulerContextState.getCurrentSavepoint());

        // Check proper state of cache transaction
        Assert.assertEquals(buildSet(allocatedReservation1, allocatedReservation2, allocatedReservation3,
                allocatedReservationParent1, allocatedReservationParent2, allocatedReservationParent3),
                schedulerContextState.getAllocatedReservations());
        Assert.assertEquals(buildSet(reusableReservation1, reusableReservation2, reusableReservation3,
                reusableReservationParent1, reusableReservationParent2, reusableReservationParent3),
                schedulerContextState.getAvailableReservations());

        // Revert savepoint which added parent reservations and check that children are retained
        savepoint2.revert();
        Assert.assertEquals(savepoint1, schedulerContextState.getCurrentSavepoint());
        Assert.assertEquals(buildSet(allocatedReservation1, allocatedReservation2, allocatedReservation3),
                schedulerContextState.getAllocatedReservations());
        Assert.assertEquals(buildSet(reusableReservation1, reusableReservation2, reusableReservation3),
                schedulerContextState.getAvailableReservations());
    }

    /**
     * @param array
     * @return set from array
     */
    @SafeVarargs
    private final <T> Set<T> buildSet(T... array)
    {
        Set<T> set = new HashSet<T>();
        for (T item : array) {
            set.add(item);
        }
        return set;
    }
}
