package cz.cesnet.shongo.controller.booking.value;

import cz.cesnet.shongo.controller.booking.value.provider.FilteredValueProvider;
import cz.cesnet.shongo.controller.booking.value.provider.ValueProvider;
import cz.cesnet.shongo.controller.cache.Cache;
import cz.cesnet.shongo.controller.cache.ResourceCache;
import cz.cesnet.shongo.controller.booking.reservation.ExistingReservation;
import cz.cesnet.shongo.controller.booking.reservation.Reservation;
import cz.cesnet.shongo.controller.booking.resource.Capability;
import cz.cesnet.shongo.controller.booking.resource.ResourceManager;
import cz.cesnet.shongo.controller.scheduler.*;
import org.joda.time.Interval;

import java.util.*;

/**
 * Represents {@link cz.cesnet.shongo.controller.scheduler.ReservationTask} for a {@link cz.cesnet.shongo.controller.booking.alias.AliasReservation}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ValueReservationTask extends ReservationTask
{
    /**
     * {@link cz.cesnet.shongo.controller.booking.value.provider.ValueProvider} to be used.
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
    protected Reservation allocateReservation() throws SchedulerException
    {
        validateReservationSlot(ValueReservation.class);

        final Interval slot = getInterval();
        final Cache cache = getCache();
        final ResourceCache resourceCache = cache.getResourceCache();

        // Check if resource can be allocated and if it is available in the future
        Capability capability = valueProvider.getCapability();
        resourceCache.checkCapabilityAvailable(capability, schedulerContext);

        // Check target value provider
        ValueProvider targetValueProvider = valueProvider.getTargetValueProvider();
        if (targetValueProvider != valueProvider) {
            // Check whether target value provider can be allocated
            capability = targetValueProvider.getCapability();
            resourceCache.checkCapabilityAvailable(capability, schedulerContext);
        }

        // Already used values for targetValueProvider in the interval
        Map<String, Interval> usedValues = getUsedValues(targetValueProvider, slot);

        // Get available value reservations
        List<AvailableReservation<ValueReservation>> availableValueReservations =
                new LinkedList<AvailableReservation<ValueReservation>>();
        availableValueReservations.addAll(schedulerContext.getAvailableValueReservations(targetValueProvider));
        sortAvailableReservations(availableValueReservations);

        // Find matching available value reservation
        for (AvailableReservation<ValueReservation> availableValueReservation : availableValueReservations) {
            // Check available value reservation
            Reservation originalReservation = availableValueReservation.getOriginalReservation();
            ValueReservation valueReservation = availableValueReservation.getTargetReservation();

            // Only reusable available reservations
            if (!availableValueReservation.isType(AvailableReservation.Type.REUSABLE)) {
                continue;
            }

            // Original reservation slot must contain requested slot
            if (!originalReservation.getSlot().contains(slot)) {
                continue;
            }

            // Value must match requested value
            if (requestedValue != null && !valueReservation.getValue().equals(requestedValue)) {
                continue;
            }

            // Available reservation will be returned so remove it from context (to not be used again)
            schedulerContext.removeAvailableReservation(availableValueReservation);

            // Create new existing value reservation
            addReport(new SchedulerReportSet.ReservationReusingReport(originalReservation));
            ExistingReservation existingValueReservation = new ExistingReservation();
            existingValueReservation.setSlot(slot);
            existingValueReservation.setReusedReservation(originalReservation);
            return existingValueReservation;
        }

        // Allocate value reservation
        try {
            String value;
            ValueReservation valueReservation;
            // Create new value reservation
            if (valueProvider instanceof FilteredValueProvider) {
                valueReservation = new FilteredValueReservation(requestedValue);
            }
            else {
                valueReservation = new ValueReservation();
            }
            // Generate new value
            if (requestedValue != null) {
                value = valueProvider.generateValue(usedValues.keySet(), requestedValue);
            }
            else {
                value = valueProvider.generateValue(usedValues.keySet());
            }
            valueReservation.setSlot(slot);
            valueReservation.setValueProvider(targetValueProvider);
            valueReservation.setValue(value);
            return valueReservation;
        }
        catch (ValueProvider.InvalidValueException exception) {
            throw new SchedulerReportSet.ValueInvalidException(requestedValue);
        }
        catch (ValueProvider.ValueAlreadyAllocatedException exception) {
            throw new SchedulerReportSet.ValueAlreadyAllocatedException(requestedValue, usedValues.get(requestedValue));
        }
        catch (ValueProvider.NoAvailableValueException exception) {
            Interval overlapInterval = slot;
            for (Interval interval : usedValues.values()) {
                overlapInterval = overlapInterval.overlap(interval);
            }
            throw new SchedulerReportSet.ValueNotAvailableException(overlapInterval);
        }
    }

    /**
     * @param valueProvider for which the used values should be returned
     * @param interval      for which interval
     * @return set of used values for given {@code valueProvider} in given {@code interval}
     */
    private Map<String, Interval> getUsedValues(ValueProvider valueProvider, Interval interval)
    {
        Map<String, Interval> usedValues = new HashMap<String, Interval>();
        ResourceManager resourceManager = new ResourceManager(schedulerContext.getEntityManager());
        Long valueProviderId = valueProvider.getId();
        List<ValueReservation> allocatedValues =
                resourceManager.listValueReservationsInInterval(valueProviderId, interval);

        schedulerContext.applyReservations(valueProviderId, allocatedValues, ValueReservation.class);
        for (ValueReservation allocatedValue : allocatedValues) {
            usedValues.put(allocatedValue.getValue(), allocatedValue.getSlot());
        }
        return usedValues;
    }
}
