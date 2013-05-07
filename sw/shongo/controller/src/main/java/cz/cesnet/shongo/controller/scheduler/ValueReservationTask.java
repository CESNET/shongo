package cz.cesnet.shongo.controller.scheduler;

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

        final SchedulerContext schedulerContext = getSchedulerContext();
        final Interval interval = getInterval();
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
        Set<String> usedValues = getUsedValues(targetValueProvider, interval, schedulerContext);

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

            // Value must match requested value
            if (requestedValue != null) {
                if (!valueReservation.getValue().equals(requestedValue)) {
                    // Value is different than requested
                    continue;
                }
            }

            // Original reservation slot must contain requested slot
            if (!originalReservation.getSlot().contains(interval)) {
                if (!availableValueReservation.isModifiable()) {
                    // Original reservation slot doesn't contain the requested and the original reservation
                    // cannot be extended because it is not modifiable
                    continue;
                }

                if (originalReservation == valueReservation) {
                    // Original equals to target reservation and it can be extended,
                    // and thus check if the value is available for the requested slot
                    String possibleValue = valueReservation.getValue();
                    if (usedValues.contains(possibleValue)) {
                        // Reservation slot doesn't contain the requested slot and the reservation cannot be extended,
                        // because the value is already allocated in the extended slot by another reservation
                        continue;
                    }
                }
                else {
                    // Check allocation reservation
                    Reservation allocationReservation = originalReservation.getAllocationReservation();
                    if (!schedulerContext.isReservationAvailable(allocationReservation)) {
                        // Allocation reservation is not available for the whole requested slot (another existing reservation reuse it)
                        continue;
                    }
                    if (!allocationReservation.getSlot().contains(interval)) {
                        // Allocation reservation slot doesn't contain the requested (allocation reservation can never be extended)
                        continue;
                    }
                }

                // Original reservation can be extended so extend it's slot to match the requested
                originalReservation.setSlot(interval);
            }

            // Available reservation will be returned so remove it from context (to not be used again)
            schedulerContext.removeAvailableReservation(availableValueReservation);

            // Return available reservation
            if (availableValueReservation.isExistingReservationRequired()) {
                addReport(new SchedulerReportSet.ReservationReusingReport(originalReservation));
                ExistingReservation existingValueReservation = new ExistingReservation();
                existingValueReservation.setSlot(interval);
                existingValueReservation.setReservation(originalReservation);
                return existingValueReservation;
            }
            else {
                addReport(new SchedulerReportSet.ReservationReallocatingReport(originalReservation));
                return originalReservation;

            }
        }

        // Allocate new reservation
        try {
            // Get new available value for allocation
            String availableValue;
            if (requestedValue != null) {
                availableValue = valueProvider.generateValue(usedValues, requestedValue);
            }
            else {
                availableValue = valueProvider.generateValue(usedValues);
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
            valueReservation.setValue(availableValue);
            return valueReservation;
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
    }

    private static Set<String> getUsedValues(ValueProvider valueProvider, Interval interval, SchedulerContext context)
    {
        Set<String> usedValues;
        ResourceManager resourceManager = new ResourceManager(context.getEntityManager());
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
