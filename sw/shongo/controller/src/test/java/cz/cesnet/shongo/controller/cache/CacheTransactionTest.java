package cz.cesnet.shongo.controller.cache;

import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.resource.Resource;
import org.joda.time.Interval;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

/**
 * Tests for {@link CacheTransaction}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class CacheTransactionTest
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
        Reservation providedReservation1 = new Reservation();
        Reservation providedReservation2 = new Reservation();
        Reservation providedReservation3 = new Reservation();

        // Init cache transaction
        CacheTransaction cacheTransaction = new CacheTransaction(interval);
        cacheTransaction.addReferencedResource(resource1);
        cacheTransaction.addAllocatedReservation(allocatedReservation1);
        cacheTransaction.addProvidedReservation(providedReservation1);

        ////////////////////////////////////////////////////////////////////////////////

        // Add objects by savepoint
        CacheTransaction.Savepoint savepoint1 = cacheTransaction.createSavepoint();
        cacheTransaction.addReferencedResource(resource2);
        cacheTransaction.addAllocatedReservation(allocatedReservation2);
        cacheTransaction.addProvidedReservation(providedReservation2);
        Assert.assertEquals(savepoint1, cacheTransaction.getCurrentSavepoint());

        // Add objects by another savepoint
        CacheTransaction.Savepoint savepoint2 = cacheTransaction.createSavepoint();
        cacheTransaction.addReferencedResource(resource3);
        cacheTransaction.addAllocatedReservation(allocatedReservation3);
        cacheTransaction.addProvidedReservation(providedReservation3);
        Assert.assertEquals(savepoint2, cacheTransaction.getCurrentSavepoint());

        // Test that cache transaction contains all added objects
        Assert.assertEquals(buildSet(resource1, resource2, resource3),
                cacheTransaction.getReferencedResources());
        Assert.assertEquals(buildSet(allocatedReservation1, allocatedReservation2, allocatedReservation3),
                cacheTransaction.getAllocatedReservations());
        Assert.assertEquals(buildSet(providedReservation1, providedReservation2, providedReservation3),
                cacheTransaction.getProvidedReservations());

        // Revert second savepoint and check the state of the transaction
        savepoint2.revert();
        Assert.assertEquals(savepoint1, cacheTransaction.getCurrentSavepoint());
        Assert.assertEquals(buildSet(resource1, resource2),
                cacheTransaction.getReferencedResources());
        Assert.assertEquals(buildSet(allocatedReservation1, allocatedReservation2),
                cacheTransaction.getAllocatedReservations());
        Assert.assertEquals(buildSet(providedReservation1, providedReservation2),
                cacheTransaction.getProvidedReservations());

        // Revert first savepoint and check the state of the transaction
        savepoint1.revert();
        Assert.assertEquals(null, cacheTransaction.getCurrentSavepoint());
        Assert.assertEquals(buildSet(resource1), cacheTransaction.getReferencedResources());
        Assert.assertEquals(buildSet(allocatedReservation1), cacheTransaction.getAllocatedReservations());
        Assert.assertEquals(buildSet(providedReservation1), cacheTransaction.getProvidedReservations());

        ////////////////////////////////////////////////////////////////////////////////

        // Create new savepoint and add some objects
        savepoint1 = cacheTransaction.createSavepoint();
        cacheTransaction.addReferencedResource(resource2);
        cacheTransaction.addReferencedResource(resource3);
        cacheTransaction.addAllocatedReservation(allocatedReservation2);
        cacheTransaction.addAllocatedReservation(allocatedReservation3);
        cacheTransaction.addProvidedReservation(providedReservation2);
        cacheTransaction.addProvidedReservation(providedReservation3);
        Assert.assertEquals(savepoint1, cacheTransaction.getCurrentSavepoint());

        // Create another savepoint and remove some added objects
        savepoint2 = cacheTransaction.createSavepoint();
        cacheTransaction.removeReferencedResource(resource2);
        cacheTransaction.removeAllocatedReservation(allocatedReservation2);
        cacheTransaction.removeProvidedReservation(providedReservation2);
        Assert.assertEquals(savepoint2, cacheTransaction.getCurrentSavepoint());

        // Check proper state of cache transaction
        Assert.assertEquals(buildSet(resource1, resource3),
                cacheTransaction.getReferencedResources());
        Assert.assertEquals(buildSet(allocatedReservation1, allocatedReservation3),
                cacheTransaction.getAllocatedReservations());
        Assert.assertEquals(buildSet(providedReservation1, providedReservation3),
                cacheTransaction.getProvidedReservations());

        // Revert savepoint which removed some objects and check that objects were restored
        savepoint2.revert();
        Assert.assertEquals(savepoint1, cacheTransaction.getCurrentSavepoint());
        Assert.assertEquals(buildSet(resource1, resource2, resource3),
                cacheTransaction.getReferencedResources());
        Assert.assertEquals(buildSet(allocatedReservation1, allocatedReservation2, allocatedReservation3),
                cacheTransaction.getAllocatedReservations());
        Assert.assertEquals(buildSet(providedReservation1, providedReservation2, providedReservation3),
                cacheTransaction.getProvidedReservations());

        // Revert first savepoint and check the starting state of cache transaction
        savepoint1.revert();
        Assert.assertEquals(null, cacheTransaction.getCurrentSavepoint());
        Assert.assertEquals(buildSet(resource1), cacheTransaction.getReferencedResources());
        Assert.assertEquals(buildSet(allocatedReservation1), cacheTransaction.getAllocatedReservations());
        Assert.assertEquals(buildSet(providedReservation1), cacheTransaction.getProvidedReservations());

        ////////////////////////////////////////////////////////////////////////////////

        // Create new savepoint and add the rest of reservations
        savepoint1 = cacheTransaction.createSavepoint();
        cacheTransaction.addAllocatedReservation(allocatedReservation2);
        cacheTransaction.addAllocatedReservation(allocatedReservation3);
        cacheTransaction.addProvidedReservation(providedReservation2);
        cacheTransaction.addProvidedReservation(providedReservation3);
        Assert.assertEquals(savepoint1, cacheTransaction.getCurrentSavepoint());

        // Create parent reservations with child which is already added
        Reservation allocatedReservationParent1 = new Reservation();
        allocatedReservationParent1.addChildReservation(allocatedReservation1);
        Reservation allocatedReservationParent2 = new Reservation();
        allocatedReservationParent2.addChildReservation(allocatedReservation2);
        Reservation allocatedReservationParent3 = new Reservation();
        allocatedReservationParent3.addChildReservation(allocatedReservation3);
        Reservation providedReservationParent1 = new Reservation();
        providedReservationParent1.addChildReservation(providedReservation1);
        Reservation providedReservationParent2 = new Reservation();
        providedReservationParent2.addChildReservation(providedReservation2);
        Reservation providedReservationParent3 = new Reservation();
        providedReservationParent3.addChildReservation(providedReservation3);

        // Add parent reservations to the cache transaction
        savepoint2 = cacheTransaction.createSavepoint();
        cacheTransaction.addAllocatedReservation(allocatedReservationParent1);
        cacheTransaction.addAllocatedReservation(allocatedReservationParent2);
        cacheTransaction.addAllocatedReservation(allocatedReservationParent3);
        cacheTransaction.addProvidedReservation(providedReservationParent1);
        cacheTransaction.addProvidedReservation(providedReservationParent2);
        cacheTransaction.addProvidedReservation(providedReservationParent3);
        Assert.assertEquals(savepoint2, cacheTransaction.getCurrentSavepoint());

        // Check proper state of cache transaction
        Assert.assertEquals(buildSet(allocatedReservation1, allocatedReservation2, allocatedReservation3,
                allocatedReservationParent1, allocatedReservationParent2, allocatedReservationParent3),
                cacheTransaction.getAllocatedReservations());
        Assert.assertEquals(buildSet(providedReservation1, providedReservation2, providedReservation3,
                providedReservationParent1, providedReservationParent2, providedReservationParent3),
                cacheTransaction.getProvidedReservations());

        // Revert savepoint which added parent reservations and check that children are retained
        savepoint2.revert();
        Assert.assertEquals(savepoint1, cacheTransaction.getCurrentSavepoint());
        Assert.assertEquals(buildSet(allocatedReservation1, allocatedReservation2, allocatedReservation3),
                cacheTransaction.getAllocatedReservations());
        Assert.assertEquals(buildSet(providedReservation1, providedReservation2, providedReservation3),
                cacheTransaction.getProvidedReservations());
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
