package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.Resource;
import cz.cesnet.shongo.controller.resource.ResourceManager;
import cz.cesnet.shongo.controller.scheduler.*;
import org.apache.commons.lang.ObjectUtils;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.OneToOne;

/**
 * Represents a specific existing resource in the compartment.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class ExistingEndpointSpecification extends EndpointSpecification implements ReservationTaskProvider
{
    /**
     * Specific resource.
     */
    private Resource resource;

    /**
     * Constructor.
     */
    public ExistingEndpointSpecification()
    {
    }

    /**
     * Constructor.
     *
     * @param resource sets the {@link #resource}
     */
    public ExistingEndpointSpecification(Resource resource)
    {
        this.resource = resource;
    }

    /**
     * @return {@link #resource}
     */
    @OneToOne
    public Resource getResource()
    {
        return resource;
    }

    /**
     * @param resource sets the {@link #resource}
     */
    public void setResource(Resource resource)
    {
        this.resource = resource;
    }

    @Override
    public void updateTechnologies()
    {
        clearTechnologies();
        if (resource instanceof DeviceResource) {
            DeviceResource deviceResource = (DeviceResource) resource;
            addTechnologies(deviceResource.getTechnologies());
        }
    }

    @Override
    public boolean synchronizeFrom(Specification specification)
    {
        ExistingEndpointSpecification existingEndpointSpecification = (ExistingEndpointSpecification) specification;

        boolean modified = super.synchronizeFrom(specification);
        modified |= !ObjectUtils.equals(getResource(), existingEndpointSpecification.getResource());

        setResource(existingEndpointSpecification.getResource());

        return modified;
    }

    @Override
    public ReservationTask createReservationTask(SchedulerContext schedulerContext)
    {
        return new ReservationTask(schedulerContext)
        {
            @Override
            protected Reservation allocateReservation(Reservation allocatedReservation) throws SchedulerException
            {
                if (!(resource instanceof DeviceResource) || !((DeviceResource) resource).isTerminal()) {
                    // Requested resource is not endpoint
                    throw new SchedulerReportSet.ResourceNotEndpointException(resource);
                }

                ResourceReservationTask resourceReservationTask = new ResourceReservationTask(getSchedulerContext(), resource);
                Reservation reservation = resourceReservationTask.perform(null);
                addReports(resourceReservationTask);
                return reservation;
            }
        };
    }

    @Override
    protected cz.cesnet.shongo.controller.api.Specification createApi()
    {
        return new cz.cesnet.shongo.controller.api.ExistingEndpointSpecification();
    }

    @Override
    public void toApi(cz.cesnet.shongo.controller.api.Specification specificationApi)
    {
        cz.cesnet.shongo.controller.api.ExistingEndpointSpecification existingEndpointSpecificationApi =
                (cz.cesnet.shongo.controller.api.ExistingEndpointSpecification) specificationApi;
        existingEndpointSpecificationApi.setResourceId(EntityIdentifier.formatId(resource));
        super.toApi(specificationApi);
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.Specification specificationApi, EntityManager entityManager)
    {
        cz.cesnet.shongo.controller.api.ExistingEndpointSpecification existingEndpointSpecificationApi =
                (cz.cesnet.shongo.controller.api.ExistingEndpointSpecification) specificationApi;
        if (existingEndpointSpecificationApi.isPropertyFilled(existingEndpointSpecificationApi.RESOURCE_ID)) {
            if (existingEndpointSpecificationApi.getResourceId() == null) {
                setResource(null);
            }
            else {
                Long resourceId = EntityIdentifier.parseId(cz.cesnet.shongo.controller.resource.Resource.class,
                        existingEndpointSpecificationApi.getResourceId());
                ResourceManager resourceManager = new ResourceManager(entityManager);
                setResource(resourceManager.get(resourceId));
            }
        }

        super.fromApi(specificationApi, entityManager);
    }
}
