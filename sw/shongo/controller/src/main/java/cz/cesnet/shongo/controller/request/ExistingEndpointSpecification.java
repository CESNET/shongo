package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.report.ReportException;
import cz.cesnet.shongo.controller.reservation.EndpointReservation;
import cz.cesnet.shongo.controller.reservation.ResourceReservation;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.Resource;
import cz.cesnet.shongo.controller.resource.ResourceManager;
import cz.cesnet.shongo.controller.scheduler.ReservationTask;
import cz.cesnet.shongo.controller.scheduler.ReservationTaskProvider;
import cz.cesnet.shongo.controller.scheduler.ResourceReservationTask;
import cz.cesnet.shongo.controller.scheduler.report.ResourceNotAllocatableReport;
import cz.cesnet.shongo.controller.scheduler.report.ResourceNotAvailableReport;
import cz.cesnet.shongo.controller.scheduler.report.ResourceNotEndpoint;
import cz.cesnet.shongo.controller.scheduler.report.ResourceRequestedMultipleTimesReport;
import cz.cesnet.shongo.fault.FaultException;
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
    public boolean synchronizeFrom(Specification specification)
    {
        ExistingEndpointSpecification existingEndpointSpecification = (ExistingEndpointSpecification) specification;

        boolean modified = super.synchronizeFrom(specification);
        modified |= !ObjectUtils.equals(getResource(), existingEndpointSpecification.getResource());

        setResource(existingEndpointSpecification.getResource());

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
                if (!(resource instanceof DeviceResource) || !((DeviceResource) resource).isTerminal()) {
                    // Requested resource is not endpoint
                    throw new ResourceNotEndpoint(resource).exception();
                }

                ResourceReservationTask resourceReservationTask = new ResourceReservationTask(getContext(), resource);
                return resourceReservationTask.perform();
            }
        };
    }

    @Override
    protected cz.cesnet.shongo.controller.api.Specification createApi()
    {
        return new cz.cesnet.shongo.controller.api.ExistingEndpointSpecification();
    }

    @Override
    public void toApi(cz.cesnet.shongo.controller.api.Specification specificationApi, Domain domain)
    {
        cz.cesnet.shongo.controller.api.ExistingEndpointSpecification existingEndpointSpecificationApi =
                (cz.cesnet.shongo.controller.api.ExistingEndpointSpecification) specificationApi;
        existingEndpointSpecificationApi.setResourceIdentifier(domain.formatIdentifier(resource.getId()));
        super.toApi(specificationApi, domain);
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.Specification specificationApi, EntityManager entityManager,
            Domain domain) throws FaultException
    {
        cz.cesnet.shongo.controller.api.ExistingEndpointSpecification existingEndpointSpecificationApi =
                (cz.cesnet.shongo.controller.api.ExistingEndpointSpecification) specificationApi;
        if (existingEndpointSpecificationApi.isPropertyFilled(existingEndpointSpecificationApi.RESOURCE_IDENTIFIER)) {
            if (existingEndpointSpecificationApi.getResourceIdentifier() == null) {
                setResource(null);
            }
            else {
                Long resourceId = domain.parseIdentifier(existingEndpointSpecificationApi.getResourceIdentifier());
                ResourceManager resourceManager = new ResourceManager(entityManager);
                setResource(resourceManager.get(resourceId));
            }
        }
        super.fromApi(specificationApi, entityManager, domain);
    }
}
