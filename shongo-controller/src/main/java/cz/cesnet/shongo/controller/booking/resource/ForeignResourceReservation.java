package cz.cesnet.shongo.controller.booking.resource;

import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.ObjectType;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.ResourceReservation;
import cz.cesnet.shongo.controller.api.domains.response.Reservation;
import cz.cesnet.shongo.controller.api.request.DomainCapabilityListRequest;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.domain.Domain;
import cz.cesnet.shongo.controller.booking.reservation.TargetedReservation;
import cz.cesnet.shongo.controller.domains.DomainService;

import javax.persistence.*;

/**
 * Represents a {@link cz.cesnet.shongo.controller.booking.reservation.Reservation} for a foreign {@link Resource}.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
@Entity
public class ForeignResourceReservation extends TargetedReservation
{
    private ForeignResources foreignResources;

    private Domain domain;

    private String foreignReservationRequestId;

    private boolean complete;

    private String resourceName;

    private String resourceDescription;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "foreign_resources_id")
    public ForeignResources getForeignResources()
    {
        return foreignResources;
    }

    public void setForeignResources(ForeignResources foreignResources)
    {
        foreignResources.validateSingleResource();
        this.foreignResources = foreignResources;
    }

    @ManyToOne
    @JoinColumn(name = "domain_id")
    public Domain getDomain()
    {
        return domain;
    }

    public void setDomain(Domain domain)
    {
        this.domain = domain;
    }

    @Column
    public String getForeignReservationRequestId()
    {
        return foreignReservationRequestId;
    }

    public void setForeignReservationRequestId(String foreignReservationRequestId)
    {
        this.foreignReservationRequestId = foreignReservationRequestId;
    }

    @Column
    public String getResourceName()
    {
        return resourceName;
    }

    public void setResourceName(String resourceName)
    {
        this.resourceName = resourceName;
    }

    @Column
    public String getResourceDescription()
    {
        return resourceDescription;
    }

    public void setResourceDescription(String resourceDescription)
    {
        this.resourceDescription = resourceDescription;
    }

    @Column
    public boolean isComplete()
    {
        return complete;
    }

    public void setComplete(boolean complete)
    {
        this.complete = complete;
    }

    @Override
    @Transient
    public Long getTargetId()
    {
        return null;
    }

    public static ForeignResourceReservation createFromApi(Reservation reservationApi, EntityManager entityManager)
    {
        ForeignResourceReservation foreignResourceReservation = new ForeignResourceReservation();
        foreignResourceReservation.fromApi(reservationApi, entityManager);
        return foreignResourceReservation;
    }

    public void fromApi(Reservation reservationApi, EntityManager entityManager)
    {
        if (!DomainCapabilityListRequest.Type.RESOURCE.equals(reservationApi.getType())) {
            throw new IllegalArgumentException("Reservation is of type resource.");
        }

        ResourceManager resourceManager = new ResourceManager(entityManager);
        ObjectIdentifier resourceId = ObjectIdentifier.parseForeignId(reservationApi.getForeignResourceId());
        foreignResources = resourceManager.findForeignResourcesByResourceId(resourceId);
        if (foreignResources == null) {
            Domain domain = resourceManager.getDomainByName(resourceId.getDomainName());
            foreignResources = new ForeignResources();
            foreignResources.setForeignResourceId(resourceId.getPersistenceId());
            foreignResources.setDomain(domain);
        }
        setSlot(reservationApi.getSlot());
    }

    @Override
    public cz.cesnet.shongo.controller.api.ResourceReservation toApi(EntityManager entityManager, boolean administrator)
    {
        ResourceReservation reservation = (ResourceReservation) super.toApi(entityManager, administrator);
        String resourceId = ObjectIdentifier.formatId(getDomain().getName(), ObjectType.RESOURCE, getForeignResources().getForeignResourceId());
        reservation.setResourceId(resourceId);
        reservation.setResourceName(resourceName);
        reservation.setResourceDescription(resourceDescription);

        return reservation;
    }
}
