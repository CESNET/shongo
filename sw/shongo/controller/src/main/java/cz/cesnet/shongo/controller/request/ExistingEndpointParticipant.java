package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.controller.common.AbstractParticipant;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.resource.DeviceResource;
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
public class ExistingEndpointParticipant extends EndpointParticipant implements ReservationTaskProvider
{
    /**
     * Specific resource.
     */
    private Resource resource;

    /**
     * Constructor.
     */
    public ExistingEndpointParticipant()
    {
    }

    /**
     * Constructor.
     *
     * @param resource sets the {@link #resource}
     */
    public ExistingEndpointParticipant(Resource resource)
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
    public boolean synchronizeFrom(AbstractParticipant participant)
    {
        ExistingEndpointParticipant existingEndpointParticipant = (ExistingEndpointParticipant) participant;

        boolean modified = super.synchronizeFrom(participant);
        modified |= !ObjectHelper.isSame(getResource(), existingEndpointParticipant.getResource());

        setResource(existingEndpointParticipant.getResource());

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
                if (!(resource instanceof DeviceResource) || !((DeviceResource) resource).isTerminal()) {
                    // Requested resource is not endpoint
                    throw new SchedulerReportSet.ResourceNotEndpointException(resource);
                }

                ResourceReservationTask reservationTask = new ResourceReservationTask(schedulerContext, resource);
                Reservation reservation = reservationTask.perform();
                addReports(reservationTask);
                return reservation;
            }
        };
    }

    @Override
    protected cz.cesnet.shongo.controller.api.AbstractParticipant createApi()
    {
        return new cz.cesnet.shongo.controller.api.ExistingEndpointParticipant();
    }

    @Override
    public void toApi(cz.cesnet.shongo.controller.api.AbstractParticipant participantApi)
    {
        cz.cesnet.shongo.controller.api.ExistingEndpointParticipant existingEndpointParticipantApi =
                (cz.cesnet.shongo.controller.api.ExistingEndpointParticipant) participantApi;
        existingEndpointParticipantApi.setResourceId(EntityIdentifier.formatId(resource));
        super.toApi(participantApi);
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.AbstractParticipant participantApi, EntityManager entityManager)
    {
        cz.cesnet.shongo.controller.api.ExistingEndpointParticipant existingEndpointParticipantApi =
                (cz.cesnet.shongo.controller.api.ExistingEndpointParticipant) participantApi;

        if (existingEndpointParticipantApi.getResourceId() == null) {
            setResource(null);
        }
        else {
            Long resourceId = EntityIdentifier.parseId(cz.cesnet.shongo.controller.resource.Resource.class,
                    existingEndpointParticipantApi.getResourceId());
            ResourceManager resourceManager = new ResourceManager(entityManager);
            setResource(resourceManager.get(resourceId));
        }

        super.fromApi(participantApi, entityManager);
    }
}
