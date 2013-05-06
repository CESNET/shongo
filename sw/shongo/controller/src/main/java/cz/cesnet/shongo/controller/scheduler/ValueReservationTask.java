package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.cache.Cache;
import cz.cesnet.shongo.controller.cache.ResourceCache;
import cz.cesnet.shongo.controller.reservation.ExistingReservation;
import cz.cesnet.shongo.controller.reservation.FilteredValueReservation;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.reservation.ValueReservation;
import cz.cesnet.shongo.controller.resource.Capability;
import cz.cesnet.shongo.controller.resource.ResourceManager;
import cz.cesnet.shongo.controller.resource.value.FilteredValueProvider;
import cz.cesnet.shongo.controller.resource.value.ValueProvider;
import org.joda.time.Interval;

import java.util.*;

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
            availableValue = getAvailableValue(valueProvider, requestedValue, schedulerContext);
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
        AvailableReservation<ValueReservation> availableValueReservation =
                availableValue.getAvailableValueReservation();
        if (availableValueReservation != null) {
            Reservation originalReservation = availableValueReservation.getOriginalReservation();
            if (availableValueReservation.getType().equals(AvailableReservation.Type.REUSABLE)) {
                addReport(new SchedulerReportSet.ReservationReusingReport(originalReservation));

                ExistingReservation existingValueReservation = new ExistingReservation();
                existingValueReservation.setSlot(interval);
                existingValueReservation.setReservation(originalReservation);
                schedulerContext.removeAvailableReservation(availableValueReservation);
                return existingValueReservation;
            }
            else {
                return originalReservation;
            }
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

    /**
     * Find available alias in given {@code aliasProviderCapability}.
     *
     * @param valueProvider
     * @param requestedValue
     * @return available alias for given {@code interval} from given {@code aliasProviderCapability}
     */
    public AvailableValue getAvailableValue(ValueProvider valueProvider, String requestedValue,
            SchedulerContext schedulerContext) throws ValueProvider.InvalidValueException,
                                                      ValueProvider.ValueAlreadyAllocatedException,
                                                      ValueProvider.NoAvailableValueException
    {
        final Interval interval = schedulerContext.getInterval();
        final ValueProvider targetValueProvider = valueProvider.getTargetValueProvider();

        // Get available value reservations
        List<AvailableReservation<ValueReservation>> availableValueReservations =
                new LinkedList<AvailableReservation<ValueReservation>>();
        availableValueReservations.addAll(schedulerContext.getAvailableValueReservations(targetValueProvider));

        // Sort available value reservations
        Collections.sort(availableValueReservations, new Comparator<AvailableReservation>()
        {
            @Override
            public int compare(AvailableReservation reservation1, AvailableReservation reservation2)
            {
                // Prefer reservations for the whole interval
                boolean firstContainsInterval = reservation1.getOriginalReservation().getSlot().contains(interval);
                boolean secondContainsInterval = reservation2.getOriginalReservation().getSlot().contains(interval);
                if (secondContainsInterval && !firstContainsInterval) {
                    return 1;
                }

                // Prefer reallocatable reservations
                boolean firstReallocatable = reservation1.getType().equals(AvailableReservation.Type.REALLOCATABLE);
                boolean secondReallocatable = reservation2.getType().equals(AvailableReservation.Type.REALLOCATABLE);
                if (secondReallocatable && !firstReallocatable) {
                    return 1;
                }

                return 0;
            }
        });

        // Value which is being allocated (must not be null)
        String value = null;
        // Available value reservation by which the value is allocated (can be null)
        AvailableReservation<ValueReservation> availableValueReservation = null;
        // Already used values for targetValueProvider in the interval
        Set<String> usedValues = getUsedValues(targetValueProvider, interval, schedulerContext);

        // Find matching available value reservation
        for (AvailableReservation<ValueReservation> possibleAvailableValueReservation : availableValueReservations) {
            // Check possible available value reservation
            Reservation originalReservation = possibleAvailableValueReservation.getOriginalReservation();
            ValueReservation valueReservation = possibleAvailableValueReservation.getTargetReservation();
            if (requestedValue != null) {
                if (!valueReservation.getValue().equals(requestedValue)) {
                    // Value is different than requested
                    continue;
                }
            }
            if (!originalReservation.getSlot().contains(interval)) {
                if (possibleAvailableValueReservation.getType().equals(AvailableReservation.Type.REUSABLE)) {
                    // Reservation slot doesn't contain the requested slot and the reservation cannot be extended,
                    // because it is only reusable as it is
                    continue;
                }

                // Check value availability
                String possibleValue = valueReservation.getValue();
                if (usedValues.contains(possibleValue)) {
                    // Reservation slot doesn't contain the requested slot and the reservation cannot be extended,
                    // because the values is already allocated in the requested slot
                    continue;
                }

                if (originalReservation != valueReservation) {
                    throw new RuntimeException("Original and target reservation should be the same.");
                }

                // Extend the reservation slot
                originalReservation.setSlot(interval);
            }

            availableValueReservation = possibleAvailableValueReservation;
            value = valueReservation.getValue();
            break;
        }

        // Generate new value
        if (value == null) {
            if (requestedValue != null) {
                value = valueProvider.generateValue(usedValues, requestedValue);
            }
            else {
                value = valueProvider.generateValue(usedValues);
            }
        }
        AvailableValue availableAlias = new AvailableValue();
        availableAlias.setValue(value);
        availableAlias.setAvailableValueReservation(availableValueReservation);
        return availableAlias;
    }

    private static Set<String> getUsedValues(ValueProvider valueProvider, Interval interval, SchedulerContext context)
    {
        Set<String> usedValues;ResourceManager resourceManager = new ResourceManager(context.getEntityManager());
        Long valueProviderId = valueProvider.getId();
        List<ValueReservation> allocatedValues =
                resourceManager.listValueReservationsInInterval(valueProviderId, interval);
        context.applyValueReservations(valueProviderId, allocatedValues);
        usedValues = new HashSet<String>();
        for (ValueReservation allocatedValue : allocatedValues) {
            usedValues.add(allocatedValue.getValue());
        }
        return usedValues;
    }
}
