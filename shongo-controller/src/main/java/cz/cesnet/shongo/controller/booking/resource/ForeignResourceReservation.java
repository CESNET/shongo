package cz.cesnet.shongo.controller.booking.resource;

import cz.cesnet.shongo.controller.ObjectType;
import cz.cesnet.shongo.controller.api.ResourceReservation;
import cz.cesnet.shongo.controller.api.domains.response.*;
import cz.cesnet.shongo.controller.api.request.DomainCapabilityListRequest;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.domain.Domain;
import cz.cesnet.shongo.controller.booking.reservation.AbstractForeignReservation;
import cz.cesnet.shongo.controller.booking.reservation.TargetedReservation;

import javax.persistence.*;

/**
 * Represents a {@link cz.cesnet.shongo.controller.booking.reservation.Reservation} for a foreign {@link Resource}.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
@Entity
public class ForeignResourceReservation extends AbstractForeignReservation
{
    private ForeignResources foreignResources;

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
        if (!DomainCapability.Type.RESOURCE.equals(reservationApi.getType())) {
            throw new IllegalArgumentException("Reservation is of type resource.");
        }

        ResourceManager resourceManager = new ResourceManager(entityManager);
        cz.cesnet.shongo.controller.api.domains.response.ResourceSpecification resourceSpecification;
        resourceSpecification = (cz.cesnet.shongo.controller.api.domains.response.ResourceSpecification) reservationApi.getSpecification();
        ObjectIdentifier resourceId = ObjectIdentifier.parseForeignId(resourceSpecification.getForeignResourceId());
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
        ResourceReservation reservation = new ResourceReservation();
        super.toApi(reservation, entityManager, administrator);
        String resourceId = ObjectIdentifier.formatId(getDomain().getName(), ObjectType.RESOURCE, getForeignResources().getForeignResourceId());
        reservation.setResourceId(resourceId);
        reservation.setResourceName(resourceName);
        reservation.setResourceDescription(resourceDescription);

        return reservation;
    }
}
