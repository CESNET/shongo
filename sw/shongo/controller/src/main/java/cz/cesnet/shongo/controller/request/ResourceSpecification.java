package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.resource.Resource;
import cz.cesnet.shongo.controller.resource.ResourceManager;
import cz.cesnet.shongo.controller.scheduler.*;
import cz.cesnet.shongo.util.ObjectHelper;

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
        modified |= !ObjectHelper.isSame(getResource(), resourceSpecification.getResource());

        setResource(resourceSpecification.getResource());

        return modified;
    }

    @Override
    public ReservationTask createReservationTask(SchedulerContext schedulerContext)
    {
        return new ReservationTask(schedulerContext)
        {
            @Override
            protected Reservation allocateReservation() throws SchedulerException
            {
                ResourceReservationTask reservationTask = new ResourceReservationTask(schedulerContext, resource);
                Reservation reservation = reservationTask.perform();
                addReports(reservationTask);
                return reservation;
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
        resourceSpecificationApi.setResourceId(EntityIdentifier.formatId(resource));
        super.toApi(specificationApi);
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.Specification specificationApi, EntityManager entityManager)
    {
        cz.cesnet.shongo.controller.api.ResourceSpecification resourceSpecificationApi =
                (cz.cesnet.shongo.controller.api.ResourceSpecification) specificationApi;
        if (resourceSpecificationApi.isPropertyFilled(resourceSpecificationApi.RESOURCE_ID)) {
            if (resourceSpecificationApi.getResourceId() == null) {
                setResource(null);
            }
            else {
                Long resourceId = EntityIdentifier.parseId(cz.cesnet.shongo.controller.resource.Resource.class,
                        resourceSpecificationApi.getResourceId());
                ResourceManager resourceManager = new ResourceManager(entityManager);
                setResource(resourceManager.get(resourceId));
            }
        }

        super.fromApi(specificationApi, entityManager);
    }
}
