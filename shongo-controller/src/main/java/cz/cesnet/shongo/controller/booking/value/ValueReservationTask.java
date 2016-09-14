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
import org.joda.time.DateTime;
import org.joda.time.Interval;

import javax.persistence.Tuple;
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
     * @param slot
     * @param valueProvider
     * @param requestedValue
     */
    public ValueReservationTask(SchedulerContext schedulerContext, Interval slot, ValueProvider valueProvider, String requestedValue)
    {
        super(schedulerContext, slot);
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
    protected Reservation allocateReservation(Reservation currentReservation) throws SchedulerException
    {
        validateReservationSlot(ValueReservation.class);

        final Cache cache = getCache();
        final ResourceCache resourceCache = cache.getResourceCache();

        // Check if resource can be allocated and if it is available in the future
        Capability capability = valueProvider.getCapability();
        resourceCache.checkCapabilityAvailable(capability, slot, schedulerContext, this);

        // Check target value provider
        ValueProvider targetValueProvider = valueProvider.getTargetValueProvider();
        if (targetValueProvider != valueProvider) {
            // Check whether target value provider can be allocated
            capability = targetValueProvider.getCapability();
            resourceCache.checkCapabilityAvailable(capability, slot, schedulerContext, this);
        }

        // Already used values for targetValueProvider in the interval
        Map<String, Interval> usedValues = getUsedValues(targetValueProvider, slot);

        // Get available value reservations
        List<AvailableReservation<ValueReservation>> availableReservations =
                new LinkedList<AvailableReservation<ValueReservation>>();
        availableReservations.addAll(schedulerContextState.getAvailableValueReservations(targetValueProvider, slot));
        sortAvailableReservations(availableReservations);

        // Find matching available value reservation
        for (AvailableReservation<ValueReservation> availableValueReservation : availableReservations) {
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
            schedulerContextState.removeAvailableReservation(availableValueReservation);

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
                Interval newOverlapInterval = overlapInterval.overlap(interval);
                if (newOverlapInterval != null) {
                    overlapInterval = newOverlapInterval;
                }
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
        ResourceManager resourceManager = new ResourceManager(schedulerContext.getEntityManager());
        Long valueProviderId = valueProvider.getId();
        List<Tuple> allocatedValues =
                resourceManager.listValueReservationsInInterval(valueProviderId, interval);

        Map<Long, Map.Entry<String, Interval>> usedReservations = new HashMap<>();
        for (Tuple allocatedValue : allocatedValues) {
            DateTime slotStart = (DateTime) allocatedValue.get(2);
            DateTime slotEnd = (DateTime) allocatedValue.get(3);
            Map.Entry<String, Interval> value = new AbstractMap.SimpleEntry<>((String) allocatedValue.get(1), new Interval(slotStart, slotEnd));
            usedReservations.put((Long) allocatedValue.get(0), value);
        }

        schedulerContextState.applyValueReservations(valueProviderId, slot, usedReservations);

        Map<String, Interval> usedValues = new HashMap<>();
        for (Map.Entry<String, Interval> reservation : usedReservations.values()) {
            usedValues.put(reservation.getKey(), reservation.getValue());
        }

        return usedValues;
    }
}
