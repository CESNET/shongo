package cz.cesnet.shongo.controller.cache;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.controller.allocation.AllocatedItem;
import cz.cesnet.shongo.controller.util.RangeSet;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import javax.persistence.EntityManager;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Represents an abstract cache.
 *
 * @param <T> type of cached object
 * @param <A> type of allocation for cached object
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class AbstractAllocationCache<T extends PersistentObject, A extends AllocatedItem>
        extends AbstractCache<T>
{
    /**
     * Working interval for which are loaded allocated virtual rooms.
     */
    private Interval workingInterval;

    /**
     * Represents a reference data time which is a rounded now().
     */
    private DateTime referenceDateTime;

    /**
     * Map of cached object states by theirs identifiers.
     */
    private Map<Long, ObjectState<A>> objectStateById = new HashMap<Long, ObjectState<A>>();

    /**
     * @return {@link #workingInterval}
     */
    public Interval getWorkingInterval()
    {
        return workingInterval;
    }

    /**
     * @return {@link #referenceDateTime}
     */
    public DateTime getReferenceDateTime()
    {
        return referenceDateTime;
    }

    /**
     * @param workingInterval sets the {@link #workingInterval}
     * @param entityManager   used for reloading allocations of resources for the new interval
     */
    public void setWorkingInterval(Interval workingInterval, EntityManager entityManager)
    {
        if (!workingInterval.equals(this.workingInterval)) {
            this.workingInterval = workingInterval;
            this.referenceDateTime = workingInterval.getStart();
            workingIntervalChanged(entityManager);
        }
    }

    /**
     * Method called when {@link #workingInterval} is changed.
     *
     * @param entityManager
     */
    protected void workingIntervalChanged(EntityManager entityManager)
    {
        for (T object : getObjects()) {
            updateObjectState(object, entityManager);
        }
    }

    /**
     * @return new state for given {@code object}
     */
    protected ObjectState<A> createObjectState()
    {
        return new ObjectState<A>();
    }

    /**
     * @param object        to be added to the cache
     * @param entityManager for loading {@code object}'s state
     */
    public void addObject(T object, EntityManager entityManager)
    {
        super.addObject(object);

        Long objectId = object.getId();
        ObjectState<A> objectState = createObjectState();
        objectState.setObjectId(objectId);
        objectStateById.put(objectId, objectState);
        if (entityManager != null) {
            updateObjectState(object, entityManager);
        }
    }

    @Override
    public void addObject(T object)
    {
        addObject(object, null);
    }

    @Override
    public void removeObject(T object)
    {
        objectStateById.remove(object.getId());

        super.removeObject(object);
    }

    /**
     * @param object for which the state should be returned
     * @return state for given {@code object}
     * @throws IllegalArgumentException when state cannot be found
     */
    protected ObjectState<A> getObjectState(T object)
    {
        Long objectId = object.getId();
        ObjectState<A> objectState = objectStateById.get(objectId);
        if (objectState == null) {
            throw new IllegalArgumentException(
                    object.getClass().getSimpleName() + " '" + objectId + "' isn't in the cache!");
        }
        return objectState;
    }

    /**
     * @param object     for which the {@code allocation} is added
     * @param allocation to be added to the cache
     */
    public void addAllocation(T object, A allocation)
    {
        allocation.checkPersisted();
        ObjectState<A> objectState = getObjectState(object);
        objectState.addAllocation(allocation);
    }

    /**
     * @param object     for which the {@code allocation} is removed
     * @param allocation to be removed from the cache
     */
    public void removeAllocation(T object, A allocation)
    {
        ObjectState<A> objectState = getObjectState(object);
        objectState.removeAllocation(allocation);
    }

    /**
     * Update {@code objectState} with given {@code entityManager}.
     *
     * @param object        for which should be state updated
     * @param entityManager which should be used for accessing database
     */
    private void updateObjectState(T object, EntityManager entityManager)
    {
        ObjectState<A> objectState = getObjectState(object);
        objectState.clear();

        if (workingInterval != null) {
            updateObjectState(object, workingInterval, entityManager);
        }
    }

    /**
     * Update {@code objectState} for given {@code workingInterval} with given {@code entityManager}.
     *
     * @param object          for which should be state updated
     * @param workingInterval for which the {@code objectState} should be updated
     * @param entityManager   which should be used for accessing database
     */
    protected abstract void updateObjectState(T object, Interval workingInterval,
            EntityManager entityManager);

    /**
     * Represents a cached object state.
     *
     * @param <A> type of allocation for cached object
     */
    public static class ObjectState<A extends AllocatedItem>
    {
        /**
         * Identifier of the cached object.
         */
        private Long objectId;

        /**
         * Allocations for the cached object.
         */
        private RangeSet<A, DateTime> allocations = new RangeSet<A, DateTime>();

        /**
         * Map of allocations by the identifier.
         */
        private Map<Long, A> allocationById = new HashMap<Long, A>();

        /**
         * @return {@link #objectId}
         */
        public Long getObjectId()
        {
            return objectId;
        }

        /**
         * @param objectId sets the {@link #objectId}
         */
        public void setObjectId(Long objectId)
        {
            this.objectId = objectId;
        }

        /**
         * @param interval
         * @return list of allocations for cached object in given {@code interval}
         */
        public Set<A> getAllocations(Interval interval)
        {
            return allocations.getValues(interval.getStart(), interval.getEnd());
        }

        /**
         * @param allocation to be added to the {@link ObjectState}
         */
        public void addAllocation(A allocation)
        {
            // TODO: check if allocation doesn't collide

            Interval slot = allocation.getSlot();
            allocationById.put(allocation.getId(), allocation);
            allocations.add(allocation, slot.getStart(), slot.getEnd());
        }

        /**
         * @param allocation to be removed from the {@link ObjectState}
         */
        public void removeAllocation(A allocation)
        {
            Long allocationId = allocation.getId();
            allocation = allocationById.get(allocationId);
            if (allocation == null) {
                throw new IllegalStateException("Allocation doesn't exist in the cache.");
            }
            allocations.remove(allocation);
            allocationById.remove(allocationId);
        }

        /**
         * Clear all allocations from the {@link ObjectState}.
         */
        public void clear()
        {
            allocations.clear();
            allocationById.clear();
        }
    }
}
