package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.controller.cache.AvailableValue;
import cz.cesnet.shongo.controller.cache.CacheTransaction;
import cz.cesnet.shongo.controller.cache.ValueCache;
import cz.cesnet.shongo.controller.reservation.ExistingReservation;
import cz.cesnet.shongo.controller.reservation.FilteredValueReservation;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.reservation.ValueReservation;
import cz.cesnet.shongo.controller.resource.Capability;
import cz.cesnet.shongo.controller.resource.Resource;
import cz.cesnet.shongo.controller.resource.value.FilteredValueProvider;
import cz.cesnet.shongo.controller.resource.value.ValueProvider;
import org.joda.time.DateTime;
import org.joda.time.Interval;

/**
 * Represents {@link cz.cesnet.shongo.controller.scheduler.ReservationTask} for a {@link cz.cesnet.shongo.controller.reservation.AliasReservation}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ValueReservationTask
{
    /**
     * @param valueProvider
     * @param requestedValue
     * @param interval
     * @param valueCache
     * @param cacheTransaction
     * @return {@link ValueReservation} if a requested value is available,
     *         null otherwise
     */
    public static Reservation createReservation(ValueProvider valueProvider, String requestedValue, Interval interval,
            ValueCache valueCache, CacheTransaction cacheTransaction)
    {
        // Find available value in the alias providers
        ValueProvider targetValueProvider = valueProvider.getTargetValueProvider();

        // Check whether target value provider can be allocated
        Capability capability = targetValueProvider.getCapability();
        Resource resource = capability.getResource();
        DateTime referenceDateTime = valueCache.getReferenceDateTime();
        if (!resource.isAllocatable() || !capability.isAvailableInFuture(interval.getEnd(), referenceDateTime)) {
            return null;
        }

        // Get new available value
        AvailableValue availableValue =
                valueCache.getAvailableValue(valueProvider, requestedValue, interval, cacheTransaction);
        if (availableValue == null) {
            return null;
        }

        // Reuse existing value reservation
        ValueReservation providedValueReservation = availableValue.getValueReservation();
        if (providedValueReservation != null) {
            ExistingReservation existingValueReservation = new ExistingReservation();
            existingValueReservation.setSlot(interval);
            existingValueReservation.setReservation(providedValueReservation);
            cacheTransaction.removeProvidedReservation(providedValueReservation);
            return existingValueReservation;
        }

        // Allocate new value reservation
        ValueReservation valueReservation;
        if (valueProvider instanceof FilteredValueProvider) {
            valueReservation = new FilteredValueReservation(requestedValue);
        }
        else {
            valueReservation = new ValueReservation();
        }
        valueReservation.setSlot(interval);
        valueReservation.setValueProvider(targetValueProvider);
        valueReservation.setValue(availableValue.getValue());
        return valueReservation;
    }
}
