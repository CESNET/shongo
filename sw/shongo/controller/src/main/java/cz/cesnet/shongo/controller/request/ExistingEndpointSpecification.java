package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.controller.report.ReportException;
import cz.cesnet.shongo.controller.reservation.EndpointReservation;
import cz.cesnet.shongo.controller.reservation.ResourceReservation;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.Resource;
import cz.cesnet.shongo.controller.scheduler.ReservationTask;
import cz.cesnet.shongo.controller.scheduler.ReservationTaskProvider;
import cz.cesnet.shongo.controller.scheduler.report.ResourceNotAllocatableReport;
import cz.cesnet.shongo.controller.scheduler.report.ResourceNotAvailableReport;
import cz.cesnet.shongo.controller.scheduler.report.ResourceNotEndpoint;
import cz.cesnet.shongo.controller.scheduler.report.ResourceRequestedMultipleTimesReport;
import cz.cesnet.shongo.fault.TodoImplementException;
import org.apache.commons.lang.ObjectUtils;

import javax.persistence.Entity;
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
        return new ReservationTask<ResourceReservation>(context){

            @Override
            protected ResourceReservation createReservation() throws ReportException
            {
                if (getCacheTransaction().containsResource(resource)) {
                    // Same resource is requested multiple times
                    throw new ResourceRequestedMultipleTimesReport(resource).exception();
                }
                if (!resource.isAllocatable()) {
                    // Requested resource cannot be allocated
                    throw new ResourceNotAllocatableReport(resource).exception();
                }
                if (!getCache().isResourceAvailable(resource, getInterval(), getCacheTransaction())) {
                    // Requested resource is not available in requested slot
                    throw new ResourceNotAvailableReport(resource).exception();
                }
                if (!(resource instanceof DeviceResource) || !((DeviceResource) resource).isTerminal()) {
                    throw new ResourceNotEndpoint(resource).exception();
                }

                EndpointReservation endpointReservation = new EndpointReservation();
                endpointReservation.setSlot(getInterval());
                endpointReservation.setResource(resource);
                return endpointReservation;
            }
        };
    }

    /*@Override
    public cz.cesnet.shongo.controller.api.ResourceSpecification toApi(Domain domain) throws FaultException
    {
        cz.cesnet.shongo.controller.api.ExistingResourceSpecification api =
                new cz.cesnet.shongo.controller.api.ExistingResourceSpecification();

        api.setResourceIdentifier(domain.formatIdentifier(resource.getId()));

        super.toApi(api);

        return api;
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.ResourceSpecification api, EntityManager entityManager,
            Domain domain) throws FaultException
    {
        cz.cesnet.shongo.controller.api.ExistingResourceSpecification apiDefiniteResource =
                (cz.cesnet.shongo.controller.api.ExistingResourceSpecification) api;
        if (apiDefiniteResource.isPropertyFilled(apiDefiniteResource.RESOURCE_IDENTIFIER)) {
            Long resourceId = domain.parseIdentifier(apiDefiniteResource.getResourceIdentifier());
            ResourceManager resourceManager = new ResourceManager(entityManager);
            setResource(resourceManager.get(resourceId));
        }
        super.fromApi(api, entityManager, domain);
    }*/
}
