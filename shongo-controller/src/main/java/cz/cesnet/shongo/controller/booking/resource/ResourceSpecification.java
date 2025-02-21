package cz.cesnet.shongo.controller.booking.resource;

import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.ForeignDomainConnectException;
import cz.cesnet.shongo.controller.ObjectType;
import cz.cesnet.shongo.controller.api.domains.response.AbstractResponse;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.domain.Domain;
import cz.cesnet.shongo.controller.booking.request.ReservationRequest;
import cz.cesnet.shongo.controller.booking.reservation.ReservationManager;
import cz.cesnet.shongo.controller.booking.specification.Specification;
import cz.cesnet.shongo.controller.booking.reservation.Reservation;
import cz.cesnet.shongo.controller.domains.InterDomainAgent;
import cz.cesnet.shongo.controller.scheduler.*;
import cz.cesnet.shongo.util.ObjectHelper;
import jakarta.persistence.ManyToOne;
import org.joda.time.Interval;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OneToOne;


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
     * Resource from foreign domain
     */
    private ForeignResources foreignResources;

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
     * Constructor.
     *
     * @param foreignResources sets the {@link #foreignResources}
     */
    public ResourceSpecification(ForeignResources foreignResources)
    {
        this.foreignResources = foreignResources;
    }

    /**
     * @return {@link #resource}
     */
    @ManyToOne(optional = false)
    public Resource getResource()
    {
        return resource;
    }

    /**
     * @param resource sets the {@link #resource}
     */
    public void setResource(Resource resource)
    {
        if (resource != null && foreignResources != null) {
            throw new CommonReportSet.ObjectInvalidException(getClass().getSimpleName(),
                    "Cannot set Resource because ForeignResources is allready set.");
        }

        this.resource = resource;
    }

    @OneToOne
    public ForeignResources getForeignResources()
    {
        return foreignResources;
    }

    public void setForeignResources(ForeignResources foreignResources)
    {
        if (foreignResources == null) {
            return;
        }
        if (resource != null) {
            throw new CommonReportSet.ObjectInvalidException(getClass().getSimpleName(),
                    "Cannot set ForeignResources because Resource is allready set.");
        }
        foreignResources.validateSingleResource();
        this.foreignResources = foreignResources;
    }

    @Override
    public boolean synchronizeFrom(Specification specification, EntityManager entityManager)
    {
        ResourceSpecification resourceSpecification = (ResourceSpecification) specification;

        boolean modified = super.synchronizeFrom(specification, entityManager);
        modified |= !ObjectHelper.isSamePersistent(getResource(), resourceSpecification.getResource());

        setResource(resourceSpecification.getResource());
        setForeignResources(resourceSpecification.getForeignResources());

        return modified;
    }

    @Override
    public ReservationTask createReservationTask(SchedulerContext schedulerContext, Interval slot)
            throws SchedulerException
    {
        if (resource != null) {
            return new ReservationTask(schedulerContext, slot)
            {
                @Override
                protected Reservation allocateReservation(Reservation currentReservation) throws SchedulerException
                {
                    ResourceReservationTask reservationTask = new ResourceReservationTask(schedulerContext, slot, resource);
                    Reservation reservation = reservationTask.perform();
                    addReports(reservationTask);
                    return reservation;
                }
            };
        }
        else if (foreignResources != null) {
            return new ReservationTask(schedulerContext, slot)
            {
                @Override
                protected Reservation allocateReservation(Reservation currentReservation) throws SchedulerException
                {
                    ForeignResourceReservation foreignResourceReservation;
                    schedulerContext.setRequestWantedState(ReservationRequest.AllocationState.COMPLETE);

                    cz.cesnet.shongo.controller.api.domains.response.Reservation foreignReservation;
                    String previousReservationRequestId = null;
                    boolean allocateNew = true;
                    if (currentReservation instanceof ForeignResourceReservation) {
                        ForeignResourceReservation previousReservation = (ForeignResourceReservation) currentReservation;
                        allocateNew = previousReservation.isComplete();
                        previousReservationRequestId = previousReservation.getForeignReservationRequestId();
                    }

                    if (allocateNew) {
                        foreignResourceReservation = new ForeignResourceReservation();
                        foreignResourceReservation.setDomain(foreignResources.getDomain());

                        try {
                            foreignReservation = InterDomainAgent.getInstance().getConnector().allocateResource(schedulerContext, slot, foreignResources, previousReservationRequestId);
                        }
                        catch (ForeignDomainConnectException e) {
                            throw new TodoImplementException("Parse returned exception");
                        }
                        foreignResourceReservation.setSlot(foreignReservation.getSlot());
                        foreignResourceReservation.setForeignResources(foreignResources);

                        cz.cesnet.shongo.controller.api.domains.response.ResourceSpecification resourceSpecification;
                        resourceSpecification = (cz.cesnet.shongo.controller.api.domains.response.ResourceSpecification) foreignReservation.getSpecification();

                        if (resourceSpecification != null) {
                            foreignResourceReservation.setResourceName(resourceSpecification.getResourceName());
                            foreignResourceReservation.setResourceDescription(resourceSpecification.getResourceDescription());
                        }

                        foreignResourceReservation.setForeignReservationRequestId(foreignReservation.getForeignReservationRequestId());
                        foreignResourceReservation.setCompletedByState(schedulerContext, foreignReservation);
                    } else {
                        foreignResourceReservation = (ForeignResourceReservation) currentReservation;

//                        ForeignResourceReservation previousReservation = (ForeignResourceReservation) currentReservation;
                        cz.cesnet.shongo.controller.api.Domain domain = foreignResourceReservation.getDomain().toApi();
                        String requestId = foreignResourceReservation.getForeignReservationRequestId();

                        try {
                            foreignReservation = InterDomainAgent.getInstance().getConnector().getReservationByRequest(domain, requestId);
                        }
                        catch (ForeignDomainConnectException e) {
                            //TODO process foreign exception
                            throw new SchedulerException(e);
                        }

                        cz.cesnet.shongo.controller.api.domains.response.ResourceSpecification resourceSpecification;
                        resourceSpecification = (cz.cesnet.shongo.controller.api.domains.response.ResourceSpecification) foreignReservation.getSpecification();

                        if (resourceSpecification != null) {
                            foreignResourceReservation.setResourceName(resourceSpecification.getResourceName());
                            foreignResourceReservation.setResourceDescription(resourceSpecification.getResourceDescription());
                        }

                        foreignResourceReservation.setCompletedByState(schedulerContext, foreignReservation);

                    }

                    return foreignResourceReservation;
                }
            };
        }
        else {
            throw new CommonReportSet.ObjectInvalidException(getClass().getSimpleName(),
                    "Cannot allocate because resource is not set.");
        }
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
        String resourceId = null;
        if (resource != null) {
            resourceId = ObjectIdentifier.formatId(resource);
        }
        if (foreignResources != null) {
            String domainName = foreignResources.getDomain().getName();
            resourceId = ObjectIdentifier.formatId(domainName, ObjectType.RESOURCE, foreignResources.getForeignResourceId());
        }
        resourceSpecificationApi.setResourceId(resourceId);

        super.toApi(specificationApi);
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.Specification specificationApi, EntityManager entityManager)
    {
        cz.cesnet.shongo.controller.api.ResourceSpecification resourceSpecificationApi =
                (cz.cesnet.shongo.controller.api.ResourceSpecification) specificationApi;

        if (resourceSpecificationApi.getResourceId() == null) {
            setResource(null);
        }
        else {
            ObjectIdentifier objectIdentifier = ObjectIdentifier.parseTypedId(resourceSpecificationApi.getResourceId(), ObjectType.RESOURCE);
            ResourceManager resourceManager = new ResourceManager(entityManager);
            if (objectIdentifier.isLocal()) {
                setResource(resourceManager.get(objectIdentifier.getPersistenceId()));
            }
            else {
                String domainName = objectIdentifier.getDomainName();
                Long resourceId = objectIdentifier.getPersistenceId();
                setForeignResources(resourceManager.findOrCreateForeignResources(domainName, resourceId));
            }
        }

        super.fromApi(specificationApi, entityManager);
    }
}
