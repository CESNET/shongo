package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.controller.cache.Cache;
import cz.cesnet.shongo.controller.cache.ResourceCache;
import cz.cesnet.shongo.controller.reservation.ExistingReservation;
import cz.cesnet.shongo.controller.reservation.FilteredValueReservation;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.reservation.ValueReservation;
import cz.cesnet.shongo.controller.resource.Capability;
import cz.cesnet.shongo.controller.resource.value.FilteredValueProvider;
import cz.cesnet.shongo.controller.resource.value.ValueProvider;
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
     * @param schedulerContext
     * @param valueProvider
     * @param requestedValue
     */
    public ValueReservationTask(SchedulerContext schedulerContext, ValueProvider valueProvider, String requestedValue)
    {
        super(schedulerContext);
        this.valueProvider = valueProvider;
        this.requestedValue = requestedValue;
    }

    @Override
    protected SchedulerReport createMainReport()
    {
        return new SchedulerReportSet.AllocatingValueReport(
                valueProvider.getTargetValueProvider().getCapabilityResource());
    }

    @Override
    protected Reservation createReservation() throws SchedulerException
    {
        validateReservationSlot(ValueReservation.class);

        SchedulerContext schedulerContext = getSchedulerContext();
        Interval interval = getInterval();
        Cache cache = getCache();
        ResourceCache resourceCache = cache.getResourceCache();

        // Check if resource can be allocated and if it is available in the future
        Capability capability = valueProvider.getCapability();
        resourceCache.checkCapabilityAvailable(capability, schedulerContext);

        // Find available value in the alias providers
        ValueProvider targetValueProvider = valueProvider.getTargetValueProvider();
        if (targetValueProvider != valueProvider) {
            // Check whether target value provider can be allocated
            capability = targetValueProvider.getCapability();
            resourceCache.checkCapabilityAvailable(capability, schedulerContext);
        }

        // Get new available value
        AvailableValue availableValue;
        try {
            availableValue = schedulerContext.getAvailableValue(valueProvider, requestedValue);
        }
        catch (ValueProvider.InvalidValueException exception) {
            throw new SchedulerReportSet.ValueInvalidException(requestedValue);
        }
        catch (ValueProvider.ValueAlreadyAllocatedException exception) {
            throw new SchedulerReportSet.ValueAlreadyAllocatedException(requestedValue);
        }
        catch (ValueProvider.NoAvailableValueException exception) {
            throw new SchedulerReportSet.ValueNotAvailableException();
        }

        // Reuse existing value reservation
        ValueReservation providedValueReservation = availableValue.getValueReservation();
        if (providedValueReservation != null) {
            addReport(new SchedulerReportSet.ReservationReusingReport(providedValueReservation));

            ExistingReservation existingValueReservation = new ExistingReservation();
            existingValueReservation.setSlot(interval);
            existingValueReservation.setReservation(providedValueReservation);
            schedulerContext.removeProvidedReservation(providedValueReservation);
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
