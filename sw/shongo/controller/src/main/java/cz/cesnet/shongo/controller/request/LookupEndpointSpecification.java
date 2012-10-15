package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.report.ReportException;
import cz.cesnet.shongo.controller.reservation.EndpointReservation;
import cz.cesnet.shongo.controller.reservation.ResourceReservation;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.scheduler.ReservationTask;
import cz.cesnet.shongo.controller.scheduler.ReservationTaskProvider;
import cz.cesnet.shongo.controller.scheduler.ResourceReservationTask;
import cz.cesnet.shongo.controller.scheduler.report.ResourceNotFoundReport;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.fault.TodoImplementException;
import org.apache.commons.lang.ObjectUtils;

import javax.persistence.*;
import java.util.*;

/**
 * Represents {@link EndpointSpecification} as parameters for endpoint which will be lookup.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class LookupEndpointSpecification extends EndpointSpecification implements ReservationTaskProvider
{
    /**
     * Set of technologies which the resource must support.
     */
    private Set<Technology> technologies = new HashSet<Technology>();

    /**
     * Constructor.
     */
    public LookupEndpointSpecification()
    {
    }

    /**
     * Constructor.
     *
     * @param technology
     */
    public LookupEndpointSpecification(Technology technology)
    {
        addTechnology(technology);
    }

    /**
     * @return {@link #technologies}
     */
    @ElementCollection
    @Enumerated(EnumType.STRING)
    @Access(AccessType.FIELD)
    public Set<Technology> getTechnologies()
    {
        return Collections.unmodifiableSet(technologies);
    }

    /**
     * @param technologies sets the {@link #technologies}
     */
    public void setTechnologies(Set<Technology> technologies)
    {
        this.technologies = technologies;
    }

    /**
     * @param technology technology to be added to the {@link #technologies}
     */
    public void addTechnology(Technology technology)
    {
        technologies.add(technology);
    }

    /**
     * @param technology technology to be removed from the {@link #technologies}
     */
    public void removeTechnology(Technology technology)
    {
        technologies.remove(technology);
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
    public ReservationTask<ResourceReservation> createReservationTask(ReservationTask.Context context)
    {
        return new ReservationTask<ResourceReservation>(context)
        {
            @Override
            protected ResourceReservation createReservation() throws ReportException
            {
                Set<Technology> technologies = getTechnologies();

                // Lookup device resources
                List<DeviceResource> deviceResources = getCache().findAvailableTerminal(getInterval(), technologies,
                        getCacheTransaction());

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
                    ResourceReservationTask task = new ResourceReservationTask(getContext(), deviceResource);
                    return task.perform();
                }
                else {
                    throw new ResourceNotFoundReport(technologies).exception();
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
    public void toApi(cz.cesnet.shongo.controller.api.Specification specificationApi, Domain domain)
    {
        cz.cesnet.shongo.controller.api.LookupEndpointSpecification lookupEndpointSpecificationApi =
                (cz.cesnet.shongo.controller.api.LookupEndpointSpecification) specificationApi;
        if (technologies.size() == 1) {
            lookupEndpointSpecificationApi.setTechnology(technologies.iterator().next());
        }
        else {
            throw new TodoImplementException();
        }
        super.toApi(specificationApi, domain);
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.Specification specificationApi, EntityManager entityManager,
            Domain domain) throws FaultException
    {
        cz.cesnet.shongo.controller.api.LookupEndpointSpecification lookupEndpointSpecificationApi =
                (cz.cesnet.shongo.controller.api.LookupEndpointSpecification) specificationApi;
        if (lookupEndpointSpecificationApi.isPropertyFilled(lookupEndpointSpecificationApi.TECHNOLOGY)) {
            technologies.clear();
            addTechnology(lookupEndpointSpecificationApi.getTechnology());
        }
        super.fromApi(specificationApi, entityManager, domain);
    }

    @Override
    protected void fillDescriptionMap(Map<String, Object> map)
    {
        super.fillDescriptionMap(map);

        map.put("technologies", technologies);
    }
}
