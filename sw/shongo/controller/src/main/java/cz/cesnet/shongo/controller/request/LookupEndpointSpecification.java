package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.cache.ResourceCache;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.TerminalCapability;
import cz.cesnet.shongo.controller.scheduler.*;
import cz.cesnet.shongo.TodoImplementException;
import org.apache.commons.lang.ObjectUtils;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Represents {@link EndpointSpecification} as parameters for endpoint which will be lookup.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class LookupEndpointSpecification extends EndpointSpecification implements ReservationTaskProvider
{
    /**
     * Constructor.
     */
    public LookupEndpointSpecification()
    {
    }

    @Override
    public boolean synchronizeFrom(Specification specification)
    {
        LookupEndpointSpecification lookupEndpointSpecification = (LookupEndpointSpecification) specification;

        boolean modified = super.synchronizeFrom(specification);
        modified |= !ObjectUtils.equals(getTechnologies(), lookupEndpointSpecification.getTechnologies());

        setTechnologies(lookupEndpointSpecification.getTechnologies());

        return modified;
    }

    @Override
    public ReservationTask createReservationTask(SchedulerContext schedulerContext)
    {
        return new ReservationTask(schedulerContext)
        {
            @Override
            protected Reservation createReservation() throws SchedulerException
            {
                ResourceCache resourceCache = getCache().getResourceCache();

                Set<Technology> technologies = getTechnologies();
                Set<Long> terminals = resourceCache.getDeviceResourcesByCapabilityTechnologies(
                        TerminalCapability.class, technologies);

                List<DeviceResource> deviceResources = new ArrayList<DeviceResource>();
                for (Long terminalId : terminals) {
                    DeviceResource deviceResource = (DeviceResource) resourceCache.getObject(terminalId);
                    if (deviceResource == null) {
                        throw new RuntimeException("Device resource should be added to the cache.");
                    }
                    if (resourceCache.isResourceAvailableByParent(deviceResource, getSchedulerContext())) {
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
                    ResourceReservationTask task = new ResourceReservationTask(getSchedulerContext(), deviceResource);
                    Reservation reservation = task.perform();
                    addReports(task);
                    return reservation;
                }
                else {
                    throw new SchedulerReportSet.ResourceNotFoundException(technologies);
                }
            }
        };
    }

    @Override
    protected cz.cesnet.shongo.controller.api.Specification createApi()
    {
        return new cz.cesnet.shongo.controller.api.LookupEndpointSpecification();
    }

    @Override
    public void toApi(cz.cesnet.shongo.controller.api.Specification specificationApi)
    {
        cz.cesnet.shongo.controller.api.LookupEndpointSpecification lookupEndpointSpecificationApi =
                (cz.cesnet.shongo.controller.api.LookupEndpointSpecification) specificationApi;
        Set<Technology> technologies = getTechnologies();
        if (technologies.size() == 1) {
            lookupEndpointSpecificationApi.setTechnology(technologies.iterator().next());
        }
        else {
            throw new TodoImplementException();
        }
        super.toApi(specificationApi);
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.Specification specificationApi, EntityManager entityManager)
    {
        cz.cesnet.shongo.controller.api.LookupEndpointSpecification lookupEndpointSpecificationApi =
                (cz.cesnet.shongo.controller.api.LookupEndpointSpecification) specificationApi;
        if (lookupEndpointSpecificationApi.isPropertyFilled(lookupEndpointSpecificationApi.TECHNOLOGY)) {
            clearTechnologies();
            addTechnology(lookupEndpointSpecificationApi.getTechnology());
        }

        super.fromApi(specificationApi, entityManager);
    }
}
