package cz.cesnet.shongo.controller.booking.participant;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.cache.ResourceCache;
import cz.cesnet.shongo.controller.booking.reservation.Reservation;
import cz.cesnet.shongo.controller.booking.resource.DeviceResource;
import cz.cesnet.shongo.controller.booking.resource.TerminalCapability;
import cz.cesnet.shongo.controller.scheduler.*;
import cz.cesnet.shongo.util.ObjectHelper;
import org.joda.time.Interval;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Represents {@link EndpointParticipant} as parameters for endpoint which will be lookup.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class LookupEndpointParticipant extends EndpointParticipant implements ReservationTaskProvider
{
    /**
     * Constructor.
     */
    public LookupEndpointParticipant()
    {
    }

    @Override
    public boolean synchronizeFrom(AbstractParticipant participant)
    {
        LookupEndpointParticipant lookupEndpointParticipant = (LookupEndpointParticipant) participant;

        boolean modified = super.synchronizeFrom(participant);
        modified |= !ObjectHelper.isSameIgnoreOrder(getTechnologies(), lookupEndpointParticipant.getTechnologies());

        setTechnologies(lookupEndpointParticipant.getTechnologies());

        return modified;
    }

    @Override
    public ReservationTask createReservationTask(SchedulerContext schedulerContext, Interval slot) throws SchedulerException
    {
        return new ReservationTask(schedulerContext, slot)
        {
            @Override
            protected Reservation allocateReservation(Reservation currentReservation) throws SchedulerException
            {
                ResourceCache resourceCache = getCache().getResourceCache();

                Set<Technology> technologies = getTechnologies();
                Set<Long> terminals = resourceCache.getDeviceResourceIdsByCapabilityTechnologies(
                        TerminalCapability.class, technologies);

                List<DeviceResource> deviceResources = new ArrayList<DeviceResource>();
                for (Long terminalId : terminals) {
                    DeviceResource deviceResource = (DeviceResource) resourceCache.getObject(terminalId);
                    if (deviceResource == null) {
                        throw new RuntimeException("Device resource should be added to the cache.");
                    }
                    if (resourceCache.isResourceAvailableByParent(deviceResource, slot, schedulerContext, this)) {
                        deviceResources.add(deviceResource);
                    }
                }

                // Select first available device resource
                // TODO: Select best endpoint based on some criteria (e.g., location in real world)
                DeviceResource deviceResource = null;
                for (DeviceResource possibleDeviceResource : deviceResources) {
                    deviceResource = possibleDeviceResource;
                    break;
                }

                // If some was found
                if (deviceResource != null) {
                    // Create reservation for the device resource
                    ResourceReservationTask task = new ResourceReservationTask(schedulerContext, slot, deviceResource);
                    Reservation reservation = task.perform();
                    addReports(task);
                    return reservation;
                }
                else {
                    throw new SchedulerReportSet.EndpointNotFoundException(technologies);
                }
            }
        };
    }

    @Override
    protected cz.cesnet.shongo.controller.api.AbstractParticipant createApi()
    {
        return new cz.cesnet.shongo.controller.api.LookupEndpointParticipant();
    }

    @Override
    public void toApi(cz.cesnet.shongo.controller.api.AbstractParticipant participantApi)
    {
        cz.cesnet.shongo.controller.api.LookupEndpointParticipant lookupEndpointParticipantApi =
                (cz.cesnet.shongo.controller.api.LookupEndpointParticipant) participantApi;
        Set<Technology> technologies = getTechnologies();
        if (technologies.size() == 1) {
            lookupEndpointParticipantApi.setTechnology(technologies.iterator().next());
        }
        else {
            throw new TodoImplementException();
        }
        super.toApi(participantApi);
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.AbstractParticipant participantApi, EntityManager entityManager)
    {
        cz.cesnet.shongo.controller.api.LookupEndpointParticipant lookupEndpointParticipantApi =
                (cz.cesnet.shongo.controller.api.LookupEndpointParticipant) participantApi;

        clearTechnologies();
        addTechnology(lookupEndpointParticipantApi.getTechnology());

        super.fromApi(participantApi, entityManager);
    }
}
