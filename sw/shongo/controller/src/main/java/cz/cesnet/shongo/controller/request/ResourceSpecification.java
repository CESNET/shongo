package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.report.ReportException;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.resource.Resource;
import cz.cesnet.shongo.controller.resource.ResourceManager;
import cz.cesnet.shongo.controller.scheduler.ReservationTask;
import cz.cesnet.shongo.controller.scheduler.ReservationTaskProvider;
import cz.cesnet.shongo.controller.scheduler.ResourceReservationTask;
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
public class ResourceSpecification extends Specification implements ReservationTaskProvider
{
    /**
     * Specific resource.
     */
    private Resource resource;

    /**
     * Constructor.
     */
    public ResourceSpecification()
    {
    }

    /**
     * Constructor.
     *
     * @param resource sets the {@link #resource}
     */
    public ResourceSpecification(Resource resource)
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
        ResourceSpecification resourceSpecification = (ResourceSpecification) specification;

        boolean modified = super.synchronizeFrom(specification);
        modified |= !ObjectUtils.equals(getResource(), resourceSpecification.getResource());

        setResource(resourceSpecification.getResource());

        return modified;
    }

    @Override
    public ReservationTask createReservationTask(ReservationTask.Context context)
    {
        return new ReservationTask(context)
        {
            @Override
            protected Reservation createReservation() throws ReportException
            {
                ResourceReservationTask resourceReservationTask = new ResourceReservationTask(getContext(), resource);
                return resourceReservationTask.perform();
            }
        };
    }

    @Override
    protected cz.cesnet.shongo.controller.api.Specification createApi()
    {
        return new cz.cesnet.shongo.controller.api.ResourceSpecification();
    }

    @Override
    public void toApi(cz.cesnet.shongo.controller.api.Specification specificationApi)
    {
        cz.cesnet.shongo.controller.api.ResourceSpecification resourceSpecificationApi =
                (cz.cesnet.shongo.controller.api.ResourceSpecification) specificationApi;
        resourceSpecificationApi.setResourceId(Domain.getLocalDomain().formatId(resource));
        super.toApi(specificationApi);
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.Specification specificationApi, EntityManager entityManager)
            throws FaultException
    {
        cz.cesnet.shongo.controller.api.ResourceSpecification resourceSpecificationApi =
                (cz.cesnet.shongo.controller.api.ResourceSpecification) specificationApi;
        if (resourceSpecificationApi.isPropertyFilled(resourceSpecificationApi.RESOURCE_ID)) {
            if (resourceSpecificationApi.getResourceId() == null) {
                setResource(null);
            }
            else {
                Long resourceId = Domain.getLocalDomain().parseId(resourceSpecificationApi.getResourceId());
                ResourceManager resourceManager = new ResourceManager(entityManager);
                setResource(resourceManager.get(resourceId));
            }
        }

        super.fromApi(specificationApi, entityManager);
    }
}
