package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.resource.Resource;
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
        AvailableReservation<Reservation> providedReservation1 = AvailableReservation.create(new Reservation());
        AvailableReservation<Reservation> providedReservation2 = AvailableReservation.create(new Reservation());
        AvailableReservation<Reservation> providedReservation3 = AvailableReservation.create(new Reservation());

        // Init cache transaction
        SchedulerContext schedulerContext = new SchedulerContext(interval, null, null);
        schedulerContext.addReferencedResource(resource1);
        schedulerContext.addAllocatedReservation(allocatedReservation1);
        schedulerContext.addAvailableReservation(providedReservation1);

        ////////////////////////////////////////////////////////////////////////////////

        // Add objects by savepoint
        SchedulerContext.Savepoint savepoint1 = schedulerContext.createSavepoint();
        schedulerContext.addReferencedResource(resource2);
        schedulerContext.addAllocatedReservation(allocatedReservation2);
        schedulerContext.addAvailableReservation(providedReservation2);
        Assert.assertEquals(savepoint1, schedulerContext.getCurrentSavepoint());

        // Add objects by another savepoint
        SchedulerContext.Savepoint savepoint2 = schedulerContext.createSavepoint();
        schedulerContext.addReferencedResource(resource3);
        schedulerContext.addAllocatedReservation(allocatedReservation3);
        schedulerContext.addAvailableReservation(providedReservation3);
        Assert.assertEquals(savepoint2, schedulerContext.getCurrentSavepoint());

        // Test that cache transaction contains all added objects
        Assert.assertEquals(buildSet(resource1, resource2, resource3),
                schedulerContext.getReferencedResources());
        Assert.assertEquals(buildSet(allocatedReservation1, allocatedReservation2, allocatedReservation3),
                schedulerContext.getAllocatedReservations());
        Assert.assertEquals(buildSet(providedReservation1, providedReservation2, providedReservation3),
                schedulerContext.getAvailableReservations());

        // Revert second savepoint and check the state of the transaction
        savepoint2.revert();
        Assert.assertEquals(savepoint1, schedulerContext.getCurrentSavepoint());
        Assert.assertEquals(buildSet(resource1, resource2),
                schedulerContext.getReferencedResources());
        Assert.assertEquals(buildSet(allocatedReservation1, allocatedReservation2),
                schedulerContext.getAllocatedReservations());
        Assert.assertEquals(buildSet(providedReservation1, providedReservation2),
                schedulerContext.getAvailableReservations());

        // Revert first savepoint and check the state of the transaction
        savepoint1.revert();
        Assert.assertEquals(null, schedulerContext.getCurrentSavepoint());
        Assert.assertEquals(buildSet(resource1), schedulerContext.getReferencedResources());
        Assert.assertEquals(buildSet(allocatedReservation1), schedulerContext.getAllocatedReservations());
        Assert.assertEquals(buildSet(providedReservation1), schedulerContext.getAvailableReservations());

        ////////////////////////////////////////////////////////////////////////////////

        // Create new savepoint and add some objects
        savepoint1 = schedulerContext.createSavepoint();
        schedulerContext.addReferencedResource(resource2);
        schedulerContext.addReferencedResource(resource3);
        schedulerContext.addAllocatedReservation(allocatedReservation2);
        schedulerContext.addAllocatedReservation(allocatedReservation3);
        schedulerContext.addAvailableReservation(providedReservation2);
        schedulerContext.addAvailableReservation(providedReservation3);
        Assert.assertEquals(savepoint1, schedulerContext.getCurrentSavepoint());

        // Create another savepoint and remove some added objects
        savepoint2 = schedulerContext.createSavepoint();
        schedulerContext.removeReferencedResource(resource2);
        schedulerContext.removeAllocatedReservation(allocatedReservation2);
        schedulerContext.removeAvailableReservation(providedReservation2);
        Assert.assertEquals(savepoint2, schedulerContext.getCurrentSavepoint());

        // Check proper state of cache transaction
        Assert.assertEquals(buildSet(resource1, resource3),
                schedulerContext.getReferencedResources());
        Assert.assertEquals(buildSet(allocatedReservation1, allocatedReservation3),
                schedulerContext.getAllocatedReservations());
        Assert.assertEquals(buildSet(providedReservation1, providedReservation3),
                schedulerContext.getAvailableReservations());

        // Revert savepoint which removed some objects and check that objects were restored
        savepoint2.revert();
        Assert.assertEquals(savepoint1, schedulerContext.getCurrentSavepoint());
        Assert.assertEquals(buildSet(resource1, resource2, resource3),
                schedulerContext.getReferencedResources());
        Assert.assertEquals(buildSet(allocatedReservation1, allocatedReservation2, allocatedReservation3),
                schedulerContext.getAllocatedReservations());
        Assert.assertEquals(buildSet(providedReservation1, providedReservation2, providedReservation3),
                schedulerContext.getAvailableReservations());

        // Revert first savepoint and check the starting state of cache transaction
        savepoint1.revert();
        Assert.assertEquals(null, schedulerContext.getCurrentSavepoint());
        Assert.assertEquals(buildSet(resource1), schedulerContext.getReferencedResources());
        Assert.assertEquals(buildSet(allocatedReservation1), schedulerContext.getAllocatedReservations());
        Assert.assertEquals(buildSet(providedReservation1), schedulerContext.getAvailableReservations());

        ////////////////////////////////////////////////////////////////////////////////

        // Create new savepoint and add the rest of reservations
        savepoint1 = schedulerContext.createSavepoint();
        schedulerContext.addAllocatedReservation(allocatedReservation2);
        schedulerContext.addAllocatedReservation(allocatedReservation3);
        schedulerContext.addAvailableReservation(providedReservation2);
        schedulerContext.addAvailableReservation(providedReservation3);
        Assert.assertEquals(savepoint1, schedulerContext.getCurrentSavepoint());

        // Create parent reservations with child which is already added
        Reservation allocatedReservationParent1 = new Reservation();
        allocatedReservationParent1.addChildReservation(allocatedReservation1);
        Reservation allocatedReservationParent2 = new Reservation();
        allocatedReservationParent2.addChildReservation(allocatedReservation2);
        Reservation allocatedReservationParent3 = new Reservation();
        allocatedReservationParent3.addChildReservation(allocatedReservation3);
        AvailableReservation<Reservation> providedReservationParent1 = AvailableReservation.create(new Reservation());
        providedReservationParent1.getOriginalReservation().addChildReservation(
                providedReservation1.getOriginalReservation());
        AvailableReservation<Reservation> providedReservationParent2 = AvailableReservation.create(new Reservation());
        providedReservationParent2.getOriginalReservation().addChildReservation(
                providedReservation2.getOriginalReservation());
        AvailableReservation<Reservation> providedReservationParent3 = AvailableReservation.create(new Reservation());
        providedReservationParent3.getOriginalReservation().addChildReservation(
                providedReservation3.getOriginalReservation());

        // Add parent reservations to the cache transaction
        savepoint2 = schedulerContext.createSavepoint();
        schedulerContext.addAllocatedReservation(allocatedReservationParent1);
        schedulerContext.addAllocatedReservation(allocatedReservationParent2);
        schedulerContext.addAllocatedReservation(allocatedReservationParent3);
        schedulerContext.addAvailableReservation(providedReservationParent1);
        schedulerContext.addAvailableReservation(providedReservationParent2);
        schedulerContext.addAvailableReservation(providedReservationParent3);
        Assert.assertEquals(savepoint2, schedulerContext.getCurrentSavepoint());

        // Check proper state of cache transaction
        Assert.assertEquals(buildSet(allocatedReservation1, allocatedReservation2, allocatedReservation3,
                allocatedReservationParent1, allocatedReservationParent2, allocatedReservationParent3),
                schedulerContext.getAllocatedReservations());
        Assert.assertEquals(buildSet(providedReservation1, providedReservation2, providedReservation3,
                providedReservationParent1, providedReservationParent2, providedReservationParent3),
                schedulerContext.getAvailableReservations());

        // Revert savepoint which added parent reservations and check that children are retained
        savepoint2.revert();
        Assert.assertEquals(savepoint1, schedulerContext.getCurrentSavepoint());
        Assert.assertEquals(buildSet(allocatedReservation1, allocatedReservation2, allocatedReservation3),
                schedulerContext.getAllocatedReservations());
        Assert.assertEquals(buildSet(providedReservation1, providedReservation2, providedReservation3),
                schedulerContext.getAvailableReservations());
    }

    /**
     * @param array
     * @return set from array
     */
    private <T> Set<T> buildSet(T... array)
    {
        Set<T> set = new HashSet<T>();
        for (T item : array) {
            set.add(item);
        }
        return set;
    }
}
