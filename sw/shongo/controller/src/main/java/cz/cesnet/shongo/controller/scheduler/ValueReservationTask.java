package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.controller.Cache;
import cz.cesnet.shongo.controller.cache.AvailableValue;
import cz.cesnet.shongo.controller.cache.CacheTransaction;
import cz.cesnet.shongo.controller.cache.ResourceCache;
import cz.cesnet.shongo.controller.cache.ValueCache;
import cz.cesnet.shongo.controller.report.Report;
import cz.cesnet.shongo.controller.report.ReportException;
import cz.cesnet.shongo.controller.reservation.ExistingReservation;
import cz.cesnet.shongo.controller.reservation.FilteredValueReservation;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.reservation.ValueReservation;
import cz.cesnet.shongo.controller.resource.Capability;
import cz.cesnet.shongo.controller.resource.Resource;
import cz.cesnet.shongo.controller.resource.value.FilteredValueProvider;
import cz.cesnet.shongo.controller.resource.value.ValueProvider;
import cz.cesnet.shongo.controller.scheduler.report.*;
import org.joda.time.DateTime;
import org.joda.time.Interval;

/**
 * Represents {@link cz.cesnet.shongo.controller.scheduler.ReservationTask} for a {@link cz.cesnet.shongo.controller.reservation.AliasReservation}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ValueReservationTask extends ReservationTask
{
    /**
     * {@link ValueProvider} to be used.
     */
    private ValueProvider valueProvider;

    /**
     * To be allocated.
     */
    private String requestedValue;

    /**
     * Constructor.
     *
     * @param context
     * @param valueProvider
     * @param requestedValue
     */
    public ValueReservationTask(Context context, ValueProvider valueProvider, String requestedValue)
    {
        super(context);
        this.valueProvider = valueProvider;
        this.requestedValue = requestedValue;
    }

    @Override
    protected Report createdMainReport()
    {
        return new AllocatingValueReport(valueProvider.getTargetValueProvider().getCapability());
    }

    @Override
    protected Reservation createReservation() throws ReportException
    {
        Context context = getContext();
        Interval interval = getInterval();
        Cache cache = getCache();
        ResourceCache resourceCache = cache.getResourceCache();
        ValueCache valueCache = cache.getValueCache();
        CacheTransaction cacheTransaction = getCacheTransaction();

        DateTime referenceDateTime = valueCache.getReferenceDateTime();

        // Check if resource can be allocated and if it is available in the future
        Capability capability = valueProvider.getCapability();
        resourceCache.checkCapabilityAvailable(capability, context);

        // Find available value in the alias providers
        ValueProvider targetValueProvider = valueProvider.getTargetValueProvider();
        if (targetValueProvider != valueProvider) {
            // Check whether target value provider can be allocated
            capability = targetValueProvider.getCapability();
            resourceCache.checkCapabilityAvailable(capability, context);
        }

        // Get new available value
        AvailableValue availableValue;
        try {
            availableValue = valueCache.getAvailableValue(valueProvider, requestedValue, interval, cacheTransaction);
        }
        catch (ValueProvider.InvalidValueException e) {
            throw new ValueInvalidReport(requestedValue).exception();
        }
        catch (ValueProvider.ValueAlreadyAllocatedException e) {
            throw new ValueAlreadyAllocatedReport(requestedValue).exception();
        }
        catch (ValueProvider.NoAvailableValueException e) {
            throw new ValueNoAvailableReport().exception();
        }

        // Reuse existing value reservation
        ValueReservation providedValueReservation = availableValue.getValueReservation();
        if (providedValueReservation != null) {
            addReport(new ReusingReservationReport(providedValueReservation));

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
